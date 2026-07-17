package com.github.auties00.cobalt.calls.media.audio.neteq;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Tracks gaps in the received audio sequence number stream and produces the list of sequence numbers to
 * request a retransmission for.
 *
 * <p>On each {@link #updateLastReceived(int, long)} the tracker advances its high water sequence number;
 * any sequence numbers skipped between the previous high water mark and the new one are recorded as
 * missing, each stamped with the time it was first noticed. {@link #updateLastDecoded(int)} prunes every
 * entry older than the most recently decoded packet, since a packet already played past can no longer be
 * usefully retransmitted, and also bounds the list to {@link NetEqConfig#maxNackListSize()} by dropping
 * the oldest entries. {@link #nackList(long, long)} returns the sequence numbers still missing whose
 * notice time is old enough, relative to the supplied round trip time estimate, that a retransmission
 * could plausibly arrive before playout; the round trip time ceiling
 * {@link NetEqConfig#nackRttLimitMs()} suppresses the whole list on a link too slow for retransmission to
 * help.
 *
 * <p>All sequence number arithmetic uses {@link #isNewerSequenceNumber(int, int)}, the canonical 16 bit
 * wrap around comparison, so the tracker behaves correctly across the {@code 65535 -> 0} rollover.
 * Instances are not thread safe; the receive path drives one tracker from a single thread, guarded by the
 * {@link LiveNetEq} insert lock.
 */
public final class NackTracker {
    /**
     * The logger for {@link NackTracker}.
     */
    private static final System.Logger LOGGER = Log.get(NackTracker.class);

    /**
     * The number of distinct values a 16 bit sequence number takes, the wrap around modulus.
     */
    private static final int SEQUENCE_MODULUS = 1 << 16;

    /**
     * Half of {@link #SEQUENCE_MODULUS}, the threshold the wrap around comparison splits on.
     */
    private static final int SEQUENCE_HALF = SEQUENCE_MODULUS / 2;

    /**
     * Whether a packet still missing after its first NACK is requested again.
     *
     * <p>When {@code false}, each gap is requested at most once; when {@code true},
     * {@link #nackList(long, long)} reissues a request for a gap once the repeat interval has elapsed.
     *
     * @implNote This implementation pins the flag to {@code true}, the value WhatsApp's server pushes in the
     * voip settings, because {@link NetEqConfig} carries no corresponding field.
     */
    private static final boolean RENACK_ENABLED = true;

    /**
     * The minimum interval in milliseconds between successive NACK requests for the same sequence number.
     *
     * <p>A repeat request is gated so the same packet is not requested more often than this, regardless of
     * how short the round trip time is.
     *
     * @implNote This implementation pins the interval to {@code 150}, the value WhatsApp's server pushes in
     * the voip settings, because {@link NetEqConfig} carries no corresponding field.
     */
    private static final long RENACK_MIN_INTERVAL_MILLIS = 150L;

    /**
     * The multiplier applied to the round trip time estimate to space successive NACK requests.
     *
     * <p>The effective repeat interval is the larger of {@link #RENACK_MIN_INTERVAL_MILLIS} and the round
     * trip time scaled by this factor, so a slow link backs the repeat cadence off proportionally.
     *
     * @implNote This implementation pins the multiplier to {@code 2.0}, the value WhatsApp's server pushes
     * in the voip settings, because {@link NetEqConfig} carries no corresponding field.
     */
    private static final double RENACK_RTT_MULTIPLIER = 2.0;

    /**
     * The configuration bounding the list size and the round trip time NACK ceiling.
     */
    private final NetEqConfig config;

    /**
     * The missing sequence numbers, keyed by sequence number and valued by their tracking state.
     *
     * <p>Ordered by sequence number so pruning the oldest entries and reading the list in order are both
     * cheap; the comparator is plain integer order, which is correct within a single run that does not wrap
     * and is reconciled across a rollover by {@link #updateLastDecoded(int)} pruning past entries. Each
     * value carries when the gap was first noticed and when it was last requested, so the repeat gate can
     * space repeated requests.
     */
    private final SortedMap<Integer, MissingPacket> missing;

    /**
     * The highest sequence number received so far, or {@code -1} before the first packet.
     *
     * <p>Held in {@code 0..65535} once seeded; advanced by {@link #updateLastReceived(int, long)} using
     * the wrap around comparison.
     */
    private int lastReceivedSequence;

    /**
     * The sequence number of the most recently decoded packet, or {@code -1} before the first decode.
     *
     * <p>Entries at or before this value are pruned, since a packet already played cannot be usefully
     * retransmitted.
     */
    private int lastDecodedSequence;

    /**
     * Whether {@link #lastReceivedSequence} has been seeded by a first packet.
     */
    private boolean seeded;

    /**
     * Constructs a NACK tracker bound by the given configuration.
     *
     * @param config the configuration carrying the list size cap and the round trip time NACK ceiling;
     *               never {@code null}
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public NackTracker(NetEqConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.missing = new TreeMap<>();
        this.lastReceivedSequence = -1;
        this.lastDecodedSequence = -1;
        this.seeded = false;
    }

    /**
     * Records a received packet, advancing the high water mark and noting any newly skipped sequence
     * numbers as missing.
     *
     * <p>The first received packet seeds the high water mark without noting any gap. A later packet newer
     * than the high water mark notes every sequence number strictly between the old mark and the new one
     * as missing, each stamped with {@code nowMillis}, and clears any entry for the arrived sequence
     * number itself (a late arrival of a previously missing packet). A packet not newer than the
     * high water mark only clears its own entry, since it fills a gap rather than extending the stream.
     *
     * @param sequenceNumber the arrived packet's 16 bit sequence number, in {@code 0..65535}
     * @param nowMillis      the local monotonic time of arrival, in milliseconds
     */
    public void updateLastReceived(int sequenceNumber, long nowMillis) {
        var masked = sequenceNumber & MAX_MASK;
        if (!seeded) {
            seeded = true;
            lastReceivedSequence = masked;
            return;
        }
        missing.remove(masked);
        if (isNewerSequenceNumber(masked, lastReceivedSequence)) {
            var gap = (masked - lastReceivedSequence + SEQUENCE_MODULUS) % SEQUENCE_MODULUS;
            for (var offset = 1; offset < gap; offset++) {
                var missingSeq = (lastReceivedSequence + offset) % SEQUENCE_MODULUS;
                missing.putIfAbsent(missingSeq, new MissingPacket(nowMillis));
            }
            if (gap > 1 && Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "nack tracker: gap detected, missing={0} packets after seq={1}",
                        gap - 1, lastReceivedSequence);
            }
            lastReceivedSequence = masked;
        }
        enforceListSize();
    }

    /**
     * Records that a packet has been decoded and prunes entries no longer worth retransmitting.
     *
     * <p>Advances the most recently decoded mark and removes every missing entry at or before it, since a
     * packet already played past cannot help if retransmitted. The list is then rebounded to the
     * configured size.
     *
     * @param sequenceNumber the decoded packet's 16 bit sequence number, in {@code 0..65535}
     */
    public void updateLastDecoded(int sequenceNumber) {
        lastDecodedSequence = sequenceNumber & MAX_MASK;
        missing.keySet().removeIf(seq -> !isNewerSequenceNumber(seq, lastDecodedSequence));
        enforceListSize();
    }

    /**
     * Returns the sequence numbers to request a retransmission for, given the current time and the path
     * round trip time estimate, and records that they were requested now.
     *
     * <p>Returns an empty list when the round trip time estimate exceeds
     * {@link NetEqConfig#nackRttLimitMs()}, since retransmission could not arrive before playout on so
     * slow a link. Otherwise returns every missing sequence number that is due, capped at
     * {@link NetEqConfig#audioNackMaxSeqReq()} entries. A gap is due for its first request once its notice
     * is at least one round trip time old, optionally extended by the lost audio detection insert time when
     * {@link NetEqConfig#ladEnabledForNack()} is set. Once requested, the same gap is requested again only
     * after the repeat interval has elapsed since its previous request: the larger of
     * {@link #RENACK_MIN_INTERVAL_MILLIS} and the round trip time scaled by {@link #RENACK_RTT_MULTIPLIER}.
     * Repeat requests are gated by {@link #RENACK_ENABLED}; when disabled, each gap is requested at most
     * once. The returned sequence numbers are stamped with {@code nowMillis} as their last request time, so
     * a caller driving the tracker on a fixed cadence observes the repeat spacing; the returned list is a
     * fresh copy in ascending sequence order.
     *
     * @param nowMillis the current local monotonic time, in milliseconds
     * @param rttMillis the current path round trip time estimate, in milliseconds
     * @return the sequence numbers to NACK, ascending; empty when none are due or the link is too slow
     */
    public List<Integer> nackList(long nowMillis, long rttMillis) {
        if (rttMillis > config.nackRttLimitMs()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "nack tracker: suppressed, rtt={0}ms exceeds limit={1}ms",
                        rttMillis, config.nackRttLimitMs());
            }
            return List.of();
        }
        var initialThreshold = rttMillis;
        if (config.ladEnabledForNack()) {
            initialThreshold += config.ladNackExtraInsertTimeMs();
        }
        // TODO: model WhatsApp's exact repeat NACK scheduler and its proactive NACK predictor; this uses
        // TODO: the standard minimum interval plus round trip time multiplier spacing until they are recovered
        var renackInterval = Math.max(RENACK_MIN_INTERVAL_MILLIS, (long) (rttMillis * RENACK_RTT_MULTIPLIER));
        var due = new ArrayList<Integer>();
        for (var entry : missing.entrySet()) {
            var packet = entry.getValue();
            boolean dueNow;
            if (packet.lastNackMillis == 0L) {
                dueNow = nowMillis - packet.firstNoticedMillis >= initialThreshold;
            } else {
                dueNow = RENACK_ENABLED && nowMillis - packet.lastNackMillis >= renackInterval;
            }
            if (dueNow) {
                due.add(entry.getKey());
                packet.lastNackMillis = nowMillis;
                if (due.size() >= config.audioNackMaxSeqReq()) {
                    break;
                }
            }
        }
        if (!due.isEmpty() && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "nack tracker: requesting retransmit count={0}", due.size());
        }
        return due;
    }

    /**
     * Returns the current number of missing sequence numbers tracked.
     *
     * @return the size of the missing packet list
     */
    public int size() {
        return missing.size();
    }

    /**
     * Clears all tracked state, returning the tracker to its condition before any packet is seeded.
     *
     * <p>Used when the buffer is flushed or the stream is reconfigured, so stale gaps from before the
     * discontinuity are not requested.
     */
    public void reset() {
        missing.clear();
        lastReceivedSequence = -1;
        lastDecodedSequence = -1;
        seeded = false;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "nack tracker: reset");
    }

    /**
     * Drops the oldest missing entries until the list is no larger than the configured cap.
     *
     * <p>The {@link NetEqConfig#maxNackListSize()} bound is applied so the list cannot grow without bound on
     * a long burst of loss; the lowest sequence numbers, which are the oldest within a run that does not
     * wrap, are dropped first.
     */
    private void enforceListSize() {
        while (missing.size() > config.maxNackListSize()) {
            missing.remove(missing.firstKey());
        }
    }

    /**
     * The bit mask retaining the low 16 bits of a sequence number.
     */
    private static final int MAX_MASK = 0xFFFF;

    /**
     * Returns whether one 16 bit sequence number is newer than another under wrap around arithmetic.
     *
     * <p>Treats {@code sequence} as newer than {@code previous} when their forward distance is shorter than
     * their backward distance, the standard RTP test that handles the {@code 65535 -> 0} rollover:
     * {@code 0} is newer than {@code 65535}. Equal values are not newer.
     *
     * @param sequence the candidate sequence number, in {@code 0..65535}
     * @param previous the reference sequence number, in {@code 0..65535}
     * @return {@code true} if {@code sequence} is strictly newer than {@code previous}
     */
    public static boolean isNewerSequenceNumber(int sequence, int previous) {
        var a = sequence & MAX_MASK;
        var b = previous & MAX_MASK;
        if (a == b) {
            return false;
        }
        return ((a - b + SEQUENCE_MODULUS) % SEQUENCE_MODULUS) < SEQUENCE_HALF;
    }

    /**
     * The per gap tracking state held against each missing sequence number.
     *
     * <p>Records when the gap was first noticed and when it was last requested, the two timestamps the
     * repeat gate in {@link #nackList(long, long)} needs to space repeated requests.
     */
    private static final class MissingPacket {
        /**
         * The local monotonic time in milliseconds the gap was first noticed.
         *
         * <p>Used as the reference for the first request gate, which fires once this is at least one round
         * trip time old.
         */
        private final long firstNoticedMillis;

        /**
         * The local monotonic time in milliseconds the gap was last requested, or {@code 0} when never yet
         * requested.
         *
         * <p>Used as the reference for the repeat gate, which fires once this is at least the repeat
         * interval old.
         */
        private long lastNackMillis;

        /**
         * Constructs the tracking state for a newly noticed gap.
         *
         * @param firstNoticedMillis the local monotonic time the gap was first noticed, in milliseconds
         */
        private MissingPacket(long firstNoticedMillis) {
            this.firstNoticedMillis = firstNoticedMillis;
            this.lastNackMillis = 0L;
        }
    }
}
