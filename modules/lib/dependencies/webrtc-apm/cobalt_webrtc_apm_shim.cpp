/*
 * cobalt_webrtc_apm_shim.cpp
 *
 * Implementation of the portable extern-C facade declared in
 * cobalt_webrtc_apm_shim.h over the webrtc-audio-processing standalone library
 * (pulseaudio fork, WEBRTC_APM_REF=v2.1). It fixes the geometry to the call's
 * single 16 kHz mono channel and exchanges audio as flat float32 arrays of one
 * 10 ms WebRTC APM frame (160 samples), normalised to [-1, 1] by the Java
 * WebRtcAudioProcessor seam (PCM_FULL_SCALE = 32768), which is the range the
 * WebRTC float ProcessStream / ProcessReverseStream API expects.
 *
 * Scope: this shim wires only the echo canceller (AEC3, or the mobile AEC) and
 * the noise suppressor. The gain controller is created only when agc_enabled is
 * non-zero; with it left off (the calls "AEC + NC" configuration) the AGC code
 * is never referenced and is dead-stripped from the combined cobalt-native
 * library by the final link's -Wl,--gc-sections, so no AGC module ships.
 *
 * The webrtc::* symbols this file calls are resolved from the statically-linked
 * webrtc-audio-processing archive in the same link group; every symbol this file
 * itself exports is prefixed cobalt_webrtc_apm_ (see the header).
 */

#include "cobalt_webrtc_apm_shim.h"

#include <modules/audio_processing/include/audio_processing.h>

#include <new>

namespace {

using webrtc::AudioProcessing;
using webrtc::AudioProcessingBuilder;
using webrtc::StreamConfig;

// Owns the reference-counted APM instance behind the opaque void* handle.
struct CobaltApm {
    webrtc::scoped_refptr<AudioProcessing> apm;
};

// Maps the WA denoiser intensity (0..1, captured default 0.55) onto the WebRTC
// noise-suppressor level. The open webrtc-audio-processing library has no ML
// denoiser (that is a WA-mobile-only path), so the ns_use_denoiser flag only
// selects a stronger suppression level here.
AudioProcessing::Config::NoiseSuppression::Level ns_level(float intensity) {
    if (intensity >= 0.75f) {
        return AudioProcessing::Config::NoiseSuppression::kVeryHigh;
    }
    if (intensity >= 0.50f) {
        return AudioProcessing::Config::NoiseSuppression::kHigh;
    }
    if (intensity >= 0.25f) {
        return AudioProcessing::Config::NoiseSuppression::kModerate;
    }
    return AudioProcessing::Config::NoiseSuppression::kLow;
}

}  // namespace

extern "C" void *cobalt_webrtc_apm_create(int32_t aec_mode,
                                          int32_t ns_enabled,
                                          int32_t ns_use_denoiser,
                                          float ns_denoiser_intensity,
                                          int32_t agc_enabled) {
    AudioProcessing::Config config;

    switch (aec_mode) {
        case COBALT_APM_AEC_AEC3:
            config.echo_canceller.enabled = true;
            config.echo_canceller.mobile_mode = false;
            break;
        case COBALT_APM_AEC_MOBILE:
            config.echo_canceller.enabled = true;
            config.echo_canceller.mobile_mode = true;
            break;
        case COBALT_APM_AEC_OFF:
        default:
            config.echo_canceller.enabled = false;
            break;
    }

    config.noise_suppression.enabled = ns_enabled != 0;
    config.noise_suppression.level =
            ns_level(ns_use_denoiser != 0 ? ns_denoiser_intensity : 0.50f);

    // AGC is off in the calls AEC+NC configuration; enabling it is opt-in.
    config.gain_controller1.enabled = agc_enabled != 0;
    config.gain_controller2.enabled = false;

    webrtc::scoped_refptr<AudioProcessing> apm = AudioProcessingBuilder().Create();
    if (apm == nullptr) {
        return nullptr;
    }
    apm->ApplyConfig(config);

    CobaltApm *handle = new (std::nothrow) CobaltApm();
    if (handle == nullptr) {
        return nullptr;
    }
    handle->apm = apm;
    return handle;
}

extern "C" int32_t cobalt_webrtc_apm_process_reverse(void *apm, const float *frame) {
    if (apm == nullptr || frame == nullptr) {
        return COBALT_APM_BAD_PARAM;
    }
    CobaltApm *handle = static_cast<CobaltApm *>(apm);
    const StreamConfig config(COBALT_APM_SAMPLE_RATE_HZ, 1);
    const float *const src[1] = {frame};
    // ProcessReverseStream also writes the (linear AEC farend) output; a local
    // scratch absorbs it since the render path only needs the analysis.
    float scratch[COBALT_APM_FRAME_SAMPLES];
    float *const dst[1] = {scratch};
    const int err = handle->apm->ProcessReverseStream(src, config, config, dst);
    return err == AudioProcessing::kNoError ? COBALT_APM_OK : COBALT_APM_ERROR;
}

extern "C" int32_t cobalt_webrtc_apm_process(void *apm, float *frame) {
    if (apm == nullptr || frame == nullptr) {
        return COBALT_APM_BAD_PARAM;
    }
    CobaltApm *handle = static_cast<CobaltApm *>(apm);
    const StreamConfig config(COBALT_APM_SAMPLE_RATE_HZ, 1);
    float *const io[1] = {frame};
    const int err = handle->apm->ProcessStream(io, config, config, io);
    return err == AudioProcessing::kNoError ? COBALT_APM_OK : COBALT_APM_ERROR;
}

extern "C" int32_t cobalt_webrtc_apm_set_stream_delay_ms(void *apm, int32_t delay_ms) {
    if (apm == nullptr) {
        return COBALT_APM_BAD_PARAM;
    }
    CobaltApm *handle = static_cast<CobaltApm *>(apm);
    handle->apm->set_stream_delay_ms(delay_ms);
    return COBALT_APM_OK;
}

extern "C" void cobalt_webrtc_apm_destroy(void *apm) {
    if (apm == nullptr) {
        return;
    }
    delete static_cast<CobaltApm *>(apm);
}
