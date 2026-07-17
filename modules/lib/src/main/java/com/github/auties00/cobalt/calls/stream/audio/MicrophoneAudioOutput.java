package com.github.auties00.cobalt.calls.stream.audio;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioOutput;

/**
 * Captures the operating system microphone as the local audio of a call.
 *
 * <p>This is the device backed {@link AudioOutput} returned by {@link AudioOutput#fromMicrophone()}. It
 * opens a {@link TargetDataLine} on the configured (or JVM default) mixer in its constructor and reads
 * signed 16 bit little endian PCM from it. The default profile captures 16 kHz mono in 160 sample frames,
 * which is exactly the format the call media stack expects, so a default microphone needs no resampling
 * when the host supports the rate (most do). A custom
 * {@link #MicrophoneAudioOutput(int, int, Mixer.Info)} may request another rate, frame size, or specific
 * mixer; capturing at a rate other than 16 kHz still produces frames at the requested geometry but the
 * caller is then responsible for downsampling before the frames reach the call, as {@link AudioFrame}
 * documents.
 *
 * <p>Each {@link #takeAudio()} blocks until a full frame's worth of samples has been read from the line, then
 * returns it with a presentation timestamp that never decreases; this hardware blocking is what paces the
 * outbound stream to real time. {@link #shutdown()} releases the capture line, which unblocks a
 * {@link #takeAudio()} parked on the line so it returns {@code null}.
 *
 * @implNote This implementation captures a single channel intentionally, because most platforms expose
 * the default microphone as mono and downmix or select one channel of a stereo device. The timestamp it
 * advances on each frame is denominated in the {@link AudioFrame#ptsMicros()} microsecond clock.
 */
public final class MicrophoneAudioOutput implements AudioOutput {
    /**
     * The logger for {@link MicrophoneAudioOutput}.
     */
    private static final System.Logger LOGGER = Log.get(MicrophoneAudioOutput.class);

    /**
     * Holds the default capture sample rate, in Hz, matching the call media profile.
     *
     * @implNote This implementation uses 16000, the rate WhatsApp's Opus call configuration runs at, so
     * a default profile microphone feeds the encoder without resampling.
     */
    static final int DEFAULT_SAMPLE_RATE = 16_000;

    /**
     * Holds the default sample count per emitted frame.
     *
     * @implNote This implementation uses 160, which is 10 ms at {@link #DEFAULT_SAMPLE_RATE}, the frame
     * cadence the call layer consumes.
     */
    static final int DEFAULT_FRAME_SIZE = 160;

    /**
     * Guards {@link #shutdown()} so the capture line is released at most once.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Holds the sample rate, in Hz, of the underlying capture line.
     */
    private final int sampleRate;

    /**
     * Holds the number of samples in each emitted frame.
     */
    private final int frameSize;

    /**
     * Holds the duration of one frame in microseconds, derived from {@link #frameSize} and
     * {@link #sampleRate} and cached for presentation timestamp arithmetic.
     *
     * @implNote This implementation computes {@code 1_000_000 * frameSize / sampleRate} so the running
     * timestamp keeps microsecond resolution rather than rounding each frame to a whole millisecond.
     */
    private final long frameDurationMicros;

    /**
     * Holds the reusable byte buffer that one frame is read into before conversion to PCM samples.
     *
     * <p>Sized to {@code frameSize * 2} bytes (two bytes per signed 16 bit sample).
     */
    private final byte[] readBuffer;

    /**
     * Holds the little endian {@link ShortBuffer} view over {@link #readBuffer}, reused across
     * {@link #takeAudio()} calls so each frame decodes by rewinding this view instead of wrapping the byte
     * buffer anew.
     *
     * @implNote This implementation is confined to the single consumer thread that drains {@link #takeAudio()},
     * the same confinement {@link #readBuffer} already relies on, so reusing the view is race free.
     */
    private final ShortBuffer sampleView;

    /**
     * Holds the reusable PCM sample buffer each frame decodes into and is lent out over, reused across
     * {@link #takeAudio()} calls under the {@link AudioOutput#takeAudio()} borrow contract rather than allocated per
     * frame.
     *
     * <p>Confined to the single consumer thread that drains {@link #takeAudio()}, the same confinement
     * {@link #readBuffer} and {@link #sampleView} rely on. The engine's capture pump copies each frame's
     * samples into its ring before the next {@link #takeAudio()}, so lending one reused buffer is safe.
     */
    private final short[] pcmBuffer;

    /**
     * Holds the opened capture line, cleared to {@code null} by {@link #shutdown()}.
     */
    private volatile TargetDataLine line;

    /**
     * Holds the presentation timestamp, in microseconds, assigned to the next emitted frame.
     */
    private long ptsMicros;

    /**
     * Opens the default profile microphone and begins capturing.
     *
     * <p>Equivalent to {@link #MicrophoneAudioOutput(int, int, Mixer.Info)} with
     * {@link #DEFAULT_SAMPLE_RATE}, {@link #DEFAULT_FRAME_SIZE}, and a {@code null} mixer, so it captures
     * 16 kHz mono in 160 sample frames from the JVM default microphone.
     *
     * @throws IllegalStateException if no capture line is available on the running platform
     */
    public MicrophoneAudioOutput() {
        this(DEFAULT_SAMPLE_RATE, DEFAULT_FRAME_SIZE, null);
    }

    /**
     * Opens a microphone with an explicit capture format and mixer and begins capturing.
     *
     * <p>The frame duration is computed once as {@code 1_000_000 * frameSize / sampleRate} microseconds
     * and used to advance each frame's presentation timestamp. A {@link TargetDataLine} matching the
     * configured signed 16 bit mono format is acquired from the preferred mixer when one is set, or from
     * the JVM default device otherwise, opened with a buffer holding several frames, and started.
     *
     * @param sampleRate     the capture sample rate in Hz (for example {@code 16000} or {@code 48000})
     * @param frameSize      the number of samples per emitted frame
     * @param preferredMixer the specific mixer to open the line on, or {@code null} for the JVM default
     * @throws IllegalArgumentException if {@code sampleRate} or {@code frameSize} is less than {@code 1}
     * @throws IllegalStateException    if no line compatible with the requested format is available on
     *                                  the running platform, or the device is in use
     */
    public MicrophoneAudioOutput(int sampleRate, int frameSize, Mixer.Info preferredMixer) {
        if (sampleRate < 1) {
            throw new IllegalArgumentException("sampleRate must be >= 1");
        }
        if (frameSize < 1) {
            throw new IllegalArgumentException("frameSize must be >= 1");
        }
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        this.frameDurationMicros = 1_000_000L * frameSize / sampleRate;
        this.readBuffer = new byte[frameSize * 2];
        this.sampleView = ByteBuffer.wrap(readBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        this.pcmBuffer = new short[frameSize];
        try {
            this.line = openLine(sampleRate, frameSize, preferredMixer);
        } catch (LineUnavailableException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot open microphone capture line", e);
            throw new IllegalStateException("cannot open microphone", e);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "microphone capture opened: sampleRate={0} frameSize={1}",
                    sampleRate, frameSize);
        }
    }

    /**
     * Acquires, opens, and starts a capture line matching the requested format.
     *
     * @param sampleRate     the capture sample rate in Hz
     * @param frameSize      the number of samples per emitted frame
     * @param preferredMixer the specific mixer to open the line on, or {@code null} for the JVM default
     * @return the started capture line
     * @throws LineUnavailableException if no line compatible with the requested format is available on
     *                                  the running platform, or the device is in use
     */
    private static TargetDataLine openLine(int sampleRate, int frameSize, Mixer.Info preferredMixer)
            throws LineUnavailableException {
        var format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 1, 2, sampleRate, false);
        var info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine acquired;
        if (preferredMixer != null) {
            acquired = (TargetDataLine) AudioSystem.getMixer(preferredMixer).getLine(info);
        } else {
            acquired = (TargetDataLine) AudioSystem.getLine(info);
        }
        acquired.open(format, frameSize * 2 * 4);
        acquired.start();
        return acquired;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A microphone bound source fills itself from the capture line inside {@link #takeAudio()} and ignores
     * application writes, so this is a no op.
     *
     * @param frame the frame that would be written; ignored
     */
    @Override
    public void writeAudio(AudioFrame frame) {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads from the open capture line until a full frame's worth of bytes is buffered, decodes them as
     * little endian signed 16 bit samples into the reusable {@link #pcmBuffer}, and returns them with the
     * next presentation timestamp. Returns {@code null} once the line reports end of input, which is how
     * {@link #shutdown()} ends the stream by closing the line. The decoded samples are lent from
     * {@link #pcmBuffer} per the {@link AudioOutput#takeAudio()} borrow contract, so a caller that retains the
     * frame past the next call copies them out first.
     *
     * @return {@inheritDoc}
     * @throws InterruptedException if the calling thread is interrupted while reading the line
     */
    @Override
    public AudioFrame takeAudio() throws InterruptedException {
        var l = line;
        if (l == null) {
            return null;
        }
        var total = 0;
        while (total < readBuffer.length) {
            int n;
            try {
                n = l.read(readBuffer, total, readBuffer.length - total);
            } catch (RuntimeException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "microphone capture line read failed", e);
                return null;
            }
            if (n < 0) {
                return null;
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            total += n;
        }
        sampleView.rewind();
        sampleView.get(pcmBuffer);
        var pts = ptsMicros;
        ptsMicros += frameDurationMicros;
        return new AudioFrame(pcmBuffer, pts);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the source ended, then stops and closes the capture line so the device is released back to
     * the operating system and a {@link #takeAudio()} parked on a blocking read returns {@code null}. Any
     * failure while stopping or closing is swallowed, so the call is idempotent and never throws.
     */
    @Override
    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "microphone capture shutdown");
            var l = line;
            line = null;
            if (l != null) {
                try {
                    l.stop();
                } catch (Throwable t) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "microphone capture line stop failed", t);
                }
                try {
                    l.close();
                } catch (Throwable t) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "microphone capture line close failed", t);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @implNote This implementation returns {@code true}: the frames come straight off the
     * operating system capture line, so they carry acoustic echo and ambient noise and need the engine's
     * echo cancellation and microphone conditioning.
     */
    @Override
    public boolean isLiveCapture() {
        return true;
    }
}
