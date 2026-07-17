package com.github.auties00.cobalt.calls.stream.audio;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioOutput;

/**
 * Transmits continuous silence as the local audio of a call.
 *
 * <p>This is the generated {@link AudioOutput} returned by {@link AudioOutput#fromSilence()}, the default
 * audio fill. Every {@link #takeAudio()} yields an {@link AudioFrame} of all zero signed 16 bit samples over one
 * shared immutable buffer, with a presentation timestamp that never decreases. The stream never ends on its
 * own; it keeps producing silence until the call engine shuts it down. The default profile emits frames of
 * 160 samples spanning 10 ms each, matching the call media format. Because the samples are constant, every
 * frame shares that one immutable buffer, so the source allocates no samples per frame regardless of how long
 * the call runs.
 *
 * <p>The emitted PCM is true digital silence, not comfort noise; a producer that needs audible comfort
 * noise should layer a noise generator on top. Because {@link #takeAudio()} never returns {@code null}, the
 * call treats this as an endless stream, so the engine swaps it out rather than relying on it to signal the
 * end of the stream.
 *
 * @implNote This implementation advances the frame timestamp in the {@link AudioFrame#ptsMicros()}
 * microsecond clock.
 */
public final class SilenceAudioOutput implements AudioOutput {
    /**
     * Holds the number of samples in each emitted frame.
     */
    private final int frameSize;

    /**
     * Holds the constant all zero PCM samples, shared by every emitted frame since silence never changes.
     *
     * <p>The buffer is never mutated, so sharing one array across every frame is safe under the
     * {@link AudioOutput#takeAudio()} borrow contract even for a consumer that retains a frame: a stale reference
     * always reads silence.
     */
    private final short[] silence;

    /**
     * Holds the duration represented by one frame, in microseconds, by which each frame's presentation
     * timestamp advances.
     */
    private final long frameDurationMicros;

    /**
     * Holds the presentation timestamp, in microseconds, of the next frame, advanced atomically so the
     * stream may be drained from any thread.
     */
    private final AtomicLong ptsMicros = new AtomicLong();

    /**
     * Marks the source ended so {@link #takeAudio()} returns {@code null}.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Constructs a silence stream for the default call profile.
     *
     * <p>Equivalent to {@link #SilenceAudioOutput(int, long)} with 160 samples and a 10000 microsecond
     * (10 ms) frame duration, matching the call media cadence.
     */
    public SilenceAudioOutput() {
        this(160, 10_000);
    }

    /**
     * Constructs a silence stream with an explicit frame geometry.
     *
     * @param frameSize           the number of samples per frame
     * @param frameDurationMicros the duration of each frame in microseconds
     * @throws IllegalArgumentException if {@code frameSize} or {@code frameDurationMicros} is less than
     *                                  {@code 1}
     */
    public SilenceAudioOutput(int frameSize, long frameDurationMicros) {
        if (frameSize < 1) {
            throw new IllegalArgumentException("frameSize must be >= 1");
        }
        if (frameDurationMicros < 1) {
            throw new IllegalArgumentException("frameDurationMicros must be >= 1");
        }
        this.frameSize = frameSize;
        this.frameDurationMicros = frameDurationMicros;
        this.silence = new short[frameSize];
    }

    /**
     * {@inheritDoc}
     *
     * <p>A generated source produces its frames inside {@link #takeAudio()} and ignores application writes, so
     * this does nothing.
     *
     * @param frame the frame that would be written; ignored
     */
    @Override
    public void writeAudio(AudioFrame frame) {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a frame over the shared all zero buffer and advances the presentation timestamp by the
     * configured frame duration. Never returns {@code null}, so the stream ends only when {@link #shutdown()}
     * runs.
     *
     * @return a new silent frame; never {@code null} until shut down
     * @implNote This implementation returns one frame per call with no sleep: the call engine's capture
     * loop paces outbound audio to the wall clock using each frame's running presentation timestamp, so the
     * silence is transmitted at its natural rate without this stream having to sleep. The samples are one
     * shared immutable buffer, so a steady silence stream allocates only the small frame record.
     */
    @Override
    public AudioFrame takeAudio() {
        if (closed.get()) {
            return null;
        }
        var pts = ptsMicros.getAndAdd(frameDurationMicros);
        return new AudioFrame(silence, pts);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the source ended so the next {@link #takeAudio()} returns {@code null}. Idempotent.
     */
    @Override
    public void shutdown() {
        closed.set(true);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @implNote This implementation returns {@code false}: generated silence is clean line level audio that
     * the engine encodes without the acoustic conditioning a live microphone capture needs.
     */
    @Override
    public boolean isLiveCapture() {
        return false;
    }
}
