package com.github.auties00.cobalt.calls.media.video.codec.h264;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;
import com.github.auties00.cobalt.calls.media.video.codec.h264.bindings.CobaltOpenH264;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import com.github.auties00.cobalt.calls.media.video.codec.EncodedVideoFrame;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodec;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodecParams;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodecStats;

/**
 * Implements {@link VideoCodec} on top of Cisco's OpenH264 encoder and decoder, reached through the
 * portable {@link CobaltOpenH264} shim binding.
 *
 * <p>An instance creates one encoder and one decoder for a single H.264 stream. The encode path copies
 * each {@link VideoFrame} into a native I420 staging buffer and drives
 * {@link CobaltOpenH264#cobalt_h264_encoder_encode}, then retrieves the one contiguous access unit the
 * shim concatenated from every NAL of every emitted layer with
 * {@link CobaltOpenH264#cobalt_h264_encoder_get_packet}; the decode path feeds each access unit to
 * {@link CobaltOpenH264#cobalt_h264_decoder_decode} and copies out the reconstructed I420 picture the
 * shim exposes through its plane getters. The {@link VideoCodecParams} map onto the scalar arguments of
 * {@link CobaltOpenH264#cobalt_h264_encoder_create}, which builds the OpenH264 {@code SEncParamExt}
 * block on the C side; the live bitrate and frame rate knobs map onto
 * {@link CobaltOpenH264#cobalt_h264_encoder_set_rates}.
 *
 * <p>The codec is single writer: the encode, decode, key frame request, and reconfiguration paths must
 * run on one thread, since the OpenH264 objects are not internally synchronized and this class reuses one
 * {@link Arena} of scratch segments and shim owned scratch behind the handles. Every native segment lives
 * in that arena and is released together on {@link #close()}, after the encoder and decoder handles are
 * destroyed by their shim destroy calls, which uninitialize and free the underlying OpenH264 objects.
 *
 * @implNote This implementation drives a fixed OpenH264 configuration matching WhatsApp's camera video
 * path: the camera real time usage type, the bitrate mode rate control, the qp window, the intra period,
 * the IDR bitrate ratio, the frame skip toggle, and the temporal layer and long term reference features,
 * all passed as scalars to the shim's encoder create. Per frame it encodes one {@code SSourcePicture} and
 * forces an IDR through {@link CobaltOpenH264#cobalt_h264_encoder_force_idr} when a key frame is due; key
 * frame classification is reported by the shim from {@code SFrameBSInfo.eFrameType} against
 * {@code videoFrameTypeIDR}. The C++ vtable dispatch that the C level idiom
 * {@code (*object)->Method(object, ...)} expresses is performed inside the shim, which calls the OpenH264
 * vtable methods directly on the C side, so no vtable layout or method slot handling reaches Java and the
 * FFM binding is host ABI independent.
 */
public final class H264VideoCodec implements VideoCodec {
    /**
     * The logger for {@link H264VideoCodec}.
     */
    private static final System.Logger LOGGER = Log.get(H264VideoCodec.class);

    /**
     * The neutral complexity that maps to the low complexity real time mode, also used for any negative
     * complexity value.
     */
    private static final int COMPLEXITY_LOW_THRESHOLD = 0;

    /**
     * The complexity value that maps to the medium complexity mode; any larger value maps to high.
     */
    private static final int COMPLEXITY_MEDIUM_VALUE = 1;

    /**
     * The codec format this instance implements, always {@link VideoDecoderCapability#H264 H264}.
     */
    private final VideoDecoderCapability codec;

    /**
     * The arena owning every native segment this codec allocates, released as a unit on {@link #close()}.
     */
    private final Arena arena;

    /**
     * The opaque OpenH264 encoder handle returned by {@link CobaltOpenH264#cobalt_h264_encoder_create}.
     *
     * <p>Reassigned by {@link #modify(VideoCodecParams)} when a parameter change takes the reopen branch,
     * where the encoder is torn down and recreated, so it is not {@code final}.
     */
    private MemorySegment encoderCtx;

    /**
     * The opaque OpenH264 decoder handle returned by {@link CobaltOpenH264#cobalt_h264_decoder_create}.
     */
    private final MemorySegment decoderCtx;

    /**
     * The reusable cell receiving the concatenated access unit pointer from
     * {@link CobaltOpenH264#cobalt_h264_encoder_get_packet}.
     */
    private final MemorySegment packetBufCell;

    /**
     * The reusable cell receiving the concatenated access unit length from
     * {@link CobaltOpenH264#cobalt_h264_encoder_get_packet}.
     */
    private final MemorySegment packetLenCell;

    /**
     * The reusable cell receiving the access unit key frame flag from
     * {@link CobaltOpenH264#cobalt_h264_encoder_get_packet}.
     */
    private final MemorySegment packetKeyCell;

    /**
     * The reusable cell receiving the decoded picture handle from
     * {@link CobaltOpenH264#cobalt_h264_decoder_get_frame}.
     */
    private final MemorySegment frameImgCell;

    /**
     * The growable native I420 staging buffer the source pixels are copied into before each encode,
     * reused across frames so the encode path makes no allocation per frame.
     */
    private MemorySegment encodeStaging;

    /**
     * The byte capacity of {@link #encodeStaging}.
     */
    private long encodeStagingSize;

    /**
     * The growable native input buffer each access unit is copied into before each decode, reused across
     * frames so the decode path makes no allocation per frame.
     */
    private MemorySegment decodeInput;

    /**
     * The byte capacity of {@link #decodeInput}.
     */
    private long decodeInputSize;

    /**
     * The codec parameters in force, updated on each {@link #modify(VideoCodecParams)} round.
     */
    private VideoCodecParams params;

    /**
     * The monotonic millisecond timestamp OpenH264 requires on each source picture.
     */
    private long encodeTimestamp;

    /**
     * Whether a key frame has been requested and not yet emitted.
     */
    private boolean keyFrameRequested;

    /**
     * The running counters surfaced by {@link #stats()}.
     */
    private long framesEncoded;
    private long framesDecoded;
    private long keyFramesEncoded;
    private long keyFrameRequests;
    private long bytesEncoded;
    private long bytesDecoded;

    /**
     * Whether {@link #close()} has been called.
     */
    private boolean closed;

    /**
     * Opens an OpenH264 codec for the given parameters, creating both the encoder and decoder.
     *
     * @param params the codec parameters; must select {@link VideoDecoderCapability#H264 H264}
     * @throws NullPointerException       if {@code params} is {@code null}
     * @throws IllegalArgumentException   if {@code params} does not select H264
     * @throws WhatsAppCallException.H264 if OpenH264 fails to create or initialize either object
     */
    public H264VideoCodec(VideoCodecParams params) {
        Objects.requireNonNull(params, "params cannot be null");
        if (params.codec() != VideoDecoderCapability.H264) {
            throw new IllegalArgumentException("H264VideoCodec requires H264 params, got " + params.codec());
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "opening h264 codec, {0}x{1} bitrate={2} fps={3}",
                    params.width(), params.height(), params.targetBitrate(), params.frameRate());
        }
        this.codec = VideoDecoderCapability.H264;
        this.params = params;
        this.arena = Arena.ofShared();
        MemorySegment enc = null;
        MemorySegment dec = null;
        try {
            enc = openEncoder();
            var decCell = arena.allocate(CobaltOpenH264.C_POINTER);
            var decErr = CobaltOpenH264.cobalt_h264_decoder_create(decCell);
            if (decErr != CobaltOpenH264.COBALT_H264_OK()) {
                throw nativeFailure("cobalt_h264_decoder_create", decErr, MemorySegment.NULL);
            }
            dec = decCell.get(CobaltOpenH264.C_POINTER, 0);
            this.encoderCtx = enc;
            this.decoderCtx = dec;
            this.packetBufCell = arena.allocate(CobaltOpenH264.C_POINTER);
            this.packetLenCell = arena.allocate(CobaltOpenH264.C_INT);
            this.packetKeyCell = arena.allocate(CobaltOpenH264.C_INT);
            this.frameImgCell = arena.allocate(CobaltOpenH264.C_POINTER);
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "h264 codec open failed", e);
            if (dec != null) {
                CobaltOpenH264.cobalt_h264_decoder_destroy(dec);
            }
            if (enc != null) {
                CobaltOpenH264.cobalt_h264_encoder_destroy(enc);
            }
            arena.close();
            throw e;
        }
    }

    /**
     * Creates an OpenH264 encoder configured for the current {@link #params}.
     *
     * <p>Shared by the constructor and the reopen branch of {@link #modify(VideoCodecParams)}; the caller
     * sets {@link #params} to the desired configuration before calling. Builds the {@code SEncParamExt} on
     * the C side from the camera real time usage, bitrate rate control mode, the mapped complexity, and the
     * geometry, quantizer window, temporal layer, keyframe interval, IDR bitrate ratio, frame skip, and
     * long term reference parameters.
     *
     * @return the opaque encoder handle
     * @throws WhatsAppCallException.H264 if OpenH264 fails to create or initialize the encoder
     */
    private MemorySegment openEncoder() {
        var encCell = arena.allocate(CobaltOpenH264.C_POINTER);
        var encErr = CobaltOpenH264.cobalt_h264_encoder_create(
                params.width(),
                params.height(),
                params.targetBitrate(),
                params.maxBitrate(),
                params.frameRate(),
                CobaltOpenH264.COBALT_H264_USAGE_CAMERA_REALTIME(),
                CobaltOpenH264.COBALT_H264_RC_BITRATE(),
                complexitySelector(),
                params.temporalLayers(),
                params.keyFrameIntervalFrames(),
                params.minQuantizer(),
                params.maxQuantizer(),
                params.idrBitrateRatio(),
                params.frameSkip() ? 1 : 0,
                params.longTermReference() ? 1 : 0,
                encCell);
        if (encErr != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_encoder_create", encErr, MemorySegment.NULL);
        }
        return encCell.get(CobaltOpenH264.C_POINTER, 0);
    }

    /**
     * Maps the configured complexity onto a {@code COBALT_H264_COMPLEXITY_*} selector.
     *
     * @implNote This implementation treats the neutral complexity {@code 0} and any negative value as
     * the low complexity real time mode, a complexity of {@code 1} as medium, and a larger value as high,
     * because OpenH264 exposes three discrete complexity modes rather than a continuous scale.
     *
     * @return the {@code COBALT_H264_COMPLEXITY_*} selector
     */
    private int complexitySelector() {
        var complexity = params.complexity();
        if (complexity <= COMPLEXITY_LOW_THRESHOLD) {
            return CobaltOpenH264.COBALT_H264_COMPLEXITY_LOW();
        }
        if (complexity == COMPLEXITY_MEDIUM_VALUE) {
            return CobaltOpenH264.COBALT_H264_COMPLEXITY_MEDIUM();
        }
        return CobaltOpenH264.COBALT_H264_COMPLEXITY_HIGH();
    }

    /**
     * {@inheritDoc}
     *
     * @return always {@link VideoDecoderCapability#H264 H264}
     */
    @Override
    public VideoDecoderCapability codec() {
        return codec;
    }

    /**
     * {@inheritDoc}
     *
     * @param frame         the raw picture to encode
     * @param forceKeyFrame whether to force this picture to a key frame
     * @return the encoded access unit, empty when the encoder dropped the frame
     * @throws NullPointerException       if {@code frame} is {@code null}
     * @throws IllegalArgumentException   if the frame dimensions do not match the configured geometry
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if the OpenH264 encode call fails
     */
    @Override
    public EncodedVideoFrame encode(VideoFrame frame, boolean forceKeyFrame) {
        ensureOpen();
        Objects.requireNonNull(frame, "frame cannot be null");
        if (frame.width() != params.width() || frame.height() != params.height()) {
            throw new IllegalArgumentException(
                    "frame " + frame.width() + "x" + frame.height() + " does not match codec geometry "
                            + params.width() + "x" + params.height());
        }
        if (forceKeyFrame || keyFrameRequested) {
            var forceErr = CobaltOpenH264.cobalt_h264_encoder_force_idr(encoderCtx);
            if (forceErr != CobaltOpenH264.COBALT_H264_OK()) {
                throw nativeFailure("cobalt_h264_encoder_force_idr", forceErr, encoderCtx);
            }
            keyFrameRequested = false;
        }
        var planar = frame.pixels();
        var pixels = encodeStagingFor(planar.length);
        MemorySegment.copy(planar, 0, pixels, ValueLayout.JAVA_BYTE, 0, planar.length);
        var err = CobaltOpenH264.cobalt_h264_encoder_encode(encoderCtx, pixels, planar.length,
                frame.width(), frame.height(), encodeTimestamp);
        if (err != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_encoder_encode", err, encoderCtx);
        }
        encodeTimestamp += Math.max(1, 1000 / params.frameRate());
        return collectAccessUnit(frame);
    }

    /**
     * Retrieves the one contiguous access unit the encoder produced into an {@link EncodedVideoFrame}.
     *
     * @implNote This implementation calls {@link CobaltOpenH264#cobalt_h264_encoder_get_packet}, which
     * concatenates every NAL of every emitted layer on the C side and returns a pointer into shim owned
     * memory plus its total length and the IDR key frame flag. The bytes are copied onto the heap
     * immediately, before any further shim call can invalidate that pointer. A skipped frame yields a zero
     * length, surfaced as an empty access unit.
     *
     * @param source the source frame, for the carried timestamp and dimensions
     * @return the encoded access unit, empty when the encoder skipped the frame
     */
    private EncodedVideoFrame collectAccessUnit(VideoFrame source) {
        var err = CobaltOpenH264.cobalt_h264_encoder_get_packet(encoderCtx, packetBufCell, packetLenCell, packetKeyCell);
        if (err != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_encoder_get_packet", err, encoderCtx);
        }
        var len = packetLenCell.get(CobaltOpenH264.C_INT, 0);
        var payload = DataUtils.EMPTY_BYTE_ARRAY;
        var keyFrame = false;
        if (len > 0) {
            var buf = packetBufCell.get(CobaltOpenH264.C_POINTER, 0).reinterpret(len);
            payload = new byte[len];
            MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, 0, payload, 0, len);
            keyFrame = packetKeyCell.get(CobaltOpenH264.C_INT, 0) != 0;
            framesEncoded++;
            bytesEncoded += len;
            if (keyFrame) {
                keyFramesEncoded++;
            }
            if (Log.TRACE) LOGGER.log(Level.TRACE, "h264 access unit encoded, bytes={0} keyFrame={1}", len, keyFrame);
        }
        return new EncodedVideoFrame(payload, codec, keyFrame, source.width(), source.height(), source.ptsMicros());
    }

    /**
     * {@inheritDoc}
     *
     * @param payload   the compressed access unit bytes
     * @param ptsMicros the timestamp to stamp on the decoded frame
     * @return the decoded I420 frame, or {@code null} when no displayable frame was produced
     * @throws NullPointerException       if {@code payload} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if the OpenH264 decode call fails
     */
    @Override
    public VideoFrame decode(byte[] payload, long ptsMicros) {
        return decode(payload, ptsMicros, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Packs the decoded picture into {@code reuse} when it exactly matches the decoded I420 byte
     * count, so a stream at steady resolution produces no pixel allocation per frame; a fresh buffer is
     * minted on the first frame and after each resolution change.
     *
     * @param payload   {@inheritDoc}
     * @param ptsMicros {@inheritDoc}
     * @param reuse     {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NullPointerException       if {@code payload} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if the OpenH264 decode call fails
     */
    @Override
    public VideoFrame decode(byte[] payload, long ptsMicros, byte[] reuse) {
        ensureOpen();
        Objects.requireNonNull(payload, "payload cannot be null");
        var input = decodeInputFor(Math.max(1, payload.length));
        MemorySegment.copy(payload, 0, input, ValueLayout.JAVA_BYTE, 0, payload.length);
        var err = CobaltOpenH264.cobalt_h264_decoder_decode(decoderCtx, input, payload.length);
        if (err != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_decoder_decode", err, decoderCtx);
        }
        var frameErr = CobaltOpenH264.cobalt_h264_decoder_get_frame(decoderCtx, frameImgCell);
        if (frameErr != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_decoder_get_frame", frameErr, decoderCtx);
        }
        var img = frameImgCell.get(CobaltOpenH264.C_POINTER, 0);
        if (img.equals(MemorySegment.NULL)) {
            return null;
        }
        var frame = copyDecodedImage(img, ptsMicros, reuse);
        framesDecoded++;
        bytesDecoded += payload.length;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "h264 picture decoded, payload bytes={0}", payload.length);
        return frame;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Packs the decoded picture's strided native planes straight into a contiguous segment from
     * {@code allocator}, stripping OpenH264's stride padding row by row, so the host render path takes the
     * decoded bytes off heap without the intermediate heap {@link VideoFrame#pixels()} array and the second
     * heap to native copy the {@link #decode(byte[], long)} path would need.
     *
     * @param payload   {@inheritDoc}
     * @param ptsMicros {@inheritDoc}
     * @param allocator {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NullPointerException       if {@code payload} or {@code allocator} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if the OpenH264 decode call fails
     */
    @Override
    public NativeDecodedFrame decodeToNative(byte[] payload, long ptsMicros, SegmentAllocator allocator) {
        ensureOpen();
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(allocator, "allocator cannot be null");
        var input = decodeInputFor(Math.max(1, payload.length));
        MemorySegment.copy(payload, 0, input, ValueLayout.JAVA_BYTE, 0, payload.length);
        var err = CobaltOpenH264.cobalt_h264_decoder_decode(decoderCtx, input, payload.length);
        if (err != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_decoder_decode", err, decoderCtx);
        }
        var frameErr = CobaltOpenH264.cobalt_h264_decoder_get_frame(decoderCtx, frameImgCell);
        if (frameErr != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_decoder_get_frame", frameErr, decoderCtx);
        }
        var img = frameImgCell.get(CobaltOpenH264.C_POINTER, 0);
        if (img.equals(MemorySegment.NULL)) {
            return null;
        }
        var picture = copyDecodedImageToNative(img, ptsMicros, allocator);
        framesDecoded++;
        bytesDecoded += payload.length;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "h264 picture decoded to native, payload bytes={0}", payload.length);
        return picture;
    }

    /**
     * Copies a decoded OpenH264 picture into a planar I420 {@link VideoFrame}, packing each plane row by
     * row to strip OpenH264's stride padding.
     *
     * @param img       the decoded picture handle from {@link CobaltOpenH264#cobalt_h264_decoder_get_frame}
     * @param ptsMicros the timestamp to stamp on the produced frame
     * @param reuse     a caller owned buffer to pack the planes into when it exactly matches the decoded
     *                  I420 byte count, or {@code null} to allocate a fresh buffer
     * @return the packed I420 frame, wrapping {@code reuse} when it was adopted
     */
    private VideoFrame copyDecodedImage(MemorySegment img, long ptsMicros, byte[] reuse) {
        var width = CobaltOpenH264.cobalt_h264_img_width(img);
        var height = CobaltOpenH264.cobalt_h264_img_height(img);
        var lumaStride = CobaltOpenH264.cobalt_h264_img_stride(img, 0);
        var chromaStride = CobaltOpenH264.cobalt_h264_img_stride(img, 1);
        var chromaWidth = width / 2;
        var chromaHeight = height / 2;
        var lumaSize = width * height;
        var chromaSize = chromaWidth * chromaHeight;
        var needed = lumaSize + 2 * chromaSize;
        var out = reuse != null && reuse.length == needed ? reuse : new byte[needed];
        var yPlane = CobaltOpenH264.cobalt_h264_img_plane(img, 0).reinterpret(Long.MAX_VALUE);
        var uPlane = CobaltOpenH264.cobalt_h264_img_plane(img, 1).reinterpret(Long.MAX_VALUE);
        var vPlane = CobaltOpenH264.cobalt_h264_img_plane(img, 2).reinterpret(Long.MAX_VALUE);
        copyPlane(yPlane, lumaStride, out, 0, width, height);
        copyPlane(uPlane, chromaStride, out, lumaSize, chromaWidth, chromaHeight);
        copyPlane(vPlane, chromaStride, out, lumaSize + chromaSize, chromaWidth, chromaHeight);
        return new VideoFrame(out, VideoPixelFormat.I420, width, height, ptsMicros);
    }

    /**
     * Copies one image plane out of native memory, row by row, discarding stride padding.
     *
     * @param plane   the native plane segment, reinterpreted large enough to read every row
     * @param stride  the plane stride in bytes
     * @param dst     the destination heap array
     * @param dstBase the offset into {@code dst} where this plane begins
     * @param rowSize the packed bytes per row (the plane width)
     * @param rows    the number of rows in the plane
     */
    private void copyPlane(MemorySegment plane, int stride, byte[] dst, int dstBase, int rowSize, int rows) {
        if (stride == rowSize) {
            MemorySegment.copy(plane, ValueLayout.JAVA_BYTE, 0, dst, dstBase, rowSize * rows);
            return;
        }
        for (var row = 0; row < rows; row++) {
            MemorySegment.copy(plane, ValueLayout.JAVA_BYTE, (long) row * stride, dst, dstBase + row * rowSize, rowSize);
        }
    }

    /**
     * Packs a decoded OpenH264 picture into a contiguous off heap I420 segment, stripping stride padding, the
     * off heap counterpart of {@link #copyDecodedImage(MemorySegment, long, byte[])}.
     *
     * @param img       the decoded picture handle from {@link CobaltOpenH264#cobalt_h264_decoder_get_frame}
     * @param ptsMicros the timestamp to stamp on the produced picture
     * @param allocator the allocator the packed segment is taken from
     * @return the packed picture, its {@link NativeDecodedFrame#plane()} owned by {@code allocator}'s arena
     */
    private NativeDecodedFrame copyDecodedImageToNative(MemorySegment img, long ptsMicros, SegmentAllocator allocator) {
        var width = CobaltOpenH264.cobalt_h264_img_width(img);
        var height = CobaltOpenH264.cobalt_h264_img_height(img);
        var lumaStride = CobaltOpenH264.cobalt_h264_img_stride(img, 0);
        var chromaStride = CobaltOpenH264.cobalt_h264_img_stride(img, 1);
        var chromaWidth = width / 2;
        var chromaHeight = height / 2;
        var lumaSize = width * height;
        var chromaSize = chromaWidth * chromaHeight;
        var needed = lumaSize + 2 * chromaSize;
        var out = allocator.allocate(needed);
        var yPlane = CobaltOpenH264.cobalt_h264_img_plane(img, 0).reinterpret(Long.MAX_VALUE);
        var uPlane = CobaltOpenH264.cobalt_h264_img_plane(img, 1).reinterpret(Long.MAX_VALUE);
        var vPlane = CobaltOpenH264.cobalt_h264_img_plane(img, 2).reinterpret(Long.MAX_VALUE);
        copyPlaneToNative(yPlane, lumaStride, out, 0, width, height);
        copyPlaneToNative(uPlane, chromaStride, out, lumaSize, chromaWidth, chromaHeight);
        copyPlaneToNative(vPlane, chromaStride, out, lumaSize + chromaSize, chromaWidth, chromaHeight);
        return new NativeDecodedFrame(out, width, height, ptsMicros);
    }

    /**
     * Copies one image plane between native segments, row by row, discarding stride padding, the off heap
     * counterpart of {@link #copyPlane(MemorySegment, int, byte[], int, int, int)}.
     *
     * @param plane   the native plane segment, reinterpreted large enough to read every row
     * @param stride  the plane stride in bytes
     * @param dst     the destination off heap segment
     * @param dstBase the offset into {@code dst} where this plane begins
     * @param rowSize the packed bytes per row (the plane width)
     * @param rows    the number of rows in the plane
     */
    private void copyPlaneToNative(MemorySegment plane, int stride, MemorySegment dst, long dstBase, int rowSize, int rows) {
        if (stride == rowSize) {
            MemorySegment.copy(plane, 0, dst, dstBase, (long) rowSize * rows);
            return;
        }
        for (var row = 0; row < rows; row++) {
            MemorySegment.copy(plane, (long) row * stride, dst, dstBase + (long) row * rowSize, rowSize);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the codec is closed
     */
    @Override
    public void requestKeyFrame() {
        ensureOpen();
        keyFrameRequested = true;
        keyFrameRequests++;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "h264 key frame requested, total requests={0}", keyFrameRequests);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation splits reconfiguration into two branches. When only the target bitrate
     * or frame rate changes, it applies them without a reopen through
     * {@link CobaltOpenH264#cobalt_h264_encoder_set_rates}, where the shim drives
     * {@code SetOption(ENCODER_OPTION_BITRATE)} with an {@code SBitrateInfo} targeting all spatial layers and
     * {@code SetOption(ENCODER_OPTION_FRAME_RATE)} with a single {@code float}. When the quantizer window,
     * frame skip, or IDR bitrate ratio changes, it tears the encoder down and recreates it with the new
     * parameters, because those have no live {@code ENCODER_OPTION} selector on the camera path (only the
     * screen content {@code ENCODER_OPTION_MAXQP_SCREEN} variant is live). The codec and geometry cannot
     * change on a live encoder.
     *
     * @param params the parameter set whose mutable fields the encoder adopts
     * @throws NullPointerException       if {@code params} is {@code null}
     * @throws IllegalArgumentException   if {@code params} changes the codec or geometry
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.H264 if reconfiguring or reopening the encoder fails
     */
    @Override
    public void modify(VideoCodecParams params) {
        ensureOpen();
        Objects.requireNonNull(params, "params cannot be null");
        if (params.equals(this.params)) {
            return;
        }
        if (params.codec() != codec) {
            throw new IllegalArgumentException("cannot change codec from " + codec + " to " + params.codec());
        }
        if (params.width() != this.params.width() || params.height() != this.params.height()) {
            throw new IllegalArgumentException("cannot change geometry on a live OpenH264 encoder");
        }
        var previous = this.params;
        this.params = params;
        var reopen = params.minQuantizer() != previous.minQuantizer()
                || params.maxQuantizer() != previous.maxQuantizer()
                || params.frameSkip() != previous.frameSkip()
                || params.idrBitrateRatio() != previous.idrBitrateRatio();
        if (reopen) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "h264 encoder reopening for quantizer/frameSkip/idrRatio change");
            var replacement = openEncoder();
            CobaltOpenH264.cobalt_h264_encoder_destroy(encoderCtx);
            encoderCtx = replacement;
            return;
        }
        var err = CobaltOpenH264.cobalt_h264_encoder_set_rates(encoderCtx, params.targetBitrate(), params.frameRate());
        if (err != CobaltOpenH264.COBALT_H264_OK()) {
            throw nativeFailure("cobalt_h264_encoder_set_rates", err, encoderCtx);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "h264 encoder rates updated, bitrate={0} fps={1}", params.targetBitrate(), params.frameRate());
    }

    /**
     * {@inheritDoc}
     *
     * @return the current stats snapshot
     */
    @Override
    public VideoCodecStats stats() {
        return new VideoCodecStats(framesEncoded, framesDecoded, keyFramesEncoded, keyFrameRequests, bytesEncoded, bytesDecoded);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Destroys both OpenH264 handles, then closes the owning arena; idempotent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        CobaltOpenH264.cobalt_h264_decoder_destroy(decoderCtx);
        CobaltOpenH264.cobalt_h264_encoder_destroy(encoderCtx);
        arena.close();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "h264 codec closed");
    }

    /**
     * Returns the reusable encode staging segment, growing it from the arena when the requested size
     * exceeds its current capacity.
     *
     * @param size the required byte capacity
     * @return a native segment of at least {@code size} bytes
     */
    private MemorySegment encodeStagingFor(long size) {
        if (encodeStaging == null || encodeStagingSize < size) {
            encodeStaging = arena.allocate(size);
            encodeStagingSize = size;
        }
        return encodeStaging;
    }

    /**
     * Returns the reusable decode input segment, growing it from the arena when the requested size
     * exceeds its current capacity.
     *
     * @param size the required byte capacity
     * @return a native segment of at least {@code size} bytes
     */
    private MemorySegment decodeInputFor(long size) {
        if (decodeInput == null || decodeInputSize < size) {
            decodeInput = arena.allocate(size);
            decodeInputSize = size;
        }
        return decodeInput;
    }

    /**
     * Builds an OpenH264 failure for a nonzero shim status, appending the shim's textual description
     * and, when available, the original native status code behind the handle.
     *
     * @implNote This implementation reads the shim's {@link CobaltOpenH264#cobalt_h264_strerror}
     * description for the stable status code, and for the {@code COBALT_H264_NATIVE_ERROR} code also
     * recovers the underlying OpenH264 status the shim stashed behind the handle with
     * {@link CobaltOpenH264#cobalt_h264_last_native_status}, which is meaningful only immediately after a
     * native error return on that handle. A {@link MemorySegment#NULL} handle skips the native status
     * lookup, for failures raised before a handle exists.
     *
     * @param operation the failing shim function name
     * @param status    the nonzero shim status code
     * @param handle    the encoder or decoder handle the call was made on, or {@link MemorySegment#NULL}
     * @return the exception to throw
     */
    private WhatsAppCallException.H264 nativeFailure(String operation, int status, MemorySegment handle) {
        var description = CobaltOpenH264.cobalt_h264_strerror(status).reinterpret(Long.MAX_VALUE).getString(0);
        var message = operation + " failed: " + description + " (" + status + ")";
        if (status == CobaltOpenH264.COBALT_H264_NATIVE_ERROR() && !handle.equals(MemorySegment.NULL)) {
            var nativeStatus = CobaltOpenH264.cobalt_h264_last_native_status(handle);
            message += ", openh264 status 0x" + Integer.toHexString(nativeStatus);
        }
        if (Log.WARNING) LOGGER.log(Level.WARNING, "h264 native call {0} failed with status {1}", operation, status);
        return new WhatsAppCallException.H264(message);
    }

    /**
     * Throws if this codec has been closed.
     *
     * @throws IllegalStateException if {@link #close()} has been called
     */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("H264VideoCodec is closed");
        }
    }
}
