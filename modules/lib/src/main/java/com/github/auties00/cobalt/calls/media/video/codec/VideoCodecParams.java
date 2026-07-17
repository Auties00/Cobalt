package com.github.auties00.cobalt.calls.media.video.codec;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;
import com.github.auties00.cobalt.calls.stream.VideoFrame;

import java.util.Objects;

/**
 * An immutable set of video encoder and decoder parameters the call media path opens a
 * {@link VideoCodec} with and reconfigures it through mid call.
 *
 * <p>The parameters cover the picture geometry ({@link #codec()}, {@link #width()}, {@link #height()},
 * {@link #frameRate()}), the bitrate triplet ({@link #targetBitrate()}, {@link #minBitrate()},
 * {@link #maxBitrate()}), the quantizer window ({@link #minQuantizer()}, {@link #maxQuantizer()}), the
 * keyframe cadence ({@link #keyFrameIntervalSeconds()}), the latency versus quality knob
 * ({@link #complexity()}), the rate control safety valves ({@link #frameSkip()},
 * {@link #idrBitrateRatio()}), and the scalability and recovery features the engine drives codecs with
 * ({@link #temporalLayers()}, {@link #longTermReference()}). On open the codec applies every control
 * these fields select; on a {@linkplain VideoCodec#modify(VideoCodecParams) modify} round only the
 * mutable subset (the bitrate triplet, the frame rate, the quantizer window, frame skip, and the IDR
 * bitrate ratio) is re applied, since the {@link #codec()}, the {@link #width()}/{@link #height()}
 * geometry, the layer count, and the long term reference toggle all require a codec reopen.
 *
 * <p>Build instances through {@link #forResolution(VideoDecoderCapability, int, int, int)}, which seeds
 * the bitrate triplet from the WhatsApp initial rate control constants and the remaining knobs from the
 * WhatsApp video defaults, then derive variants with the {@code with*} copy methods. The {@link #width()}
 * and {@link #height()} must be even so the 4:2:0 chroma planes have integral sizes, matching the
 * {@link VideoFrame} the encoder consumes.
 *
 * @param codec                  the codec this parameter set configures
 * @param width                  the encoded picture width in pixels; even and at least {@code 2}
 * @param height                 the encoded picture height in pixels; even and at least {@code 2}
 * @param frameRate              the target frame rate in frames per second; positive
 * @param targetBitrate          the target bitrate in bits per second
 * @param minBitrate             the floor bitrate in bits per second the rate controller will not go
 *                               below
 * @param maxBitrate             the ceiling bitrate in bits per second the rate controller will not
 *                               exceed
 * @param minQuantizer           the lowest quantization parameter the rate controller may pick
 * @param maxQuantizer           the highest quantization parameter the rate controller may pick
 * @param keyFrameIntervalSeconds the maximum number of seconds between forced key frames; {@code 0}
 *                               disables periodic key frames and emits one only on request
 * @param complexity             the latency versus quality level; lower trades quality for less encode
 *                               time
 * @param frameSkip              whether the rate controller may drop frames to hold the bitrate
 * @param idrBitrateRatio        the percentage by which a key frame's byte budget exceeds an inter
 *                               frame's
 * @param temporalLayers         the number of temporal scalability layers; {@code 1} is single layer
 * @param longTermReference      whether long term reference frames are enabled for loss recovery
 * @implNote This implementation collects the controls the OpenH264 and libvpx encoder open and
 * reconfigure paths apply. For OpenH264 the fields drive the {@code SEncParamExt} block
 * ({@code iTargetBitrate}, {@code iMaxBitrate}, {@code fMaxFrameRate}, {@code iMinQp}/{@code iMaxQp},
 * {@code uiIntraPeriod}, {@code iComplexityMode}, {@code bEnableFrameSkip}, {@code iIdrBitrateRatio},
 * {@code iTemporalLayerNum}, {@code bEnableLongTermReference}); for VP8 they drive the
 * {@code vpx_codec_enc_cfg} ({@code rc_target_bitrate} in kbps, {@code rc_min_quantizer}/
 * {@code rc_max_quantizer}, {@code kf_max_dist}, {@code g_timebase}) plus the {@code VP8E_SET_CPUUSED}
 * control for {@link #complexity()}.
 */
public record VideoCodecParams(
        VideoDecoderCapability codec,
        int width,
        int height,
        int frameRate,
        int targetBitrate,
        int minBitrate,
        int maxBitrate,
        int minQuantizer,
        int maxQuantizer,
        int keyFrameIntervalSeconds,
        int complexity,
        boolean frameSkip,
        int idrBitrateRatio,
        int temporalLayers,
        boolean longTermReference
) {
    /**
     * The default target frame rate in frames per second when none is specified.
     *
     * <p>This {@code 30} fps default is the unconstrained capture target; the WhatsApp rate control
     * caps the encode frame rate per runtime condition rather than fixing one encode rate.
     */
    // TODO: apply the runtime per condition frame rate cap (WhatsApp bounds the encode rate by
    //  network medium, round trip time, and packet loss) rather than encoding at the static default
    public static final int DEFAULT_FRAME_RATE = 30;

    /**
     * The default lowest quantization parameter for the OpenH264 encoder, its {@code CAMERA_VIDEO_REAL_TIME}
     * compiled default.
     *
     * <p>WhatsApp pushes no minimum quantizer, so the operative floor is the encoder's own default. OpenH264
     * fills {@code iMinQp} with {@code 0}, then clamps it to {@code 12} for the {@code CAMERA_VIDEO_REAL_TIME}
     * usage type a realtime call selects (with a matching upper bound of {@code 42}); H.265 reuses this value.
     */
    public static final int OPENH264_MIN_QUANTIZER = 12;

    /**
     * The default lowest quantization parameter for the libvpx VP8 encoder, its {@code rc_min_quantizer}
     * compiled default.
     */
    public static final int VP8_MIN_QUANTIZER = 4;

    /**
     * The default lowest quantization parameter for the libvpx VP9 encoder, its {@code rc_min_quantizer}
     * compiled default; also used for AV1, whose rav1e encoder ignores this bound.
     */
    public static final int VP9_MIN_QUANTIZER = 0;

    /**
     * The default highest quantization parameter.
     *
     * <p>Matches the WhatsApp base video rate control rule, whose OpenH264 maximum quantizer is
     * {@code 51} (the AV1 ceiling for that codec is {@code 63}).
     */
    public static final int DEFAULT_MAX_QUANTIZER = 51;

    /**
     * The default maximum seconds between forced key frames.
     *
     * <p>Matches the WhatsApp key frame interval of {@code 60} frames, which maps to the codec side
     * {@code uiIntraPeriod}/{@code kf_max_dist} frame count; sixty frames at the thirty frame per second
     * capture rate is two seconds. A peer joining mid call misses the stream's first key frame and only
     * recovers a decodable picture at the next interval, so a short interval lets a joining peer
     * configure its decoder from a key frame promptly instead of sitting on a placeholder geometry with
     * no decoded frames.
     */
    public static final int DEFAULT_KEY_FRAME_INTERVAL_SECONDS = 2;

    /**
     * The default percentage by which a key frame's byte budget exceeds an inter frame's.
     *
     * <p>Matches the WhatsApp base video rate control rule {@code openh264_idr_bitrate_ratio = 100};
     * {@code 100} means a key frame is budgeted the same as an inter frame.
     */
    public static final int DEFAULT_IDR_BITRATE_RATIO = 100;

    /**
     * The initial target bitrate the resolution seed opens the encoder at, in bits per second.
     *
     * <p>WhatsApp does not derive the initial video bitrate from the picture size: the bandwidth
     * estimator initializes the target independently of the resolution and then drives it. This is the
     * initial bandwidth estimate ceiling ({@code 350000}), the value the estimator starts the encoder
     * at before it ramps toward {@link #DEFAULT_MAX_BITRATE}.
     */
    public static final int DEFAULT_INIT_TARGET_BITRATE = 350_000;

    /**
     * The floor bitrate the resolution seed configures the rate controller with, in bits per second.
     *
     * <p>The minimum initial bitrate ({@code 128000}) below which the estimator does not initialize the
     * encoder target.
     */
    public static final int DEFAULT_MIN_BITRATE = 128_000;

    /**
     * The ceiling bitrate the resolution seed configures the rate controller with, in bits per second.
     *
     * <p>The base rate control ceiling ({@code 1020000}), the global cap the estimator ramps the target
     * toward; the runtime rules may tighten it further per condition.
     */
    public static final int DEFAULT_MAX_BITRATE = 1_020_000;

    /**
     * Validates the geometry, the bitrate ordering, and the layer count.
     *
     * @throws NullPointerException     if {@code codec} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or below {@code 2}, if
     *                                  {@code frameRate} is not positive, if the bitrate triplet is not
     *                                  ordered {@code minBitrate <= targetBitrate <= maxBitrate} with a
     *                                  positive target, if the quantizer window is not ordered
     *                                  {@code 0 <= minQuantizer <= maxQuantizer <= 63}, if
     *                                  {@code keyFrameIntervalSeconds} is negative, or if
     *                                  {@code temporalLayers} is below {@code 1}
     */
    public VideoCodecParams {
        Objects.requireNonNull(codec, "codec cannot be null");
        if (width < 2 || width % 2 != 0) {
            throw new IllegalArgumentException("width must be even and >= 2, got " + width);
        }
        if (height < 2 || height % 2 != 0) {
            throw new IllegalArgumentException("height must be even and >= 2, got " + height);
        }
        if (frameRate <= 0) {
            throw new IllegalArgumentException("frameRate must be positive, got " + frameRate);
        }
        if (targetBitrate <= 0) {
            throw new IllegalArgumentException("targetBitrate must be positive, got " + targetBitrate);
        }
        if (minBitrate < 0 || minBitrate > targetBitrate || targetBitrate > maxBitrate) {
            throw new IllegalArgumentException(
                    "bitrate triplet must satisfy 0 <= min <= target <= max, got min=" + minBitrate
                            + " target=" + targetBitrate + " max=" + maxBitrate);
        }
        if (minQuantizer < 0 || minQuantizer > maxQuantizer || maxQuantizer > 63) {
            throw new IllegalArgumentException(
                    "quantizer window must satisfy 0 <= min <= max <= 63, got min=" + minQuantizer
                            + " max=" + maxQuantizer);
        }
        if (keyFrameIntervalSeconds < 0) {
            throw new IllegalArgumentException(
                    "keyFrameIntervalSeconds must be non-negative, got " + keyFrameIntervalSeconds);
        }
        if (temporalLayers < 1) {
            throw new IllegalArgumentException("temporalLayers must be >= 1, got " + temporalLayers);
        }
    }

    /**
     * Returns the maximum number of frames between forced key frames for the configured frame rate.
     *
     * <p>Computed as {@code keyFrameIntervalSeconds * frameRate}; this is the {@code kf_max_dist}
     * argument for libvpx and the basis for {@code uiIntraPeriod} for OpenH264. A
     * {@link #keyFrameIntervalSeconds()} of {@code 0} yields {@code 0}, which the codecs read as
     * "emit a key frame only on request".
     *
     * @return the maximum number of frames between forced key frames
     */
    public int keyFrameIntervalFrames() {
        return keyFrameIntervalSeconds * frameRate;
    }

    /**
     * Returns a copy with the given target bitrate, clamping it into the {@code [min, max]} window.
     *
     * @param bitrate the requested target bitrate in bits per second
     * @return a copy whose target bitrate is the clamped value
     */
    public VideoCodecParams withTargetBitrate(int bitrate) {
        var clamped = Math.clamp(bitrate, minBitrate, maxBitrate);
        if (clamped == targetBitrate) {
            return this;
        }
        return new VideoCodecParams(codec, width, height, frameRate, clamped, minBitrate, maxBitrate,
                minQuantizer, maxQuantizer, keyFrameIntervalSeconds, complexity, frameSkip,
                idrBitrateRatio, temporalLayers, longTermReference);
    }

    /**
     * Returns a copy with the given target frame rate.
     *
     * @param frameRate the requested frame rate in frames per second; must be positive
     * @return a copy carrying the new frame rate
     * @throws IllegalArgumentException if {@code frameRate} is not positive
     */
    public VideoCodecParams withFrameRate(int frameRate) {
        return new VideoCodecParams(codec, width, height, frameRate, targetBitrate, minBitrate, maxBitrate,
                minQuantizer, maxQuantizer, keyFrameIntervalSeconds, complexity, frameSkip,
                idrBitrateRatio, temporalLayers, longTermReference);
    }

    /**
     * Returns a copy with the given quantizer window.
     *
     * @param minQuantizer the lowest quantization parameter
     * @param maxQuantizer the highest quantization parameter
     * @return a copy carrying the new quantizer window
     * @throws IllegalArgumentException if the window is not ordered {@code 0 <= min <= max <= 63}
     */
    public VideoCodecParams withQuantizerWindow(int minQuantizer, int maxQuantizer) {
        return new VideoCodecParams(codec, width, height, frameRate, targetBitrate, minBitrate, maxBitrate,
                minQuantizer, maxQuantizer, keyFrameIntervalSeconds, complexity, frameSkip,
                idrBitrateRatio, temporalLayers, longTermReference);
    }

    /**
     * Builds a parameter set for the given codec and picture geometry, seeding the bitrate triplet from
     * the WhatsApp initial rate control constants and the remaining knobs from the WhatsApp video
     * defaults.
     *
     * <p>The bitrate triplet is seeded independently of the picture size from the WhatsApp base rate
     * control block: the target is {@link #DEFAULT_INIT_TARGET_BITRATE}, the floor is
     * {@link #DEFAULT_MIN_BITRATE}, and the ceiling is {@link #DEFAULT_MAX_BITRATE}. WhatsApp
     * initializes the video bitrate from the bandwidth estimator rather than the picture size and drives
     * it at runtime, so the geometry sets only the encoded {@link #width()}, {@link #height()}, and
     * {@link #frameRate()} here, not the bitrate. The quantizer window (each codec's compiled default low
     * bound, {@link #DEFAULT_MAX_QUANTIZER}), keyframe interval ({@link #DEFAULT_KEY_FRAME_INTERVAL_SECONDS}),
     * and IDR bitrate ratio ({@link #DEFAULT_IDR_BITRATE_RATIO}) take their defaults, the layer count is
     * a single temporal layer, frame skip is enabled, complexity is the codec neutral mid level
     * ({@code 0}), and long term references are disabled.
     *
     * @param codec     the codec to configure
     * @param width     the encoded picture width in pixels; even and at least {@code 2}
     * @param height    the encoded picture height in pixels; even and at least {@code 2}
     * @param frameRate the target frame rate in frames per second; positive
     * @return the seeded parameter set
     * @throws NullPointerException     if {@code codec} is {@code null}
     * @throws IllegalArgumentException if the geometry or frame rate is invalid
     * @implNote This implementation seeds the static bitrate triplet the rate controller then drives
     * from a live bandwidth estimate.
     */
    public static VideoCodecParams forResolution(VideoDecoderCapability codec, int width, int height, int frameRate) {
        Objects.requireNonNull(codec, "codec cannot be null");
        if (frameRate <= 0) {
            throw new IllegalArgumentException("frameRate must be positive, got " + frameRate);
        }
        // WhatsApp pushes no minimum quantizer, so each codec takes its own encoder default: OpenH264
        // (H.264/H.265) its realtime iMinQp, libvpx VP8/VP9 their rc_min_quantizer, and AV1 a bound rav1e
        // ignores.
        var minQuantizer = switch (codec) {
            case H264, H265 -> OPENH264_MIN_QUANTIZER;
            case VP8 -> VP8_MIN_QUANTIZER;
            case VP9, AV1 -> VP9_MIN_QUANTIZER;
        };
        // TODO: drive the per frame bitrate from the runtime bandwidth estimator bounded by the WhatsApp
        //  dynamic rate control rules (per condition target, key frame interval, IDR ratio, and encode
        //  width caps keyed on bitrate range, network medium, round trip time, and packet loss); this
        //  seeds only the static triplet the rate controller starts from
        return new VideoCodecParams(
                codec,
                width,
                height,
                frameRate,
                DEFAULT_INIT_TARGET_BITRATE,
                DEFAULT_MIN_BITRATE,
                DEFAULT_MAX_BITRATE,
                minQuantizer,
                DEFAULT_MAX_QUANTIZER,
                DEFAULT_KEY_FRAME_INTERVAL_SECONDS,
                0,
                true,
                DEFAULT_IDR_BITRATE_RATIO,
                // TODO: seed temporal layers from the WhatsApp base rate control config (recovered value 2)
                //  once the rate control config that selects the layer count is wired into video setup
                1,
                false
        );
    }
}
