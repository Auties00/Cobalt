package com.github.auties00.cobalt.calls.media.video.codec.vpx;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;
import com.github.auties00.cobalt.calls.media.video.codec.vpx.bindings.CobaltVpx;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import com.github.auties00.cobalt.calls.media.video.codec.EncodedVideoFrame;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodec;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodecParams;
import com.github.auties00.cobalt.calls.media.video.codec.VideoCodecStats;

/**
 * The {@link VideoCodec} implementation backed by libvpx, wrapping the VP8 and VP9 encoder and decoder
 * of the bundled libvpx through its portable {@link CobaltVpx} shim binding.
 *
 * <p>An instance opens one libvpx encoder and one decoder for a single stream. The encode path copies
 * each {@link VideoFrame} into a native staging buffer and drives {@link CobaltVpx#cobalt_vpx_encoder_encode}
 * at the realtime deadline, then drains the one compressed packet the realtime encoder produces; the
 * decode path feeds each access unit to {@link CobaltVpx#cobalt_vpx_decoder_decode} and copies out the
 * reconstructed I420 picture. Bitrate, quantizer window, keyframe cadence, and frame rate are passed as
 * scalar arguments to {@link CobaltVpx#cobalt_vpx_encoder_create} and
 * {@link CobaltVpx#cobalt_vpx_encoder_reconfigure}, which build the libvpx configuration on the C side; the
 * {@code cpu-used} speed control maps onto {@link VideoCodecParams#complexity()}.
 *
 * <p>This codec is single writer: the encode, decode, key frame request, and reconfiguration paths must
 * run on one thread, since libvpx contexts are not internally synchronized and this class reuses a
 * per codec {@link Arena} of scratch segments. All native segments live in that arena and are released
 * together on {@link #close()}; the encoder and decoder handles are freed by their shim destroy calls.
 *
 * @implNote This implementation drives libvpx at its realtime settings: encode at {@code VPX_DL_REALTIME},
 * the {@code VP8E_SET_CPUUSED} speed control, and constant bitrate rate control ({@code rc_end_usage = VPX_CBR})
 * seeded from {@code vpx_codec_enc_config_default}. The key frame flag is read from the {@code VPX_FRAME_IS_KEY}
 * bit of the compressed packet, and a requested key frame is forced with the {@code VPX_EFLAG_FORCE_KF}
 * encode flag. libvpx's {@code rc_target_bitrate} is in kilobits per second, so the bits per second
 * {@link VideoCodecParams#targetBitrate()} is divided by 1000 inside the shim. VP9 shares this class
 * because libvpx exposes both behind one context API; the concrete interface is selected on the C side by the
 * {@code COBALT_VPX_CODEC_*} argument the shim resolves from {@link VideoCodecParams#codec()}. The shim
 * keeps every libvpx struct on the C side so the FFM binding is host ABI independent.
 */
public final class VpxVideoCodec implements VideoCodec {
    /**
     * The neutral {@code cpu-used} speed applied when {@link VideoCodecParams#complexity()} is {@code 0}.
     *
     * <p>libvpx treats a higher {@code cpu-used} value as faster and lower quality; {@code -6} is the call
     * encoder's realtime default.
     */
    private static final int DEFAULT_CPU_USED = -6;

    /**
     * The frame drop threshold percentage applied when {@link VideoCodecParams#frameSkip()} is set.
     */
    private static final int FRAME_SKIP_DROP_THRESH = 30;

    /**
     * The logger for {@link VpxVideoCodec}.
     */
    private static final System.Logger LOGGER = Log.get(VpxVideoCodec.class);

    /**
     * The codec format this instance implements, {@link VideoDecoderCapability#VP8 VP8} or
     * {@link VideoDecoderCapability#VP9 VP9}.
     */
    private final VideoDecoderCapability codec;

    /**
     * The arena owning every native segment this codec allocates, released as a unit on
     * {@link #close()}.
     */
    private final Arena arena;

    /**
     * The opaque libvpx encoder handle returned by {@link CobaltVpx#cobalt_vpx_encoder_create}.
     */
    private final MemorySegment encoderCtx;

    /**
     * The opaque libvpx decoder handle returned by {@link CobaltVpx#cobalt_vpx_decoder_create}.
     */
    private final MemorySegment decoderCtx;

    /**
     * The reusable cell receiving the compressed packet pointer from
     * {@link CobaltVpx#cobalt_vpx_encoder_get_packet}.
     */
    private final MemorySegment packetBufCell;

    /**
     * The reusable cell receiving the compressed packet length from
     * {@link CobaltVpx#cobalt_vpx_encoder_get_packet}.
     */
    private final MemorySegment packetLenCell;

    /**
     * The reusable cell receiving the compressed packet key frame flag from
     * {@link CobaltVpx#cobalt_vpx_encoder_get_packet}.
     */
    private final MemorySegment packetKeyCell;

    /**
     * The reusable cell receiving the decoded picture handle from
     * {@link CobaltVpx#cobalt_vpx_decoder_get_frame}.
     */
    private final MemorySegment frameImgCell;

    /**
     * The growable native staging buffer the source pixels are copied into before each encode, reused
     * across frames so the encode path makes no allocation per frame.
     */
    private MemorySegment encodeStaging;

    /**
     * The byte capacity of {@link #encodeStaging}.
     */
    private long encodeStagingSize;

    /**
     * The growable native input buffer each access unit is copied into before each decode, reused
     * across frames so the decode path makes no allocation per frame.
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
     * The monotonic presentation timestamp counter libvpx requires; incremented per encoded frame.
     */
    private long encodePts;

    /**
     * Whether a key frame has been requested and not yet emitted.
     */
    private boolean keyFrameRequested;

    /**
     * The count of frames handed to the encoder that produced a compressed packet, surfaced by
     * {@link #stats()}.
     */
    private long framesEncoded;

    /**
     * The count of access units that decoded to a displayable picture, surfaced by {@link #stats()}.
     */
    private long framesDecoded;

    /**
     * The count of encoded frames flagged as key frames, surfaced by {@link #stats()}.
     */
    private long keyFramesEncoded;

    /**
     * The count of {@link #requestKeyFrame()} calls, surfaced by {@link #stats()}.
     */
    private long keyFrameRequests;

    /**
     * The total compressed byte volume the encoder produced, surfaced by {@link #stats()}.
     */
    private long bytesEncoded;

    /**
     * The total compressed byte volume fed to the decoder, surfaced by {@link #stats()}.
     */
    private long bytesDecoded;

    /**
     * Whether {@link #close()} has been called.
     */
    private boolean closed;

    /**
     * Opens a libvpx codec for the given parameters, creating both the encoder and decoder.
     *
     * @param params the codec parameters; must select {@link VideoDecoderCapability#VP8 VP8} or
     *               {@link VideoDecoderCapability#VP9 VP9}
     * @throws NullPointerException      if {@code params} is {@code null}
     * @throws IllegalArgumentException  if {@code params} does not select VP8 or VP9
     * @throws WhatsAppCallException.Vpx if libvpx fails to create either the encoder or the decoder
     */
    public VpxVideoCodec(VideoCodecParams params) {
        Objects.requireNonNull(params, "params cannot be null");
        if (params.codec() != VideoDecoderCapability.VP8 && params.codec() != VideoDecoderCapability.VP9) {
            throw new IllegalArgumentException("VpxVideoCodec requires VP8 or VP9 params, got " + params.codec());
        }
        this.codec = params.codec();
        this.params = params;
        this.arena = Arena.ofShared();
        MemorySegment enc = null;
        MemorySegment dec = null;
        try {
            var encCell = arena.allocate(CobaltVpx.C_POINTER);
            var encErr = CobaltVpx.cobalt_vpx_encoder_create(codecSelector(), params.width(), params.height(),
                    params.targetBitrate(), params.frameRate(), params.minQuantizer(), params.maxQuantizer(),
                    dropFrameThresh(), params.keyFrameIntervalFrames(), cpuUsed(), encCell);
            if (encErr != CobaltVpx.COBALT_VPX_OK()) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx encoder create rejected: codec={0} rc={1}", codec, encErr);
                throw WhatsAppCallException.Vpx.fromErr("cobalt_vpx_encoder_create", encErr);
            }
            enc = encCell.get(CobaltVpx.C_POINTER, 0);
            var decCell = arena.allocate(CobaltVpx.C_POINTER);
            var decErr = CobaltVpx.cobalt_vpx_decoder_create(codecSelector(), params.width(), params.height(), decCell);
            if (decErr != CobaltVpx.COBALT_VPX_OK()) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx decoder create rejected: codec={0} rc={1}", codec, decErr);
                throw WhatsAppCallException.Vpx.fromErr("cobalt_vpx_decoder_create", decErr);
            }
            dec = decCell.get(CobaltVpx.C_POINTER, 0);
            this.encoderCtx = enc;
            this.decoderCtx = dec;
            this.packetBufCell = arena.allocate(CobaltVpx.C_POINTER);
            this.packetLenCell = arena.allocate(CobaltVpx.C_INT);
            this.packetKeyCell = arena.allocate(CobaltVpx.C_INT);
            this.frameImgCell = arena.allocate(CobaltVpx.C_POINTER);
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "calls vpx codec opened: codec={0} width={1} height={2} bitrate={3}",
                        codec, params.width(), params.height(), params.targetBitrate());
            }
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx codec open failed", e);
            if (dec != null) {
                CobaltVpx.cobalt_vpx_decoder_destroy(dec);
            }
            if (enc != null) {
                CobaltVpx.cobalt_vpx_encoder_destroy(enc);
            }
            arena.close();
            throw e;
        }
    }

    /**
     * Resolves the {@code COBALT_VPX_CODEC_*} selector for the configured codec.
     *
     * @return {@link CobaltVpx#COBALT_VPX_CODEC_VP8} or {@link CobaltVpx#COBALT_VPX_CODEC_VP9}
     */
    private int codecSelector() {
        return switch (codec) {
            case VP8 -> CobaltVpx.COBALT_VPX_CODEC_VP8();
            case VP9 -> CobaltVpx.COBALT_VPX_CODEC_VP9();
            default -> throw new WhatsAppCallException.Vpx("unsupported libvpx codec " + codec);
        };
    }

    /**
     * Computes the {@code VP8E_SET_CPUUSED} speed for the current {@link #params}.
     *
     * @implNote This implementation negates and clamps {@link VideoCodecParams#complexity()} into the
     * realtime range {@code [-16, -4]}, using {@link #DEFAULT_CPU_USED} when complexity is the neutral
     * {@code 0}; libvpx treats a higher {@code cpu-used} value as faster and lower quality.
     *
     * @return the {@code cpu-used} speed value
     */
    private int cpuUsed() {
        return params.complexity() == 0 ? DEFAULT_CPU_USED : Math.clamp(-params.complexity(), -16, -4);
    }

    /**
     * Computes the libvpx frame drop threshold for the current {@link #params}.
     *
     * @return {@link #FRAME_SKIP_DROP_THRESH} when frame skipping is enabled, otherwise {@code 0}
     */
    private int dropFrameThresh() {
        return params.frameSkip() ? FRAME_SKIP_DROP_THRESH : 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return the codec format this instance implements
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
     * @return the encoded access unit, empty when the rate controller dropped the frame
     * @throws NullPointerException      if {@code frame} is {@code null}
     * @throws IllegalArgumentException  if the frame dimensions do not match the configured geometry
     * @throws IllegalStateException     if the codec is closed
     * @throws WhatsAppCallException.Vpx if the libvpx encode call fails
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
        var planar = frame.pixels();
        var pixels = encodeStagingFor(planar.length);
        MemorySegment.copy(planar, 0, pixels, ValueLayout.JAVA_BYTE, 0, planar.length);
        var keyFrameForced = forceKeyFrame || keyFrameRequested;
        var err = CobaltVpx.cobalt_vpx_encoder_encode(encoderCtx, pixels, planar.length,
                params.width(), params.height(), encodePts++, keyFrameForced ? 1 : 0);
        if (err != CobaltVpx.COBALT_VPX_OK()) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx encode rejected: codec={0} rc={1}", codec, err);
            throw WhatsAppCallException.Vpx.fromErr("cobalt_vpx_encoder_encode", err);
        }
        keyFrameRequested = false;
        var encoded = drainEncodedPackage(frame);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls vpx encode: codec={0} bytes={1} keyFrame={2} forced={3}",
                    codec, encoded.payload().length, encoded.keyFrame(), keyFrameForced);
        }
        return encoded;
    }

    /**
     * Drains the single compressed packet the realtime encoder produced into an
     * {@link EncodedVideoFrame}.
     *
     * @implNote This implementation calls {@link CobaltVpx#cobalt_vpx_encoder_get_packet}, which returns
     * the one frame packet the realtime encoder emits per input frame as a pointer into memory libvpx
     * owns plus its length and key frame flag. The bytes are copied onto the heap immediately, before
     * any further libvpx call can invalidate that pointer. A dropped frame yields a zero length,
     * surfaced as an empty access unit.
     *
     * @param source the source frame, for the carried timestamp and dimensions
     * @return the encoded access unit, empty when no packet was produced
     */
    private EncodedVideoFrame drainEncodedPackage(VideoFrame source) {
        var err = CobaltVpx.cobalt_vpx_encoder_get_packet(encoderCtx, packetBufCell, packetLenCell, packetKeyCell);
        if (err != CobaltVpx.COBALT_VPX_OK()) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx get packet rejected: codec={0} rc={1}", codec, err);
            throw WhatsAppCallException.Vpx.fromErr("cobalt_vpx_encoder_get_packet", err);
        }
        var len = packetLenCell.get(CobaltVpx.C_INT, 0);
        var payload = DataUtils.EMPTY_BYTE_ARRAY;
        var keyFrame = false;
        if (len > 0) {
            var buf = packetBufCell.get(CobaltVpx.C_POINTER, 0).reinterpret(len);
            payload = new byte[len];
            MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, 0, payload, 0, len);
            keyFrame = packetKeyCell.get(CobaltVpx.C_INT, 0) != 0;
            framesEncoded++;
            bytesEncoded += len;
            if (keyFrame) {
                keyFramesEncoded++;
            }
        }
        return new EncodedVideoFrame(payload, codec, keyFrame, source.width(), source.height(), source.ptsMicros());
    }

    /**
     * {@inheritDoc}
     *
     * @param payload   the compressed access unit bytes
     * @param ptsMicros the timestamp to stamp on the decoded frame
     * @return the decoded I420 frame, or {@code null} when no displayable frame was produced
     * @throws NullPointerException      if {@code payload} is {@code null}
     * @throws IllegalStateException     if the codec is closed
     * @throws WhatsAppCallException.Vpx if the libvpx decode call fails
     */
    @Override
    public VideoFrame decode(byte[] payload, long ptsMicros) {
        return decode(payload, ptsMicros, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Packs the decoded picture into {@code reuse} when it exactly matches the decoded I420 byte
     * count, so a steady resolution stream produces no pixel allocation per frame; a fresh buffer is
     * minted on the first frame and after each resolution change.
     *
     * @param payload   {@inheritDoc}
     * @param ptsMicros {@inheritDoc}
     * @param reuse     {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NullPointerException      if {@code payload} is {@code null}
     * @throws IllegalStateException     if the codec is closed
     * @throws WhatsAppCallException.Vpx if the libvpx decode call fails
     */
    @Override
    public VideoFrame decode(byte[] payload, long ptsMicros, byte[] reuse) {
        ensureOpen();
        Objects.requireNonNull(payload, "payload cannot be null");
        var data = decodeInputFor(Math.max(1, payload.length));
        MemorySegment.copy(payload, 0, data, ValueLayout.JAVA_BYTE, 0, payload.length);
        var err = CobaltVpx.cobalt_vpx_decoder_decode(decoderCtx, data, payload.length);
        if (err != CobaltVpx.COBALT_VPX_OK()) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx decode rejected: codec={0} rc={1}", codec, err);
            throw WhatsAppCallException.Vpx.fromErr("cobalt_vpx_decoder_decode", err);
        }
        var frameErr = CobaltVpx.cobalt_vpx_decoder_get_frame(decoderCtx, frameImgCell);
        if (frameErr != CobaltVpx.COBALT_VPX_OK()) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx get frame rejected: codec={0} rc={1}", codec, frameErr);
            throw WhatsAppCallException.Vpx.fromErr("cobalt_vpx_decoder_get_frame", frameErr);
        }
        var img = frameImgCell.get(CobaltVpx.C_POINTER, 0);
        if (img.equals(MemorySegment.NULL)) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls vpx decode: codec={0} no displayable frame", codec);
            return null;
        }
        var frame = copyDecodedImage(img, ptsMicros, reuse);
        framesDecoded++;
        bytesDecoded += payload.length;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls vpx decode: codec={0} bytes={1} width={2} height={3}",
                    codec, payload.length, frame.width(), frame.height());
        }
        return frame;
    }

    /**
     * Copies a decoded libvpx picture into a planar I420 {@link VideoFrame}, packing each plane row by
     * row to strip any libvpx stride padding.
     *
     * @param img       the decoded picture handle from {@link CobaltVpx#cobalt_vpx_decoder_get_frame}
     * @param ptsMicros the timestamp to stamp on the produced frame
     * @param reuse     a caller owned buffer to pack the planes into when it exactly matches the decoded
     *                  I420 byte count, or {@code null} to allocate a fresh buffer
     * @return the packed I420 frame, wrapping {@code reuse} when it was adopted
     */
    private VideoFrame copyDecodedImage(MemorySegment img, long ptsMicros, byte[] reuse) {
        var width = CobaltVpx.cobalt_vpx_img_width(img);
        var height = CobaltVpx.cobalt_vpx_img_height(img);
        var chromaWidth = width / 2;
        var chromaHeight = height / 2;
        var lumaSize = width * height;
        var chromaSize = chromaWidth * chromaHeight;
        var needed = lumaSize + 2 * chromaSize;
        var out = reuse != null && reuse.length == needed ? reuse : new byte[needed];
        copyPlane(CobaltVpx.cobalt_vpx_img_plane(img, CobaltVpx.COBALT_VPX_PLANE_Y()).reinterpret(Long.MAX_VALUE),
                CobaltVpx.cobalt_vpx_img_stride(img, CobaltVpx.COBALT_VPX_PLANE_Y()), out, 0, width, height);
        copyPlane(CobaltVpx.cobalt_vpx_img_plane(img, CobaltVpx.COBALT_VPX_PLANE_U()).reinterpret(Long.MAX_VALUE),
                CobaltVpx.cobalt_vpx_img_stride(img, CobaltVpx.COBALT_VPX_PLANE_U()), out, lumaSize, chromaWidth, chromaHeight);
        copyPlane(CobaltVpx.cobalt_vpx_img_plane(img, CobaltVpx.COBALT_VPX_PLANE_V()).reinterpret(Long.MAX_VALUE),
                CobaltVpx.cobalt_vpx_img_stride(img, CobaltVpx.COBALT_VPX_PLANE_V()), out, lumaSize + chromaSize, chromaWidth, chromaHeight);
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
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the codec is closed
     */
    @Override
    public void requestKeyFrame() {
        ensureOpen();
        keyFrameRequested = true;
        keyFrameRequests++;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls vpx key frame requested: codec={0}", codec);
    }

    /**
     * {@inheritDoc}
     *
     * @param params the parameter set whose mutable fields the encoder adopts
     * @throws NullPointerException      if {@code params} is {@code null}
     * @throws IllegalArgumentException  if {@code params} changes the codec or geometry
     * @throws IllegalStateException     if the codec is closed
     * @throws WhatsAppCallException.Vpx if reconfiguring the encoder fails
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
            throw new IllegalArgumentException("cannot change geometry on a live libvpx encoder");
        }
        this.params = params;
        var err = CobaltVpx.cobalt_vpx_encoder_reconfigure(encoderCtx, params.width(), params.height(),
                params.targetBitrate(), params.frameRate(), params.minQuantizer(), params.maxQuantizer(),
                dropFrameThresh(), params.keyFrameIntervalFrames(), cpuUsed());
        if (err != CobaltVpx.COBALT_VPX_OK()) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "calls vpx reconfigure rejected: codec={0} rc={1}", codec, err);
            throw WhatsAppCallException.Vpx.fromErr("cobalt_vpx_encoder_reconfigure", err);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "calls vpx codec reconfigured: codec={0} bitrate={1} frameRate={2}",
                    codec, params.targetBitrate(), params.frameRate());
        }
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
     * <p>Destroys both libvpx handles and closes the owning arena; idempotent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        CobaltVpx.cobalt_vpx_decoder_destroy(decoderCtx);
        CobaltVpx.cobalt_vpx_encoder_destroy(encoderCtx);
        arena.close();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "calls vpx codec closed: codec={0} framesEncoded={1} framesDecoded={2}",
                    codec, framesEncoded, framesDecoded);
        }
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
     * Throws if this codec has been closed.
     *
     * @throws IllegalStateException if {@link #close()} has been called
     */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("VpxVideoCodec is closed");
        }
    }
}
