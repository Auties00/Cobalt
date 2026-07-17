package com.github.auties00.cobalt.calls.platform.audio;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * A ring of signed 16 bit PCM samples that carries decoded audio from the call engine to the host
 * playback endpoint, with a demand signal that wakes the producer when the consumer needs more samples.
 *
 * <p>This is the playback data plane and the mirror of {@link AudioCaptureRing}. It admits exactly one
 * producer thread and one consumer thread. The producer is the {@link AudioWriterPump} that
 * {@linkplain #write(short[], int, int) fills} the ring with samples the engine has decoded, and the
 * consumer is the host playback endpoint (a speaker, a file recorder, or a bridge between two calls)
 * that {@linkplain #read(short[], int, int) drains} them for rendering. Unlike the capture ring, this
 * side is demand driven: rather than the producer running freely, the consumer raises a demand whenever
 * it is about to render and the producer sleeps until that demand arrives, so the engine renders audio
 * only as fast as it is consumed and the buffered depth stays near the consumer's appetite. The ring is
 * in the sample domain; every slot holds one signed 16 bit mono PCM sample at the negotiated rate.
 *
 * <p>The demand signal is a monotonically increasing counter the consumer bumps through
 * {@link #signalDemand()}. The producer parks on {@link #awaitDemand(long)} until the counter advances
 * past the value it last observed or the deadline elapses, then writes a fresh batch and loops. A
 * counter rather than a flag is the signal, so a demand raised between the producer's write and its next
 * park is not lost: the producer compares the current counter against its own watermark and skips the
 * park when it has fallen behind. This is an edge signal read against a level watermark, the same
 * distinction the engine relies on for its playback wake.
 *
 * <p>The class is free of locks on the data path. The sample cursors use acquire and release ordering as
 * in {@link AudioCaptureRing}; the demand path uses an {@link AtomicInteger} counter plus a reference to
 * the parked producer that is unparked on each signal. The producer never overwrites unread samples and
 * the consumer reads whole blocks only, matching the capture ring's contracts of no overwrite and no
 * partial read.
 *
 * @implNote This implementation replaces the native writer's futex wait with a {@link LockSupport} park
 * keyed on a Java demand counter, because the JVM has no futex and a virtual thread parks without an OS
 * wait; comparing the counter against the producer's watermark preserves immunity to a missed wake, and
 * the wake and timeout counters live in {@link AudioWriterPump}. The buffered sample arithmetic is a
 * power of two mask over free running cursors, equivalent to {@code (writeCursor - readCursor)} taken
 * modulo the capacity.
 */
public final class AudioPlaybackRing {
    /**
     * Number of usable sample slots in the ring.
     *
     * <p>Always a power of two so the mapping from cursor to slot is a bitwise mask.
     */
    private final int capacity;

    /**
     * Mask applied to a free running cursor to obtain its slot index in {@link #buffer}.
     *
     * <p>Equal to {@link #capacity} minus one.
     */
    private final int mask;

    /**
     * Backing sample store, indexed by a cursor masked with {@link #mask}.
     *
     * <p>Each slot holds one signed 16 bit PCM sample awaiting playback.
     */
    private final short[] buffer;

    /**
     * Free running count of samples the producer has written since construction.
     *
     * <p>Advanced by the writer pump with release ordering; read by the consumer with acquire ordering.
     */
    private final AtomicInteger writeCursor;

    /**
     * Free running count of samples the consumer has read since construction.
     *
     * <p>Advanced by the playback endpoint with release ordering; read by the producer with acquire
     * ordering.
     */
    private final AtomicInteger readCursor;

    /**
     * Monotonically increasing count of demand pulses raised by the consumer.
     *
     * <p>Bumped by {@link #signalDemand()} each time the playback endpoint wants more samples. The
     * producer remembers the value it last serviced and parks only while this counter has not advanced
     * past it, which makes a pulse raised between a write and the next park impossible to miss.
     */
    private final AtomicInteger demand;

    /**
     * The producer thread currently parked in {@link #awaitDemand(long)}, or {@code null} when none is
     * parked.
     *
     * <p>Set by the producer immediately before it parks and cleared when it resumes; read by
     * {@link #signalDemand()} to unpark a waiting producer. Holding the reference lets a demand pulse
     * unpark the producer without a condition variable.
     */
    private volatile Thread parkedProducer;

    /**
     * Constructs a playback ring holding at least the requested number of samples.
     *
     * <p>The requested capacity is rounded up to the next power of two. The ring starts empty with both
     * cursors and the demand counter at zero and no producer parked.
     *
     * @param minimumCapacitySamples the least number of samples the ring must hold; must be positive
     * @throws IllegalArgumentException if {@code minimumCapacitySamples} is not positive or exceeds the
     *                                  largest representable power of two
     */
    public AudioPlaybackRing(int minimumCapacitySamples) {
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
        this.demand = new AtomicInteger();
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
     * @return the count of buffered samples, in the range {@code [0, capacity]}
     */
    public int available() {
        return writeCursor.getAcquire() - readCursor.getAcquire();
    }

    /**
     * Returns the number of samples that may be written before the ring is full.
     *
     * @return the count of free sample slots, in the range {@code [0, capacity]}
     */
    public int free() {
        return capacity - available();
    }

    /**
     * Writes a run of decoded samples into the ring without overwriting unread samples.
     *
     * <p>Behaves exactly as {@link AudioCaptureRing#write(short[], int, int)}: copies, wrap aware, into
     * the slots after the producer cursor and publishes the advanced cursor, or writes nothing and
     * returns {@code 0} when the free space is too small. A {@code length} of zero is a no operation
     * returning {@code 0}.
     *
     * @param source the decoded samples to enqueue; never {@code null}
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
        var write = writeCursor.getAcquire();
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
     * Reads up to one block of samples out of the ring for playback, returning the count actually
     * available.
     *
     * <p>Copies the smaller of {@code length} and the buffered sample count from the slots after the
     * consumer cursor into {@code destination} starting at {@code offset}, wrap aware, then publishes the
     * advanced cursor and returns the number of samples copied. Unlike the capture ring's whole block
     * read, this returns a short count when the ring is nearly drained so the playback endpoint can zero
     * the remainder rather than stall, which matches a playback worklet that must always hand the device
     * a full frame. A {@code length} of zero, or an empty ring, returns {@code 0}.
     *
     * @param destination the array to copy samples into; never {@code null}
     * @param offset      the index in {@code destination} of the first sample to write
     * @param length      the maximum block size in samples to read
     * @return the number of samples copied, in the range {@code [0, length]}
     * @throws NullPointerException      if {@code destination} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} and {@code length} fall outside
     *                                   {@code destination}
     */
    public int read(short[] destination, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, destination.length);
        if (length == 0) {
            return 0;
        }
        var read = readCursor.getAcquire();
        var write = writeCursor.getAcquire();
        var count = Math.min(length, write - read);
        if (count <= 0) {
            return 0;
        }
        var start = read & mask;
        var firstRun = Math.min(count, capacity - start);
        System.arraycopy(buffer, start, destination, offset, firstRun);
        if (firstRun < count) {
            System.arraycopy(buffer, 0, destination, offset + firstRun, count - firstRun);
        }
        readCursor.setRelease(read + count);
        return count;
    }

    /**
     * Raises a demand pulse and wakes the producer if it is parked.
     *
     * <p>Increments the demand counter and unparks the producer thread recorded by the most recent
     * {@link #awaitDemand(long)}. Invoked by the playback endpoint just before it renders, so the writer
     * pump tops the ring up in time. Calling it when no producer is parked still advances the counter, so
     * the next park observes the pulse and returns immediately.
     */
    public void signalDemand() {
        demand.getAndIncrement();
        var parked = parkedProducer;
        if (parked != null) {
            LockSupport.unpark(parked);
        }
    }

    /**
     * Returns the current value of the demand counter.
     *
     * <p>The producer reads this once before its first park to seed its watermark, then passes the
     * watermark to {@link #awaitDemand(long)} on every subsequent park.
     *
     * @return the number of demand pulses raised since construction
     */
    public int demandCount() {
        return demand.getAcquire();
    }

    /**
     * Parks the calling producer thread without a timeout until a demand pulse arrives, and returns the
     * demand count observed on resume.
     *
     * <p>When the demand counter already exceeds {@code lastObservedDemand} the method returns at once
     * without parking, so a pulse raised since the producer's last service is never slept through.
     * Otherwise it records the calling thread as the parked producer and parks until
     * {@link #signalDemand()} unparks it or the thread is interrupted, then clears the parked producer
     * reference and returns the current demand count. A spurious unpark is absorbed by the caller
     * comparing the returned count against its watermark and parking again. Unlike
     * {@link #awaitDemandFor(long, long)} this never wakes on its own, so a producer that uses it emits
     * samples strictly on demand and relies on the consumer to keep pulsing; a stop must raise a final
     * pulse to release it.
     *
     * @param lastObservedDemand the demand count the producer last serviced
     * @return the demand count observed on resume, never less than {@code lastObservedDemand}
     */
    public int awaitDemand(long lastObservedDemand) {
        var current = demand.getAcquire();
        if (current - (int) lastObservedDemand > 0) {
            return current;
        }
        parkedProducer = Thread.currentThread();
        try {
            current = demand.getAcquire();
            if (current - (int) lastObservedDemand <= 0) {
                LockSupport.park(this);
            }
        } finally {
            parkedProducer = null;
        }
        return demand.getAcquire();
    }

    /**
     * Parks the calling producer thread until a demand pulse arrives or the timeout elapses, and returns
     * the demand count observed on resume.
     *
     * <p>Equivalent to {@link #awaitDemand(long)} computing its deadline from a relative timeout against
     * {@link System#nanoTime()}. A non positive timeout still services an already advanced demand counter
     * but otherwise returns promptly.
     *
     * @param lastObservedDemand the demand count the producer last serviced
     * @param timeoutNanos       the maximum time to park, in nanoseconds
     * @return the demand count observed on resume, never less than {@code lastObservedDemand}
     */
    public int awaitDemandFor(long lastObservedDemand, long timeoutNanos) {
        var current = demand.getAcquire();
        if (current - (int) lastObservedDemand > 0) {
            return current;
        }
        parkedProducer = Thread.currentThread();
        try {
            current = demand.getAcquire();
            if (current - (int) lastObservedDemand <= 0) {
                LockSupport.parkNanos(this, timeoutNanos);
            }
        } finally {
            parkedProducer = null;
        }
        return demand.getAcquire();
    }

    /**
     * Discards every buffered sample, leaving the ring empty.
     *
     * <p>Snaps the consumer cursor up to the producer cursor. Intended for teardown; racing the producer
     * may drop samples and is acceptable only when both pumps are stopping.
     */
    public void clear() {
        readCursor.setRelease(writeCursor.getAcquire());
    }
}
