package com.github.auties00.cobalt.calls.platform.audio;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Production {@link AudioPlaybackDriver} backed by a {@code javax.sound.sampled.SourceDataLine}.
 *
 * <p>Binds a platform playback line in {@link #init(String, int, int, int)}, starts a virtual playback
 * thread in {@link #start()} that pulls rendered audio from the installed {@link RenderedAudioSource} and
 * writes it to the line, and stops the thread and line in {@link #stop()}. The {@link AudioDriverState}
 * machine is the single source of truth for which lifecycle calls are legal.
 *
 * <p>Playback is demand driven by the device: each iteration the thread writes one block to the line,
 * which blocks while the line buffer is full, so the line itself paces the pull. When the source yields
 * fewer samples than the block size the driver zero fills the remainder and plays silence, so a starved
 * source does not desynchronize the device clock. The lifecycle methods are guarded by an intrinsic lock;
 * the playback thread reads the line and the volatile state without the lock so it observes a
 * {@link #stop()} promptly.
 *
 * @implNote This implementation paces the pull with a blocking {@link SourceDataLine#write(byte[], int, int)}
 * on a virtual thread rather than a condition variable: the full buffer block on the line supplies the
 * demand driven wake, so no futex or {@link java.util.concurrent.locks.LockSupport} condition is needed
 * inside the driver. The pulled samples are signed 16 bit PCM, the format the rendered source produces.
 */
public final class LiveAudioPlaybackDriver implements AudioPlaybackDriver {
    /**
     * The logger for {@link LiveAudioPlaybackDriver}.
     */
    private static final System.Logger LOGGER = Log.get(LiveAudioPlaybackDriver.class);

    /**
     * Guards the lifecycle transitions and the line and thread fields against concurrent driver calls.
     */
    private final Object lock;

    /**
     * Holds the current lifecycle state; volatile so the playback thread observes a stop without the lock.
     */
    private volatile AudioDriverState state;

    /**
     * Holds the source pulled for rendered audio, or {@code null} when none is installed.
     */
    private volatile RenderedAudioSource source;

    /**
     * Holds the bound playback line, or {@code null} when uninitialized.
     */
    private SourceDataLine line;

    /**
     * Holds the recorded playback sample rate in hertz.
     */
    private int sampleRate;

    /**
     * Holds the recorded number of samples pulled per demand.
     */
    private int framesPerBuffer;

    /**
     * Holds the recorded number of played channels.
     */
    private int channelCount;

    /**
     * Holds the running playback thread, or {@code null} when not playing.
     */
    private Thread playbackThread;

    /**
     * Constructs an uninitialized playback driver bound to no device.
     *
     * <p>The driver starts in {@link AudioDriverState#UNINITIALIZED}; the bound device and format are set
     * by the first {@link #init(String, int, int, int)}.
     */
    public LiveAudioPlaybackDriver() {
        this.lock = new Object();
        this.state = AudioDriverState.UNINITIALIZED;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public AudioDriverState state() {
        return state;
    }

    /**
     * {@inheritDoc}
     *
     * @param source {@inheritDoc}
     */
    @Override
    public void onRequestAudio(RenderedAudioSource source) {
        this.source = source;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Acquires a {@link SourceDataLine} matching the requested signed 16 bit format and opens it with
     * a buffer holding several blocks. If the driver is already {@linkplain AudioDriverState#INITIALIZED
     * initialized} the previously bound line is closed before the new one is opened.
     *
     * @param deviceId        {@inheritDoc}
     * @param sampleRate      {@inheritDoc}
     * @param framesPerBuffer {@inheritDoc}
     * @param channelCount    {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IllegalStateException    {@inheritDoc}
     */
    @Override
    public void init(String deviceId, int sampleRate, int framesPerBuffer, int channelCount) {
        if (sampleRate < 1) {
            throw new IllegalArgumentException("sampleRate must be >= 1");
        }
        if (framesPerBuffer < 1) {
            throw new IllegalArgumentException("framesPerBuffer must be >= 1");
        }
        if (channelCount < 1) {
            throw new IllegalArgumentException("channelCount must be >= 1");
        }
        synchronized (lock) {
            if (state == AudioDriverState.ACTIVE) {
                throw new IllegalStateException("cannot initialize an active playback driver; stop it first");
            }
            closeLineLocked();
            SourceDataLine opened;
            try {
                opened = openLine(deviceId, sampleRate, framesPerBuffer, channelCount);
            } catch (LineUnavailableException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot open playback device " + deviceId, e);
                throw new IllegalStateException("cannot open playback device", e);
            }
            this.line = opened;
            this.sampleRate = sampleRate;
            this.framesPerBuffer = framesPerBuffer;
            this.channelCount = channelCount;
            this.state = AudioDriverState.INITIALIZED;
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "audio playback initialized: rate={0} frames={1} channels={2}",
                        sampleRate, framesPerBuffer, channelCount);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Starts the bound line and spawns the virtual playback thread that pulls and writes block by
     * block.
     *
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void start() {
        synchronized (lock) {
            if (state == AudioDriverState.ACTIVE) {
                return;
            }
            if (state != AudioDriverState.INITIALIZED || line == null) {
                throw new IllegalStateException("Driver not initialized");
            }
            try {
                line.start();
            } catch (RuntimeException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot start playback device", e);
                throw new IllegalStateException("cannot start playback device", e);
            }
            this.state = AudioDriverState.ACTIVE;
            this.playbackThread = Thread.ofVirtual()
                    .name("calls-audio-playback")
                    .start(this::playbackLoop);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio playback started");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Flips the state to {@link AudioDriverState#INITIALIZED}, flushes and stops the line so a blocked
     * write returns, and joins the playback thread so no further samples are written after this returns.
     */
    @Override
    public void stop() {
        Thread toJoin;
        synchronized (lock) {
            if (state != AudioDriverState.ACTIVE) {
                return;
            }
            this.state = AudioDriverState.INITIALIZED;
            if (line != null) {
                try {
                    line.flush();
                } catch (Throwable _) {
                }
                try {
                    line.stop();
                } catch (Throwable _) {
                }
            }
            toJoin = playbackThread;
            playbackThread = null;
        }
        joinQuietly(toJoin);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio playback stopped");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        Thread toJoin;
        synchronized (lock) {
            this.state = AudioDriverState.UNINITIALIZED;
            if (line != null) {
                try {
                    line.flush();
                } catch (Throwable _) {
                }
                try {
                    line.stop();
                } catch (Throwable _) {
                }
            }
            toJoin = playbackThread;
            playbackThread = null;
        }
        joinQuietly(toJoin);
        synchronized (lock) {
            closeLineLocked();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio playback driver closed");
    }

    /**
     * Pulls rendered audio from the installed source and writes it to the playback line block by block.
     *
     * <p>Runs on the virtual playback thread. Each iteration asks the {@link RenderedAudioSource} for up
     * to {@code framesPerBuffer * channelCount} samples; the unfilled remainder is zero filled to silence
     * so a starved source does not desynchronize the device clock. The block is encoded to little endian
     * bytes and written to the line, whose full buffer block paces the loop. The loop exits when the
     * driver leaves {@link AudioDriverState#ACTIVE} or a line write fails.
     */
    private void playbackLoop() {
        var l = line;
        if (l == null) {
            return;
        }
        var blockSamples = framesPerBuffer * channelCount;
        var pcm = new short[blockSamples];
        var bytes = new byte[blockSamples * 2];
        var shortView = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        while (state == AudioDriverState.ACTIVE) {
            var produced = 0;
            var s = source;
            if (s != null) {
                try {
                    produced = s.requestAudio(pcm, blockSamples);
                } catch (RuntimeException e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "rendered audio source failed", e);
                    produced = 0;
                }
            }
            if (produced < 0) {
                produced = 0;
            } else if (produced > blockSamples) {
                produced = blockSamples;
            }
            if (produced < blockSamples) {
                Arrays.fill(pcm, produced, blockSamples, (short) 0);
            }
            shortView.rewind();
            shortView.put(pcm);
            if (state != AudioDriverState.ACTIVE) {
                break;
            }
            try {
                l.write(bytes, 0, bytes.length);
            } catch (RuntimeException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "playback line write failed", e);
                break;
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio playback loop exited");
    }

    /**
     * Closes and clears the bound line while holding the lock.
     *
     * <p>Stops and closes the line if one is bound, swallowing failures so a device fault does not leak
     * the line, then clears the field. The caller holds {@link #lock}.
     */
    private void closeLineLocked() {
        if (line != null) {
            try {
                line.stop();
            } catch (Throwable _) {
            }
            try {
                line.close();
            } catch (Throwable _) {
            }
            line = null;
        }
    }

    /**
     * Acquires, opens, and prepares a playback line matching the requested format.
     *
     * <p>Builds a signed 16 bit little endian {@link AudioFormat} for the requested rate and channels,
     * resolves a {@link SourceDataLine} for it from the named platform mixer (or the default device when
     * {@code deviceId} is {@code null}), and opens it with a buffer holding four blocks for resilience to
     * scheduling jitter. The line is opened but not started; {@link #start()} starts it.
     *
     * @param deviceId        the platform mixer name, or {@code null} for the default device
     * @param sampleRate      the playback sample rate in hertz
     * @param framesPerBuffer the number of samples per block
     * @param channelCount    the number of channels to play
     * @return the opened playback line, not yet started
     * @throws LineUnavailableException if no line compatible with the requested format is available
     */
    private static SourceDataLine openLine(String deviceId, int sampleRate, int framesPerBuffer, int channelCount)
            throws LineUnavailableException {
        var frameBytes = 2 * channelCount;
        var format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, channelCount, frameBytes, sampleRate, false);
        var info = new DataLine.Info(SourceDataLine.class, format);
        var acquired = (SourceDataLine) resolveLine(deviceId, info);
        acquired.open(format, framesPerBuffer * frameBytes * 4);
        return acquired;
    }

    /**
     * Resolves a line for the requested info from a named mixer or the default device.
     *
     * <p>When {@code deviceId} names a mixer present on the platform, the line is taken from that mixer;
     * otherwise the JVM default device is used. An unknown name falls back to the default rather than
     * failing, since device identifiers are not stable across hosts.
     *
     * @param deviceId the platform mixer name, or {@code null} for the default device
     * @param info     the data line info describing the requested format
     * @return the resolved line, not yet opened
     * @throws LineUnavailableException if no compatible line is available
     */
    private static javax.sound.sampled.Line resolveLine(String deviceId, DataLine.Info info)
            throws LineUnavailableException {
        if (deviceId != null) {
            for (var mixerInfo : AudioSystem.getMixerInfo()) {
                if (deviceId.equals(mixerInfo.getName())) {
                    var mixer = AudioSystem.getMixer(mixerInfo);
                    if (mixer.isLineSupported(info)) {
                        return mixer.getLine(info);
                    }
                }
            }
        }
        return AudioSystem.getLine(info);
    }

    /**
     * Joins a thread, ignoring an interruption.
     *
     * <p>Waits for the given thread to finish so the caller knows no further work runs on it; a
     * {@code null} thread returns immediately. An interruption while joining restores the interrupt flag
     * and returns rather than propagating, since teardown must complete.
     *
     * @param thread the thread to join, or {@code null}
     */
    private static void joinQuietly(Thread thread) {
        if (thread == null || thread == Thread.currentThread()) {
            return;
        }
        try {
            thread.join();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
