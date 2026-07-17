package com.github.auties00.cobalt.media.transcode.sticker;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecContext;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecParameters;
import com.github.auties00.cobalt.util.ffmpeg.AVFilterContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFilterInOut;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFrame;
import com.github.auties00.cobalt.util.ffmpeg.AVPacket;
import com.github.auties00.cobalt.util.ffmpeg.AVStream;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegError;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegLoader;
import com.github.auties00.cobalt.util.ffmpeg.Ffmpeg;
import com.github.auties00.cobalt.media.transcode.MediaTranscoderService;
import com.github.auties00.cobalt.media.transcode.avio.AvioReadBuffer;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.media.StickerMessage;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;

/**
 * Decodes a source image, normalises it to the WhatsApp sticker shape, and encodes it as an extended WebP.
 *
 * <p>This type drives the sticker branch of the upload transcoder. It decodes the first frame of the source image,
 * scales it to fit inside a {@value #STICKER_DIMENSION}-pixel square while preserving aspect ratio, pads the remaining
 * area with transparent black, and encodes the result as a single-frame WebP. When the caller supplies sticker
 * metadata (publisher, pack id, emojis, accessibility text and the like) the pipeline appends a WhatsApp-format EXIF
 * chunk via {@link WebpMetadataWriter#write(byte[], Map)}. The transparent padding keeps the rendered sticker the same
 * size on every receiver regardless of the source aspect ratio. The single instance is owned by the parent
 * {@link MediaTranscoderService}.
 *
 * @implNote This implementation drives libavfilter with the sticker filter chain
 * {@snippet :
 *  scale=512:512:force_original_aspect_ratio=decrease,
 *  pad=512:512:(ow-iw)/2:(oh-ih)/2:0x00000000,
 *  format=rgba
 * }
 * then hands the RGBA frame to FFmpeg's {@code libwebp} encoder at quality {@value #WEBP_QUALITY} with
 * {@code lossless=0}. Encoding stops at a single packet because stickers are static one-frame WebPs. The source is
 * read through {@link AvioReadBuffer} backed by the supplied {@link SeekableByteChannel}; the encoded output is a
 * single libwebp packet copied straight into a {@link MediaPayload.OfBytes} so no temporary file is created.
 */
public final class StickerPipeline {
    /** The logger for {@link StickerPipeline}. */
    private static final System.Logger LOGGER = Log.get(StickerPipeline.class);

    /**
     * Side length, in pixels, of the square output sticker.
     *
     * @implNote This implementation uses 512, matching the fixed sticker canvas WhatsApp renders.
     */
    private static final int STICKER_DIMENSION = 512;

    /**
     * libwebp encoder quality on the {@code 0..100} scale.
     *
     * @implNote This implementation uses 90, matching WhatsApp's sticker encoder configuration.
     */
    private static final int WEBP_QUALITY = 90;

    /**
     * Constructs an empty pipeline.
     *
     * <p>The instance is stateless; the parent {@link MediaTranscoderService} owns the single instance.
     */
    public StickerPipeline() {
    }

    /**
     * Transcodes the source image into the sticker wire format, applies codec-derived metadata to {@code provider},
     * and returns the encoded payload.
     *
     * <p>This overload behaves exactly like {@link #run(MediaProvider, SeekableByteChannel, Map)} invoked with an
     * empty metadata map, so no EXIF metadata chunk is embedded in the output.
     *
     * @param provider the upload target; codec-derived fields are applied to this instance
     * @param source   the raw image channel; not closed by this method
     * @return the encoded WebP payload
     * @throws WhatsAppMediaException.Processing if decoding or encoding fails
     */
    public MediaPayload run(MediaProvider provider, SeekableByteChannel source)
            throws WhatsAppMediaException.Processing {
        return run(provider, source, Map.of());
    }

    /**
     * Transcodes the source image into the sticker wire format with an optional WhatsApp metadata chunk, applies
     * codec-derived metadata to {@code provider}, and returns the encoded payload.
     *
     * <p>The keys of {@code metadata} are the canonical WhatsApp descriptor names (for example
     * {@code "sticker-pack-id"}, {@code "emojis"}, {@code "accessibility-text"}). The map is serialised to JSON and
     * embedded as an EXIF chunk via {@link WebpMetadataWriter#write(byte[], Map)}; passing an empty map skips the embed
     * step entirely. When {@code provider} is a {@link StickerMessage} the {@code mimetype}, {@code mediaSize},
     * {@code width}, and {@code height} fields are populated; every other {@link MediaProvider} variant receives only
     * the common {@code mediaSize} update.
     *
     * @param provider the upload target; codec-derived fields are applied to this instance
     * @param source   the raw image channel; not closed by this method
     * @param metadata sticker descriptor map; pass an empty map to skip the metadata embed
     * @return the encoded WebP payload
     * @throws WhatsAppMediaException.Processing if decoding, encoding, or the metadata embed fails
     */
    public MediaPayload run(MediaProvider provider, SeekableByteChannel source,
                            Map<String, Object> metadata)
            throws WhatsAppMediaException.Processing {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sticker transcode started, metadataEntries={0}", metadata.size());
        FFmpegLoader.ensureLoaded();
        try (var arena = Arena.ofShared();
             var decoded = decodeFirstFrame(arena, source)) {
            var scaled = scaleAndPad(decoded);
            byte[] webp;
            try {
                webp = encodeWebp(scaled);
            } finally {
                freeFrame(scaled);
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sticker encoded, bytes={0}", webp.length);
            if (!metadata.isEmpty()) {
                webp = WebpMetadataWriter.write(webp, metadata);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sticker metadata embedded, bytes={0}", webp.length);
            }
            provider.setMediaSize(webp.length);
            if (provider instanceof StickerMessage sticker) {
                sticker.setMimetype("image/webp");
                sticker.setWidth(STICKER_DIMENSION);
                sticker.setHeight(STICKER_DIMENSION);
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sticker transcode complete, bytes={0}", webp.length);
            return new MediaPayload.OfBytes(webp);
        } catch (WhatsAppMediaException.Processing failure) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sticker transcode failed", failure);
            throw failure;
        }
    }

    /**
     * Drives the decoded source frame through the sticker filter chain and returns the scaled, padded RGBA frame.
     *
     * <p>The chain scales the source with aspect-fit into the {@value #STICKER_DIMENSION}-square box, pads the
     * remaining area with transparent black so the sticker overlay is always the same size on every receiver, and
     * coerces the result to RGBA so the libwebp encoder picks up the alpha channel. The returned frame is freshly
     * allocated and must be released by the caller via {@link #freeFrame(MemorySegment)}.
     *
     * @param decoded the demuxed source state holding the first input frame
     * @return a freshly-allocated {@link AVFrame} the caller must free
     * @throws WhatsAppMediaException.Processing if the filter graph fails to produce an output frame
     */
    private static MemorySegment scaleAndPad(DecodedSource decoded) {
        var arena = decoded.arena;
        var graph = FFmpegError.requireNonNull("avfilter_graph_alloc",
                Ffmpeg.avfilter_graph_alloc());
        try {
            var srcArgs = String.format(
                    "video_size=%dx%d:pix_fmt=%d:time_base=1/1:pixel_aspect=1/1",
                    AVFrame.width(decoded.frame),
                    AVFrame.height(decoded.frame),
                    AVFrame.format(decoded.frame));
            var bufferFilter = FFmpegError.requireNonNull("avfilter_get_by_name(buffer)",
                    Ffmpeg.avfilter_get_by_name(arena.allocateFrom("buffer")));
            var sinkFilter = FFmpegError.requireNonNull("avfilter_get_by_name(buffersink)",
                    Ffmpeg.avfilter_get_by_name(arena.allocateFrom("buffersink")));
            var srcCtxPtr = arena.allocate(ValueLayout.ADDRESS);
            var sinkCtxPtr = arena.allocate(ValueLayout.ADDRESS);
            FFmpegError.check("avfilter_graph_create_filter(in)",
                    Ffmpeg.avfilter_graph_create_filter(srcCtxPtr, bufferFilter,
                            arena.allocateFrom("in"), arena.allocateFrom(srcArgs),
                            MemorySegment.NULL, graph));
            FFmpegError.check("avfilter_graph_create_filter(out)",
                    Ffmpeg.avfilter_graph_create_filter(sinkCtxPtr, sinkFilter,
                            arena.allocateFrom("out"), MemorySegment.NULL,
                            MemorySegment.NULL, graph));
            var srcCtx = srcCtxPtr.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFilterContext.layout().byteSize());
            var sinkCtx = sinkCtxPtr.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFilterContext.layout().byteSize());
            var chain = String.format(
                    "scale=%d:%d:force_original_aspect_ratio=decrease,"
                            + "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:0x00000000,"
                            + "format=rgba",
                    STICKER_DIMENSION, STICKER_DIMENSION,
                    STICKER_DIMENSION, STICKER_DIMENSION);
            var outputs = FFmpegError.requireNonNull("avfilter_inout_alloc(outputs)",
                    Ffmpeg.avfilter_inout_alloc());
            var inputs = FFmpegError.requireNonNull("avfilter_inout_alloc(inputs)",
                    Ffmpeg.avfilter_inout_alloc());
            AVFilterInOut.name(outputs, Ffmpeg.av_strdup(arena.allocateFrom("in")));
            AVFilterInOut.filter_ctx(outputs, srcCtx);
            AVFilterInOut.pad_idx(outputs, 0);
            AVFilterInOut.next(outputs, MemorySegment.NULL);
            AVFilterInOut.name(inputs, Ffmpeg.av_strdup(arena.allocateFrom("out")));
            AVFilterInOut.filter_ctx(inputs, sinkCtx);
            AVFilterInOut.pad_idx(inputs, 0);
            AVFilterInOut.next(inputs, MemorySegment.NULL);
            var outputsPp = arena.allocate(ValueLayout.ADDRESS);
            var inputsPp = arena.allocate(ValueLayout.ADDRESS);
            outputsPp.set(ValueLayout.ADDRESS, 0L, outputs);
            inputsPp.set(ValueLayout.ADDRESS, 0L, inputs);
            try {
                FFmpegError.check("avfilter_graph_parse_ptr",
                        Ffmpeg.avfilter_graph_parse_ptr(graph, arena.allocateFrom(chain),
                                inputsPp, outputsPp, MemorySegment.NULL));
                FFmpegError.check("avfilter_graph_config",
                        Ffmpeg.avfilter_graph_config(graph, MemorySegment.NULL));
            } finally {
                Ffmpeg.avfilter_inout_free(inputsPp);
                Ffmpeg.avfilter_inout_free(outputsPp);
            }
            FFmpegError.check("av_buffersrc_add_frame_flags",
                    Ffmpeg.av_buffersrc_add_frame_flags(srcCtx, decoded.frame, 0));
            var outFrame = allocFrame();
            var got = Ffmpeg.av_buffersink_get_frame(sinkCtx, outFrame);
            if (got < 0) {
                freeFrame(outFrame);
                throw new WhatsAppMediaException.Processing(
                        "av_buffersink_get_frame failed: " + FFmpegError.describe(got));
            }
            return outFrame;
        } finally {
            try (var local = Arena.ofConfined()) {
                var pp = local.allocate(ValueLayout.ADDRESS);
                pp.set(ValueLayout.ADDRESS, 0L, graph);
                Ffmpeg.avfilter_graph_free(pp);
            }
        }
    }

    /**
     * Encodes the given RGBA frame as a WebP image via FFmpeg's {@code libwebp} encoder.
     *
     * <p>libwebp emits a single packet for a static image. The returned bytes are a complete WebP file (RIFF outer
     * header, {@code WEBP} form type, and the encoded image chunk), ready for {@link WebpMetadataWriter#write(byte[],
     * Map)} when sticker metadata needs to be embedded.
     *
     * @param frame the RGBA input frame at sticker dimensions
     * @return the encoded WebP bytes
     * @throws WhatsAppMediaException.Processing if any encoder call fails
     */
    private static byte[] encodeWebp(MemorySegment frame)
            throws WhatsAppMediaException.Processing {
        var codec = FFmpegError.requireNonNull("avcodec_find_encoder(WEBP)",
                Ffmpeg.avcodec_find_encoder(Ffmpeg.AV_CODEC_ID_WEBP()));
        var ctx = FFmpegError.requireNonNull("avcodec_alloc_context3(webp)",
                Ffmpeg.avcodec_alloc_context3(codec));
        try (var local = Arena.ofConfined()) {
            AVCodecContext.width(ctx, STICKER_DIMENSION);
            AVCodecContext.height(ctx, STICKER_DIMENSION);
            AVCodecContext.pix_fmt(ctx, Ffmpeg.AV_PIX_FMT_RGBA());
            var tb = local.allocate(8);
            tb.set(ValueLayout.JAVA_INT, 0L, 1);
            tb.set(ValueLayout.JAVA_INT, 4L, 1);
            AVCodecContext.time_base(ctx, tb);
            var searchChildren = Ffmpeg.AV_OPT_SEARCH_CHILDREN();
            FFmpegError.check("av_opt_set_int(quality)",
                    Ffmpeg.av_opt_set_int(ctx,
                            local.allocateFrom("quality"),
                            WEBP_QUALITY, searchChildren));
            FFmpegError.check("av_opt_set_int(lossless)",
                    Ffmpeg.av_opt_set_int(ctx,
                            local.allocateFrom("lossless"),
                            0, searchChildren));
            FFmpegError.check("avcodec_open2(webp)",
                    Ffmpeg.avcodec_open2(ctx, codec, MemorySegment.NULL));
            AVFrame.pts(frame, 0);
            FFmpegError.check("avcodec_send_frame",
                    Ffmpeg.avcodec_send_frame(ctx, frame));
            FFmpegError.check("avcodec_send_frame(EOF)",
                    Ffmpeg.avcodec_send_frame(ctx, MemorySegment.NULL));
            var packet = FFmpegError.requireNonNull("av_packet_alloc",
                    Ffmpeg.av_packet_alloc());
            try {
                var got = Ffmpeg.avcodec_receive_packet(ctx, packet);
                if (got < 0) {
                    throw new WhatsAppMediaException.Processing(
                            "avcodec_receive_packet failed: " + FFmpegError.describe(got));
                }
                var size = AVPacket.size(packet);
                var data = AVPacket.data(packet).reinterpret(size);
                var out = new byte[size];
                MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0, out, 0, size);
                return out;
            } finally {
                Ffmpeg.av_packet_unref(packet);
                var pp = local.allocate(ValueLayout.ADDRESS);
                pp.set(ValueLayout.ADDRESS, 0L, packet);
                Ffmpeg.av_packet_free(pp);
            }
        } finally {
            try (var local = Arena.ofConfined()) {
                var pp = local.allocate(ValueLayout.ADDRESS);
                pp.set(ValueLayout.ADDRESS, 0L, ctx);
                Ffmpeg.avcodec_free_context(pp);
            }
        }
    }

    /**
     * Opens the source channel through FFmpeg and decodes its first frame.
     *
     * <p>The channel is wrapped in an {@link AvioReadBuffer} so libavformat reads through the Java channel rather than
     * a file path. The method probes the container, selects the first video stream, opens its decoder, and runs the
     * demux-decode loop until a single frame is produced. On any failure the half-initialised native resources are
     * released before the exception propagates.
     *
     * @param arena   the shared arena that owns the AVIO bridge
     * @param channel the source channel
     * @return the decoded source bundle holding the first frame and the open native contexts
     * @throws WhatsAppMediaException.Processing if probing, opening, or decoding fails
     */
    private static DecodedSource decodeFirstFrame(Arena arena, SeekableByteChannel channel)
            throws WhatsAppMediaException.Processing {
        var bridge = new AvioReadBuffer(arena, channel);
        var formatCtx = MemorySegment.NULL;
        var codecCtx = MemorySegment.NULL;
        var frame = MemorySegment.NULL;
        var packet = MemorySegment.NULL;
        try {
            formatCtx = FFmpegError.requireNonNull("avformat_alloc_context",
                    Ffmpeg.avformat_alloc_context());
            AVFormatContext.pb(formatCtx, bridge.ioContext());
            var formatPp = arena.allocate(ValueLayout.ADDRESS);
            formatPp.set(ValueLayout.ADDRESS, 0L, formatCtx);
            FFmpegError.check("avformat_open_input",
                    Ffmpeg.avformat_open_input(formatPp, MemorySegment.NULL,
                            MemorySegment.NULL, MemorySegment.NULL));
            formatCtx = formatPp.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFormatContext.layout().byteSize());
            FFmpegError.check("avformat_find_stream_info",
                    Ffmpeg.avformat_find_stream_info(formatCtx, MemorySegment.NULL));
            var streamIndex = pickVideoStream(formatCtx);
            if (streamIndex < 0) {
                throw new WhatsAppMediaException.Processing("no video stream in sticker source");
            }
            var stream = streamPointer(formatCtx, streamIndex);
            var params = AVStream.codecpar(stream);
            var codecId = AVCodecParameters.codec_id(params);
            var codec = FFmpegError.requireNonNull(
                    "avcodec_find_decoder(" + codecId + ")",
                    Ffmpeg.avcodec_find_decoder(codecId));
            codecCtx = FFmpegError.requireNonNull("avcodec_alloc_context3",
                    Ffmpeg.avcodec_alloc_context3(codec));
            FFmpegError.check("avcodec_parameters_to_context",
                    Ffmpeg.avcodec_parameters_to_context(codecCtx, params));
            FFmpegError.check("avcodec_open2",
                    Ffmpeg.avcodec_open2(codecCtx, codec, MemorySegment.NULL));
            packet = FFmpegError.requireNonNull("av_packet_alloc", Ffmpeg.av_packet_alloc());
            frame = allocFrame();
            while (true) {
                var read = Ffmpeg.av_read_frame(formatCtx, packet);
                if (read < 0) {
                    Ffmpeg.avcodec_send_packet(codecCtx, MemorySegment.NULL);
                } else if (AVPacket.stream_index(packet) != streamIndex) {
                    Ffmpeg.av_packet_unref(packet);
                    continue;
                } else {
                    var sent = Ffmpeg.avcodec_send_packet(codecCtx, packet);
                    Ffmpeg.av_packet_unref(packet);
                    if (sent < 0 && !FFmpegError.isAgain(sent)) {
                        throw new WhatsAppMediaException.Processing(
                                "avcodec_send_packet failed: " + FFmpegError.describe(sent));
                    }
                }
                var got = Ffmpeg.avcodec_receive_frame(codecCtx, frame);
                if (got == 0) {
                    break;
                }
                if (FFmpegError.isAgain(got)) {
                    if (read < 0) {
                        throw new WhatsAppMediaException.Processing(
                                "decoder produced no frame before EOF");
                    }
                    continue;
                }
                if (FFmpegError.isEof(got)) {
                    throw new WhatsAppMediaException.Processing(
                            "decoder reported EOF before producing a frame");
                }
                throw new WhatsAppMediaException.Processing(
                        "avcodec_receive_frame failed: " + FFmpegError.describe(got));
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "sticker source decoded, stream={0}, width={1}, height={2}",
                        streamIndex, AVFrame.width(frame), AVFrame.height(frame));
            }
            return new DecodedSource(arena, bridge, formatCtx, codecCtx, packet, frame);
        } catch (RuntimeException e) {
            freeOnFailure(formatCtx, codecCtx, frame, packet, bridge);
            throw e;
        }
    }

    /**
     * Returns the index of the first video stream in the demuxer, or {@code -1} if none exists.
     *
     * @param formatCtx the open demuxer
     * @return the zero-based index of the first video stream, or {@code -1} when the source has no video stream
     */
    private static int pickVideoStream(MemorySegment formatCtx) {
        var n = AVFormatContext.nb_streams(formatCtx);
        var streams = AVFormatContext.streams(formatCtx);
        for (var i = 0; i < n; i++) {
            var stream = streams.getAtIndex(ValueLayout.ADDRESS, i)
                    .reinterpret(AVStream.layout().byteSize());
            var params = AVStream.codecpar(stream);
            if (AVCodecParameters.codec_type(params) == Ffmpeg.AVMEDIA_TYPE_VIDEO()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the stream pointer at the given index from the demuxer's stream array.
     *
     * @param formatCtx the open demuxer
     * @param index     the zero-based stream index
     * @return the stream pointer reinterpreted to the {@link AVStream} layout
     */
    private static MemorySegment streamPointer(MemorySegment formatCtx, int index) {
        return AVFormatContext.streams(formatCtx)
                .getAtIndex(ValueLayout.ADDRESS, index)
                .reinterpret(AVStream.layout().byteSize());
    }

    /**
     * Allocates a fresh frame via {@code av_frame_alloc}.
     *
     * @return the allocated {@link AVFrame} pointer
     */
    private static MemorySegment allocFrame() {
        return FFmpegError.requireNonNull("av_frame_alloc", Ffmpeg.av_frame_alloc());
    }

    /**
     * Frees the given frame via {@code av_frame_free}, tolerating a {@code null} or {@code NULL} argument.
     *
     * @param frame the frame to free; ignored when {@code null} or {@link MemorySegment#NULL}
     */
    private static void freeFrame(MemorySegment frame) {
        if (frame == null || frame == MemorySegment.NULL) {
            return;
        }
        try (var local = Arena.ofConfined()) {
            var pp = local.allocate(ValueLayout.ADDRESS);
            pp.set(ValueLayout.ADDRESS, 0L, frame);
            Ffmpeg.av_frame_free(pp);
        }
    }

    /**
     * Releases half-initialised native resources on the decode failure path.
     *
     * <p>Each argument may be {@link MemorySegment#NULL} (or {@code null}) when its allocation step had not yet run;
     * the method skips any such resource and tolerates being called at any point during partial initialisation.
     *
     * @param formatCtx the demuxer context, or {@link MemorySegment#NULL}
     * @param codecCtx  the decoder context, or {@link MemorySegment#NULL}
     * @param frame     the decoder output frame, or {@link MemorySegment#NULL}
     * @param packet    the demuxer packet, or {@link MemorySegment#NULL}
     * @param bridge    the AVIO bridge to close
     */
    private static void freeOnFailure(MemorySegment formatCtx, MemorySegment codecCtx,
                                       MemorySegment frame, MemorySegment packet,
                                       AvioReadBuffer bridge) {
        if (packet != null && packet != MemorySegment.NULL) {
            try (var local = Arena.ofConfined()) {
                var pp = local.allocate(ValueLayout.ADDRESS);
                pp.set(ValueLayout.ADDRESS, 0L, packet);
                Ffmpeg.av_packet_free(pp);
            }
        }
        freeFrame(frame);
        if (codecCtx != null && codecCtx != MemorySegment.NULL) {
            try (var local = Arena.ofConfined()) {
                var pp = local.allocate(ValueLayout.ADDRESS);
                pp.set(ValueLayout.ADDRESS, 0L, codecCtx);
                Ffmpeg.avcodec_free_context(pp);
            }
        }
        if (formatCtx != null && formatCtx != MemorySegment.NULL) {
            try (var local = Arena.ofConfined()) {
                var pp = local.allocate(ValueLayout.ADDRESS);
                pp.set(ValueLayout.ADDRESS, 0L, formatCtx);
                Ffmpeg.avformat_close_input(pp);
            }
        }
        bridge.close();
    }

    /**
     * Holds the per-run FFmpeg state across the decode and filter passes.
     *
     * <p>The bundle owns the open demuxer, decoder, demuxer packet, and the first decoded frame, together with the
     * shared arena and the AVIO bridge that back them. It implements {@link AutoCloseable} so the pipeline can wrap it
     * in a try-with-resources block and guarantee the native cleanup runs even when the filter or encode passes throw.
     */
    private static final class DecodedSource implements AutoCloseable {
        /**
         * Shared arena holding the AVIO bridge and helper allocations.
         */
        final Arena arena;

        /**
         * AVIO bridge backing the demuxer.
         */
        final AvioReadBuffer bridge;

        /**
         * Open libavformat demuxer.
         */
        final MemorySegment formatCtx;

        /**
         * Open libavcodec decoder.
         */
        final MemorySegment codecCtx;

        /**
         * Reusable demuxer packet.
         */
        final MemorySegment packet;

        /**
         * The first decoded frame, reused as the input to the filter pass.
         */
        final MemorySegment frame;

        /**
         * Constructs the decoded source bundle from the supplied native state.
         *
         * @param arena     the shared arena
         * @param bridge    the AVIO bridge
         * @param formatCtx the open demuxer
         * @param codecCtx  the open decoder
         * @param packet    the demuxer packet
         * @param frame     the first decoded frame
         */
        DecodedSource(Arena arena, AvioReadBuffer bridge, MemorySegment formatCtx,
                      MemorySegment codecCtx, MemorySegment packet, MemorySegment frame) {
            this.arena = arena;
            this.bridge = bridge;
            this.formatCtx = formatCtx;
            this.codecCtx = codecCtx;
            this.packet = packet;
            this.frame = frame;
        }

        /**
         * Releases the demuxer packet, decoded frame, decoder, and demuxer, then closes the AVIO bridge.
         *
         * <p>Each native resource is skipped when it is {@link MemorySegment#NULL}, so the method is safe regardless of
         * how far initialisation progressed.
         */
        @Override
        public void close() {
            try (var local = Arena.ofConfined()) {
                if (packet != null && packet != MemorySegment.NULL) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, packet);
                    Ffmpeg.av_packet_free(pp);
                }
            }
            freeFrame(frame);
            if (codecCtx != null && codecCtx != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, codecCtx);
                    Ffmpeg.avcodec_free_context(pp);
                }
            }
            if (formatCtx != null && formatCtx != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, formatCtx);
                    Ffmpeg.avformat_close_input(pp);
                }
            }
            bridge.close();
        }
    }
}
