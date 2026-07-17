package com.github.auties00.cobalt.calls.platform.audio;

/**
 * Controls the call audio playback endpoint: the speaker the engine renders the mixed remote audio to.
 *
 * <p>A driver owns one operating system playback device and runs the same strict
 * {@link AudioDriverState} machine as the capture driver: {@link AudioDriverState#UNINITIALIZED} to
 * {@link AudioDriverState#INITIALIZED} (after {@link #init(String, int, int, int)}), to
 * {@link AudioDriverState#ACTIVE} (after {@link #start()}), then back to
 * {@link AudioDriverState#INITIALIZED} (after {@link #stop()}).
 *
 * <p>Playback is demand driven rather than pushed. While {@linkplain AudioDriverState#ACTIVE active} the
 * driver pulls rendered audio from the {@link RenderedAudioSource} installed with
 * {@link #onRequestAudio(RenderedAudioSource)} whenever its device needs more samples, and writes the
 * returned signed 16 bit PCM to the device. The host device sets the pace, asking the source for exactly
 * as many samples as it can play; when the source returns no samples the driver plays silence rather than
 * stalling.
 *
 * <p>The playback format is fixed at initialization: a sample rate, a frame size, and a channel count.
 * Unlike capture there is no device type distinction, because playback is always to the speaker. The
 * contract is single threaded for the lifecycle calls ({@link #init(String, int, int, int)},
 * {@link #start()}, {@link #stop()}, {@link #close()}): the owning service drives them in order; the pull
 * from the source runs on the driver's own playback thread.
 *
 * <p>This type is sealed and permits only its production implementation {@link LiveAudioPlaybackDriver},
 * which backs onto a {@code javax.sound.sampled.SourceDataLine}. The call engine constructs and drives
 * that implementation; the {@link #onRequestAudio(RenderedAudioSource)} source belongs to the engine's
 * decoder, not to application code.
 */
public sealed interface AudioPlaybackDriver permits LiveAudioPlaybackDriver {
    /**
     * Supplies rendered audio for the driver to play when its device demands more samples.
     *
     * <p>The engine installs a source with {@link AudioPlaybackDriver#onRequestAudio(RenderedAudioSource)}
     * and the driver invokes it on its playback thread whenever the device buffer needs filling while
     * {@linkplain AudioDriverState#ACTIVE active}.
     */
    @FunctionalInterface
    interface RenderedAudioSource {
        /**
         * Renders up to {@code frameCount} samples of mixed remote audio into {@code out}.
         *
         * <p>Invoked on the driver's playback thread when the device needs more samples. The
         * implementation writes signed 16 bit PCM into {@code out} starting at index {@code 0} and
         * returns the number of samples it produced, which may be fewer than {@code frameCount} (or
         * {@code 0}) when no audio is available; the driver plays silence for the unfilled remainder.
         *
         * @param out        the buffer to render signed 16 bit PCM into; never {@code null}, at least
         *                   {@code frameCount} long
         * @param frameCount the maximum number of samples to render
         * @return the number of samples actually rendered, between {@code 0} and {@code frameCount}
         */
        int requestAudio(short[] out, int frameCount);
    }

    /**
     * Returns the current lifecycle state of this driver.
     *
     * @return the driver state, never {@code null}
     */
    AudioDriverState state();

    /**
     * Installs the source that supplies rendered audio for playback.
     *
     * <p>The source is pulled whenever the device needs samples while the driver is
     * {@linkplain AudioDriverState#ACTIVE active}. Installing a new source replaces any previous one;
     * passing {@code null} detaches the source so the driver plays silence until one is reinstalled. The
     * source may be changed in any state.
     *
     * @param source the source to pull rendered audio from, or {@code null} to detach
     */
    void onRequestAudio(RenderedAudioSource source);

    /**
     * Binds a playback device and records the requested format, moving the driver to
     * {@link AudioDriverState#INITIALIZED}.
     *
     * <p>Opens the operating system playback device identified by {@code deviceId} (or the platform
     * default when it is {@code null}) for the requested {@code sampleRate}, {@code framesPerBuffer}, and
     * {@code channelCount}. Calling this in {@link AudioDriverState#UNINITIALIZED} performs the first
     * bind; calling it again in {@link AudioDriverState#INITIALIZED} rebinds with the new parameters.
     * The driver must not be {@linkplain AudioDriverState#ACTIVE active}: an active driver is
     * {@link #stop() stopped} first.
     *
     * @param deviceId        the platform device identifier, or {@code null} for the default playback
     *                        device
     * @param sampleRate      the playback sample rate in hertz; must be at least {@code 1}
     * @param framesPerBuffer the number of samples pulled per demand (a 10 ms or 20 ms block); must be
     *                        at least {@code 1}
     * @param channelCount    the number of channels to play; must be at least {@code 1}
     * @throws IllegalArgumentException if {@code sampleRate}, {@code framesPerBuffer}, or
     *                                  {@code channelCount} is less than {@code 1}
     * @throws IllegalStateException    if the driver is {@linkplain AudioDriverState#ACTIVE active}, or no
     *                                  playback device matching the requested format is available
     */
    void init(String deviceId, int sampleRate, int framesPerBuffer, int channelCount);

    /**
     * Begins playing to the bound device, moving the driver to {@link AudioDriverState#ACTIVE}.
     *
     * <p>Starts the device and the playback thread that pulls rendered audio from the installed
     * {@link RenderedAudioSource} on demand and writes it to the device. The driver must be
     * {@link AudioDriverState#INITIALIZED}. Starting an already {@linkplain AudioDriverState#ACTIVE
     * active} driver is a no op.
     *
     * @throws IllegalStateException if the driver is {@linkplain AudioDriverState#UNINITIALIZED
     *                               uninitialized}, or the bound device cannot be started
     */
    void start();

    /**
     * Stops playing, moving the driver from {@link AudioDriverState#ACTIVE} back to
     * {@link AudioDriverState#INITIALIZED}.
     *
     * <p>Stops the playback thread and the device but keeps the device bound so the driver can be
     * {@link #start() started} again without reinitializing. Stopping a driver that is not
     * {@linkplain AudioDriverState#ACTIVE active} is a no op.
     */
    void stop();

    /**
     * Releases the bound device and renders the driver unusable.
     *
     * <p>Stops playback if active, closes the operating system device, and returns the driver to
     * {@link AudioDriverState#UNINITIALIZED}. The driver may be rebound with
     * {@link #init(String, int, int, int)} afterward. This is idempotent and never throws, so it is safe
     * to call during a racing call teardown.
     */
    void close();
}
