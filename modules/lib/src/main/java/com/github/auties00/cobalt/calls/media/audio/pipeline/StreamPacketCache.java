package com.github.auties00.cobalt.calls.media.audio.pipeline;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A bounded ring of recently sent audio packets keyed by RTP extended sequence number, from which the
 * sender draws the windows the three redundancy schemes need.
 *
 * <p>After an audio packet is built and handed to the transport, its bytes are retained here so they can
 * be reused without encoding them again: a hop by hop NACK can replay a packet the relay missed, an out of
 * band forward error correction stream can xor a span of recent payloads, and an RFC 2198 MLow redundancy
 * block can prepend copies of the most recent payloads ahead of a fresh one. The cache is a fixed size
 * ring: each {@linkplain #store(long, byte[], boolean) store} overwrites the oldest slot once the ring
 * is full, so it always holds at most its capacity of the newest packets and a packet ages out silently
 * rather than growing the cache without bound.
 *
 * <p>The key is the 32 bit RTP extended sequence number (the 16 bit RTP sequence widened by its rollover
 * count), which increases monotonically across a stream, so ordering and span queries compare keys
 * directly. Three acquire windows expose the retained packets: {@link #acquireUnsent()} returns the
 * packets still flagged unsent in ascending order for the initial transmit drain;
 * {@link #acquireOpusFecRange(long, int)} returns up to a bounded number of packets at or below a target
 * sequence in ascending order for building an out of band FEC packet; and
 * {@link #acquireMlowRedRange(long, int)} returns recent packets in descending (newest first) order for
 * prepending RFC 2198 redundancy blocks. Each window copies into a fresh list of references to the
 * stored payloads; callers must not mutate a returned payload array, since the cache still owns it.
 *
 * <p>The cache is single threaded: the one audio encode and send path stores packets and immediately
 * draws its redundancy windows on the same thread, so no internal locking is needed. The ring holds
 * variable length payload arrays directly rather than fixed width entry buffers, and the per entry byte
 * cap together with the exact MLow redundancy stop math (the cumulative size versus stream MTU and the
 * extended sequence and time offset bounds) lives in the redundancy packers that consume these windows,
 * not in the cache itself.
 */
public final class StreamPacketCache {
    /**
     * The logger for {@link StreamPacketCache}.
     */
    private static final System.Logger LOGGER = Log.get(StreamPacketCache.class);

    /**
     * One retained packet in the cache: its key, bytes, and whether it has been transmitted.
     *
     * <p>The {@link #payload()} is the exact bytes handed to the transport, held by reference so a
     * redundancy scheme replays them without a copy; a consumer must not mutate the array. The
     * {@link #sent()} flag distinguishes a packet already on the wire from one still queued for the
     * initial transmit, which the {@linkplain #acquireUnsent() unsent window} drains.
     *
     * @param extendedSequence the 32 bit RTP extended sequence number keying this packet
     * @param payload          the retained packet bytes; never {@code null}, owned by the cache
     * @param sent             whether the packet has been transmitted at least once
     */
    public record CachedPacket(long extendedSequence, byte[] payload, boolean sent) {
        /**
         * Validates the cached packet, rejecting a {@code null} payload.
         *
         * @throws NullPointerException if {@code payload} is {@code null}
         */
        public CachedPacket {
            Objects.requireNonNull(payload, "payload cannot be null");
        }
    }

    /**
     * Number of packet slots in the ring.
     *
     * <p>Fixed at construction; once this many packets are stored, each further store overwrites the
     * oldest, so the cache holds at most this many of the newest packets.
     */
    private final int capacity;

    /**
     * Backing slots holding the retained packets in store order.
     *
     * <p>Indexed by {@code count % capacity} while filling and thereafter as a ring; a {@code null} slot
     * is one not yet written. The newest packet is at the slot just before the next write position.
     */
    private final CachedPacket[] slots;

    /**
     * Total number of packets stored since construction.
     *
     * <p>Used to compute the next write slot and to distinguish the filling phase (fewer stores than the
     * capacity) from the steady state ring; only its value modulo the capacity selects a slot.
     */
    private long stored;

    /**
     * Constructs an empty packet cache holding the given number of packets.
     *
     * @param capacity the number of packet slots; must be positive
     * @throws IllegalArgumentException if {@code capacity} is not positive
     */
    public StreamPacketCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.slots = new CachedPacket[capacity];
        this.stored = 0;
    }

    /**
     * Returns the number of packet slots in the ring.
     *
     * @return the fixed capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the number of packets currently retained.
     *
     * <p>Grows with each store until the ring fills, then stays at the capacity as older packets are
     * overwritten.
     *
     * @return the count of retained packets, in {@code [0, capacity]}
     */
    public int size() {
        return (int) Math.min(stored, capacity);
    }

    /**
     * Stores one sent packet into the cache, overwriting the oldest when full.
     *
     * <p>The packet is copied by reference into the next ring slot and the store count advances; once the
     * ring is full this discards the oldest retained packet. The {@code payload} array is retained as
     * supplied and must not be mutated afterward, since the redundancy windows replay it directly.
     *
     * @param extendedSequence the 32 bit RTP extended sequence number keying the packet
     * @param payload          the packet bytes to retain; never {@code null}
     * @param sent             whether the packet has already been transmitted
     * @throws NullPointerException if {@code payload} is {@code null}
     */
    public void store(long extendedSequence, byte[] payload, boolean sent) {
        var slot = (int) (stored % capacity);
        slots[slot] = new CachedPacket(extendedSequence, payload, sent);
        stored++;
    }

    /**
     * Returns the retained packet with the given extended sequence, or {@code null} if it has aged out.
     *
     * <p>Used by a hop by hop NACK responder to replay a single missed packet; a sequence older than the
     * oldest retained packet, or never stored, yields {@code null} because the cache no longer holds it.
     *
     * @param extendedSequence the 32 bit RTP extended sequence number to look up
     * @return the retained packet, or {@code null} if not currently cached
     */
    public CachedPacket get(long extendedSequence) {
        for (var packet : liveSlots()) {
            if (packet.extendedSequence() == extendedSequence) {
                return packet;
            }
        }
        return null;
    }

    /**
     * Returns the retained packets still flagged unsent, in ascending sequence order.
     *
     * <p>Drives the initial transmit drain: a packet stored with its sent flag clear waits here until the
     * sender ships it. The list is a fresh snapshot of references to the cached payloads in increasing
     * extended sequence order; it does not clear the unsent flags, since the cache does not track the
     * transition (the sender restores the packet as sent, or lets it age out).
     *
     * @return the unsent packets in ascending sequence order, possibly empty, never {@code null}
     */
    public List<CachedPacket> acquireUnsent() {
        var live = liveSlots();
        live.sort((a, b) -> Long.compareUnsigned(a.extendedSequence(), b.extendedSequence()));
        var result = new ArrayList<CachedPacket>();
        for (var packet : live) {
            if (!packet.sent()) {
                result.add(packet);
            }
        }
        return result;
    }

    /**
     * Returns up to a bounded number of retained packets at or below a target sequence, ascending.
     *
     * <p>Drives out of band Opus FEC: the sender selects a span of recent payloads to protect and folds
     * them into a separate FEC packet. The window holds at most {@code maxPackets} packets whose extended
     * sequence is at or below {@code throughExtendedSequence}, taken as the most recent such packets and
     * returned in ascending sequence order so the FEC builder walks them oldest first.
     *
     * @param throughExtendedSequence the highest extended sequence to include
     * @param maxPackets              the maximum number of packets to return; a value that is not positive
     *                                yields an empty list
     * @return the selected packets in ascending sequence order, possibly empty, never {@code null}
     */
    public List<CachedPacket> acquireOpusFecRange(long throughExtendedSequence, int maxPackets) {
        if (maxPackets <= 0) {
            return List.of();
        }
        var size = size();
        var newest = (int) ((stored - 1) % capacity);
        var selected = new ArrayList<CachedPacket>();
        for (var step = 0; step < size; step++) {
            var index = newest - step;
            if (index < 0) {
                index += capacity;
            }
            var packet = slots[index];
            if (Long.compareUnsigned(packet.extendedSequence(), throughExtendedSequence) <= 0) {
                selected.add(packet);
                if (selected.size() == maxPackets) {
                    break;
                }
            }
        }
        Collections.reverse(selected);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "opus fec range acquired count={0} throughSeq={1}", selected.size(), throughExtendedSequence);
        return selected;
    }

    /**
     * Returns up to a bounded number of the most recent retained packets, newest first.
     *
     * <p>Drives RFC 2198 MLow redundancy: the packer prepends copies of the most recent payloads ahead of
     * the primary one, walking from newest to oldest and stopping when the cumulative size would exceed
     * the stream MTU. This window supplies that newest first order, holding at most {@code maxPackets}
     * packets strictly older than {@code beforeExtendedSequence} (the primary being built); the cumulative
     * size and extended sequence stop conditions are applied by the packer, not here.
     *
     * @param beforeExtendedSequence the extended sequence of the primary packet; only packets older than
     *                               this are returned
     * @param maxPackets             the maximum number of redundancy packets to return; a value that is not
     *                               positive yields an empty list
     * @return the selected packets in descending (newest first) sequence order, possibly empty, never
     * {@code null}
     */
    public List<CachedPacket> acquireMlowRedRange(long beforeExtendedSequence, int maxPackets) {
        if (maxPackets <= 0) {
            return List.of();
        }
        var size = size();
        var newest = (int) ((stored - 1) % capacity);
        var selected = new ArrayList<CachedPacket>();
        for (var step = 0; step < size; step++) {
            var index = newest - step;
            if (index < 0) {
                index += capacity;
            }
            var packet = slots[index];
            if (Long.compareUnsigned(packet.extendedSequence(), beforeExtendedSequence) < 0) {
                selected.add(packet);
                if (selected.size() == maxPackets) {
                    break;
                }
            }
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "mlow red range acquired count={0} beforeSeq={1}", selected.size(), beforeExtendedSequence);
        return selected;
    }

    /**
     * Discards every retained packet, leaving the cache empty.
     *
     * <p>Called when the stream restarts so a redundancy window does not replay a packet from the prior
     * stream incarnation. The store count is reset so the next store begins filling the ring afresh.
     */
    public void clear() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "packet cache cleared, discarded {0} packets", size());
        for (var i = 0; i < capacity; i++) {
            slots[i] = null;
        }
        stored = 0;
    }

    /**
     * Returns a mutable list of the currently occupied slots, in no particular order.
     *
     * <p>Skips the {@code null} slots left before the ring first fills; the callers that need ordering
     * sort the result themselves. The list holds references to the live {@link CachedPacket} records,
     * which are immutable, so the snapshot is safe to reorder.
     *
     * @return the occupied slots as a fresh mutable list
     */
    private List<CachedPacket> liveSlots() {
        var live = new ArrayList<CachedPacket>(size());
        for (var packet : slots) {
            if (packet != null) {
                live.add(packet);
            }
        }
        return live;
    }
}
