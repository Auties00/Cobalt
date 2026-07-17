package com.github.auties00.cobalt.calls.media.audio.processing;

import com.github.auties00.cobalt.calls.media.audio.processing.bindings.CobaltWebRtcApm;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.NativeLibLoader;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * Conditions one call's live microphone capture with the WebRTC Audio Processing Module (AEC3, the WebRTC
 * noise suppressor including its ML denoiser, and the WebRTC gain controller) before the codec encodes it.
 *
 * <p>WhatsApp conditions live capture audio with the WebRTC APM, an echo canceller over WebRTC AEC3, a
 * noise suppressor with the ML denoiser, and a gain controller, and not with SpeexDSP. The recovered
 * {@code voip_settings} set {@code aec.algorithm="aec"}, {@code aec.mode="2"} (AEC3), {@code ns.enable="1"},
 * {@code ns.use_denoiser="true"}, and {@code ns.denoiser_intensity="0.55"}. This processor is the
 * conditioner the live capture path uses once the native WebRTC APM is present.
 *
 * <p>An instance owns one native WebRTC APM handle pinned to the fixed call audio geometry of
 * {@value #SAMPLE_RATE_HZ} Hz mono. The WebRTC APM processes one 10 ms frame ({@value #APM_FRAME_SAMPLES}
 * samples at {@value #SAMPLE_RATE_HZ} Hz) per call, while the capture pump delivers one
 * {@value #BLOCK_SAMPLES} sample (20 ms) block per tick, so each {@link #process(short[], short[], short[])}
 * splits the block into {@value #FRAMES_PER_BLOCK} successive 10 ms frames, supplies the far end reference
 * frame to the echo canceller through {@code ProcessReverseStream}, and conditions the near end frame
 * through {@code ProcessStream}. The 16 bit PCM the call path carries is converted to and from the APM's
 * {@code float32} samples (normalised to {@code [-1, 1]}) inside this processor; this conversion is internal
 * to the conditioner and is not the {@code float32} capture and playback boundary of the FFM audio driver.
 *
 * <p>It is single writer: {@link #process(short[], short[], short[])} must be driven from one thread (the
 * capture pump's), and it carries no internal lock. This conditioning is warranted only for a true live
 * acoustic capture ({@link com.github.auties00.cobalt.calls.stream.AudioOutput#isLiveCapture()}); a clean
 * line level source (a media file, a synthetic tone, silence, or application written frames) is already
 * conditioned and must bypass this processor, since running an echo canceller, a denoiser, and a gain
 * controller over clean audio distorts it.
 *
 * @implNote This implementation wraps the {@code cobalt_webrtc_apm_*} shim of the
 * {@code webrtc-audio-processing} archive through {@link CobaltWebRtcApm}: it builds the APM with
 * {@link CobaltWebRtcApm#COBALT_APM_AEC_AEC3()} (the recovered {@code aec.mode="2"}), the ML denoiser at the
 * recovered intensity, and the gain controller, then per 20 ms block runs the two 10 ms APM frames in
 * order, each as {@code ProcessReverseStream} on the far end reference followed by {@code ProcessStream} on
 * the near end capture. The processor is constructed only when {@link #nativeApmAvailable()} is
 * {@code true}. The gain controller target and compression and the AEC3 internal mode constants are WebRTC
 * voip parameters configured inside the native APM from the values passed here; the only such value the
 * recovered {@code voip_settings} pin is the denoiser intensity, so the gain controller tuning is left at
 * the WebRTC APM defaults the shim applies.
 */
public final class WebRtcAudioProcessor implements AutoCloseable {
    /**
     * The logger for {@link WebRtcAudioProcessor}.
     */
    private static final System.Logger LOGGER = Log.get(WebRtcAudioProcessor.class);

    /**
     * The fixed call audio sample rate in hertz: 16 kHz mono, matching the call codec configuration and
     * the capture pump's sample rate.
     */
    public static final int SAMPLE_RATE_HZ = 16_000;

    /**
     * The WebRTC APM frame duration in milliseconds: {@code kChunkSizeMs}, the fixed chunk the APM
     * processes per call.
     */
    public static final int APM_FRAME_MILLIS = 10;

    /**
     * The WebRTC APM frame size in samples: a {@value #APM_FRAME_MILLIS} ms frame at
     * {@value #SAMPLE_RATE_HZ} Hz, which is 160.
     */
    public static final int APM_FRAME_SAMPLES = SAMPLE_RATE_HZ / 1000 * APM_FRAME_MILLIS;

    /**
     * The capture pump block duration in milliseconds delivered to {@link #process(short[], short[],
     * short[])}.
     *
     * <p>The capture pump drains one 20 ms block per tick (the media session's {@code AUDIO_FRAME_MILLIS}),
     * so each block spans two WebRTC APM frames.
     */
    public static final int BLOCK_MILLIS = 20;

    /**
     * The capture pump block size in samples: a {@value #BLOCK_MILLIS} ms block at
     * {@value #SAMPLE_RATE_HZ} Hz, which is 320 and equals the media session's {@code AUDIO_FRAME_SAMPLES}.
     */
    public static final int BLOCK_SAMPLES = SAMPLE_RATE_HZ / 1000 * BLOCK_MILLIS;

    /**
     * The number of WebRTC APM frames in one capture pump block: {@value #BLOCK_SAMPLES} divided by
     * {@value #APM_FRAME_SAMPLES}, which is 2.
     */
    public static final int FRAMES_PER_BLOCK = BLOCK_SAMPLES / APM_FRAME_SAMPLES;

    /**
     * The full scale value a 16 bit PCM sample is divided by to normalise it into the APM's
     * {@code [-1, 1]} float range, and multiplied by on the way back.
     *
     * @implNote This implementation uses {@code 32768.0f} ({@code 1 << 15}), the magnitude of the most
     * negative 16 bit sample, so the conversion is symmetric and never overflows on the round trip.
     */
    private static final float PCM_FULL_SCALE = 32768.0f;

    /**
     * The default render to capture delay reported to the echo canceller, in milliseconds, when the
     * caller does not measure one.
     *
     * @implNote This implementation uses {@code 0}: the platform render to capture delay is a device
     * property the FFM audio driver measures, absent here, so the canceller is told zero and adapts from
     * the reference and capture streams alone. It is overridable through {@link #setStreamDelayMs(int)}
     * once a measured delay becomes available.
     */
    private static final int DEFAULT_STREAM_DELAY_MS = 0;

    /**
     * The configuration this processor applies to the WebRTC APM, sourced from the recovered
     * {@code voip_settings}.
     *
     * <p>Every field comes from a recovered {@code voip_settings} value or its compiled default; the
     * processor does not invent enablement.
     *
     * @param aecMode             the echo canceller selection: {@link CobaltWebRtcApm#COBALT_APM_AEC_OFF()},
     *                            {@link CobaltWebRtcApm#COBALT_APM_AEC_MOBILE()}, or
     *                            {@link CobaltWebRtcApm#COBALT_APM_AEC_AEC3()}
     * @param noiseSuppression    whether the noise suppressor is enabled (recovered {@code ns.enable="1"})
     * @param useDenoiser         whether noise suppression runs through the ML denoiser (recovered
     *                            {@code ns.use_denoiser="true"})
     * @param denoiserIntensity   the ML denoiser intensity in {@code [0, 1]} (recovered
     *                            {@code ns.denoiser_intensity="0.55"}); applied only when
     *                            {@code useDenoiser} is {@code true}
     * @param automaticGainControl whether the WebRTC gain controller is enabled
     */
    public record Config(int aecMode,
                         boolean noiseSuppression,
                         boolean useDenoiser,
                         float denoiserIntensity,
                         boolean automaticGainControl) {
        /**
         * Validates the configuration, rejecting an unknown echo canceller mode or an out of range denoiser
         * intensity.
         *
         * @throws IllegalArgumentException if {@code aecMode} is not one of the
         *                                  {@link CobaltWebRtcApm#COBALT_APM_AEC_OFF()},
         *                                  {@link CobaltWebRtcApm#COBALT_APM_AEC_MOBILE()},
         *                                  {@link CobaltWebRtcApm#COBALT_APM_AEC_AEC3()} selectors, or if
         *                                  {@code denoiserIntensity} is outside {@code [0, 1]}
         */
        public Config {
            if (aecMode != CobaltWebRtcApm.COBALT_APM_AEC_OFF() && aecMode != CobaltWebRtcApm.COBALT_APM_AEC_MOBILE()
                    && aecMode != CobaltWebRtcApm.COBALT_APM_AEC_AEC3()) {
                throw new IllegalArgumentException("unknown aecMode: " + aecMode);
            }
            if (denoiserIntensity < 0.0f || denoiserIntensity > 1.0f) {
                throw new IllegalArgumentException("denoiserIntensity must be in [0, 1]: " + denoiserIntensity);
            }
        }

        /**
         * Returns the configuration recovered from the {@code voip_settings}: AEC3, noise suppression
         * through the ML denoiser at intensity {@code 0.55}, and the gain controller enabled.
         *
         * @return the recovered live capture conditioning configuration
         */
        public static Config captured() {
            return new Config(CobaltWebRtcApm.COBALT_APM_AEC_AEC3(), true, true, 0.55f, true);
        }

        /**
         * Returns the configuration that applies echo cancellation and noise suppression only: AEC3 and
         * noise suppression through the ML denoiser at intensity {@code 0.55}, with the gain controller
         * disabled.
         *
         * <p>This is {@link #captured()} with automatic gain control turned off, so the conditioner cancels
         * the far end echo and suppresses ambient noise but leaves the captured level untouched. It is the
         * conditioning the live microphone capture path plugs in: the codec and the peer receive the cleaned
         * signal at its natural level, so an embedder or the remote side that already applies its own gain
         * staging is not fighting a second, engine side gain controller.
         *
         * @return the echo cancellation and noise suppression conditioning configuration with gain control
         *         disabled
         */
        public static Config aecAndNoiseSuppression() {
            return new Config(CobaltWebRtcApm.COBALT_APM_AEC_AEC3(), true, true, 0.55f, false);
        }
    }

    /**
     * Per instance arena owning the native APM frame scratch buffers.
     */
    private final Arena arena;

    /**
     * The configuration this processor applies, retained for {@link #config()}.
     */
    private final Config config;

    /**
     * Pointer to the native WebRTC APM instance, or {@link MemorySegment#NULL} once closed.
     */
    private MemorySegment apm;

    /**
     * Reusable native buffer one 10 ms near end capture frame is converted into and conditioned in place.
     */
    private final MemorySegment captureFrame;

    /**
     * Reusable native buffer one 10 ms far end reference frame is converted into.
     */
    private final MemorySegment referenceFrame;

    /**
     * Constructs an audio processor over a fresh WebRTC APM instance configured for the call audio
     * geometry.
     *
     * <p>Builds the APM with the {@link Config}'s echo canceller mode, noise suppressor, ML denoiser
     * intensity, and gain controller. If the native create fails the arena is released before the
     * exception propagates.
     *
     * @param config the capture conditioning configuration
     * @throws NullPointerException        if {@code config} is {@code null}
     * @throws IllegalStateException       if the WebRTC APM shim is not
     *                                     {@linkplain #nativeApmAvailable() linked into cobalt-native}
     * @throws WhatsAppCallException.AudioProcessing if the native APM instance could not be created
     */
    public WebRtcAudioProcessor(Config config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        // TODO: build the webrtc audio processing archive into cobalt native so the shim symbols resolve; until then they are absent, this processor cannot be constructed, and the media plane leaves the live capture unconditioned
        if (!nativeApmAvailable()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "webrtc apm shim unavailable, live capture stays unconditioned");
            throw new IllegalStateException(
                    "WebRTC APM shim is not available in cobalt-native; live capture stays unconditioned");
        }
        this.arena = Arena.ofShared();
        try {
            // TODO: feed capture and render frames through SplittingFilter.analysis() before the band limited APM stages and synthesis() after, once a pure Java APM or 48 kHz super wideband path is brought up
            this.apm = CobaltWebRtcApm.cobalt_webrtc_apm_create(config.aecMode(), config.noiseSuppression() ? 1 : 0,
                    config.useDenoiser() ? 1 : 0, config.denoiserIntensity(),
                    config.automaticGainControl() ? 1 : 0);
            if (apm.equals(MemorySegment.NULL)) {
                throw new WhatsAppCallException.AudioProcessing("cobalt_webrtc_apm_create returned NULL");
            }
            CobaltWebRtcApm.cobalt_webrtc_apm_set_stream_delay_ms(apm, DEFAULT_STREAM_DELAY_MS);
            this.captureFrame = arena.allocate((long) APM_FRAME_SAMPLES * Float.BYTES);
            this.referenceFrame = arena.allocate((long) APM_FRAME_SAMPLES * Float.BYTES);
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "webrtc apm create failed", e);
            destroyApm();
            arena.close();
            throw e;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "webrtc apm created aecMode={0} ns={1} denoiser={2} agc={3}",
                    config.aecMode(), config.noiseSuppression(), config.useDenoiser(), config.automaticGainControl());
        }
    }

    /**
     * Returns the configuration this processor applies.
     *
     * @return the capture conditioning configuration
     */
    public Config config() {
        return config;
    }

    /**
     * Returns the per block sample count this processor conditions, so a caller can validate its block
     * size against the processor's block.
     *
     * @return the block size in samples, {@value #BLOCK_SAMPLES}
     */
    public int blockSamples() {
        return BLOCK_SAMPLES;
    }

    /**
     * Conditions one captured 20 ms block: cancels the far end echo against the supplied reference and
     * runs the noise suppressor and gain controller, returning the cleaned block.
     *
     * <p>The near end {@code capture} and the far end {@code reference} are both one
     * {@value #BLOCK_SAMPLES} sample 16 kHz mono block; {@code out} receives the conditioned block and may
     * be the same array as {@code capture} for in place conditioning. The block is split into
     * {@value #FRAMES_PER_BLOCK} successive 10 ms WebRTC APM frames; for each frame the far end reference
     * is supplied to the echo canceller through {@code ProcessReverseStream} and the near end frame is
     * conditioned through {@code ProcessStream}. The far end reference is the block most recently rendered
     * to the local speaker, which the caller tracks.
     *
     * @param capture   the near end captured block, {@value #BLOCK_SAMPLES} samples; never {@code null}
     * @param reference the far end reference block most recently played, {@value #BLOCK_SAMPLES} samples;
     *                  never {@code null}
     * @param out       the destination for the conditioned block, {@value #BLOCK_SAMPLES} samples; never
     *                  {@code null}, may alias {@code capture}
     * @throws NullPointerException        if {@code capture}, {@code reference}, or {@code out} is
     *                                     {@code null}
     * @throws IllegalArgumentException    if any array is shorter than {@value #BLOCK_SAMPLES} samples
     * @throws IllegalStateException       if this processor has been closed
     * @throws WhatsAppCallException.AudioProcessing if a native WebRTC APM call fails
     */
    public void process(short[] capture, short[] reference, short[] out) {
        Objects.requireNonNull(capture, "capture cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");
        Objects.requireNonNull(out, "out cannot be null");
        requireOpen();
        if (capture.length < BLOCK_SAMPLES || reference.length < BLOCK_SAMPLES || out.length < BLOCK_SAMPLES) {
            throw new IllegalArgumentException(
                    "capture, reference, and out must each hold at least " + BLOCK_SAMPLES + " samples");
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "webrtc apm process block samples={0}", BLOCK_SAMPLES);
        for (var frame = 0; frame < FRAMES_PER_BLOCK; frame++) {
            var offset = frame * APM_FRAME_SAMPLES;
            pcmToFloat(reference, offset, referenceFrame);
            apmCall("process_reverse", CobaltWebRtcApm.cobalt_webrtc_apm_process_reverse(apm, referenceFrame));
            pcmToFloat(capture, offset, captureFrame);
            apmCall("process", CobaltWebRtcApm.cobalt_webrtc_apm_process(apm, captureFrame));
            floatToPcm(captureFrame, out, offset);
        }
    }

    /**
     * Reports the running render to capture delay, in milliseconds, to the echo canceller.
     *
     * <p>Invoked when the platform measures a fresh device latency so AEC3 can realign the reference and
     * capture streams. Does nothing once the processor has been closed.
     *
     * @param delayMs the render to capture delay in milliseconds; non negative
     * @throws IllegalArgumentException    if {@code delayMs} is negative
     * @throws WhatsAppCallException.AudioProcessing if the native call fails
     */
    public void setStreamDelayMs(int delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must not be negative: " + delayMs);
        }
        if (apm.equals(MemorySegment.NULL)) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "webrtc apm stream delay updated delayMs={0}", delayMs);
        apmCall("set_stream_delay_ms", CobaltWebRtcApm.cobalt_webrtc_apm_set_stream_delay_ms(apm, delayMs));
    }

    /**
     * Releases the native WebRTC APM instance.
     *
     * <p>Idempotent: a second invocation does nothing. After it runs {@link #process(short[], short[],
     * short[])} fails fast with an {@link IllegalStateException}.
     */
    @Override
    public void close() {
        if (apm.equals(MemorySegment.NULL)) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "webrtc apm closed");
        destroyApm();
        arena.close();
    }

    /**
     * Converts one 10 ms frame of 16 bit PCM samples into the APM's {@code float32} range and writes it
     * into the native frame buffer.
     *
     * @param pcm    the source PCM block
     * @param offset the sample offset within {@code pcm} of this 10 ms frame
     * @param dest   the native {@code float32} frame buffer to fill, {@value #APM_FRAME_SAMPLES} samples
     */
    private static void pcmToFloat(short[] pcm, int offset, MemorySegment dest) {
        for (var i = 0; i < APM_FRAME_SAMPLES; i++) {
            dest.setAtIndex(ValueLayout.JAVA_FLOAT, i, pcm[offset + i] / PCM_FULL_SCALE);
        }
    }

    /**
     * Converts one 10 ms {@code float32} frame back into 16 bit PCM samples, clamping to the 16 bit range,
     * and writes it into the output block.
     *
     * @param src    the native {@code float32} frame buffer, {@value #APM_FRAME_SAMPLES} samples
     * @param out    the destination PCM block
     * @param offset the sample offset within {@code out} of this 10 ms frame
     */
    private static void floatToPcm(MemorySegment src, short[] out, int offset) {
        for (var i = 0; i < APM_FRAME_SAMPLES; i++) {
            var sample = Math.round(src.getAtIndex(ValueLayout.JAVA_FLOAT, i) * PCM_FULL_SCALE);
            out[offset + i] = (short) Math.clamp(sample, Short.MIN_VALUE, Short.MAX_VALUE);
        }
    }

    /**
     * Verifies a WebRTC APM call returned the success code.
     *
     * @param call the call name, for the failure message
     * @param rc   the return code from the native call
     * @throws WhatsAppCallException.AudioProcessing if {@code rc} is not
     *         {@link CobaltWebRtcApm#COBALT_APM_OK()}
     */
    private static void apmCall(String call, int rc) {
        if (rc != CobaltWebRtcApm.COBALT_APM_OK()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "webrtc apm call {0} failed rc={1}", call, rc);
            throw new WhatsAppCallException.AudioProcessing("cobalt_webrtc_apm_" + call + " returned " + rc);
        }
    }

    /**
     * Destroys the native APM instance, tolerating an already released handle.
     *
     * <p>Clears the handle to {@link MemorySegment#NULL} so a second close does nothing.
     */
    private void destroyApm() {
        if (apm != null && !apm.equals(MemorySegment.NULL)) {
            CobaltWebRtcApm.cobalt_webrtc_apm_destroy(apm);
            apm = MemorySegment.NULL;
        }
    }

    /**
     * Returns whether the {@code cobalt_webrtc_apm_*} shim is linked into {@code cobalt-native}.
     *
     * <p>Loads {@code cobalt-native} (idempotent) and probes it for {@code cobalt_webrtc_apm_create}; when
     * the {@code webrtc-audio-processing} archive is not yet built into the combined library the symbol is
     * absent and this processor cannot be constructed, so the media plane leaves the live capture
     * unconditioned. Loading the library here also lets the {@link CobaltWebRtcApm} binding's own
     * {@linkplain java.lang.foreign.SymbolLookup#loaderLookup() loader lookup} resolve the same symbols on
     * the downcalls. Any failure to load or resolve is reported as unavailable rather than propagated.
     *
     * @return {@code true} when the WebRTC APM shim is callable in {@code cobalt-native}
     */
    public static boolean nativeApmAvailable() {
        try {
            return NativeLibLoader.load("cobalt-native", Arena.global())
                    .find("cobalt_webrtc_apm_create")
                    .isPresent();
        } catch (Throwable t) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "webrtc apm availability probe failed", t);
            return false;
        }
    }

    /**
     * Verifies the processor's native instance is still open.
     *
     * @throws IllegalStateException if this processor has been closed
     */
    private void requireOpen() {
        if (apm.equals(MemorySegment.NULL)) {
            throw new IllegalStateException("WebRtcAudioProcessor has been closed");
        }
    }
}
