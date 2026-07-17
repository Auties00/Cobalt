package com.github.auties00.cobalt.calls.media.video.codec;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import com.github.auties00.cobalt.calls.media.video.codec.av1.Av1VideoCodec;
import com.github.auties00.cobalt.calls.media.video.codec.h264.H264VideoCodec;
import com.github.auties00.cobalt.calls.media.video.codec.vpx.VpxVideoCodec;

/**
 * The call video codec abstraction: encodes raw 4:2:0 pictures into compressed access units, decodes
 * compressed access units back to raw pictures, forces an intra refresh on demand, and reconfigures
 * itself mid call.
 *
 * <p>An instance owns one encoder and one decoder for a single video stream and is single writer: the
 * encode path, decode path, key frame request, and reconfiguration must be driven from one thread,
 * since the codec holds mutable native state and reusable scratch buffers. The
 * {@linkplain #encode(VideoFrame, boolean) encode} method compresses one picture, optionally forcing
 * it to be a key frame; the {@linkplain #decode(byte[], long) decode} method reconstructs the next
 * picture from a compressed access unit; {@linkplain #requestKeyFrame() requestKeyFrame} arms the
 * encoder so its next output is a key frame, the recovery action a receiver takes after a loss it
 * could not conceal; {@linkplain #modify(VideoCodecParams) modify} reapplies the mutable rate control
 * settings; and {@link #stats()} snapshots the lifetime counters.
 *
 * <p>The hierarchy is sealed for exhaustive matching: {@link H264VideoCodec} wraps OpenH264,
 * {@link VpxVideoCodec} wraps libvpx (VP8 and VP9), and {@link Av1VideoCodec} is the AV1 codec the
 * engine can select once its native binding is available. The 1:1 and group video paths negotiate the
 * concrete codec through {@link VideoDecoderCapability}, defaulting to
 * {@link VideoDecoderCapability#H264 H264} when no higher priority codec is common to both sides.
 */
public sealed interface VideoCodec extends AutoCloseable
        permits H264VideoCodec, VpxVideoCodec, Av1VideoCodec {
    /**
     * Returns the codec format this instance implements.
     *
     * @return the codec format, such as {@link VideoDecoderCapability#H264 H264} or
     *         {@link VideoDecoderCapability#VP8 VP8}
     */
    VideoDecoderCapability codec();

    /**
     * Encodes one raw 4:2:0 picture into a compressed access unit, optionally forcing it to be a key
     * frame.
     *
     * @implSpec Implementations must accept a planar
     * {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#I420 I420} {@link VideoFrame} whose
     * {@linkplain VideoFrame#width() width} and {@linkplain VideoFrame#height() height} match the
     * configured encoder geometry; the caller normalizes any capture to that layout and geometry (repacking
     * an {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#NV12 NV12} source and resampling
     * one whose geometry differs) before the encode handoff, so implementations feed the frame's pixels
     * straight to the native encoder. When {@code forceKeyFrame} is {@code true}, or when an earlier
     * {@linkplain #requestKeyFrame() key frame request} is pending, the output must be an intra picture
     * and the {@linkplain EncodedVideoFrame#keyFrame() key frame flag} must be set. When the rate
     * controller drops the frame, the result must be an {@linkplain EncodedVideoFrame#isEmpty() empty}
     * access unit rather than {@code null}. The returned frame must carry the source
     * {@linkplain VideoFrame#ptsMicros() presentation timestamp} and the configured dimensions.
     *
     * @param frame         the raw picture to encode
     * @param forceKeyFrame whether to force this picture to be an intra (key) frame
     * @return the encoded access unit with its key frame classification
     * @throws NullPointerException                if {@code frame} is {@code null}
     * @throws IllegalArgumentException            if the frame dimensions do not match the configured
     *                                             geometry
     * @throws IllegalStateException               if the codec is closed
     * @throws WhatsAppCallException.H264          if an OpenH264 encode call fails
     * @throws WhatsAppCallException.Vpx           if a libvpx encode call fails
     */
    EncodedVideoFrame encode(VideoFrame frame, boolean forceKeyFrame);

    /**
     * Decodes one compressed access unit into a raw 4:2:0 picture.
     *
     * @implSpec Implementations must reconstruct the next picture from the supplied access unit and
     * return it as an {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#I420 I420}
     * {@link VideoFrame} stamped with the supplied presentation timestamp. A codec that buffers
     * references internally may consume an access unit that yields no displayable picture yet (for
     * example a decoder waiting for the first key frame); in that case implementations must return
     * {@code null} rather than an empty frame. When the access unit is malformed or the decoder rejects
     * it, implementations throw the exception specific to the codec.
     *
     * @param payload   the compressed access unit bytes
     * @param ptsMicros the presentation timestamp in microseconds to stamp the decoded frame with
     * @return the decoded picture, or {@code null} when the access unit produced no displayable frame
     * @throws NullPointerException       if {@code payload} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if an OpenH264 decode call fails
     * @throws WhatsAppCallException.Vpx  if a libvpx decode call fails
     */
    VideoFrame decode(byte[] payload, long ptsMicros);

    /**
     * Decodes one compressed access unit into a raw 4:2:0 picture, packing the result into {@code reuse}
     * when that buffer exactly fits the decoded geometry.
     *
     * <p>This is the pooled decode path the media plane drives so a stream at steady resolution produces no
     * pixel allocation per frame: the caller passes a buffer it owns from a small ring, and the codec
     * writes the decoded planes into it instead of minting a fresh array. The decoded picture size is
     * known only after the access unit is parsed, so {@code reuse} is a hint rather than a guarantee: the
     * codec adopts it only when {@code reuse.length} equals the decoded picture's packed 4:2:0 byte
     * count, and otherwise allocates a correctly sized buffer, so the caller inspects the returned
     * frame's {@linkplain VideoFrame#pixels() buffer} to learn which array was used and to reseed its
     * ring slot after a geometry change.
     *
     * @implSpec The default implementation ignores {@code reuse} and delegates to
     * {@link #decode(byte[], long)}, always returning a freshly allocated buffer. An implementation that
     * packs its output into a caller buffer overrides this to write into {@code reuse} when
     * {@code reuse != null} and {@code reuse.length} equals the decoded picture's packed byte count,
     * returning a frame that wraps {@code reuse}; when {@code reuse} does not fit, when it is
     * {@code null}, or when the access unit yields no displayable picture, the implementation must not
     * write into {@code reuse} and must behave exactly as {@link #decode(byte[], long)}. The returned
     * frame and the {@code null} end of stream contract are identical to {@link #decode(byte[], long)}.
     *
     * @param payload   the compressed access unit bytes
     * @param ptsMicros the presentation timestamp in microseconds to stamp the decoded frame with
     * @param reuse     a caller owned buffer to pack the decoded picture into when it fits, or
     *                  {@code null} to always allocate
     * @return the decoded picture, wrapping {@code reuse} when it was adopted, or {@code null} when the
     *         access unit produced no displayable frame
     * @throws NullPointerException       if {@code payload} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if an OpenH264 decode call fails
     * @throws WhatsAppCallException.Vpx  if a libvpx decode call fails
     */
    default VideoFrame decode(byte[] payload, long ptsMicros, byte[] reuse) {
        return decode(payload, ptsMicros);
    }

    /**
     * One decoded picture packed contiguously into a caller owned off heap segment, the shape the host
     * render path hands to {@link com.github.auties00.cobalt.calls.platform.VoipHostApi#renderVideoFrame}.
     *
     * <p>The {@link #plane()} segment holds the same contiguous {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#I420 I420}
     * bytes a {@link VideoFrame#pixels()} array would carry, packed without stride padding, and is allocated
     * from the {@link SegmentAllocator} the caller passed to {@link #decodeToNative(byte[], long, SegmentAllocator)};
     * its lifetime is therefore the caller's, valid only until that allocator's arena is closed.
     *
     * @param plane     the contiguous off heap I420 picture bytes; never {@code null}
     * @param width     the picture width in pixels
     * @param height    the picture height in pixels
     * @param ptsMicros the presentation timestamp in microseconds stamped on the decoded frame
     */
    record NativeDecodedFrame(MemorySegment plane, int width, int height, long ptsMicros) {
    }

    /**
     * Decodes one compressed access unit straight into a caller allocated off heap segment, the render fast
     * path taken when the call renders received video through the host rather than an application sink.
     *
     * <p>The host render surface consumes an off heap {@link com.github.auties00.cobalt.calls.stream.VideoPixelFormat#I420 I420}
     * plane segment, so this returns the decoded picture already packed off heap: a codec whose decoder holds
     * its picture in native memory packs it directly into a segment obtained from {@code allocator}, sparing
     * the round trip through a heap {@link VideoFrame#pixels()} array and a second copy from heap to native
     * that {@link #decode(byte[], long)} followed by a manual copy would incur.
     *
     * @implSpec The default implementation decodes through {@link #decode(byte[], long)} and copies the
     * resulting heap picture into a fresh segment from {@code allocator}, so it is byte identical to that
     * path for a codec that does not override it. An overriding implementation must pack the same contiguous
     * I420 bytes {@link #decode(byte[], long)} would produce and must return {@code null} for an access unit
     * that yields no displayable picture, exactly as {@link #decode(byte[], long)} does.
     *
     * @param payload   the compressed access unit bytes
     * @param ptsMicros the presentation timestamp in microseconds to stamp the decoded frame with
     * @param allocator the allocator the decoded picture segment is taken from; its arena owns the segment's
     *                  lifetime
     * @return the decoded picture packed off heap, or {@code null} when the access unit produced no
     *         displayable frame
     * @throws NullPointerException       if {@code payload} or {@code allocator} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if an OpenH264 decode call fails
     * @throws WhatsAppCallException.Vpx  if a libvpx decode call fails
     */
    default NativeDecodedFrame decodeToNative(byte[] payload, long ptsMicros, SegmentAllocator allocator) {
        var frame = decode(payload, ptsMicros);
        if (frame == null) {
            return null;
        }
        var pixels = frame.pixels();
        var plane = allocator.allocate(pixels.length);
        MemorySegment.copy(pixels, 0, plane, ValueLayout.JAVA_BYTE, 0, pixels.length);
        return new NativeDecodedFrame(plane, frame.width(), frame.height(), frame.ptsMicros());
    }

    /**
     * Arms the encoder so that its next {@linkplain #encode(VideoFrame, boolean) encode} produces a key
     * frame.
     *
     * @implSpec Implementations must make the very next encoded picture an intra picture, equivalent to
     * passing {@code forceKeyFrame = true} on that call, and must count the request in
     * {@link VideoCodecStats#keyFrameRequests()}. The request is one shot: it is consumed by the next
     * encode and does not persist. Calling this method has no effect on the decode path.
     *
     * @throws IllegalStateException if the codec is closed
     */
    void requestKeyFrame();

    /**
     * Reconfigures the encoder mid call from the mutable subset of the given parameters.
     *
     * @implSpec Implementations must reapply only the controls a live encoder accepts without a
     * reopen: the bitrate triplet, the frame rate, the quantizer window, the frame skip toggle, and the
     * IDR bitrate ratio. The codec ({@link VideoCodecParams#codec()}), the picture geometry
     * ({@link VideoCodecParams#width()}, {@link VideoCodecParams#height()}), the temporal layer count,
     * and the long term reference toggle must not change; such a change requires tearing the codec down
     * and reopening it.
     *
     * @param params the parameter set whose mutable fields the encoder adopts
     * @throws NullPointerException       if {@code params} is {@code null}
     * @throws IllegalArgumentException   if {@code params} selects a different codec or geometry than
     *                                    the one this instance was opened with
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if an OpenH264 control call fails
     * @throws WhatsAppCallException.Vpx  if a libvpx control call fails
     */
    void modify(VideoCodecParams params);

    /**
     * Returns a snapshot of this codec's lifetime counters.
     *
     * @return the current stats snapshot
     */
    VideoCodecStats stats();

    /**
     * Releases the native codec state and any owned resources.
     *
     * @implSpec Implementations must be idempotent. After closing, every other method except a repeated
     * {@link #close()} throws {@link IllegalStateException}.
     */
    @Override
    void close();
}
