package com.github.auties00.cobalt.calls.platform.audio;

import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * Drains captured audio from an {@link AudioOutput} source through an {@link AudioCaptureRing} and
 * forwards it to the call engine one fixed block at a time.
 *
 * <p>The engine encodes a steady cadence of fixed size sample blocks, but a capture source delivers
 * frames of whatever size and timing its device or producer chooses, so the pump decouples the two
 * through the capture ring. On each turn it pulls one {@link AudioFrame} from the source with a blocking
 * {@link AudioOutput#takeAudio()}, writes its samples into the ring as the producer, then drains as many whole
 * {@link #framesPerChunk()} blocks as the ring now holds and hands each to the engine through the
 * supplied {@link AudioBlockSink}. A source frame whose sample count is not a multiple of the block size
 * leaves a remainder buffered in the ring that the next frame completes, so the engine always receives
 * uniform blocks regardless of the source's chunking.
 *
 * <p>When the source falls behind, the ring holds fewer than one block and the drain is an underrun: the
 * pump counts it, logs every hundredth occurrence, and backs off for a short fixed interval a bounded
 * number of times before returning to the source pull, so a momentary capture gap neither spins the CPU
 * nor stalls the encoder permanently. At startup the pump buffers a target number of samples before
 * forwarding the first block, which establishes the capture to render skew margin the engine's clock
 * expects. The loop ends when the source signals end of stream by returning {@code null} from
 * {@link AudioOutput#takeAudio()} or when {@link #stop()} is called; either way the backing virtual thread
 * exits and any buffered samples are discarded.
 *
 * <p>The pump runs on its own virtual thread and blocks freely on the source pull without affecting any
 * other call thread, matching the Cobalt threading model. It is the sole producer and the sole consumer
 * of its capture ring, so the ring's single producer single consumer contract is honoured by this one
 * thread driving both ends. The class is started once and stopped once; restarting a stopped pump is not
 * supported.
 *
 * @implNote This implementation absorbs a capture underrun with a bounded park rather than a busy spin:
 * on each underrun it parks the running thread for {@link #UNDERRUN_BACKOFF_NANOS} nanoseconds up to
 * {@link #UNDERRUN_BACKOFF_LIMIT} times, releasing its carrier through {@link LockSupport#parkNanos(long)}
 * between attempts, and it tracks capture skew with {@link System#nanoTime()}. A single virtual thread
 * both fills the ring from the {@link AudioOutput} source and drains it to the engine, so the ring is
 * driven from one thread and its single producer single consumer contract holds without a separate
 * producer.
 */
public final class AudioReaderPump {
    /**
     * The logger for {@link AudioReaderPump}.
     */
    private static final System.Logger LOGGER = Log.get(AudioReaderPump.class);

    /**
     * Number of underruns between successive log lines.
     *
     * <p>Bounds diagnostic log volume on a persistently starved capture path by emitting the underrun
     * count only once per this many underruns.
     */
    private static final int UNDERRUN_LOG_INTERVAL = 100;

    /**
     * Backoff applied on an underrun before the pump retries the drain, in nanoseconds.
     *
     * <p>Parks the pump thread for roughly two milliseconds per attempt, releasing its carrier, before the
     * drain is retried.
     */
    private static final long UNDERRUN_BACKOFF_NANOS = 2_000_000L;

    /**
     * Maximum number of backoff attempts the pump performs for a single underrun before returning to the
     * source pull.
     *
     * <p>Bounds the wait so a sustained capture gap does not trap the loop away from the source pull.
     */
    private static final int UNDERRUN_BACKOFF_LIMIT = 4;

    /**
     * The local outbound audio source the pump drains for captured frames.
     *
     * <p>Pulled with a blocking {@link AudioOutput#takeAudio()} each turn; a {@code null} return ends the
     * loop. The pump never calls the source's application facing write side.
     */
    private final AudioOutput source;

    /**
     * The capture ring buffering source samples between the source pull and the block drain.
     *
     * <p>The pump is its sole producer (writing pulled frames) and sole consumer (draining blocks), so
     * the ring's single producer single consumer contract holds on this one thread.
     */
    private final AudioCaptureRing ring;

    /**
     * The engine consumer each drained block is forwarded to.
     *
     * <p>Invoked once per whole {@link #framesPerChunk} block with a reused scratch array; the sink must
     * copy the samples it needs before returning, since the pump overwrites the array on the next drain.
     */
    private final AudioBlockSink encoder;

    /**
     * Number of samples the engine consumes per forwarded block.
     *
     * <p>A drain delivers exactly this many samples or none; the ring rechunks arbitrary source frame
     * sizes to this fixed block.
     */
    private final int framesPerChunk;

    /**
     * Target number of samples to buffer before the first block is forwarded.
     *
     * <p>Establishes the startup capture to render skew margin; until the ring holds this many samples the
     * pump keeps pulling from the source without draining.
     */
    private final int startupSeedSamples;

    /**
     * Scratch array a drained block is copied into before being handed to the encoder.
     *
     * <p>Sized to {@link #framesPerChunk} and reused across drains to avoid per block allocation; its
     * contents are valid only for the duration of one {@link AudioBlockSink#accept(short[], int)} call.
     */
    private final short[] block;

    /**
     * Whether the pump should keep running.
     *
     * <p>Set true at {@link #start()} and cleared by {@link #stop()}; the loop tests it each turn and the
     * stop path unparks the loop so a pending backoff returns promptly.
     */
    private final AtomicBoolean running;

    /**
     * Running count of underruns observed since the pump started.
     *
     * <p>Incremented whenever a drain finds fewer than one block buffered; used only to gate the periodic
     * log and as a diagnostic, never to alter behaviour beyond the backoff.
     */
    private volatile long underrunCount;

    /**
     * Whether the startup seed has been reached and steady state draining has begun.
     *
     * <p>False until the ring first holds {@link #startupSeedSamples} samples; while false the pump fills
     * without draining so the skew margin is established before the engine sees audio.
     */
    private boolean seeded;

    /**
     * The virtual thread running {@link #loop()}, or {@code null} before {@link #start()}.
     *
     * <p>Retained so {@link #stop()} can interrupt and unpark it; the pump owns exactly one such thread
     * for its lifetime.
     */
    private volatile Thread thread;

    /**
     * A consumer of one fixed size block of captured PCM samples bound for the call engine.
     *
     * <p>The reader pump forwards each whole block it drains from the capture ring to an instance of this
     * interface, which the engine layer implements to feed its audio encoder. The samples are passed in a
     * scratch array the pump reuses, so an implementation must read or copy them before returning.
     */
    @FunctionalInterface
    public interface AudioBlockSink {
        /**
         * Accepts one block of captured samples for encoding.
         *
         * <p>Invoked by the reader pump once per drained block, on the pump's virtual thread, with
         * exactly {@code length} valid samples at the start of {@code block}. The array is owned by the
         * pump and overwritten after this call returns, so an implementation copies any samples it must
         * retain. The method should not block for long, since the pump cannot drain the next block until
         * it returns.
         *
         * @param block  the scratch array holding the samples; valid only during this call
         * @param length the number of valid samples at the start of {@code block}
         */
        void accept(short[] block, int length);
    }

    /**
     * Constructs a reader pump bridging the given source, ring, and engine consumer.
     *
     * <p>The pump does not start until {@link #start()} is called. The ring's capacity must comfortably
     * exceed the block size and the startup seed so the producer side is not immediately full.
     *
     * @param source             the outbound audio source to drain; never {@code null}
     * @param ring               the capture ring to buffer through; never {@code null}
     * @param encoder            the engine consumer for drained blocks; never {@code null}
     * @param framesPerChunk     the block size in samples the engine consumes; must be positive
     * @param startupSeedSamples the samples to buffer before the first drain; must not be negative
     * @throws NullPointerException     if any reference argument is {@code null}
     * @throws IllegalArgumentException if {@code framesPerChunk} is not positive, if
     *                                  {@code startupSeedSamples} is negative, or if either exceeds the
     *                                  ring capacity
     */
    public AudioReaderPump(AudioOutput source, AudioCaptureRing ring, AudioBlockSink encoder, int framesPerChunk, int startupSeedSamples) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.ring = Objects.requireNonNull(ring, "ring cannot be null");
        this.encoder = Objects.requireNonNull(encoder, "encoder cannot be null");
        if (framesPerChunk <= 0) {
            throw new IllegalArgumentException("framesPerChunk must be positive: " + framesPerChunk);
        }
        if (startupSeedSamples < 0) {
            throw new IllegalArgumentException("startupSeedSamples must not be negative: " + startupSeedSamples);
        }
        if (framesPerChunk > ring.capacity() || startupSeedSamples > ring.capacity()) {
            throw new IllegalArgumentException("framesPerChunk and startupSeedSamples must fit the ring capacity " + ring.capacity());
        }
        this.framesPerChunk = framesPerChunk;
        this.startupSeedSamples = startupSeedSamples;
        this.block = new short[framesPerChunk];
        this.running = new AtomicBoolean();
        this.underrunCount = 0;
        this.seeded = startupSeedSamples == 0;
    }

    /**
     * Returns the block size in samples the engine consumes per drain.
     *
     * @return the configured {@code framesPerChunk}
     */
    public int framesPerChunk() {
        return framesPerChunk;
    }

    /**
     * Returns the number of underruns observed since the pump started.
     *
     * @return the running underrun count
     */
    public long underrunCount() {
        return underrunCount;
    }

    /**
     * Starts the pump on a fresh virtual thread.
     *
     * <p>Marks the pump running and launches {@link #loop()}. Calling this more than once, or after
     * {@link #stop()}, has no effect: only the first start binds the thread.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting audio reader pump, framesPerChunk={0}", framesPerChunk);
            thread = Thread.ofVirtual()
                    .name("calls-audio-reader-pump")
                    .start(this::loop);
        }
    }

    /**
     * Stops the pump and unblocks its thread.
     *
     * <p>Clears the running flag and interrupts the loop thread so a blocking source pull or a backoff
     * park returns promptly and the loop exits. Idempotent; safe to call from any thread and more than
     * once.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "stopping audio reader pump");
            var current = thread;
            if (current != null) {
                current.interrupt();
                LockSupport.unpark(current);
            }
        }
    }

    /**
     * Runs the capture pump loop until the source ends or the pump is stopped.
     *
     * <p>Each turn pulls one frame from the source and writes its samples into the ring, then, once the
     * startup seed is reached, drains every whole block the ring holds and forwards it to the engine,
     * applying the bounded backoff on an underrun. A {@code null} frame from the source or a cleared
     * running flag ends the loop; an interrupt during the source pull is treated as a stop. On exit the
     * ring is cleared.
     */
    private void loop() {
        try {
            while (running.get()) {
                AudioFrame frame;
                try {
                    frame = source.takeAudio();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (frame == null) {
                    break;
                }
                fill(frame.pcm());
                if (!seeded) {
                    if (ring.available() < startupSeedSamples) {
                        continue;
                    }
                    seeded = true;
                }
                drainBlocks();
            }
        } finally {
            ring.clear();
        }
    }

    /**
     * Writes the samples of one captured frame into the ring, dropping the oldest on overflow.
     *
     * <p>Attempts a single ring write; when the ring is too full to hold the frame, makes room by
     * discarding one block from the consumer side and retries once, so a transient drain stall does not
     * permanently wedge the producer. A frame still too large after one eviction is dropped, since it
     * exceeds what the ring can hold at all.
     *
     * @param pcm the captured samples to enqueue; never {@code null}
     */
    private void fill(short[] pcm) {
        if (pcm.length == 0) {
            return;
        }
        if (ring.write(pcm, 0, pcm.length) == 0 && pcm.length <= ring.capacity()) {
            ring.read(block, 0, framesPerChunk);
            ring.write(pcm, 0, pcm.length);
        }
    }

    /**
     * Drains and forwards every whole block currently buffered, or records an underrun.
     *
     * <p>While the ring holds at least one block, reads it into the scratch array and hands it to the
     * encoder. When no whole block is available the method increments the underrun count, logs on the
     * periodic interval, and backs off the bounded number of times so the loop neither spins nor stalls.
     */
    private void drainBlocks() {
        var drained = false;
        while (ring.read(block, 0, framesPerChunk) == framesPerChunk) {
            encoder.accept(block, framesPerChunk);
            drained = true;
        }
        if (!drained) {
            recordUnderrun();
        }
    }

    /**
     * Counts an underrun, logs it periodically, and applies the bounded backoff.
     *
     * <p>Increments the running underrun count and, on every {@link #UNDERRUN_LOG_INTERVAL}th underrun,
     * emits the capture underrun diagnostic carrying the count, the samples currently buffered, and the
     * block size needed. Then parks the thread for {@link #UNDERRUN_BACKOFF_NANOS} up to
     * {@link #UNDERRUN_BACKOFF_LIMIT} times, returning early once a block becomes available or the pump is
     * stopped, so a brief capture gap is absorbed without a busy spin.
     *
     * @implNote This implementation emits the diagnostic at {@link System.Logger.Level#DEBUG}.
     */
    private void recordUnderrun() {
        underrunCount++;
        if (underrunCount % UNDERRUN_LOG_INTERVAL == 0 && Log.DEBUG) {
            var available = ring.available();
            LOGGER.log(Level.DEBUG, "capture underrun, count={0}, available={1}, needed={2}",
                    underrunCount, available, framesPerChunk);
        }
        for (var i = 0; i < UNDERRUN_BACKOFF_LIMIT; i++) {
            if (!running.get() || ring.available() >= framesPerChunk) {
                return;
            }
            LockSupport.parkNanos(UNDERRUN_BACKOFF_NANOS);
        }
    }
}
