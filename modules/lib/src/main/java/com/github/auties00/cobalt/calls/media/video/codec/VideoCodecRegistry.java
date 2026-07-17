package com.github.auties00.cobalt.calls.media.video.codec;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.github.auties00.cobalt.calls.media.video.codec.av1.Av1VideoCodec;
import com.github.auties00.cobalt.calls.media.video.codec.h264.H264VideoCodec;
import com.github.auties00.cobalt.calls.media.video.codec.vpx.VpxVideoCodec;

/**
 * Reports which video codecs this build can encode and decode, negotiates a codec against a peer's
 * advertised capability set, and instantiates a {@link VideoCodec} for a chosen {@link VideoCodecParams}.
 *
 * <p>The registry is the single entry point the media engine asks for a codec rather than constructing
 * {@link H264VideoCodec}, {@link VpxVideoCodec}, or {@link Av1VideoCodec} directly. The
 * {@linkplain #supportedCodecs() supported set} is the subset of {@link VideoDecoderCapability} this
 * build has a working encoder and decoder for; the signaling layer serializes it into the
 * {@code vid_dec} descriptor and intersects it with the peer's set during negotiation.
 * {@link #open(VideoCodecParams)} maps a codec onto its concrete implementation, while
 * {@link #negotiateAndOpen(VideoCodecParams, Set, Set)} intersects the local, self, and peer sets, takes
 * the surviving codec of highest {@link VideoDecoderCapability#priority() priority}, and opens it.
 *
 * <p>The advertised codecs are H.264, VP8, VP9, and AV1, ordered by
 * {@link VideoDecoderCapability#priority() priority}: H.264 and VP8 rank highest, VP9 follows, and AV1
 * ranks lowest so it is chosen only when it is the sole common codec.
 * {@link VideoDecoderCapability#H265 H265} carries a capability bit but no codec is wired in for it, so
 * it is excluded from {@link #SUPPORTED} and never negotiated.
 *
 * <p>The registry is stateless and its methods are pure factories, so one instance is safe to share
 * across calls and threads; the {@link VideoCodec} instances it returns are the single writer objects,
 * not the registry itself.
 *
 * @implNote VP8 and VP9 both open through {@link VpxVideoCodec}: libvpx exposes the two codecs behind
 * one context API, and {@link VpxVideoCodec} resolves which one to run from
 * {@link VideoCodecParams#codec()}.
 */
public final class VideoCodecRegistry {
    /**
     * The logger for {@link VideoCodecRegistry}.
     */
    private static final System.Logger LOGGER = Log.get(VideoCodecRegistry.class);

    /**
     * The codecs this build can both encode and decode, advertised to peers and intersected during
     * negotiation.
     *
     * <p>Holds {@link VideoDecoderCapability#H264 H264}, {@link VideoDecoderCapability#VP8 VP8},
     * {@link VideoDecoderCapability#VP9 VP9}, and {@link VideoDecoderCapability#AV1 AV1}. H.264 opens
     * through {@link H264VideoCodec}, VP8 and VP9 through {@link VpxVideoCodec}, and AV1 through
     * {@link Av1VideoCodec}. {@link VideoDecoderCapability#H265 H265} is omitted because no H.265 codec is
     * wired in.
     */
    private static final Set<VideoDecoderCapability> SUPPORTED =
            EnumSet.of(VideoDecoderCapability.H264, VideoDecoderCapability.VP8,
                    VideoDecoderCapability.VP9, VideoDecoderCapability.AV1);

    /**
     * The shared stateless registry instance.
     *
     * <p>Because the registry holds no state per call and its methods are pure factories, one instance is
     * safe to share across every call and thread; callers use this rather than constructing a fresh
     * registry per call.
     */
    public static final VideoCodecRegistry INSTANCE = new VideoCodecRegistry();

    /**
     * Constructs a stateless registry.
     */
    public VideoCodecRegistry() {

    }

    /**
     * Returns the codecs this build can both encode and decode.
     *
     * <p>The returned set is a defensive copy, so a caller may freely mutate it; this is the local
     * capability set the signaling layer serializes into the {@code vid_dec} descriptor.
     *
     * @return a mutable copy of the supported codec set, never empty
     */
    public Set<VideoDecoderCapability> supportedCodecs() {
        return EnumSet.copyOf(SUPPORTED);
    }

    /**
     * Returns whether this build can both encode and decode the given codec.
     *
     * @param codec the codec to test
     * @return {@code true} if {@code codec} is supported
     * @throws NullPointerException if {@code codec} is {@code null}
     */
    public boolean isSupported(VideoDecoderCapability codec) {
        Objects.requireNonNull(codec, "codec cannot be null");
        return SUPPORTED.contains(codec);
    }

    /**
     * Opens a {@link VideoCodec} for the given parameters, selecting the concrete implementation from
     * {@link VideoCodecParams#codec()}.
     *
     * @apiNote Prefer {@link #negotiateAndOpen(VideoCodecParams, Set, Set)} on the call setup path so
     * the codec is chosen against the peer's capabilities; call this directly only when the codec is
     * already fixed.
     *
     * @param params the codec parameters, whose {@link VideoCodecParams#codec() codec} selects the
     *               implementation
     * @return the opened codec
     * @throws NullPointerException       if {@code params} is {@code null}
     * @throws IllegalArgumentException   if {@link VideoCodecParams#codec()} is not supported by this
     *                                    build
     * @throws WhatsAppCallException.H264 if an OpenH264 object fails to open
     * @throws WhatsAppCallException.Vpx  if a libvpx context fails to open
     * @throws WhatsAppCallException.Av1  if the dav1d decoder or rav1e encoder fails to open
     */
    public VideoCodec open(VideoCodecParams params) {
        Objects.requireNonNull(params, "params cannot be null");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "opening video codec {0}, {1}x{2}", params.codec(), params.width(), params.height());
        }
        return switch (params.codec()) {
            case H264 -> new H264VideoCodec(params);
            case VP8, VP9 -> new VpxVideoCodec(params);
            case AV1 -> new Av1VideoCodec(params);
            case H265 -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "video codec open rejected, H265 is not supported by this build");
                throw new IllegalArgumentException(
                        "H265 is not supported by this build: no H.265 codec is wired in");
            }
        };
    }

    /**
     * Negotiates a codec against a peer's advertised capabilities and opens it with the given base
     * parameters.
     *
     * <p>The negotiation intersects the local {@linkplain #supportedCodecs() supported set} with both
     * the supplied {@code self} and {@code peer} sets and selects the surviving codec of highest
     * {@link VideoDecoderCapability#priority() priority}; if a codec is chosen, the base parameters are
     * retargeted to it via {@link VideoCodecParams#codec()} and opened. When no codec is common to all
     * three sets, no codec is opened.
     *
     * @param baseParams the geometry and rate control parameters to open the chosen codec with; its own
     *                   {@link VideoCodecParams#codec()} is overridden by the negotiated codec
     * @param self       the local advertised capability set
     * @param peer       the peer's advertised capability set
     * @return the opened codec, or {@link Optional#empty()} if the three sets share no codec
     * @throws NullPointerException       if any argument is {@code null}
     * @throws WhatsAppCallException.H264 if the negotiated OpenH264 object fails to open
     * @throws WhatsAppCallException.Vpx  if the negotiated libvpx context fails to open
     * @throws WhatsAppCallException.Av1  if the negotiated dav1d decoder or rav1e encoder fails to open
     */
    public Optional<VideoCodec> negotiateAndOpen(VideoCodecParams baseParams,
                                                 Set<VideoDecoderCapability> self,
                                                 Set<VideoDecoderCapability> peer) {
        Objects.requireNonNull(baseParams, "baseParams cannot be null");
        Objects.requireNonNull(self, "self cannot be null");
        Objects.requireNonNull(peer, "peer cannot be null");
        var localCapable = VideoDecoderCapability.intersect(SUPPORTED, self);
        var negotiated = VideoDecoderCapability.negotiate(localCapable, peer);
        if (negotiated.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "video codec negotiation failed, no codec common to local, self, and peer sets");
            return Optional.empty();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video codec negotiated: {0}", negotiated.get());
        return negotiated.map(chosen -> open(retarget(baseParams, chosen)));
    }

    /**
     * Returns a copy of the base parameters retargeted to the negotiated codec, or the base parameters
     * unchanged when they already select that codec.
     *
     * @param baseParams the base parameters
     * @param codec      the negotiated codec
     * @return the parameters bound to {@code codec}
     */
    private VideoCodecParams retarget(VideoCodecParams baseParams, VideoDecoderCapability codec) {
        if (baseParams.codec() == codec) {
            return baseParams;
        }
        return new VideoCodecParams(
                codec,
                baseParams.width(),
                baseParams.height(),
                baseParams.frameRate(),
                baseParams.targetBitrate(),
                baseParams.minBitrate(),
                baseParams.maxBitrate(),
                baseParams.minQuantizer(),
                baseParams.maxQuantizer(),
                baseParams.keyFrameIntervalSeconds(),
                baseParams.complexity(),
                baseParams.frameSkip(),
                baseParams.idrBitrateRatio(),
                baseParams.temporalLayers(),
                baseParams.longTermReference());
    }
}
