package com.github.auties00.cobalt.calls.platform.audio;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * A demand driven loop, running on a virtual thread, that pulls decoded audio from the call engine into
 * an {@link AudioPlaybackRing} when the host playback endpoint signals it needs more samples.
 *
 * <p>This is the playback pump and the producer counterpart to the host playback endpoint that drains
 * the ring. It is not free running: rather than render audio as fast as the engine can decode, the pump
 * sleeps until the playback endpoint raises a demand through {@link AudioPlaybackRing#signalDemand()},
 * then pulls one batch of decoded samples from the engine via the supplied {@link AudioBlockSource} and
 * writes them into the ring for the endpoint to consume. Pacing the engine to the consumer this way keeps
 * the buffered playback depth near the endpoint's appetite and avoids the engine decoding far ahead of
 * what is heard, which would inflate latency.
 *
 * <p>The demand wait is bounded by a timeout so the pump also wakes periodically without an explicit
 * pulse; on a timed wake with no fresh demand the pump still tops the ring up, which lets playback start
 * cleanly and recover if a pulse is ever lost. When a pull yields no samples the pump records it and
 * loops back to the wait rather than busy spinning, since the engine simply has nothing decoded yet. The
 * pump tracks how many times it woke on a demand pulse, how many times it woke on the timeout, and how
 * many pulls returned nothing, as diagnostics of playback health. The loop ends when {@link #stop()} is
 * called; the backing virtual thread then exits and the ring is cleared.
 *
 * <p>The pump runs on its own virtual thread and is the sole producer of its playback ring; the host
 * playback endpoint is the sole consumer and the sole raiser of demand, so the ring's single producer
 * single consumer contract and its single parked producer demand contract are both honoured. The class
 * is started once and stopped once; restarting a stopped pump is not supported.
 *
 * @implNote This implementation waits for demand through
 * {@link AudioPlaybackRing#awaitDemandFor(long, long)} over a {@link LockSupport} park rather than a
 * plain timed wait: the demand counter passed on each wait means a pulse raised between two services is
 * not slept through, giving the loop immunity to a missed wake. The wake, timeout, and empty pull tallies
 * are kept only as diagnostic fields and never alter control flow. The decode itself is not owned here; it
 * is the injected {@link AudioBlockSource}, so this class carries only the host side pacing.
 */
public final class AudioWriterPump {
    /**
     * Default bound on a single demand wait, in nanoseconds.
     *
     * <p>Caps how long the pump parks without a demand pulse so playback wakes periodically even if a
     * pulse is lost; twenty milliseconds matches the engine's nominal render block period.
     */
    private static final long DEFAULT_WAIT_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(20);

    /**
     * The logger for {@link AudioWriterPump}.
     */
    private static final System.Logger LOGGER = Log.get(AudioWriterPump.class);

    /**
     * The playback ring the pump fills and waits on for demand.
     *
     * <p>The pump is its sole producer; the host playback endpoint is its sole consumer and the sole
     * caller of {@link AudioPlaybackRing#signalDemand()}.
     */
    private final AudioPlaybackRing ring;

    /**
     * The engine source the pump pulls decoded samples from.
     *
     * <p>Invoked on each serviced demand with a reused scratch array; returns the number of decoded
     * samples written into it, possibly zero when nothing is ready.
     */
    private final AudioBlockSource decoder;

    /**
     * Number of samples the pump requests from the engine per pull.
     *
     * <p>Sizes the scratch array and the batch requested each turn; chosen by the caller to match the engine's
     * render block so one pull yields one block.
     */
    private final int framesPerChunk;

    /**
     * Maximum time the pump parks on a single demand wait, in nanoseconds.
     *
     * <p>Bounds the park so the pump wakes on the timeout when no pulse arrives.
     */
    private final long waitTimeoutNanos;

    /**
     * Scratch array decoded samples are pulled into before being written to the ring.
     *
     * <p>Sized to {@link #framesPerChunk} and reused across turns to avoid allocating on each pull.
     */
    private final short[] block;

    /**
     * Whether the pump should keep running.
     *
     * <p>Set true at {@link #start()} and cleared by {@link #stop()}; the loop tests it each turn and the
     * stop path unparks the loop so a pending demand wait returns promptly.
     */
    private final AtomicBoolean running;

    /**
     * Count of demand waits that returned because a demand pulse advanced the counter.
     *
     * <p>Diagnostic counter only; never alters behaviour.
     */
    private volatile long demandWakeCount;

    /**
     * Count of demand waits that returned because the timeout elapsed without a fresh pulse.
     *
     * <p>Diagnostic counter only; never alters behaviour.
     */
    private volatile long timeoutWakeCount;

    /**
     * Count of engine pulls that returned no decoded samples.
     *
     * <p>Diagnostic counter only; a high value indicates the engine is not keeping the playback path fed.
     */
    private volatile long noDataCount;

    /**
     * Count of engine pulls that threw and were skipped so the pump kept servicing later packets.
     *
     * <p>A decoder that throws on one malformed packet must not silence the whole leg: the throw is caught,
     * counted here, logged once, and the pump continues rather than unwinding and terminating its thread.
     */
    private volatile long pumpFailureCount;

    /**
     * The demand value the pump last serviced.
     *
     * <p>Passed to {@link AudioPlaybackRing#awaitDemandFor(long, long)} so a pulse raised since the last
     * service is not slept through; updated to the value observed on each resume.
     */
    private long lastServicedDemand;

    /**
     * The virtual thread running {@link #loop()}, or {@code null} before {@link #start()}.
     *
     * <p>Retained so {@link #stop()} can interrupt and unpark it.
     */
    private volatile Thread thread;

    /**
     * A source of decoded PCM samples the playback pump pulls from the call engine.
     *
     * <p>The writer pump asks an instance of this interface for a batch of rendered samples each time it
     * services a playback demand; the engine layer implements it over its decode and mixing path. The
     * pump supplies a scratch array to fill and expects the count actually produced, which may be fewer
     * than requested or zero when nothing is ready.
     */
    @FunctionalInterface
    public interface AudioBlockSource {
        /**
         * Fills the given array with up to {@code length} decoded samples and returns the count written.
         *
         * <p>Invoked by the writer pump on its virtual thread once per serviced demand. The
         * implementation writes between zero and {@code length} samples at the start of {@code block} and
         * returns how many it wrote; returning zero signals that no audio is ready and is not an error.
         * The array is owned by the pump and reused after this call, so the implementation must not retain
         * it.
         *
         * @param block  the scratch array to fill; writable only during this call
         * @param length the maximum number of samples to write
         * @return the number of samples written, in the range {@code [0, length]}
         */
        int pull(short[] block, int length);
    }

    /**
     * Constructs a writer pump with the default demand wait timeout.
     *
     * <p>Equivalent to {@link #AudioWriterPump(AudioPlaybackRing, AudioBlockSource, int, long)} with a
     * twenty millisecond timeout, matching the engine's nominal render block period.
     *
     * @param ring           the playback ring to fill; never {@code null}
     * @param decoder        the engine source of decoded samples; never {@code null}
     * @param framesPerChunk the samples to request per pull; must be positive
     * @throws NullPointerException     if {@code ring} or {@code decoder} is {@code null}
     * @throws IllegalArgumentException if {@code framesPerChunk} is not positive or exceeds the ring
     *                                  capacity
     */
    public AudioWriterPump(AudioPlaybackRing ring, AudioBlockSource decoder, int framesPerChunk) {
        this(ring, decoder, framesPerChunk, DEFAULT_WAIT_TIMEOUT_NANOS);
    }

    /**
     * Constructs a writer pump bridging the given engine source and playback ring.
     *
     * <p>The pump does not start until {@link #start()} is called.
     *
     * @param ring             the playback ring to fill; never {@code null}
     * @param decoder          the engine source of decoded samples; never {@code null}
     * @param framesPerChunk   the samples to request per pull; must be positive
     * @param waitTimeoutNanos the maximum time to park on a demand wait, in nanoseconds; must be positive
     * @throws NullPointerException     if {@code ring} or {@code decoder} is {@code null}
     * @throws IllegalArgumentException if {@code framesPerChunk} is not positive, if it exceeds the ring
     *                                  capacity, or if {@code waitTimeoutNanos} is not positive
     */
    public AudioWriterPump(AudioPlaybackRing ring, AudioBlockSource decoder, int framesPerChunk, long waitTimeoutNanos) {
        this.ring = Objects.requireNonNull(ring, "ring cannot be null");
        this.decoder = Objects.requireNonNull(decoder, "decoder cannot be null");
        if (framesPerChunk <= 0) {
            throw new IllegalArgumentException("framesPerChunk must be positive: " + framesPerChunk);
        }
        if (framesPerChunk > ring.capacity()) {
            throw new IllegalArgumentException("framesPerChunk must fit the ring capacity " + ring.capacity());
        }
        if (waitTimeoutNanos <= 0) {
            throw new IllegalArgumentException("waitTimeoutNanos must be positive: " + waitTimeoutNanos);
        }
        this.framesPerChunk = framesPerChunk;
        this.waitTimeoutNanos = waitTimeoutNanos;
        this.block = new short[framesPerChunk];
        this.running = new AtomicBoolean();
    }

    /**
     * Returns the number of samples requested from the engine per pull.
     *
     * @return the configured {@code framesPerChunk}
     */
    public int framesPerChunk() {
        return framesPerChunk;
    }

    /**
     * Returns the number of demand waits that woke on a demand pulse.
     *
     * @return the demand wake count
     */
    public long demandWakeCount() {
        return demandWakeCount;
    }

    /**
     * Returns the number of demand waits that woke on the timeout.
     *
     * @return the timeout wake count
     */
    public long timeoutWakeCount() {
        return timeoutWakeCount;
    }

    /**
     * Returns the number of engine pulls that returned no samples.
     *
     * @return the no data count
     */
    public long noDataCount() {
        return noDataCount;
    }

    /**
     * Starts the pump on a fresh virtual thread.
     *
     * <p>Marks the pump running and launches {@link #loop()}. Calling this more than once, or after
     * {@link #stop()}, has no effect.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting audio writer pump, framesPerChunk={0}", framesPerChunk);
            thread = Thread.ofVirtual()
                    .name("calls-audio-writer-pump")
                    .start(this::loop);
        }
    }

    /**
     * Stops the pump and unblocks its thread.
     *
     * <p>Clears the running flag, raises a demand pulse so a parked demand wait returns, and interrupts
     * the loop thread so it exits promptly. Idempotent; safe to call from any thread and more than once.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "stopping audio writer pump");
            ring.signalDemand();
            var current = thread;
            if (current != null) {
                current.interrupt();
                LockSupport.unpark(current);
            }
        }
    }

    /**
     * Runs the playback pump loop until the pump is stopped.
     *
     * <p>Each turn parks on the ring's demand wait, classifies the resume as a demand or timeout wake,
     * then pulls one batch from the engine and writes whatever it produced into the ring, counting an
     * empty pull. The running flag is rechecked after the wait so a stop unblocks the loop. On exit the
     * ring is cleared.
     */
    private void loop() {
        try {
            while (running.get()) {
                var observed = ring.awaitDemandFor(lastServicedDemand, waitTimeoutNanos);
                if (!running.get()) {
                    break;
                }
                if (observed - (int) lastServicedDemand > 0) {
                    demandWakeCount++;
                } else {
                    timeoutWakeCount++;
                }
                lastServicedDemand = observed;
                try {
                    pumpOnce();
                } catch (RuntimeException exception) {
                    // A single malformed packet must never silence the whole leg: the decoder throw is caught,
                    // counted, and logged once, and the pump keeps servicing later packets instead of
                    // unwinding and terminating this thread.
                    var firstFailure = pumpFailureCount++ == 0;
                    if (firstFailure && Log.WARNING) {
                        LOGGER.log(Level.WARNING,
                                "audio writer pump decode failed, skipping packet", exception);
                    }
                }
            }
        } finally {
            ring.clear();
        }
    }

    /**
     * Pulls one batch of decoded samples from the engine and writes it into the ring.
     *
     * <p>Requests up to {@link #framesPerChunk} samples; an empty pull increments the no data count and
     * returns without touching the ring. Otherwise the produced samples are written into the ring,
     * limited to the ring's current free space so the no overwrite contract holds; any samples beyond the
     * free space are dropped, since the consumer has not yet drained room for them.
     */
    private void pumpOnce() {
        var produced = decoder.pull(block, framesPerChunk);
        if (produced <= 0) {
            noDataCount++;
            return;
        }
        var writable = Math.min(produced, ring.free());
        if (writable > 0) {
            ring.write(block, 0, writable);
        }
    }
}
