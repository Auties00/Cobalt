package com.github.auties00.cobalt.calls.platform.audio;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A single producer single consumer ring of signed 16 bit PCM samples carrying captured microphone
 * or system audio from the host into the call engine.
 *
 * <p>This is the capture data plane: the producer is the host capture endpoint (a microphone or a
 * system audio source) that {@linkplain #write(short[], int, int) writes} samples as they arrive, and
 * the consumer is the {@link AudioReaderPump} that {@linkplain #read(short[], int, int) drains} them one
 * fixed block at a time and forwards each block to the encoder. The ring is sample domain, not byte
 * domain: every slot holds one signed 16 bit sample of mono PCM at the negotiated rate, so a length is
 * always a sample count and never a byte count. The buffer is on the JVM heap, since neither side is
 * native; the byte exact off heap layout the engine uses for the outbound SCTP plane does not apply
 * here.
 *
 * <p>Capacity is fixed at construction and rounded up to a power of two so the read and write cursors
 * advance with a mask rather than a modulo. The two cursors are monotonically increasing free running
 * counters; the number of samples currently buffered is their difference, and the free space is the
 * capacity minus that difference. The producer never overwrites unread samples: a {@link #write(short[],
 * int, int)} that does not fit in the free space writes nothing and reports a short count, leaving the
 * caller to drop or retry, which mirrors the engine's discipline of never overwriting unread audio on
 * its own rings. The consumer reads only whole blocks: a {@link #read(short[], int, int)} for a block
 * larger than the buffered sample count reports an underrun and copies nothing, so a partial block is
 * never delivered to the encoder.
 *
 * <p>The class is lock free and safe for exactly one producer thread and one consumer thread used
 * concurrently. The cursors are held as {@link AtomicInteger}s read with acquire and written with
 * release ordering, so a sample stored before a cursor publication is visible to the other side once it
 * observes the new cursor; using more than one producer or more than one consumer breaks the single
 * writer ownership of each cursor and is unsupported.
 *
 * @implNote This implementation computes the buffered sample count as the difference between the
 * producer and consumer cursors and keeps that arithmetic exact, but it maps a cursor to a slot with a
 * power of two mask instead of a modulo and lets both cursors run monotonically rather than wrapping at
 * the capacity, so the ambiguity between a full ring and an empty ring at equal indices never arises. A
 * buffered count below the requested block size is an underrun, which is the contract of
 * {@link #read(short[], int, int)}; the underrun back off and clock skew accounting live in
 * {@link AudioReaderPump} rather than in the ring.
 */
public final class AudioCaptureRing {
    /**
     * Number of usable sample slots in the ring.
     *
     * <p>Always a power of two so the cursor to slot mapping is a bitwise mask. The producer can buffer
     * up to this many samples ahead of the consumer before {@link #write(short[], int, int)} reports a
     * short count.
     */
    private final int capacity;

    /**
     * Mask applied to a free running cursor to obtain its slot index in {@link #buffer}.
     *
     * <p>Equal to {@link #capacity} minus one; valid precisely because the capacity is a power of two.
     */
    private final int mask;

    /**
     * Backing sample store, indexed by a cursor masked with {@link #mask}.
     *
     * <p>Each slot holds one signed 16 bit PCM sample. A slot in the span between the read and write
     * cursors holds a sample not yet consumed; a slot outside that span holds a stale sample that the
     * next write may overwrite.
     */
    private final short[] buffer;

    /**
     * Free running count of samples the producer has written since construction.
     *
     * <p>Read by the consumer with acquire ordering to learn how many samples are available, and
     * advanced by the producer with release ordering after the samples it counts have been stored. Wraps
     * around the 32 bit range harmlessly because only its difference with {@link #readCursor} is ever
     * used.
     */
    private final AtomicInteger writeCursor;

    /**
     * Free running count of samples the consumer has read since construction.
     *
     * <p>Read by the producer with acquire ordering to learn how much free space exists, and advanced by
     * the consumer with release ordering after the samples it counts have been copied out. Trails
     * {@link #writeCursor} by the number of buffered samples.
     */
    private final AtomicInteger readCursor;

    /**
     * Constructs a capture ring holding at least the requested number of samples.
     *
     * <p>The requested capacity is rounded up to the next power of two, so the ring may hold more samples
     * than asked for. The ring starts empty with both cursors at zero.
     *
     * @param minimumCapacitySamples the least number of samples the ring must hold; must be positive
     * @throws IllegalArgumentException if {@code minimumCapacitySamples} is not positive or exceeds the
     *                                  largest representable power of two
     */
    public AudioCaptureRing(int minimumCapacitySamples) {
        if (minimumCapacitySamples <= 0) {
            throw new IllegalArgumentException("minimumCapacitySamples must be positive: " + minimumCapacitySamples);
        }
        var rounded = Integer.highestOneBit(minimumCapacitySamples);
        if (rounded < minimumCapacitySamples) {
            rounded <<= 1;
        }
        if (rounded <= 0) {
            throw new IllegalArgumentException("minimumCapacitySamples too large: " + minimumCapacitySamples);
        }
        this.capacity = rounded;
        this.mask = rounded - 1;
        this.buffer = new short[rounded];
        this.writeCursor = new AtomicInteger();
        this.readCursor = new AtomicInteger();
    }

    /**
     * Returns the usable sample capacity of the ring.
     *
     * @return the number of sample slots, a power of two at least as large as the requested minimum
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the number of samples currently buffered and available to read.
     *
     * <p>Computed as the difference between the producer and consumer cursors. The value is a snapshot
     * that the producer may grow and the consumer may shrink concurrently; the consumer may rely on it
     * not to shrink, and the producer may rely on it not to grow, beyond their own actions.
     *
     * @return the count of buffered samples, in the range {@code [0, capacity]}
     */
    public int available() {
        return writeCursor.getAcquire() - readCursor.getAcquire();
    }

    /**
     * Returns the number of samples that may be written before the ring is full.
     *
     * <p>Equal to the capacity minus the {@linkplain #available() buffered count}. A snapshot under
     * concurrent access, reliable as a lower bound from the producer's view because only the consumer
     * frees space.
     *
     * @return the count of free sample slots, in the range {@code [0, capacity]}
     */
    public int free() {
        return capacity - available();
    }

    /**
     * Writes a run of samples into the ring without overwriting unread samples.
     *
     * <p>Copies {@code length} samples from {@code source} starting at {@code offset} into the slots
     * after the producer cursor, wrapping at the end of the backing array, then publishes the advanced
     * cursor. When the free space is smaller than {@code length} the method writes nothing and returns
     * {@code 0}, so a producer that outpaces the consumer loses the newest block rather than corrupting
     * buffered audio; the caller decides whether to drop or retry. A {@code length} of zero is a no op
     * returning {@code 0}.
     *
     * @param source the samples to enqueue; never {@code null}
     * @param offset the index of the first sample to read from {@code source}
     * @param length the number of samples to enqueue
     * @return the number of samples written, either {@code length} or {@code 0}
     * @throws NullPointerException      if {@code source} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} and {@code length} fall outside {@code source}
     */
    public int write(short[] source, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length == 0) {
            return 0;
        }
        var write = writeCursor.get();
        var read = readCursor.getAcquire();
        if (capacity - (write - read) < length) {
            return 0;
        }
        var start = write & mask;
        var firstRun = Math.min(length, capacity - start);
        System.arraycopy(source, offset, buffer, start, firstRun);
        if (firstRun < length) {
            System.arraycopy(source, offset + firstRun, buffer, 0, length - firstRun);
        }
        writeCursor.setRelease(write + length);
        return length;
    }

    /**
     * Reads one whole block of samples out of the ring, or none on underrun.
     *
     * <p>When at least {@code length} samples are buffered, copies {@code length} samples from the slots
     * after the consumer cursor into {@code destination} starting at {@code offset}, wrapping at the end
     * of the backing array, publishes the advanced cursor, and returns {@code length}. When fewer than
     * {@code length} samples are buffered the method is an underrun: it copies nothing and returns
     * {@code 0}, so the consumer never receives a partial block. A {@code length} of zero returns
     * {@code 0} without touching the cursors.
     *
     * @param destination the array to copy the block into; never {@code null}
     * @param offset      the index in {@code destination} of the first sample to write
     * @param length      the block size in samples to read
     * @return {@code length} on success, or {@code 0} on underrun
     * @throws NullPointerException      if {@code destination} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} and {@code length} fall outside
     *                                   {@code destination}
     */
    public int read(short[] destination, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, destination.length);
        if (length == 0) {
            return 0;
        }
        var read = readCursor.get();
        var write = writeCursor.getAcquire();
        if (write - read < length) {
            return 0;
        }
        var start = read & mask;
        var firstRun = Math.min(length, capacity - start);
        System.arraycopy(buffer, start, destination, offset, firstRun);
        if (firstRun < length) {
            System.arraycopy(buffer, 0, destination, offset + firstRun, length - firstRun);
        }
        readCursor.setRelease(read + length);
        return length;
    }

    /**
     * Discards every buffered sample, leaving the ring empty.
     *
     * <p>Snaps the consumer cursor up to the producer cursor so the buffered count becomes zero. Intended
     * for the consumer side at teardown; calling it while the producer is active races the producer's
     * next write and may drop samples written in the interim, which is acceptable only when both pumps
     * are stopping.
     */
    public void clear() {
        readCursor.setRelease(writeCursor.getAcquire());
    }
}
