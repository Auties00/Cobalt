package com.github.auties00.cobalt.calls.platform.audio;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Production {@link AudioCaptureDriver} backed by a {@code javax.sound.sampled.TargetDataLine}.
 *
 * <p>Binds a platform capture line in {@link #init(String, int, int, int, AudioDeviceType)}, starts a
 * virtual capture thread in {@link #start()} that reads fixed size blocks of signed 16 bit little endian
 * PCM from the line and forwards each to the installed {@link CapturedAudioSink}, and stops the thread
 * and line in {@link #stop()}. The {@link AudioDriverState} machine is the single source of truth for
 * which lifecycle calls are legal.
 *
 * <p>The lifecycle methods are guarded by an intrinsic lock so the owning service can drive them without
 * external synchronization; the capture thread reads the line and the volatile state without holding
 * that lock so it observes a {@link #stop()} promptly. Sample delivery to the sink happens on the
 * capture thread, off the lock.
 *
 * @implNote This implementation reads the platform capture line with a blocking {@code TargetDataLine}
 * read on a virtual thread rather than polling a capture ring: the operating system line supplies the
 * back pressure, so no ring buffer or clock skew compensation loop is needed. The captured blocks carry
 * signed 16 bit PCM, the format the sink consumes.
 */
public final class LiveAudioCaptureDriver implements AudioCaptureDriver {
    /**
     * The logger for {@link LiveAudioCaptureDriver}.
     */
    private static final System.Logger LOGGER = Log.get(LiveAudioCaptureDriver.class);

    /**
     * Guards the lifecycle transitions and the line and thread fields against concurrent driver calls.
     */
    private final Object lock;

    /**
     * Holds the current lifecycle state; volatile so the capture thread observes a stop without the lock.
     */
    private volatile AudioDriverState state;

    /**
     * Holds the kind of endpoint this driver captures from, set at initialization.
     */
    private volatile AudioDeviceType deviceType;

    /**
     * Holds the sink that receives captured blocks, or {@code null} when none is installed.
     */
    private volatile CapturedAudioSink sink;

    /**
     * Holds the bound capture line, or {@code null} when uninitialized.
     */
    private TargetDataLine line;

    /**
     * Holds the recorded number of samples consumed per read.
     */
    private int framesPerBuffer;

    /**
     * Holds the recorded number of captured channels.
     */
    private int channelCount;

    /**
     * Holds the running capture thread, or {@code null} when not capturing.
     */
    private Thread captureThread;

    /**
     * Constructs an uninitialized capture driver bound to no device.
     *
     * <p>The driver starts in {@link AudioDriverState#UNINITIALIZED} with the
     * {@link AudioDeviceType#MICROPHONE} default device type; both are set definitively by the first
     * {@link #init(String, int, int, int, AudioDeviceType)}.
     */
    public LiveAudioCaptureDriver() {
        this.lock = new Object();
        this.state = AudioDriverState.UNINITIALIZED;
        this.deviceType = AudioDeviceType.MICROPHONE;
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
     * @return {@inheritDoc}
     */
    @Override
    public AudioDeviceType deviceType() {
        return deviceType;
    }

    /**
     * {@inheritDoc}
     *
     * @param sink {@inheritDoc}
     */
    @Override
    public void onCapturedAudio(CapturedAudioSink sink) {
        this.sink = sink;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Acquires a {@link TargetDataLine} matching the requested signed 16 bit format and opens it with
     * a buffer holding several blocks. If the driver is already {@linkplain AudioDriverState#INITIALIZED
     * initialized} the previously bound line is closed before the new one is opened.
     *
     * @param deviceId        {@inheritDoc}
     * @param sampleRate      {@inheritDoc}
     * @param framesPerBuffer {@inheritDoc}
     * @param channelCount    {@inheritDoc}
     * @param deviceType      {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IllegalStateException    {@inheritDoc}
     */
    @Override
    public void init(String deviceId, int sampleRate, int framesPerBuffer, int channelCount, AudioDeviceType deviceType) {
        Objects.requireNonNull(deviceType, "deviceType cannot be null");
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
                throw new IllegalStateException("cannot initialize an active capture driver; stop it first");
            }
            closeLineLocked();
            TargetDataLine opened;
            try {
                opened = openLine(deviceId, sampleRate, framesPerBuffer, channelCount);
            } catch (LineUnavailableException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot open capture device " + deviceId, e);
                throw new IllegalStateException("cannot open capture device", e);
            }
            this.line = opened;
            this.framesPerBuffer = framesPerBuffer;
            this.channelCount = channelCount;
            this.deviceType = deviceType;
            this.state = AudioDriverState.INITIALIZED;
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "audio capture initialized: rate={0} frames={1} channels={2} type={3}",
                        sampleRate, framesPerBuffer, channelCount, deviceType);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Starts the bound line and spawns the virtual capture thread that drains it block by block.
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
                if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot start capture device", e);
                throw new IllegalStateException("cannot start capture device", e);
            }
            this.state = AudioDriverState.ACTIVE;
            this.captureThread = Thread.ofVirtual()
                    .name("calls-audio-capture")
                    .start(this::captureLoop);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio capture started: type={0}", deviceType);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Flips the state to {@link AudioDriverState#INITIALIZED}, stops the line so a blocked read
     * returns, and joins the capture thread so no further blocks are delivered after this returns.
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
                    line.stop();
                } catch (Throwable _) {
                }
                try {
                    line.flush();
                } catch (Throwable _) {
                }
            }
            toJoin = captureThread;
            captureThread = null;
        }
        joinQuietly(toJoin);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio capture stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceType {@inheritDoc}
     * @throws NullPointerException  {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void setDeviceType(AudioDeviceType deviceType) {
        Objects.requireNonNull(deviceType, "deviceType cannot be null");
        synchronized (lock) {
            if (state == AudioDriverState.ACTIVE) {
                throw new IllegalStateException("cannot change device type while capture is active");
            }
            this.deviceType = deviceType;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio capture device type set to {0}", deviceType);
        }
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
                    line.stop();
                } catch (Throwable _) {
                }
            }
            toJoin = captureThread;
            captureThread = null;
        }
        joinQuietly(toJoin);
        synchronized (lock) {
            closeLineLocked();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio capture driver closed");
    }

    /**
     * Drains the capture line block by block and forwards each captured block to the installed sink.
     *
     * <p>Runs on the virtual capture thread. Each iteration reads exactly one block of
     * {@code framesPerBuffer * channelCount} samples, decodes the little endian bytes to PCM, and hands
     * them to the {@link CapturedAudioSink} when one is installed. The loop exits when the driver leaves
     * {@link AudioDriverState#ACTIVE}, when the line reports end of input, or when the thread is
     * interrupted. A reused sample buffer is passed to the sink, whose contract is to copy if it retains.
     */
    private void captureLoop() {
        var l = line;
        if (l == null) {
            return;
        }
        var bytesPerSample = 2;
        var blockSamples = framesPerBuffer * channelCount;
        var readBuffer = new byte[blockSamples * bytesPerSample];
        var pcm = new short[blockSamples];
        var shortView = ByteBuffer.wrap(readBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        while (state == AudioDriverState.ACTIVE) {
            var total = 0;
            var ended = false;
            while (total < readBuffer.length) {
                int n;
                try {
                    n = l.read(readBuffer, total, readBuffer.length - total);
                } catch (RuntimeException e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "capture line read failed", e);
                    ended = true;
                    break;
                }
                if (n < 0) {
                    ended = true;
                    break;
                }
                total += n;
                if (state != AudioDriverState.ACTIVE) {
                    ended = true;
                    break;
                }
            }
            if (ended || total < readBuffer.length) {
                break;
            }
            shortView.rewind();
            shortView.get(pcm);
            var s = sink;
            if (s != null) {
                try {
                    s.onCapturedAudio(pcm, deviceType);
                } catch (RuntimeException e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "captured audio sink failed", e);
                }
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio capture loop exited");
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
     * Acquires, opens, and prepares a capture line matching the requested format.
     *
     * <p>Builds a signed 16 bit little endian {@link AudioFormat} for the requested rate and channels,
     * resolves a {@link TargetDataLine} for it from the named platform mixer (or the default device when
     * {@code deviceId} is {@code null}), and opens it with a buffer holding four blocks for resilience to
     * scheduling jitter. The line is opened but not started; {@link #start()} starts it.
     *
     * @param deviceId        the platform mixer name, or {@code null} for the default device
     * @param sampleRate      the capture sample rate in hertz
     * @param framesPerBuffer the number of samples per read
     * @param channelCount    the number of channels to capture
     * @return the opened, not yet started capture line
     * @throws LineUnavailableException if no line compatible with the requested format is available
     */
    private static TargetDataLine openLine(String deviceId, int sampleRate, int framesPerBuffer, int channelCount)
            throws LineUnavailableException {
        var frameBytes = 2 * channelCount;
        var format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, channelCount, frameBytes, sampleRate, false);
        var info = new DataLine.Info(TargetDataLine.class, format);
        var acquired = (TargetDataLine) resolveLine(deviceId, info);
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
     * @return the resolved, not yet opened line
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
     * {@code null} thread returns immediately. An interruption while joining re sets the interrupt flag
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
