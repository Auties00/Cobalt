package com.github.auties00.cobalt.calls.transport.rtcp;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Tracks gaps in one remote video stream's received RTP sequence numbers and produces the list of sequence
 * numbers to request a retransmission for.
 *
 * <p>One instance tracks a single remote video synchronization source. On each
 * {@link #recordReceived(int)} the tracker advances its highest received extended sequence number across the
 * sixteen bit rollover and records every sequence number skipped between the previous high water mark and
 * the new one as missing; a sequence number that arrives late is removed from the missing set, and a very
 * large forward jump is treated as a stream restart that clears the pending set rather than a loss of the
 * intervening packets. {@link #nackList(long, long)} returns the missing sequence numbers that are due,
 * subject to a reorder window that holds back a freshly noticed gap until the stream has advanced past it,
 * a repeat request gate that spaces further requests for the same packet, and a round trip time ceiling that
 * suppresses the whole list on a link too slow for retransmission to help. {@link #reset()} clears all
 * state, which the receive path invokes on a key frame so no retransmission is requested across the refresh
 * point.
 *
 * <p>All sequence number arithmetic is carried in an extended (rollover counter plus sixteen bit) space, so
 * the tracker behaves correctly across the {@code 65535 -> 0} wrap, and "oldest", "distance behind the
 * high water mark", and the gap enumeration are all plain {@code long} order. Instances are not thread safe;
 * one tracker is driven from the single transport receive thread that delivers the stream's media packets.
 *
 * @implNote This implementation is the receive side loss detector feeding the RFC 4585 generic NACK the
 * transport packs and ships; the wire form is produced by {@link RtcpReportBuilder#buildNack(int, int, int)}.
 * The rollover counter sequence bookkeeping reproduces the RFC 3550 {@code update_seq} validity window the
 * receiver statistics in {@link RtcpReceptionStats} also maintain. The reorder window, the bounded schedule
 * of one request plus a single repeat, and the list caps below follow the standard WebRTC video NACK
 * behaviour.
 */
public final class VideoNackTracker {
    /**
     * The logger for {@link VideoNackTracker}.
     */
    private static final System.Logger LOGGER = Log.get(VideoNackTracker.class);

    /**
     * The number of distinct values a sixteen bit RTP sequence number takes, the wrap around modulus.
     */
    private static final long SEQUENCE_MODULUS = 1L << 16;

    /**
     * Half the {@link #SEQUENCE_MODULUS}, the window an incoming sequence number is mapped into relative to
     * the high water mark when resolving the rollover.
     */
    private static final long SEQUENCE_HALF = SEQUENCE_MODULUS / 2;

    /**
     * The number of sequence positions the high water mark must advance past a missing sequence number
     * before it becomes eligible for its first NACK.
     *
     * <p>A missing sequence number is held back until the highest received sequence number is more than this
     * many positions beyond it, so a genuinely reordered packet has up to this many slots of slack to arrive
     * before a retransmission is requested.
     *
     * @implNote This implementation uses {@code 2}: a gap is requested only once at least three newer packets
     * have been seen past it, the small reorder tolerance the WebRTC video NACK module applies before
     * declaring a sequence number lost rather than merely reordered.
     */
    private static final long REORDER_TOLERANCE = 2;

    /**
     * The maximum number of times a single missing sequence number is requested.
     *
     * <p>A gap is requested once when first eligible and requested at most one further time, after the
     * repeat interval, before it is left to the jitter buffer to conceal.
     *
     * @implNote This implementation uses {@code 2} (one initial NACK plus one repeat): the video frame is
     * useful only briefly, so beyond a single retransmission attempt per packet a further request would
     * arrive too late to decode.
     */
    private static final int MAX_NACK_COUNT = 2;

    /**
     * The minimum interval in milliseconds between the initial NACK and the single repeat NACK of the same
     * sequence number.
     *
     * <p>The repeat NACK is spaced by the larger of this floor and the round trip time, so a near zero
     * round trip estimate does not request the same packet again before the first retransmission could have
     * arrived.
     *
     * @implNote This implementation uses {@code 20}: roughly one video frame interval, the floor below which
     * a repeat NACK cannot usefully precede the first retransmission's arrival.
     */
    private static final long RENACK_MIN_INTERVAL_MILLIS = 20L;

    /**
     * The multiplier applied to the round trip time estimate to space the repeat NACK from the initial NACK.
     *
     * <p>The effective repeat interval is the larger of {@link #RENACK_MIN_INTERVAL_MILLIS} and the
     * round trip time scaled by this factor, so the first retransmission is given roughly a full round trip
     * to arrive before the packet is requested again.
     *
     * @implNote This implementation uses {@code 1.0}: one round trip time, the interval after which the
     * absence of the first retransmission justifies a single repeat request.
     */
    private static final double RENACK_RTT_MULTIPLIER = 1.0;

    /**
     * The round trip time ceiling in milliseconds above which the whole NACK list is suppressed.
     *
     * <p>On a link slower than this a retransmission could not arrive before the frame's decode deadline, so
     * no request is worth sending.
     *
     * @implNote This implementation uses {@code 500}, the same round trip ceiling the audio jitter buffer
     * applies so the two media planes share one too slow to help bound.
     */
    private static final long VIDEO_NACK_RTT_LIMIT_MS = 500L;

    /**
     * The maximum distance, in sequence positions, a missing sequence number may fall behind the high water
     * mark before it is dropped from the tracked set.
     *
     * <p>A gap further behind the highest received packet than this is past any useful retransmission
     * deadline, so it is discarded rather than requested.
     *
     * @implNote This implementation uses {@code 1000}: at the ninety thousand hertz video clock and typical
     * frame rates this spans well beyond the buffering a retransmission could still feed, bounding the
     * tracked window on a long burst of loss.
     */
    private static final long MAX_NACK_DISTANCE = 1000L;

    /**
     * The hard cap on the number of missing sequence numbers tracked at once.
     *
     * <p>On a burst of loss wider than this the oldest entries are dropped first, so the tracked set cannot
     * grow without bound.
     *
     * @implNote This implementation uses {@code 256}, the standard WebRTC video NACK list bound.
     */
    private static final int MAX_NACK_LIST_SIZE = 256;

    /**
     * The maximum number of sequence numbers returned by a single {@link #nackList(long, long)} call.
     *
     * <p>One emit carries at most this many requests; the remaining due sequence numbers are returned on the
     * following calls, so a single NACK round never grows unbounded.
     *
     * @implNote This implementation uses {@code 64}: a span the transport packs into a small run of generic
     * NACK records without flooding the return path.
     */
    private static final int MAX_SEQUENCES_PER_REQUEST = 64;

    /**
     * The forward sequence number jump beyond which a packet is treated as a stream restart rather than a
     * run of lost packets.
     *
     * <p>A jump this far ahead of the high water mark is a discontinuity (a source restart or a resynchronized
     * stream); the intervening sequence numbers are not enqueued as missing, since they were never sent.
     *
     * @implNote This implementation uses {@code 3000}, the RFC 3550 {@code MAX_DROPOUT} default
     * {@link RtcpReceptionStats} also applies when distinguishing a large gap from an in order advance.
     */
    private static final long DISCONTINUITY_THRESHOLD = 3000L;

    /**
     * The missing sequence numbers, keyed by extended (rollover extended) sequence number and valued by
     * their request state, in ascending order so the oldest entries are first.
     */
    private final SortedMap<Long, MissingPacket> missing = new TreeMap<>();

    /**
     * The highest received extended sequence number, valid only once {@link #seeded} is set.
     */
    private long highestExtended;

    /**
     * Whether {@link #highestExtended} has been seeded by a first received packet.
     */
    private boolean seeded;

    /**
     * Records a received packet, advancing the high water mark and noting any newly skipped sequence numbers
     * as missing.
     *
     * <p>The first packet seeds the high water mark without noting a gap. A later packet newer than the mark
     * records every sequence number strictly between the old mark and the new one as missing and advances the
     * mark; a forward jump beyond {@link #DISCONTINUITY_THRESHOLD} is treated as a stream restart that clears
     * the pending set and reseeds the mark. A packet at or behind the mark removes its own missing entry, the
     * late arrival of a previously skipped packet, and does not advance the mark. After an advance the tracked
     * set is pruned of entries past the retransmission deadline and bounded to its cap.
     *
     * @param sequenceNumber the arrived packet's sixteen bit RTP sequence number
     */
    public void recordReceived(int sequenceNumber) {
        var masked = sequenceNumber & 0xFFFF;
        if (!seeded) {
            seeded = true;
            highestExtended = masked;
            return;
        }
        var delta = masked - (highestExtended & 0xFFFF);
        var candidate = highestExtended + delta;
        if (delta > SEQUENCE_HALF) {
            candidate -= SEQUENCE_MODULUS;
        } else if (delta < -SEQUENCE_HALF) {
            candidate += SEQUENCE_MODULUS;
        }
        var forward = candidate - highestExtended;
        if (forward == 0) {
            return;
        }
        if (forward < 0) {
            missing.remove(candidate);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "video nack tracker late arrival, sequence={0}", sequenceNumber);
            }
            return;
        }
        if (forward > DISCONTINUITY_THRESHOLD) {
            missing.clear();
            highestExtended = masked;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "video nack tracker stream restart detected, forward={0}", forward);
            }
            return;
        }
        for (var extended = highestExtended + 1; extended < candidate; extended++) {
            missing.putIfAbsent(extended, new MissingPacket());
        }
        highestExtended = candidate;
        pruneAndBound();
    }

    /**
     * Returns the sequence numbers to request a retransmission for, given the current time and the path
     * round trip time estimate, and records that they were requested now.
     *
     * <p>Returns an empty list when the round trip time estimate exceeds {@link #VIDEO_NACK_RTT_LIMIT_MS},
     * since a retransmission could not arrive in time on so slow a link. Otherwise returns every due missing
     * sequence number, ascending, capped at {@link #MAX_SEQUENCES_PER_REQUEST} entries. A gap is withheld
     * until the high water mark is more than {@link #REORDER_TOLERANCE} positions past it, so a reordered
     * packet is not requested prematurely. A gap past the reorder window is due for its first request
     * immediately; once requested it is requested again at most once, and only after the repeat interval has
     * elapsed: the larger of {@link #RENACK_MIN_INTERVAL_MILLIS} and the round trip time scaled by
     * {@link #RENACK_RTT_MULTIPLIER}. Beyond {@link #MAX_NACK_COUNT} requests a gap is left to the jitter
     * buffer to conceal. Returned sequence numbers are stamped with {@code nowMillis} as their last request
     * time, so a caller driving the tracker on a fixed cadence observes the repeat spacing; the returned
     * list is a fresh copy.
     *
     * @param nowMillis the current local monotonic time, in milliseconds
     * @param rttMillis the current path round trip time estimate, in milliseconds
     * @return the sixteen bit sequence numbers to NACK, ascending; empty when none are due or the link is too
     *         slow
     */
    public List<Integer> nackList(long nowMillis, long rttMillis) {
        if (rttMillis > VIDEO_NACK_RTT_LIMIT_MS) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "video nack list suppressed, rttMillis={0} limit={1}",
                        rttMillis, VIDEO_NACK_RTT_LIMIT_MS);
            }
            return List.of();
        }
        var renackInterval = Math.max(RENACK_MIN_INTERVAL_MILLIS, (long) (rttMillis * RENACK_RTT_MULTIPLIER));
        var due = new ArrayList<Integer>();
        for (var entry : missing.entrySet()) {
            var extended = entry.getKey();
            if (highestExtended - extended <= REORDER_TOLERANCE) {
                continue;
            }
            var packet = entry.getValue();
            boolean dueNow;
            if (packet.nackCount == 0) {
                dueNow = true;
            } else if (packet.nackCount < MAX_NACK_COUNT) {
                dueNow = nowMillis - packet.lastNackMillis >= renackInterval;
            } else {
                dueNow = false;
            }
            if (dueNow) {
                due.add((int) (extended & 0xFFFF));
                packet.lastNackMillis = nowMillis;
                packet.nackCount++;
                if (due.size() >= MAX_SEQUENCES_PER_REQUEST) {
                    break;
                }
            }
        }
        if (!due.isEmpty() && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "video nack list due, count={0} tracked={1}", due.size(), missing.size());
        }
        return due;
    }

    /**
     * Returns the current number of missing sequence numbers tracked.
     *
     * @return the size of the missing packet set
     */
    public int size() {
        return missing.size();
    }

    /**
     * Clears all tracked state, returning the tracker to its condition before the first packet is seen.
     *
     * <p>Invoked on a key frame or a stream reconfiguration so stale gaps from before the refresh point are
     * not requested across it.
     */
    public void reset() {
        missing.clear();
        highestExtended = 0;
        seeded = false;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "video nack tracker reset");
        }
    }

    /**
     * Drops missing entries past the retransmission deadline and bounds the tracked set to its cap.
     *
     * <p>Entries more than {@link #MAX_NACK_DISTANCE} positions behind the high water mark are too late to
     * retransmit and are removed; if the set still exceeds {@link #MAX_NACK_LIST_SIZE}, the oldest entries
     * (the lowest extended sequence numbers) are dropped until it fits.
     */
    private void pruneAndBound() {
        var cutoff = highestExtended - MAX_NACK_DISTANCE;
        if (!missing.isEmpty() && missing.firstKey() < cutoff) {
            missing.headMap(cutoff).clear();
        }
        if (missing.size() > MAX_NACK_LIST_SIZE && Log.WARNING) {
            LOGGER.log(Level.WARNING, "video nack tracker list overflow, size={0} cap={1}",
                    missing.size(), MAX_NACK_LIST_SIZE);
        }
        while (missing.size() > MAX_NACK_LIST_SIZE) {
            missing.remove(missing.firstKey());
        }
    }

    /**
     * The per gap request state held against each missing sequence number.
     *
     * <p>Records how many times the gap has been requested and when it was last requested, the two values the
     * repeat request gate in {@link #nackList(long, long)} needs to space and cap further requests.
     */
    private static final class MissingPacket {
        /**
         * The local monotonic time in milliseconds the gap was last requested, or {@code 0} when never yet
         * requested.
         */
        private long lastNackMillis;

        /**
         * The number of times the gap has been requested, {@code 0} until its first NACK.
         */
        private int nackCount;

        /**
         * Constructs the request state for a newly noticed gap, never yet requested.
         */
        private MissingPacket() {
            this.lastNackMillis = 0L;
            this.nackCount = 0;
        }
    }
}
