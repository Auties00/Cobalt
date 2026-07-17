/*
 * cobalt_webrtc_apm_shim.h
 *
 * Portable extern-C facade over the WebRTC Audio Processing Module (the
 * webrtc-audio-processing standalone library: AEC3, the WebRTC noise suppressor
 * including its ML denoiser, and the WebRTC gain controller) for the Cobalt
 * calls audio-processing stack. It re-exposes only the APM surface the live
 * capture path uses, through PORTABLE SCALAR TYPES ONLY (the fixed-width
 * <stdint.h> integers, float, and an opaque void* handle), so the
 * jextract-generated Java binding is identical on every host ABI.
 *
 * Why this shim exists: the WebRTC APM has no C ABI. Its public surface is the
 * C++ webrtc::AudioProcessing class, configured through a nested
 * AudioProcessing::Config aggregate and driven through ProcessStream /
 * ProcessReverseStream over float* const* deinterleaved channel pointers and
 * StreamConfig objects. None of that is bindable by jextract, and its layout
 * differs per build. This shim collapses the whole object behind ONE opaque
 * void* handle, fixes the geometry to the call's single 16 kHz mono channel,
 * exchanges audio as flat float32 arrays of one 10 ms WebRTC APM frame
 * (kChunkSizeMs = 10, so 160 samples at 16 kHz), and flattens the Config to the
 * exact scalar knobs the wa-voip audio-processing layer sets, so the Java
 * binding never names a WebRTC type.
 *
 * Reverse-engineering provenance (cwd-relative to re/calls/out/ff-tScznZ8P-full4/):
 * the wa-voip engine builds this same stack in
 * tree/xplat/wa-voip/wacall/media/src/audio/wa_mobile_audio_processing.cc
 * (flat/fn6819.c: the resampler at param1_00+0x76, the AEC built by fn6801, the
 * NS built by fn6792, and the AGC built by fn6815/fn6820 gated on the per-call
 * use_audio_processing flags param6+0x95 / param6+0x96), over WebRTC's AEC3
 * (the "webrtc aec3" / "Failed to create webrtc AEC module" strings, the
 * pjmedia_echo_create2 wrapper), the WebRTC noise suppressor with the ML
 * denoiser (the "NS: clock_rate=%d, enabled=1, mode=%d, use_denoiser=%d,
 * use_ml_ns=%d" string, p->use_denoiser / p->denoiser_intensity /
 * p->denoiser_intensity_with_ml_ns), and wa_gain_control.cc. The captured
 * voip_settings (re/calls2-spec/captures/voip-settings-merged.json) set
 * aec.algorithm="aec", aec.mode="2", ns.enable="1", ns.use_denoiser="true",
 * ns.denoiser_intensity="0.55". See re/calls2-spec/NATIVE-BINDINGS.md and the
 * WebRtcAudioProcessor Java seam for the integration contract.
 *
 * Symbol naming: every exported symbol is prefixed cobalt_webrtc_apm_ so it
 * coexists in the combined cobalt-native library with the statically-linked real
 * webrtc::* symbols these wrappers drive internally.
 *
 * Portability rule for this header: it uses ONLY int32_t/float/void*. It never
 * names a WebRTC type, and never uses bare `long`, `unsigned long` or
 * `long double`.
 */

#ifndef COBALT_WEBRTC_APM_SHIM_H
#define COBALT_WEBRTC_APM_SHIM_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Status codes returned by the process/create entry points. Zero is success.
 * Negative values are shim-level failures: an invalid argument supplied to the
 * shim, an allocation failure inside the APM factory, or an error code returned
 * by the underlying WebRTC ProcessStream / ProcessReverseStream call.
 *
 *   COBALT_APM_OK         0   the call succeeded
 *   COBALT_APM_BAD_PARAM (-1) a NULL handle/buffer or a wrong frame length
 *   COBALT_APM_NOMEM     (-2) the APM instance could not be allocated
 *   COBALT_APM_ERROR     (-3) the underlying WebRTC APM returned an error
 */
#define COBALT_APM_OK         0
#define COBALT_APM_BAD_PARAM (-1)
#define COBALT_APM_NOMEM     (-2)
#define COBALT_APM_ERROR     (-3)

/*
 * The single supported sample rate, in hertz. The call audio geometry is fixed
 * at 16 kHz mono (the wa-voip codec configuration and the Cobalt capture pump),
 * so the shim hard-codes one rate and one channel rather than exposing a
 * StreamConfig.
 */
#define COBALT_APM_SAMPLE_RATE_HZ 16000

/*
 * The samples in one WebRTC APM frame: kChunkSizeMs (10 ms) at 16 kHz, i.e.
 * 160. ProcessStream and ProcessReverseStream each consume exactly this many
 * samples per call; the caller splits a longer capture block (the Cobalt pump
 * delivers 20 ms / 320-sample blocks) into successive 10 ms frames.
 */
#define COBALT_APM_FRAME_SAMPLES 160

/*
 * The echo-canceller selection passed to cobalt_webrtc_apm_create as aec_mode,
 * mirroring the captured aec.mode voip-param. Value 2 selects AEC3 (the
 * "webrtc aec3" path), the captured setting (aec.mode="2"). Value 0 disables the
 * echo canceller; value 1 selects the mobile echo controller (AECM). The shim
 * maps these onto the WebRTC AudioProcessing::Config::EchoCanceller fields
 * (enabled / mobile_mode).
 */
#define COBALT_APM_AEC_OFF    0
#define COBALT_APM_AEC_MOBILE 1
#define COBALT_APM_AEC_AEC3   2

/**
 * Creates a WebRTC Audio Processing Module instance configured for the call's
 * 16 kHz mono live-capture conditioning.
 *
 * Mirrors the wa_mobile_audio_processing.cc stack builder (fn6819): it builds an
 * APM instance, enables the echo canceller selected by aec_mode (AEC3 for the
 * captured aec.mode="2"), enables the noise suppressor with the ML denoiser when
 * ns_use_denoiser is non-zero at the supplied ns_denoiser_intensity (the
 * captured ns.use_denoiser="true" / ns.denoiser_intensity="0.55"), and enables
 * the gain controller when agc_enabled is non-zero. The returned handle is
 * opaque; on failure the function returns NULL.
 *
 * @param aec_mode             one of COBALT_APM_AEC_OFF / _MOBILE / _AEC3; the
 *                             captured value is COBALT_APM_AEC_AEC3.
 * @param ns_enabled           non-zero to enable noise suppression (captured
 *                             ns.enable="1").
 * @param ns_use_denoiser      non-zero to route noise suppression through the ML
 *                             denoiser (captured ns.use_denoiser="true").
 * @param ns_denoiser_intensity the ML denoiser intensity in [0, 1] (captured
 *                             ns.denoiser_intensity="0.55"); ignored when
 *                             ns_use_denoiser is zero.
 * @param agc_enabled          non-zero to enable the WebRTC gain controller
 *                             (the wa_gain_control.cc stage).
 * @return an opaque APM handle on success, or NULL on failure.
 */
void *cobalt_webrtc_apm_create(int32_t aec_mode,
                               int32_t ns_enabled,
                               int32_t ns_use_denoiser,
                               float ns_denoiser_intensity,
                               int32_t agc_enabled);

/**
 * Feeds one 10 ms far-end render frame to the echo canceller as the reference.
 *
 * Mirrors webrtc::AudioProcessing::ProcessReverseStream: the most recently
 * rendered (played) frame is handed to the APM so the echo canceller can model
 * and subtract its leakage from the next captured frame. The frame is one
 * COBALT_APM_FRAME_SAMPLES (160-sample) 16 kHz mono float32 block, samples
 * normalised to [-1, 1].
 *
 * @param apm   the handle from cobalt_webrtc_apm_create; must not be NULL.
 * @param frame the far-end reference frame, COBALT_APM_FRAME_SAMPLES float32
 *              samples; must not be NULL.
 * @return COBALT_APM_OK on success, COBALT_APM_BAD_PARAM for a NULL argument, or
 *         COBALT_APM_ERROR when the underlying call fails.
 */
int32_t cobalt_webrtc_apm_process_reverse(void *apm, const float *frame);

/**
 * Conditions one 10 ms near-end capture frame in place.
 *
 * Mirrors webrtc::AudioProcessing::ProcessStream: it runs the configured echo
 * canceller (against the reference last supplied through
 * cobalt_webrtc_apm_process_reverse), the noise suppressor, and the gain
 * controller over the captured frame and writes the conditioned samples back
 * into the same buffer. The frame is one COBALT_APM_FRAME_SAMPLES (160-sample)
 * 16 kHz mono float32 block, samples normalised to [-1, 1].
 *
 * @param apm   the handle from cobalt_webrtc_apm_create; must not be NULL.
 * @param frame the near-end capture frame, conditioned in place,
 *              COBALT_APM_FRAME_SAMPLES float32 samples; must not be NULL.
 * @return COBALT_APM_OK on success, COBALT_APM_BAD_PARAM for a NULL argument, or
 *         COBALT_APM_ERROR when the underlying call fails.
 */
int32_t cobalt_webrtc_apm_process(void *apm, float *frame);

/**
 * Reports the running delay, in milliseconds, between the render and capture
 * streams to the echo canceller.
 *
 * Mirrors webrtc::AudioProcessing::set_stream_delay_ms: AEC3 uses this hint to
 * align the reference and capture streams. The caller supplies the platform's
 * measured round-trip render-to-capture delay; passing it each tick before
 * cobalt_webrtc_apm_process keeps the canceller aligned when the device latency
 * drifts.
 *
 * @param apm      the handle from cobalt_webrtc_apm_create; must not be NULL.
 * @param delay_ms the render-to-capture delay in milliseconds; non-negative.
 * @return COBALT_APM_OK on success, COBALT_APM_BAD_PARAM for a NULL handle.
 */
int32_t cobalt_webrtc_apm_set_stream_delay_ms(void *apm, int32_t delay_ms);

/**
 * Destroys an APM instance created by cobalt_webrtc_apm_create.
 *
 * Mirrors the APM destructor: NULL-safe, and must be called at most once per
 * handle.
 *
 * @param apm the handle, or NULL.
 */
void cobalt_webrtc_apm_destroy(void *apm);

#ifdef __cplusplus
}
#endif

#endif /* COBALT_WEBRTC_APM_SHIM_H */
