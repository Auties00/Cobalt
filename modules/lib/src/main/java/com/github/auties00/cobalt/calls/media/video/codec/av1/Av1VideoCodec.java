package com.github.auties00.cobalt.calls.media.video.codec.av1;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;
import com.github.auties00.cobalt.calls.media.video.codec.av1.bindings.CobaltDav1d;
import com.github.auties00.cobalt.calls.media.video.codec.av1.bindings.CobaltRav1e;
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
 * Implements the AV1 {@link VideoCodec} using dav1d for decode and rav1e for encode.
 *
 * <p>An instance opens one dav1d decoder and one rav1e encoder for a single stream. The encode path
 * copies each {@link VideoFrame} into a native staging buffer, feeds it to
 * {@link CobaltRav1e#cobalt_rav1e_encoder_send}, and drains the compressed packet with
 * {@link CobaltRav1e#cobalt_rav1e_encoder_receive}; the decode path feeds each access unit to
 * {@link CobaltDav1d#cobalt_dav1d_send_data}, drains decoded pictures with
 * {@link CobaltDav1d#cobalt_dav1d_get_picture}, and copies out the reconstructed I420 image with its
 * stride padding stripped.
 *
 * <p>Only the 8 bit I420 decode path is built, so a picture that is not 8 bit I420 is rejected.
 *
 * <p>The codec has a single writer: the encode, decode, key frame request, and reconfiguration paths
 * must run on one thread, since the native contexts are reused across calls and this class reuses a
 * per codec {@link Arena} of scratch segments. All native segments live in that arena and are released
 * together on {@link #close()}; the encoder and decoder are freed by their shim destroy and close calls.
 *
 * <p>rav1e is a lookahead encoder, so a {@code send} need not yield a packet: the first frames buffer
 * and {@link #encode(VideoFrame, boolean)} returns an empty access unit until output catches up. The
 * codec opens rav1e in low latency mode to keep that delay small.
 *
 * @implNote WhatsApp's own AV1 encoder library is unidentified, so this implementation stands in with
 * rav1e, whose C API imposes two limitations a WhatsApp native encoder would not. It cannot force a key
 * frame on the next frame, so a {@link #requestKeyFrame()} or an {@link #encode(VideoFrame, boolean)} with
 * {@code forceKeyFrame} set falls on the configured key frame interval instead; and it exposes no live rate
 * control reconfiguration, so a {@link #modify(VideoCodecParams)} bitrate or quantizer change takes effect
 * only when the codec is recreated. Each limitation is logged where it takes effect.
 */
public final class Av1VideoCodec implements VideoCodec {
    /**
     * The logger for {@link Av1VideoCodec}.
     */
    private static final System.Logger LOGGER = Log.get(Av1VideoCodec.class);

    /**
     * The dav1d decode thread count.
     *
     * @implNote This implementation uses a small fixed thread count: a video call decodes at modest
     * resolutions where two threads keep up, while a larger count would widen the frame delay latency
     * that dav1d's frame threading introduces on the realtime receive path.
     */
    private static final int DECODE_THREADS = 2;

    /**
     * The rav1e encode speed preset.
     *
     * @implNote This implementation pins rav1e to its fastest preset ({@code 10}): AV1 encode is heavy
     * and the realtime call path cannot afford a slower, higher quality preset.
     */
    private static final int ENCODE_SPEED = 10;

    /**
     * The rav1e encode thread count.
     */
    private static final int ENCODE_THREADS = 4;

    /**
     * The rav1e low latency flag, enabled so the encoder's lookahead delay is kept small on the realtime
     * send path.
     */
    private static final int LOW_LATENCY = 1;

    /**
     * The parameters this codec was constructed with, updated on each {@link #modify(VideoCodecParams)}
     * round.
     */
    private VideoCodecParams params;

    /**
     * The arena owning every native segment this codec allocates, released as a unit on {@link #close()}.
     */
    private final Arena arena;

    /**
     * The opaque dav1d decoder handle returned by {@link CobaltDav1d#cobalt_dav1d_open}.
     */
    private final MemorySegment decoderCtx;

    /**
     * The opaque rav1e encoder handle returned by {@link CobaltRav1e#cobalt_rav1e_encoder_create}.
     */
    private final MemorySegment encoderCtx;

    /**
     * The reusable cell receiving the decoded picture handle from
     * {@link CobaltDav1d#cobalt_dav1d_get_picture}.
     */
    private final MemorySegment picCell;

    /**
     * The reusable cells receiving the compressed packet pointer, length, key frame flag, and packet
     * handle from {@link CobaltRav1e#cobalt_rav1e_encoder_receive}.
     */
    private final MemorySegment outBufCell;
    private final MemorySegment outLenCell;
    private final MemorySegment outIsKeyCell;
    private final MemorySegment outPacketCell;

    /**
     * The growable native staging buffer the source pixels are copied into before each encode, reused
     * across frames so the encode path makes no per frame allocation.
     */
    private MemorySegment encodeStaging;

    /**
     * The byte capacity of {@link #encodeStaging}.
     */
    private long encodeStagingSize;

    /**
     * The growable native input buffer each access unit is copied into before each decode, reused across
     * frames so the decode path makes no per frame allocation.
     */
    private MemorySegment decodeInput;

    /**
     * The byte capacity of {@link #decodeInput}.
     */
    private long decodeInputSize;

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
     * Opens an AV1 codec for the given parameters, creating both the dav1d decoder and the rav1e encoder.
     *
     * @param params the codec parameters; must select {@link VideoDecoderCapability#AV1 AV1}
     * @throws NullPointerException      if {@code params} is {@code null}
     * @throws IllegalArgumentException  if {@code params} does not select AV1
     * @throws WhatsAppCallException.Av1 if dav1d or rav1e fails to open
     */
    public Av1VideoCodec(VideoCodecParams params) {
        Objects.requireNonNull(params, "params cannot be null");
        if (params.codec() != VideoDecoderCapability.AV1) {
            throw new IllegalArgumentException("Av1VideoCodec requires AV1 params, got " + params.codec());
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "opening av1 codec, {0}x{1} bitrate={2} fps={3}",
                    params.width(), params.height(), params.targetBitrate(), params.frameRate());
        }
        this.params = params;
        this.arena = Arena.ofShared();
        MemorySegment dec = null;
        MemorySegment enc = null;
        try {
            var decCell = arena.allocate(CobaltDav1d.C_POINTER);
            int decRc;
            try {
                decRc = CobaltDav1d.cobalt_dav1d_open(DECODE_THREADS, decCell);
            } catch (Throwable t) {
                throw new WhatsAppCallException.Av1("cobalt_dav1d_open failed", t);
            }
            if (decRc != CobaltDav1d.COBALT_DAV1D_OK()) {
                throw WhatsAppCallException.Av1.fromErr("cobalt_dav1d_open", decRc);
            }
            dec = decCell.get(CobaltDav1d.C_POINTER, 0);
            if (dec.equals(MemorySegment.NULL)) {
                throw new WhatsAppCallException.Av1("cobalt_dav1d_open returned NULL");
            }
            var encCell = arena.allocate(CobaltRav1e.C_POINTER);
            int encRc;
            try {
                encRc = CobaltRav1e.cobalt_rav1e_encoder_create(
                        params.width(), params.height(), params.targetBitrate(),
                        params.frameRate(), 1, ENCODE_SPEED, params.keyFrameIntervalFrames(),
                        LOW_LATENCY, ENCODE_THREADS, encCell);
            } catch (Throwable t) {
                throw new WhatsAppCallException.Av1("cobalt_rav1e_encoder_create failed", t);
            }
            if (encRc != CobaltRav1e.COBALT_RAV1E_OK()) {
                throw WhatsAppCallException.Av1.fromErr("cobalt_rav1e_encoder_create", encRc);
            }
            enc = encCell.get(CobaltRav1e.C_POINTER, 0);
            if (enc.equals(MemorySegment.NULL)) {
                throw new WhatsAppCallException.Av1("cobalt_rav1e_encoder_create returned NULL");
            }
            this.decoderCtx = dec;
            this.encoderCtx = enc;
            this.picCell = arena.allocate(CobaltDav1d.C_POINTER);
            this.outBufCell = arena.allocate(CobaltRav1e.C_POINTER);
            this.outLenCell = arena.allocate(CobaltRav1e.C_INT);
            this.outIsKeyCell = arena.allocate(CobaltRav1e.C_INT);
            this.outPacketCell = arena.allocate(CobaltRav1e.C_POINTER);
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "av1 codec open failed", e);
            if (enc != null) {
                try {
                    CobaltRav1e.cobalt_rav1e_encoder_destroy(enc);
                } catch (Throwable ignored) {
                }
            }
            if (dec != null) {
                try {
                    CobaltDav1d.cobalt_dav1d_close(dec);
                } catch (Throwable ignored) {
                }
            }
            arena.close();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return always {@link VideoDecoderCapability#AV1 AV1}
     */
    @Override
    public VideoDecoderCapability codec() {
        return VideoDecoderCapability.AV1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Because rav1e is a lookahead encoder, an empty access unit (zero length payload) is returned
     * while the encoder buffers input and no packet is ready yet; output catches up to one packet per
     * input frame once the lookahead is primed.
     *
     * @param frame         the raw picture to encode
     * @param forceKeyFrame ignored; key frames fall on the configured interval
     * @return the encoded access unit, empty when the encoder produced no packet for this frame
     * @throws NullPointerException      if {@code frame} is {@code null}
     * @throws IllegalArgumentException  if the frame dimensions do not match the configured geometry
     * @throws IllegalStateException     if the codec is closed
     * @throws WhatsAppCallException.Av1 if the rav1e encode call fails
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
        if (forceKeyFrame && Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "av1 encoder cannot force a key frame on the next frame; it falls on the configured interval");
        }
        var pixels = encodeStagingFor(planar.length);
        MemorySegment.copy(planar, 0, pixels, ValueLayout.JAVA_BYTE, 0, planar.length);
        int sendRc;
        try {
            sendRc = CobaltRav1e.cobalt_rav1e_encoder_send(encoderCtx, pixels, planar.length,
                    params.width(), params.height());
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "av1 native encoder send call failed", t);
            throw new WhatsAppCallException.Av1("cobalt_rav1e_encoder_send failed", t);
        }
        if (sendRc != CobaltRav1e.COBALT_RAV1E_OK()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "av1 encoder send failed, rc={0}", sendRc);
            throw WhatsAppCallException.Av1.fromErr("cobalt_rav1e_encoder_send", sendRc);
        }
        return drainPacket(frame);
    }

    /**
     * Drains one compressed packet from the rav1e encoder into an {@link EncodedVideoFrame}.
     *
     * @implNote This implementation calls {@link CobaltRav1e#cobalt_rav1e_encoder_receive}; a NULL packet
     * handle means the lookahead encoder buffered the frame and has no output yet, surfaced as an empty
     * access unit. A returned packet's bytes are copied onto the heap before
     * {@link CobaltRav1e#cobalt_rav1e_packet_unref} releases the rav1e owned memory.
     *
     * @param source the source frame, for the carried timestamp and dimensions
     * @return the encoded access unit, empty when no packet was produced
     * @throws WhatsAppCallException.Av1 if the receive call fails
     */
    private EncodedVideoFrame drainPacket(VideoFrame source) {
        int rc;
        try {
            rc = CobaltRav1e.cobalt_rav1e_encoder_receive(encoderCtx, outBufCell, outLenCell, outIsKeyCell, outPacketCell);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "av1 native encoder receive call failed", t);
            throw new WhatsAppCallException.Av1("cobalt_rav1e_encoder_receive failed", t);
        }
        if (rc != CobaltRav1e.COBALT_RAV1E_OK()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "av1 encoder receive failed, rc={0}", rc);
            throw WhatsAppCallException.Av1.fromErr("cobalt_rav1e_encoder_receive", rc);
        }
        var packet = outPacketCell.get(CobaltRav1e.C_POINTER, 0);
        if (packet.equals(MemorySegment.NULL)) {
            return new EncodedVideoFrame(DataUtils.EMPTY_BYTE_ARRAY, VideoDecoderCapability.AV1, false,
                    source.width(), source.height(), source.ptsMicros());
        }
        var len = outLenCell.get(CobaltRav1e.C_INT, 0);
        byte[] bytes;
        boolean keyFrame;
        try {
            bytes = outBufCell.get(CobaltRav1e.C_POINTER, 0).reinterpret(len).toArray(ValueLayout.JAVA_BYTE);
            keyFrame = outIsKeyCell.get(CobaltRav1e.C_INT, 0) != 0;
        } finally {
            try {
                CobaltRav1e.cobalt_rav1e_packet_unref(packet);
            } catch (Throwable ignored) {
            }
        }
        framesEncoded++;
        bytesEncoded += bytes.length;
        if (keyFrame) {
            keyFramesEncoded++;
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "av1 packet encoded, bytes={0} keyFrame={1}", bytes.length, keyFrame);
        return new EncodedVideoFrame(bytes, VideoDecoderCapability.AV1, keyFrame,
                source.width(), source.height(), source.ptsMicros());
    }

    /**
     * {@inheritDoc}
     *
     * @param payload   the compressed access unit bytes
     * @param ptsMicros the timestamp to stamp on the decoded frame
     * @return the decoded I420 frame, or {@code null} when no displayable frame was produced
     * @throws NullPointerException      if {@code payload} is {@code null}
     * @throws IllegalStateException     if the codec is closed
     * @throws WhatsAppCallException.Av1 if the dav1d decode call fails, or the picture is not 8 bit I420
     */
    @Override
    public VideoFrame decode(byte[] payload, long ptsMicros) {
        ensureOpen();
        Objects.requireNonNull(payload, "payload cannot be null");
        if (payload.length == 0) {
            return null;
        }
        var data = decodeInputFor(payload.length);
        MemorySegment.copy(payload, 0, data, ValueLayout.JAVA_BYTE, 0, payload.length);
        bytesDecoded += payload.length;
        VideoFrame result = null;
        // Feed the access unit, draining buffered pictures when dav1d signals EAGAIN so the unit is
        // accepted without loss (dav1d consumes the whole unit, returning OK, or asks to drain first,
        // returning EAGAIN). The realtime drain every frame pattern keeps the decoder empty, so the unit
        // is normally accepted on the first send and this loop does not run.
        var rc = sendData(data, payload.length);
        while (rc == CobaltDav1d.COBALT_DAV1D_EAGAIN()) {
            var drained = drainPicture(ptsMicros);
            if (drained == null) {
                break;
            }
            if (result == null) {
                result = drained;
            }
            rc = sendData(data, payload.length);
        }
        if (rc != CobaltDav1d.COBALT_DAV1D_OK()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "av1 decoder send data failed, rc={0}", rc);
            throw WhatsAppCallException.Av1.fromErr("cobalt_dav1d_send_data", rc);
        }
        if (result == null) {
            result = drainPicture(ptsMicros);
        }
        if (result != null) {
            framesDecoded++;
            if (Log.TRACE) LOGGER.log(Level.TRACE, "av1 picture decoded, payload bytes={0}", payload.length);
        }
        return result;
    }

    /**
     * Feeds one access unit to the dav1d decoder.
     *
     * @param data the native segment holding the access unit bytes
     * @param len  the access unit length in bytes
     * @return the dav1d shim status code
     * @throws WhatsAppCallException.Av1 if the native call itself fails
     */
    private int sendData(MemorySegment data, int len) {
        try {
            return CobaltDav1d.cobalt_dav1d_send_data(decoderCtx, data, len);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "av1 native send data call failed", t);
            throw new WhatsAppCallException.Av1("cobalt_dav1d_send_data failed", t);
        }
    }

    /**
     * Retrieves and copies out the next decoded picture, releasing the dav1d reference.
     *
     * @param ptsMicros the timestamp to stamp on the produced frame
     * @return the decoded I420 frame, or {@code null} when no picture is ready
     * @throws WhatsAppCallException.Av1 if the native call fails or the picture is not 8 bit I420
     */
    private VideoFrame drainPicture(long ptsMicros) {
        int rc;
        try {
            rc = CobaltDav1d.cobalt_dav1d_get_picture(decoderCtx, picCell);
        } catch (Throwable t) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "av1 native get picture call failed", t);
            throw new WhatsAppCallException.Av1("cobalt_dav1d_get_picture failed", t);
        }
        if (rc == CobaltDav1d.COBALT_DAV1D_EAGAIN()) {
            return null;
        }
        if (rc != CobaltDav1d.COBALT_DAV1D_OK()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "av1 decoder get picture failed, rc={0}", rc);
            throw WhatsAppCallException.Av1.fromErr("cobalt_dav1d_get_picture", rc);
        }
        var pic = picCell.get(CobaltDav1d.C_POINTER, 0);
        if (pic.equals(MemorySegment.NULL)) {
            return null;
        }
        try {
            return copyPicture(pic, ptsMicros);
        } finally {
            try {
                CobaltDav1d.cobalt_dav1d_picture_unref(pic);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Copies a decoded dav1d picture into a planar I420 {@link VideoFrame}, packing each plane row by row
     * to strip dav1d's stride padding.
     *
     * @param pic       the decoded picture handle from {@link CobaltDav1d#cobalt_dav1d_get_picture}
     * @param ptsMicros the timestamp to stamp on the produced frame
     * @return the packed I420 frame
     * @throws WhatsAppCallException.Av1 if the picture is not 8 bit I420
     */
    private VideoFrame copyPicture(MemorySegment pic, long ptsMicros) {
        var layout = CobaltDav1d.cobalt_dav1d_pic_layout(pic);
        if (layout != CobaltDav1d.COBALT_DAV1D_LAYOUT_I420()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "av1 decode rejected, unsupported layout {0}", layout);
            throw new WhatsAppCallException.Av1("unsupported AV1 layout " + layout + "; only I420 is supported");
        }
        var bitDepth = CobaltDav1d.cobalt_dav1d_pic_bitdepth(pic);
        if (bitDepth != 8) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "av1 decode rejected, unsupported bit depth {0}", bitDepth);
            throw new WhatsAppCallException.Av1("unsupported AV1 bit depth " + bitDepth + "; only 8-bit is built");
        }
        var width = CobaltDav1d.cobalt_dav1d_pic_width(pic);
        var height = CobaltDav1d.cobalt_dav1d_pic_height(pic);
        var chromaWidth = width / 2;
        var chromaHeight = height / 2;
        var lumaSize = width * height;
        var chromaSize = chromaWidth * chromaHeight;
        var out = new byte[lumaSize + 2 * chromaSize];
        copyPlane(pic, 0, out, 0, width, height);
        copyPlane(pic, 1, out, lumaSize, chromaWidth, chromaHeight);
        copyPlane(pic, 2, out, lumaSize + chromaSize, chromaWidth, chromaHeight);
        return new VideoFrame(out, VideoPixelFormat.I420, width, height, ptsMicros);
    }

    /**
     * Copies one picture plane out of native memory, row by row, discarding stride padding.
     *
     * @param pic     the decoded picture handle
     * @param plane   the plane index, 0 for luma or 1/2 for chroma
     * @param dst     the destination heap array
     * @param dstBase the offset into {@code dst} where this plane begins
     * @param rowSize the packed bytes per row (the plane width)
     * @param rows    the number of rows in the plane
     */
    private void copyPlane(MemorySegment pic, int plane, byte[] dst, int dstBase, int rowSize, int rows) {
        var src = CobaltDav1d.cobalt_dav1d_pic_plane(pic, plane).reinterpret(Long.MAX_VALUE);
        var stride = CobaltDav1d.cobalt_dav1d_pic_stride(pic, plane);
        for (var row = 0; row < rows; row++) {
            MemorySegment.copy(src, ValueLayout.JAVA_BYTE, (long) row * stride, dst, dstBase + row * rowSize, rowSize);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation records the request for {@link #stats()}.
     *
     * @throws IllegalStateException if the codec is closed
     */
    @Override
    public void requestKeyFrame() {
        ensureOpen();
        keyFrameRequests++;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "av1 key frame requested (total {0}); rav1e cannot force it on the next frame, it falls on the configured interval",
                    keyFrameRequests);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation guards the codec and geometry, which rav1e cannot change on a live
     * encoder, and adopts the new parameter set for geometry and stats.
     *
     * @param params the parameter set whose mutable fields are adopted
     * @throws NullPointerException     if {@code params} is {@code null}
     * @throws IllegalArgumentException if {@code params} changes the codec or geometry
     * @throws IllegalStateException    if the codec is closed
     */
    @Override
    public void modify(VideoCodecParams params) {
        ensureOpen();
        Objects.requireNonNull(params, "params cannot be null");
        if (params.codec() != VideoDecoderCapability.AV1) {
            throw new IllegalArgumentException("cannot change codec from AV1 to " + params.codec());
        }
        if (params.width() != this.params.width() || params.height() != this.params.height()) {
            throw new IllegalArgumentException("cannot change geometry on a live AV1 encoder");
        }
        if (Log.DEBUG) {
            if (params.targetBitrate() != this.params.targetBitrate()) {
                LOGGER.log(Level.DEBUG,
                        "av1 rate control change (bitrate {0}->{1}) does not take effect until the codec is recreated; rav1e exposes no live reconfiguration",
                        this.params.targetBitrate(), params.targetBitrate());
            } else {
                LOGGER.log(Level.DEBUG, "av1 codec params updated, bitrate={0} fps={1}",
                        params.targetBitrate(), params.frameRate());
            }
        }
        this.params = params;
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
     * <p>Destroys the rav1e encoder and the dav1d decoder, then closes the owning arena; idempotent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            CobaltRav1e.cobalt_rav1e_encoder_destroy(encoderCtx);
        } catch (Throwable ignored) {
        }
        try {
            CobaltDav1d.cobalt_dav1d_close(decoderCtx);
        } catch (Throwable ignored) {
        }
        arena.close();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "av1 codec closed");
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
            throw new IllegalStateException("Av1VideoCodec is closed");
        }
    }
}
