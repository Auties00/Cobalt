package com.github.auties00.cobalt.calls.media.audio.neteq;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Stores received audio packets in sequence order, bounded by a capacity, so the decision logic can peek
 * at and extract the next packet due for playout.
 *
 * <p>Packets are keyed by 16 bit sequence number and held until they are extracted for decoding or
 * discarded as too late. {@link #insert(RtpAudioPacket)} adds a packet, discarding it as a duplicate if
 * its sequence number is already buffered or as too late if it precedes the playout cursor, and flushing
 * the buffer if it has grown past the configured length; {@link #peekNext()} returns the lowest sequence
 * packet without removing it; {@link #extractNext()} removes and returns it, advancing the playout cursor;
 * {@link #nextSequenceContiguous()} reports whether the lowest buffered packet is the one the cursor
 * expects next, which distinguishes a normal decode from a gap that calls for concealment.
 * {@link #spanMillis(int)} reports the buffered span as a playout duration so the decision logic can
 * compare it against the target delay.
 *
 * <p>The smart flush drains a buffer that has grown grossly over the configured maximum length, a recovery
 * from a stall the decision logic could not otherwise unwind; an ordinary insert never flushes unless the
 * buffer exceeds the bound. Instances are not safe for concurrent use; insert and extract are serialized by
 * the {@link LiveNetEq} locks.
 */
public final class PacketBuffer {
    /**
     * The logger for {@link PacketBuffer}.
     */
    private static final System.Logger LOGGER = Log.get(PacketBuffer.class);

    /**
     * The upper bound on a single packet's sample span, in samples, used to reject a corrupt RTP timestamp
     * delta when measuring the packet spacing.
     *
     * <p>Fixed at one second of the 16 kHz call audio clock; the longest packet WhatsApp emits is the 60 ms
     * MLow frame ({@code 960} samples), so a measured delta above this bound is a malformed timestamp rather
     * than a real frame and is ignored by {@link #observeSpacing(RtpAudioPacket)}.
     */
    private static final long MAX_SAMPLES_PER_PACKET = LiveNetEq.SAMPLE_RATE_HZ;

    /**
     * The configuration carrying the capacity bound and the smart flush length.
     */
    private final NetEqConfig config;

    /**
     * The buffered packets, keyed by sequence number in ascending order.
     *
     * <p>The lowest key is the next packet due for playout; ordering is plain integer order, correct within
     * a run that does not wrap and reconciled across a rollover by the playout cursor discarding older
     * packets on insert.
     */
    private final NavigableMap<Integer, RtpAudioPacket> packets;

    /**
     * The sequence number last extracted for playout, or {@code -1} before the first extract.
     *
     * <p>An inserted packet not newer than this cursor is discarded as too late; the cursor advances to each
     * extracted packet's sequence number.
     */
    private int playoutCursor;

    /**
     * The most recently observed sample count one packet spans, derived from the RTP timestamp delta between
     * two packets whose sequence numbers differ by one, or {@code 0} before any such pair has been seen.
     *
     * <p>Updated on {@link #insert(RtpAudioPacket)} whenever the inserted packet's sequence number is exactly
     * one past, or one before, an already buffered packet: the absolute RTP timestamp difference between the
     * two is the sample count one packet spans. For the 20 ms Opus stream the difference is the 320 sample
     * frame; for the 60 ms MLow stream it is the 960 sample frame. Surfaced through
     * {@link #approximateSamplesPerPacket()}, which falls back to the 20 ms get period sample count while
     * this is zero so the span accounting never scales by a bogus value when too few packets have arrived to
     * measure the spacing.
     */
    private int lastObservedSamplesPerPacket;

    /**
     * The running count of packets discarded as late or duplicate.
     *
     * <p>Surfaced through {@link #packetsDiscarded()} into the jitter buffer statistics.
     */
    private long packetsDiscarded;

    /**
     * The running count of buffer flushes performed for over buffering.
     *
     * <p>Surfaced through {@link #bufferFlushes()} into the jitter buffer statistics.
     */
    private long bufferFlushes;

    /**
     * Constructs an empty packet buffer bound by the given configuration.
     *
     * @param config the configuration carrying the capacity and smart flush length; never {@code null}
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public PacketBuffer(NetEqConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.packets = new TreeMap<>();
        this.playoutCursor = -1;
        this.packetsDiscarded = 0;
        this.bufferFlushes = 0;
        this.lastObservedSamplesPerPacket = 0;
    }

    /**
     * Inserts a packet, discarding it if duplicate or late and flushing if the buffer is over capacity.
     *
     * <p>A packet whose sequence number is already buffered, or whose sequence number is not newer than the
     * playout cursor, is discarded and counted. Otherwise the packet is stored. After storing, the buffer is
     * flushed if it exceeds the capacity bound or, when smart flush is enabled, if its span exceeds the smart
     * flush length; a flush keeps only the newest packet so playout can resume promptly.
     *
     * @param packet the packet to insert; never {@code null}
     * @return {@code true} if the packet was stored, {@code false} if it was discarded
     * @throws NullPointerException if {@code packet} is {@code null}
     */
    public boolean insert(RtpAudioPacket packet) {
        Objects.requireNonNull(packet, "packet cannot be null");
        var seq = packet.sequenceNumber();
        if (playoutCursor >= 0 && !NackTracker.isNewerSequenceNumber(seq, playoutCursor)) {
            packetsDiscarded++;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "packet buffer: discarding late packet, seq={0} cursor={1}", seq, playoutCursor);
            }
            return false;
        }
        if (packets.putIfAbsent(seq, packet) != null) {
            packetsDiscarded++;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "packet buffer: discarding duplicate packet, seq={0}", seq);
            return false;
        }
        observeSpacing(packet);
        if (packets.size() > config.maxPacketsInBuffer()) {
            flushKeepingNewest();
        } else if (config.smartBufferFlushEnabled()
                && spanMillis(approximateSamplesPerPacket()) > config.bufferFlushMaxLengthMs()) {
            flushKeepingNewest();
        }
        return true;
    }

    /**
     * Records the sample count one packet spans from the RTP timestamp delta to an adjacent buffered packet.
     *
     * <p>When the just inserted packet's sequence number is exactly one past or one before an already
     * buffered packet, the absolute difference of their RTP timestamps is the number of samples one packet
     * spans, the count {@link #approximateSamplesPerPacket()} reports. A delta of zero or larger than a
     * generous bound is ignored as malformed, so a corrupt timestamp cannot poison the estimate.
     *
     * @param packet the packet just inserted; never {@code null}
     */
    private void observeSpacing(RtpAudioPacket packet) {
        var seq = packet.sequenceNumber();
        var neighbour = packets.get((seq + 1) & RtpAudioPacket.MAX_SEQUENCE_NUMBER);
        if (neighbour == null) {
            neighbour = packets.get((seq - 1) & RtpAudioPacket.MAX_SEQUENCE_NUMBER);
        }
        if (neighbour == null) {
            return;
        }
        var delta = Math.abs(packet.timestamp() - neighbour.timestamp());
        if (delta > 0 && delta <= MAX_SAMPLES_PER_PACKET) {
            lastObservedSamplesPerPacket = (int) delta;
        }
    }

    /**
     * Returns the next packet due for playout without removing it.
     *
     * @return the lowest sequence buffered packet, or {@code null} if the buffer is empty
     */
    public RtpAudioPacket peekNext() {
        var entry = packets.firstEntry();
        return entry == null ? null : entry.getValue();
    }

    /**
     * Removes and returns the next packet due for playout, advancing the playout cursor.
     *
     * @return the lowest sequence buffered packet, or {@code null} if the buffer is empty
     */
    public RtpAudioPacket extractNext() {
        var entry = packets.pollFirstEntry();
        if (entry == null) {
            return null;
        }
        playoutCursor = entry.getKey();
        return entry.getValue();
    }

    /**
     * Returns whether the next buffered packet is the one the playout cursor expects next in sequence.
     *
     * <p>True when the buffer is not empty and the lowest buffered sequence number is exactly one past the
     * playout cursor, or when nothing has been extracted yet; false when a gap separates the cursor from the
     * next buffered packet, which signals the decision logic to conceal rather than decode.
     *
     * @return {@code true} if the next packet continues the sequence without a gap
     */
    public boolean nextSequenceContiguous() {
        if (packets.isEmpty()) {
            return false;
        }
        if (playoutCursor < 0) {
            return true;
        }
        var next = packets.firstKey();
        return next == ((playoutCursor + 1) & RtpAudioPacket.MAX_SEQUENCE_NUMBER);
    }

    /**
     * Returns the buffered span as a playout duration in milliseconds for the given samples per packet.
     *
     * <p>Computed as the buffered packet count times the duration one packet spans, which is the sample count
     * one packet spans over the 16 kHz sample rate; the decision logic compares this against the target
     * delay.
     *
     * @param samplesPerPacket the per channel sample count one packet decodes to
     * @return the buffered span in milliseconds
     */
    public int spanMillis(int samplesPerPacket) {
        if (samplesPerPacket <= 0) {
            return 0;
        }
        var perPacketMs = samplesPerPacket * 1000 / LiveNetEq.SAMPLE_RATE_HZ;
        return packets.size() * perPacketMs;
    }

    /**
     * Returns the number of packets currently buffered.
     *
     * @return the buffered packet count
     */
    public int size() {
        return packets.size();
    }

    /**
     * Returns whether the buffer holds no packets.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return packets.isEmpty();
    }

    /**
     * Returns the lifetime count of packets discarded as late or duplicate.
     *
     * @return the discarded packet count
     */
    public long packetsDiscarded() {
        return packetsDiscarded;
    }

    /**
     * Returns the lifetime count of buffer flushes performed for over buffering.
     *
     * @return the buffer flush count
     */
    public long bufferFlushes() {
        return bufferFlushes;
    }

    /**
     * Empties the buffer and resets the playout cursor to its initial state before any playout.
     *
     * <p>Used when the stream is reconfigured; unlike {@link #flushKeepingNewest()} this retains no packet
     * and does not count as an over buffering flush.
     */
    public void clear() {
        packets.clear();
        playoutCursor = -1;
        lastObservedSamplesPerPacket = 0;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "packet buffer cleared");
    }

    /**
     * Drops every buffered packet except the newest, counting the operation as a flush.
     *
     * <p>The recovery from gross over buffering: keeping only the highest sequence packet collapses the
     * backlog to a single frame so the decision logic can resume near the target level without rendering a
     * long run of stale audio.
     */
    private void flushKeepingNewest() {
        if (packets.isEmpty()) {
            return;
        }
        var newestKey = packets.lastKey();
        var newest = packets.get(newestKey);
        var droppedCount = packets.size() - 1;
        packets.clear();
        packets.put(newestKey, newest);
        bufferFlushes++;
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "packet buffer: flushed for overbuffering, dropped={0} kept seq={1}",
                    droppedCount, newestKey);
        }
    }

    /**
     * Returns the sample count one packet spans, measured from the buffered packets' RTP timestamp spacing.
     *
     * <p>The RTP timestamp delta between two packets whose sequence numbers differ by one is the number of
     * samples one packet spans: {@code 320} for the 20 ms Opus stream and {@code 960} for the 60 ms MLow
     * stream, both at 16 kHz mono. The most recent such measurement is reported. Before two adjacent packets
     * have ever been buffered the spacing cannot be measured, so the 20 ms get period sample count is
     * returned as a safe default; this keeps the span accounting from scaling by zero on a nearly empty
     * buffer and makes the result identical to {@link LiveNetEq#frameSamples() the frame size} for a 20 ms
     * stream, whose measured spacing is itself the 320 sample frame.
     *
     * @return the sample count one packet spans, never below the 20 ms get period sample count fallback
     * @implNote This implementation measures the spacing from the wire RTP timestamps rather than assuming a
     * fixed packet duration, because WhatsApp runs two packet durations over the same 20 ms NetEq get period:
     * Opus at 20 ms per packet and MLow at 60 ms per packet. The RTP timestamp advances by exactly the
     * decoded frame's sample count, so reading the timestamp delta recovers the duration one packet spans
     * without decoding the payload.
     */
    public int approximateSamplesPerPacket() {
        if (lastObservedSamplesPerPacket > 0) {
            return lastObservedSamplesPerPacket;
        }
        return LiveNetEq.SAMPLE_RATE_HZ * config.getPeriodMs() / 1000;
    }
}
