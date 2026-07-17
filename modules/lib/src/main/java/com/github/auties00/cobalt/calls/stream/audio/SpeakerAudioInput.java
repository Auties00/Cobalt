package com.github.auties00.cobalt.calls.stream.audio;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioInput;

/**
 * Plays the remote audio of a call through the operating system's speaker.
 *
 * <p>This is the device backed {@link AudioInput} returned by {@link AudioInput#toSpeaker()}. Its
 * constructor acquires a {@link SourceDataLine} for signed 16 bit mono PCM, defaulting to the 16 kHz call
 * media rate, opens it, and starts it. Each {@link #offerAudio(AudioFrame)} converts the frame's samples to
 * little endian bytes and pushes them to the line, blocking while the line's buffer is full so playback
 * paces the call. {@link #shutdown()} drains and releases the line. The stream is mono only; the operating
 * system spreads the single channel across whatever channels the physical speaker has.
 *
 * @implNote This implementation renders only what the call decoder emits at the line's sample rate; a
 * decoder that emits at a different rate must be resampled before reaching the stream, otherwise audio
 * plays at the wrong pitch.
 */
public final class SpeakerAudioInput implements AudioInput {
    /**
     * The logger for {@link SpeakerAudioInput}.
     */
    private static final System.Logger LOGGER = Log.get(SpeakerAudioInput.class);

    /**
     * Names the default playback sample rate, matching the WhatsApp call media profile of 16 kHz.
     */
    private static final int DEFAULT_SAMPLE_RATE = 16_000;

    /**
     * Guards {@link #shutdown()} so the line is released at most once.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Released by {@link #shutdown()} so a pending {@link #readAudio()} returns.
     */
    private final CountDownLatch done = new CountDownLatch(1);

    /**
     * Holds the opened playback line, cleared to {@code null} by {@link #shutdown()}.
     */
    private volatile SourceDataLine line;

    /**
     * Holds the reusable little endian byte scratch that one frame's samples are encoded into before being
     * written to the line, grown on demand when a larger frame arrives.
     *
     * <p>Confined to the single call render thread that drives {@link #offerAudio(AudioFrame)}, so reuse across
     * frames is race free.
     */
    private byte[] scratch;

    /**
     * Holds the {@link ShortBuffer} view over {@link #scratch}, rebuilt whenever {@link #scratch} is grown,
     * so each frame encodes by rewinding this view instead of wrapping a fresh buffer.
     */
    private ShortBuffer scratchView;

    /**
     * Opens the default profile speaker on the JVM default output device and starts playback.
     *
     * @throws IllegalStateException if no playback line is available on the running platform
     */
    public SpeakerAudioInput() {
        this(DEFAULT_SAMPLE_RATE, null);
    }

    /**
     * Opens a speaker with an explicit sample rate and output device and starts playback.
     *
     * <p>Acquires a signed 16 bit mono line at the configured sample rate, from the preferred mixer when
     * one was supplied or the JVM default device otherwise, opens it, and starts it so offered frames play
     * immediately.
     *
     * @param sampleRate     the playback sample rate in hertz; must be at least 1
     * @param preferredMixer the mixer to acquire the line from, or {@code null} for the JVM default output
     *                       device
     * @throws IllegalArgumentException if {@code sampleRate} is less than 1
     * @throws IllegalStateException    if no compatible line is available on the running platform
     * @implNote This implementation sizes the line buffer at one tenth of a second of samples, that is
     * {@code sampleRate / 10} frames of two bytes each, trading a small latency floor for resilience to
     * scheduling jitter on the offering thread.
     */
    public SpeakerAudioInput(int sampleRate, Mixer.Info preferredMixer) {
        if (sampleRate < 1) {
            throw new IllegalArgumentException("sampleRate must be >= 1");
        }
        try {
            this.line = openLine(sampleRate, preferredMixer);
        } catch (LineUnavailableException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot open speaker playback line", e);
            throw new IllegalStateException("cannot open speaker", e);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "speaker playback opened: sampleRate={0}", sampleRate);
    }

    /**
     * Acquires, opens, and starts a playback line matching the requested format.
     *
     * @param sampleRate     the playback sample rate in hertz
     * @param preferredMixer the mixer to acquire the line from, or {@code null} for the JVM default output
     *                       device
     * @return the started playback line
     * @throws LineUnavailableException if no compatible line is available on the running platform
     * @implNote This implementation opens the line at the call media rate and relies on
     * {@code javax.sound.sampled} to convert to the device's native rate implicitly, rather than opening the
     * line at the device's native rate and resampling the decoder output itself. Relying on the JDK converter
     * is deliberate: it keeps this the pure Java, dependency free playback path and is sufficient for the
     * fixed call media profile. WhatsApp instead drives a WebRTC {@code PushSincResampler} (a windowed sinc
     * resampler) explicitly on both the playback path (after NetEq or the decoder) and, symmetrically, the
     * capture path (before the encoder); the ffmpeg backed path mirrors that with {@code libswresample}.
     * Porting that resampler here, to open the line at the device native rate and resample explicitly, would
     * gain deterministic resampling quality independent of the platform and JDK default converter, robustness
     * on a mixer that cannot open a line at the call rate at all, and fidelity parity with the ffmpeg and
     * WhatsApp paths; it is left unimplemented because the JDK conversion is good enough for this profile.
     */
    private static SourceDataLine openLine(int sampleRate, Mixer.Info preferredMixer)
            throws LineUnavailableException {
        var format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 1, 2, sampleRate, false);
        var info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine acquired;
        if (preferredMixer != null) {
            acquired = (SourceDataLine) AudioSystem.getMixer(preferredMixer).getLine(info);
        } else {
            acquired = (SourceDataLine) AudioSystem.getLine(info);
        }
        acquired.open(format, sampleRate / 10 * 2);
        acquired.start();
        return acquired;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Converts the frame's samples to little endian bytes and writes them to the playback line,
     * blocking while the line buffer is full so playback paces the producer. A frame offered after
     * {@link #shutdown()} is dropped, and any line write failure is swallowed so a transient device fault
     * does not stall the call.
     *
     * @param frame {@inheritDoc}
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    @Override
    public void offerAudio(AudioFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        if (closed.get()) {
            return;
        }
        var l = line;
        if (l == null) {
            return;
        }
        var pcm = frame.pcm();
        var needed = pcm.length * 2;
        var bytes = scratch;
        if (bytes == null || bytes.length < needed) {
            bytes = new byte[needed];
            scratch = bytes;
            scratchView = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        }
        scratchView.rewind();
        scratchView.put(pcm);
        try {
            l.write(bytes, 0, needed);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "speaker playback line write failed", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A speaker bound sink is not read from: this blocks until {@link #shutdown()} and then returns
     * {@code null}.
     *
     * @return {@code null} once the sink has been ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    @Override
    public AudioFrame readAudio() throws InterruptedException {
        done.await();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the sink ended, wakes a pending {@link #readAudio()}, then drains so buffered audio finishes
     * playing, stops the line, and closes it. Each device step ignores failures so a fault in one does not
     * leak the line, making the call idempotent.
     */
    @Override
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "speaker playback shutdown");
        done.countDown();
        var l = line;
        line = null;
        if (l == null) {
            return;
        }
        try {
            l.drain();
        } catch (Throwable t) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "speaker playback line drain failed", t);
        }
        try {
            l.stop();
        } catch (Throwable t) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "speaker playback line stop failed", t);
        }
        try {
            l.close();
        } catch (Throwable t) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "speaker playback line close failed", t);
        }
    }
}
