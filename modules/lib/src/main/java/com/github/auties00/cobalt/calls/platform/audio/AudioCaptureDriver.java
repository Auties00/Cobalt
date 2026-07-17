package com.github.auties00.cobalt.calls.platform.audio;

/**
 * Controls one call audio capture endpoint: a microphone or a system audio loopback the engine reads
 * local audio from.
 *
 * <p>A driver owns one operating system capture device and runs the strict {@link AudioDriverState}
 * machine over it: {@link AudioDriverState#UNINITIALIZED} to {@link AudioDriverState#INITIALIZED} (after
 * {@link #init(String, int, int, int, AudioDeviceType)}), to {@link AudioDriverState#ACTIVE} (after
 * {@link #start()}), back to {@link AudioDriverState#INITIALIZED} (after {@link #stop()}). While
 * {@linkplain AudioDriverState#ACTIVE active} the driver reads signed 16 bit PCM from its device and
 * forwards each captured block to the {@link CapturedAudioSink} installed with
 * {@link #onCapturedAudio(CapturedAudioSink)}, which the engine wires to its encoder.
 *
 * <p>The captured format is fixed at initialization: a sample rate, a frame size (samples consumed per
 * read, a 10 ms or 20 ms block at that rate), and a channel count. The {@link AudioDeviceType}
 * additionally tags whether the audio is live microphone capture (needing acoustic conditioning) or a
 * clean system audio loopback. The contract is single threaded for the lifecycle calls
 * ({@link #init(String, int, int, int, AudioDeviceType)}, {@link #start()}, {@link #stop()},
 * {@link #setDeviceType(AudioDeviceType)}, {@link #close()}): the owning service drives them in order,
 * while sample delivery to the sink runs on the driver's own capture thread.
 *
 * <p>This type is sealed and permits only its production implementation {@link LiveAudioCaptureDriver},
 * which backs onto a {@code javax.sound.sampled.TargetDataLine}. The reader thread pump that pulls fixed
 * size blocks lives in a separate platform service.
 *
 * @apiNote An embedder never implements this interface; the call engine constructs and drives a
 * {@link LiveAudioCaptureDriver}. The {@link #onCapturedAudio(CapturedAudioSink)} sink belongs to the
 * engine's encoder, not to application code.
 */
public sealed interface AudioCaptureDriver permits LiveAudioCaptureDriver {
    /**
     * Consumes one block of captured audio the driver read from its device.
     *
     * <p>This is the handoff for captured samples from the driver to the engine. The engine installs a
     * sink with {@link AudioCaptureDriver#onCapturedAudio(CapturedAudioSink)} and the driver invokes it
     * for every block it captures while {@linkplain AudioDriverState#ACTIVE active}, on the driver's
     * capture thread.
     */
    @FunctionalInterface
    interface CapturedAudioSink {
        /**
         * Accepts one captured block of signed 16 bit PCM samples.
         *
         * <p>Invoked on the driver's capture thread once per read while the driver is
         * {@linkplain AudioDriverState#ACTIVE active}. The {@code samples} array is owned by the caller
         * and reused across invocations, so an implementation that retains the data copies it; the
         * length is the driver's configured frame size times its channel count. The {@code deviceType}
         * tags the source so the engine can route it and decide on acoustic conditioning.
         *
         * @param samples    the captured signed 16 bit PCM block; never {@code null}, reused across calls
         * @param deviceType the kind of endpoint the block was captured from; never {@code null}
         */
        void onCapturedAudio(short[] samples, AudioDeviceType deviceType);
    }

    /**
     * Returns the current lifecycle state of this driver.
     *
     * @return the driver state, never {@code null}
     */
    AudioDriverState state();

    /**
     * Returns the kind of endpoint this driver captures from.
     *
     * <p>The kind is set at {@linkplain #init(String, int, int, int, AudioDeviceType) initialization}
     * and may be changed with {@link #setDeviceType(AudioDeviceType)} while not
     * {@linkplain AudioDriverState#ACTIVE active}.
     *
     * @return the device type, never {@code null}
     */
    AudioDeviceType deviceType();

    /**
     * Installs the sink that receives captured audio blocks.
     *
     * <p>The sink is invoked for every block captured while the driver is
     * {@linkplain AudioDriverState#ACTIVE active}. Installing a new sink replaces any previous one;
     * passing {@code null} detaches the sink so captured blocks are dropped until one is reinstalled.
     * The sink may be changed at any state.
     *
     * @param sink the sink to receive captured blocks, or {@code null} to detach
     */
    void onCapturedAudio(CapturedAudioSink sink);

    /**
     * Binds a capture device and records the requested format, moving the driver to
     * {@link AudioDriverState#INITIALIZED}.
     *
     * <p>Opens the operating system capture device identified by {@code deviceId} (or the platform
     * default when it is {@code null}) for the requested {@code sampleRate}, {@code framesPerBuffer}, and
     * {@code channelCount}, and records the {@code deviceType}. Calling this in
     * {@link AudioDriverState#UNINITIALIZED} performs the first bind; calling it again in
     * {@link AudioDriverState#INITIALIZED} rebinds with the new parameters. The driver must not be
     * {@linkplain AudioDriverState#ACTIVE active}: an active driver is {@link #stop() stopped} first.
     *
     * @param deviceId        the platform device identifier, or {@code null} for the default capture
     *                        device
     * @param sampleRate      the capture sample rate in hertz; must be at least {@code 1}
     * @param framesPerBuffer the number of samples consumed per read (a 10 ms or 20 ms block); must be
     *                        at least {@code 1}
     * @param channelCount    the number of channels to capture; must be at least {@code 1}
     * @param deviceType      the kind of endpoint to capture from; never {@code null}
     * @throws NullPointerException     if {@code deviceType} is {@code null}
     * @throws IllegalArgumentException if {@code sampleRate}, {@code framesPerBuffer}, or
     *                                  {@code channelCount} is less than {@code 1}
     * @throws IllegalStateException    if the driver is {@linkplain AudioDriverState#ACTIVE active}, or no
     *                                  capture device matching the requested format is available
     */
    void init(String deviceId, int sampleRate, int framesPerBuffer, int channelCount, AudioDeviceType deviceType);

    /**
     * Begins capturing from the bound device, moving the driver to {@link AudioDriverState#ACTIVE}.
     *
     * <p>Starts the device and the capture thread that reads fixed size blocks and forwards them to the
     * installed {@link CapturedAudioSink}. The driver must be {@link AudioDriverState#INITIALIZED};
     * starting from {@link AudioDriverState#UNINITIALIZED} is the recoverable {@code Driver not
     * initialized} fault. Starting an active driver again is a no op.
     *
     * @throws IllegalStateException if the driver is {@linkplain AudioDriverState#UNINITIALIZED
     *                               uninitialized}, or the bound device cannot be started
     */
    void start();

    /**
     * Stops capturing, moving the driver from {@link AudioDriverState#ACTIVE} back to
     * {@link AudioDriverState#INITIALIZED}.
     *
     * <p>Stops the capture thread and the device but keeps the device bound so the driver can be
     * {@link #start() started} again without reinitializing. Stopping a driver that is not
     * {@linkplain AudioDriverState#ACTIVE active} is a no op.
     */
    void stop();

    /**
     * Changes the kind of endpoint this driver captures from.
     *
     * <p>Updates the {@link #deviceType()} used to tag captured blocks and to select the capture path on
     * the next {@link #start()}. The driver must not be {@linkplain AudioDriverState#ACTIVE active},
     * since changing the source kind of a running capture is undefined.
     *
     * @param deviceType the new device type; never {@code null}
     * @throws NullPointerException  if {@code deviceType} is {@code null}
     * @throws IllegalStateException if the driver is {@linkplain AudioDriverState#ACTIVE active}
     */
    void setDeviceType(AudioDeviceType deviceType);

    /**
     * Releases the bound device and renders the driver unusable.
     *
     * <p>Stops capture if active, closes the operating system device, and returns the driver to
     * {@link AudioDriverState#UNINITIALIZED}. The driver may be rebound with
     * {@link #init(String, int, int, int, AudioDeviceType)} afterward. This is idempotent and never
     * throws, so it is safe to call during a racing call teardown.
     */
    void close();
}
