package com.github.auties00.cobalt.calls.media.video.codec;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;

import java.util.Objects;

/**
 * Holds one compressed picture produced by a {@link VideoCodec}, tagged with the codec that produced
 * it, whether it is independently decodable, and the picture dimensions in force when it was encoded.
 *
 * <p>The {@link #payload()} is the codec's compressed access unit: a concatenation of the picture's NAL
 * units without length prefixes for {@link VideoDecoderCapability#H264 H264}, or the single compressed
 * frame for {@link VideoDecoderCapability#VP8 VP8} and {@link VideoDecoderCapability#VP9 VP9}. The
 * {@link #keyFrame()} flag reports whether the access unit is an intra picture that can be decoded
 * without any earlier frame: a {@code true} value marks an instantaneous decoder refresh that resets
 * the decoder's reference state, while a {@code false} value marks an inter picture that depends on
 * previously decoded references. The packetizer reads this flag to mark RTP keyframe boundaries, and
 * the bandwidth estimator reads it because a keyframe is a deliberate, large cost spike rather than a
 * congestion signal.
 *
 * <p>The {@link #width()} and {@link #height()} record the encoded picture size, which a resolution
 * change during a call carries forward picture by picture; for an inter frame they equal the dimensions
 * of the keyframe that opened the current resolution. An empty access unit is represented by an array
 * of length zero, not {@code null}, and arises when the encoder drops a frame under the rate
 * controller's frame skip control.
 *
 * @param payload   the compressed access unit bytes; never {@code null}, possibly empty when the
 *                  encoder dropped the frame
 * @param codec     the codec that produced this access unit
 * @param keyFrame  whether the access unit is an independently decodable intra picture
 * @param width     the encoded picture width in pixels
 * @param height    the encoded picture height in pixels
 * @param ptsMicros the presentation timestamp in microseconds carried through from the source frame
 * @implNote This implementation derives {@link #keyFrame()} from the classification the codec attaches
 * to each emitted picture: for {@link VideoDecoderCapability#H264 H264} the OpenH264 frame type of an
 * instantaneous decoder refresh picture sets the flag, and for {@link VideoDecoderCapability#VP8 VP8}
 * the {@code VPX_FRAME_IS_KEY} bit of the codec output packet sets it. The dimensions are taken from the
 * source frame the encoder consumed rather than parsed again from the bitstream, since the encode path
 * always drives the encoder at a known configured size.
 */
public record EncodedVideoFrame(
        byte[] payload,
        VideoDecoderCapability codec,
        boolean keyFrame,
        int width,
        int height,
        long ptsMicros
) {
    /**
     * Validates that the payload and codec are present.
     *
     * @throws NullPointerException if {@code payload} or {@code codec} is {@code null}
     */
    public EncodedVideoFrame {
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(codec, "codec cannot be null");
    }

    /**
     * Returns whether this access unit carries no compressed bytes.
     *
     * <p>An empty access unit is produced when the rate controller's frame skip logic drops a frame;
     * the sender transmits nothing for it.
     *
     * @return {@code true} if the {@linkplain #payload() payload} is zero length
     */
    public boolean isEmpty() {
        return payload.length == 0;
    }
}
