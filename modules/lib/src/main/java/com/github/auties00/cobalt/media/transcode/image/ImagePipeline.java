package com.github.auties00.cobalt.media.transcode.image;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecContext;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecParameters;
import com.github.auties00.cobalt.util.ffmpeg.AVDictionaryEntry;
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
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.SeekableByteChannel;

/**
 * Decodes a source image, rescales it to the WhatsApp upload preset, and re-encodes it as a
 * sanitised JFIF JPEG.
 *
 * <p>Drives the image branch of the upload transcoder through FFmpeg. The source is demuxed and
 * decoded through {@code libavformat} and {@code libavcodec}, the decoded frame is pushed through a
 * {@code libavfilter} graph that scales it to the per-quality maximum edge and coerces the pixel
 * format to {@code yuvj420p}, the result is encoded with the native {@code mjpeg} encoder, and the
 * encoded bytes are then run through {@link JpegCleaner}. A second pass with a much smaller maximum
 * edge produces the micro-thumbnail that ships in the message protobuf. The single instance is owned
 * by {@link MediaTranscoderService}.
 *
 * @implNote
 * This implementation mirrors the FFmpeg-driven encode WhatsApp Web runs in its worker pool: scale
 * via the filter graph, coerce to JPEG-range YUV, encode with {@code mjpeg}, strip non-essential
 * APP/COM markers. The quality presets follow {@link SettingsSyncAction.MediaQualitySetting}:
 * {@code STANDARD} caps at {@value #MAX_EDGE_STANDARD} pixels and uses qscale
 * {@value #QSCALE_STANDARD}; {@code HD} caps at {@value #MAX_EDGE_HD} pixels and uses qscale
 * {@value #QSCALE_HD}. EXIF orientation is read from the stream metadata dictionary; values of 90
 * and 270 swap the bound dimensions before scaling and prepend a {@code transpose} filter so the
 * encoded output is upright. The thumbnail pass tunes the encoded size through a qscale ramp
 * ({@value #QSCALE_THUMB_START} to {@value #QSCALE_THUMB_CAP}) until the encoded JPEG fits within
 * the {@value #MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES}-byte WhatsApp wire budget.
 */
public final class ImagePipeline {
    /** The logger for {@link ImagePipeline}. */
    private static final System.Logger LOGGER = Log.get(ImagePipeline.class);

    /**
     * Maximum edge in pixels for the {@code STANDARD} quality preset.
     *
     * @implNote
     * This implementation mirrors {@code WAWebMediaConstants.IMG_MAX_EDGE_STANDARD}.
     */
    private static final int MAX_EDGE_STANDARD = 1600;

    /**
     * Maximum edge in pixels for the {@code HD} quality preset.
     *
     * @implNote
     * This implementation mirrors {@code WAWebMediaConstants.IMG_MAX_EDGE_HD}.
     */
    private static final int MAX_EDGE_HD = 4096;

    /**
     * MJPEG qscale used for the {@code STANDARD} quality preset.
     */
    private static final int QSCALE_STANDARD = 4;

    /**
     * MJPEG qscale used for the {@code HD} quality preset.
     */
    private static final int QSCALE_HD = 2;

    /**
     * Maximum edge in pixels for the micro-thumbnail.
     *
     * @implNote
     * This implementation mirrors {@code WAWebMediaConstants.IMG_THUMB_MAX_EDGE}.
     */
    private static final int THUMB_MAX_EDGE = 100;

    /**
     * Initial MJPEG qscale used for the thumbnail pass.
     *
     * @implNote
     * This implementation mirrors the start value WhatsApp Web ships in its worker pool.
     */
    private static final int QSCALE_THUMB_START = 6;

    /**
     * Upper bound on MJPEG qscale for the thumbnail pass.
     *
     * <p>The size-tune loop in {@link #encodeThumbnail(DecodedSource, int, int, int)} gives up once
     * the output reaches this qscale without fitting the wire budget.
     */
    private static final int QSCALE_THUMB_CAP = 12;

    /**
     * Maximum encoded size for the micro-thumbnail in bytes.
     *
     * @implNote
     * This implementation mirrors {@code WAWebMediaConstants.MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES}.
     */
    private static final int MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES = 1300;

    /**
     * Value of {@code AV_CODEC_FLAG_QSCALE} from {@code <libavcodec/avcodec.h>}.
     *
     * <p>Set on the encoder context so {@code global_quality} is honoured as a fixed quantizer scale
     * rather than ignored in favour of rate control.
     *
     * @implNote
     * This implementation resolves the value through {@code Ffmpeg#AV_CODEC_FLAG_QSCALE()} once and
     * caches it as a Java constant to avoid the native lookup on every encode.
     */
    private static final int CODEC_FLAG_QSCALE = Ffmpeg.AV_CODEC_FLAG_QSCALE();

    /**
     * Value of {@code FF_QP2LAMBDA} from {@code <libavutil/avutil.h>}.
     *
     * <p>The scalar that converts a quantizer scale into the encoder's {@code global_quality} field.
     *
     * @implNote
     * This implementation resolves the value through {@code Ffmpeg#FF_QP2LAMBDA()} once and caches
     * it as a Java constant.
     */
    private static final int FF_QP2LAMBDA = Ffmpeg.FF_QP2LAMBDA();

    /**
     * Final pixel format produced by the filter graph.
     *
     * <p>JPEG-range YUV 4:2:0, the format the {@code mjpeg} encoder accepts directly.
     *
     * @implNote
     * This implementation resolves the value through {@code Ffmpeg#AV_PIX_FMT_YUVJ420P()} once and
     * caches it as a Java constant.
     */
    private static final int OUTPUT_PIX_FMT = Ffmpeg.AV_PIX_FMT_YUVJ420P();

    /**
     * Constructs the pipeline.
     *
     * <p>The parent {@link MediaTranscoderService} owns the single instance.
     */
    public ImagePipeline() {
    }

    /**
     * Transcodes the source image, applies codec-derived metadata to {@code provider}, and returns
     * the encoded payload stream.
     *
     * <p>Mutates the provider in place. When {@code provider} is an {@link ImageMessage} the
     * {@code mimetype}, {@code mediaSize}, {@code width}, {@code height}, and {@code jpegThumbnail}
     * fields are populated; every other {@link MediaProvider} variant receives only the common
     * {@code mediaSize} update. The decoded source is held open for the duration of the call and
     * released through {@link DecodedSource#close()} before this method returns; the {@code source}
     * channel itself is not closed by this method.
     *
     * @param provider the upload target; codec-derived fields are applied to this instance
     * @param source   the raw image channel; not closed by this method
     * @param quality  the quality preset; selects edge cap and qscale
     * @return the encoded JPEG payload
     * @throws WhatsAppMediaException.Processing if decoding, filtering, or encoding fails
     */
    public MediaPayload run(MediaProvider provider, SeekableByteChannel source,
                            SettingsSyncAction.MediaQualitySetting quality)
            throws WhatsAppMediaException.Processing {
        FFmpegLoader.ensureLoaded();
        var maxEdge = quality == SettingsSyncAction.MediaQualitySetting.HD
                ? MAX_EDGE_HD
                : MAX_EDGE_STANDARD;
        var qscale = quality == SettingsSyncAction.MediaQualitySetting.HD
                ? QSCALE_HD
                : QSCALE_STANDARD;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "image transcode start: quality={0}", quality);
        try (var arena = Arena.ofShared();
             var decoded = decodeFirstFrame(arena, source)) {
            var orientation = readOrientation(decoded.stream);
            var srcW = AVFrame.width(decoded.frame);
            var srcH = AVFrame.height(decoded.frame);
            int targetW;
            int targetH;
            if (orientation == 90 || orientation == 270) {
                targetH = srcW;
                targetW = srcH;
            } else {
                targetW = srcW;
                targetH = srcH;
            }
            var mainDims = boundEdge(targetW, targetH, maxEdge);
            var mainJpeg = encodeScaledJpeg(decoded, mainDims.width, mainDims.height,
                    orientation, qscale);
            mainJpeg = JpegCleaner.clean(mainJpeg);
            var thumbDims = boundEdge(targetW, targetH, THUMB_MAX_EDGE);
            var thumbnail = encodeThumbnail(decoded, thumbDims.width, thumbDims.height,
                    orientation);
            provider.setMediaSize(mainJpeg.length);
            if (provider instanceof ImageMessage image) {
                image.setMimetype("image/jpeg");
                image.setWidth(mainDims.width);
                image.setHeight(mainDims.height);
                image.setJpegThumbnail(thumbnail);
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "image transcode done: width={0} height={1} encodedBytes={2} thumbnailBytes={3} orientation={4}",
                        mainDims.width, mainDims.height, mainJpeg.length, thumbnail.length, orientation);
            }
            return new MediaPayload.OfBytes(mainJpeg);
        } catch (WhatsAppMediaException.Processing e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "image transcode failed", e);
            throw e;
        }
    }

    /**
     * Builds a {@code libavfilter} graph that scales the source frame to the given dimensions,
     * applies the orientation transpose, and coerces the output pixel format, then drives a single
     * frame through it and returns the resized output.
     *
     * <p>The returned frame is owned by the caller and must be released with
     * {@link #freeFrame(MemorySegment)}; it is allocated through {@link #allocFrame()}, so callers
     * wrap the result in a try/finally that frees on every path. An orientation of 90 or 270 prepends
     * a {@code transpose} filter so the encoded output is upright.
     *
     * @param decoded     the demuxed source state holding the first input frame and the source
     *                    stream parameters
     * @param dstW        target width in pixels
     * @param dstH        target height in pixels
     * @param orientation EXIF rotation in degrees clockwise (0, 90, 180, or 270)
     * @return a freshly-allocated {@code AVFrame} pointer holding the scaled output
     */
    private static MemorySegment scaleAndFormat(DecodedSource decoded,
                                                 int dstW, int dstH, int orientation) {
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
            var chain = String.format("%sscale=%d:%d:flags=bicubic,format=yuvj420p",
                    transposeFilterPrefix(orientation), dstW, dstH);
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
     * Returns the leading transpose-filter directive for the given EXIF orientation, including the
     * trailing comma when non-empty.
     *
     * <p>EXIF orientation values map to FFmpeg's {@code transpose} parameter as follows: 90 deg
     * clockwise (EXIF orientation 6) becomes {@code transpose=1}, 180 deg (EXIF orientation 3)
     * becomes {@code transpose=1,transpose=1}, and 270 deg clockwise (EXIF orientation 8, equivalent
     * to 90 deg counter-clockwise) becomes {@code transpose=2}.
     *
     * @param orientation EXIF rotation in degrees clockwise
     * @return the filter prefix to inject into the chain, or an empty string for orientation
     *         {@code 0}
     */
    private static String transposeFilterPrefix(int orientation) {
        return switch (orientation) {
            case 90 -> "transpose=1,";
            case 180 -> "transpose=1,transpose=1,";
            case 270 -> "transpose=2,";
            default -> "";
        };
    }

    /**
     * Encodes the given decoded source as a scaled MJPEG and returns the raw bytes before
     * {@link JpegCleaner} sanitisation.
     *
     * <p>Scales and reformats the frame through {@link #scaleAndFormat(DecodedSource, int, int, int)}
     * and encodes it through {@link #encodeMjpeg(MemorySegment, int, int, int)}, freeing the scaled
     * frame on every path.
     *
     * @param decoded     the demuxed source state
     * @param dstW        target width in pixels
     * @param dstH        target height in pixels
     * @param orientation EXIF rotation in degrees clockwise
     * @param qscale      MJPEG qscale to apply via {@code global_quality = qscale * FF_QP2LAMBDA}
     * @return the encoded JPEG bytes
     * @throws WhatsAppMediaException.Processing if the encode pipeline fails
     */
    private static byte[] encodeScaledJpeg(DecodedSource decoded, int dstW, int dstH,
                                            int orientation, int qscale)
            throws WhatsAppMediaException.Processing {
        var scaled = scaleAndFormat(decoded, dstW, dstH, orientation);
        try {
            return encodeMjpeg(scaled, dstW, dstH, qscale);
        } finally {
            freeFrame(scaled);
        }
    }

    /**
     * Encodes a single MJPEG-ready frame at the requested qscale and returns the encoded packet
     * bytes.
     *
     * <p>The input frame must already be at the target size and in {@code yuvj420p}. The encoder is
     * driven in single-frame, single-packet mode: the frame is sent, a flush ({@code NULL} frame)
     * follows, and the one resulting packet is copied out.
     *
     * @implNote
     * This implementation allocates and tears down an MJPEG encoder context per call. MJPEG holds no
     * inter-frame state, so the per-call allocation cost is negligible compared to the encode itself.
     *
     * @param frame  the input frame pointer
     * @param w      frame width in pixels
     * @param h      frame height in pixels
     * @param qscale MJPEG quantizer scale; lower is higher quality
     * @return the encoded JPEG bytes
     * @throws WhatsAppMediaException.Processing if any encoder call fails
     */
    private static byte[] encodeMjpeg(MemorySegment frame, int w, int h, int qscale)
            throws WhatsAppMediaException.Processing {
        var codec = FFmpegError.requireNonNull("avcodec_find_encoder(MJPEG)",
                Ffmpeg.avcodec_find_encoder(Ffmpeg.AV_CODEC_ID_MJPEG()));
        var ctx = FFmpegError.requireNonNull("avcodec_alloc_context3(mjpeg)",
                Ffmpeg.avcodec_alloc_context3(codec));
        try {
            AVCodecContext.width(ctx, w);
            AVCodecContext.height(ctx, h);
            AVCodecContext.pix_fmt(ctx, OUTPUT_PIX_FMT);
            try (var local = Arena.ofConfined()) {
                var tb = local.allocate(8);
                tb.set(ValueLayout.JAVA_INT, 0L, 1);
                tb.set(ValueLayout.JAVA_INT, 4L, 1);
                AVCodecContext.time_base(ctx, tb);
            }
            AVCodecContext.flags(ctx, AVCodecContext.flags(ctx) | CODEC_FLAG_QSCALE);
            AVCodecContext.global_quality(ctx, qscale * FF_QP2LAMBDA);
            FFmpegError.check("avcodec_open2(mjpeg)",
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
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, packet);
                    Ffmpeg.av_packet_free(pp);
                }
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
     * Produces the in-band micro-thumbnail JPEG, retrying with progressively coarser qscale until the
     * encoded output fits within {@value #MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES} bytes or
     * {@value #QSCALE_THUMB_CAP} is reached.
     *
     * <p>The frame is scaled once; each iteration re-encodes that scaled frame at the next qscale and
     * runs the bytes through {@link JpegCleaner}. The first encode that fits the budget is returned;
     * if none fit, the last (coarsest) attempt is returned so a thumbnail is always produced.
     *
     * @param decoded     the demuxed source state
     * @param dstW        thumbnail width in pixels
     * @param dstH        thumbnail height in pixels
     * @param orientation EXIF rotation in degrees clockwise
     * @return the encoded thumbnail bytes, already passed through {@link JpegCleaner}
     * @throws WhatsAppMediaException.Processing if every encode attempt fails
     */
    private static byte[] encodeThumbnail(DecodedSource decoded, int dstW, int dstH,
                                           int orientation)
            throws WhatsAppMediaException.Processing {
        byte[] last = null;
        var scaled = scaleAndFormat(decoded, dstW, dstH, orientation);
        try {
            for (var qscale = QSCALE_THUMB_START; qscale <= QSCALE_THUMB_CAP; qscale++) {
                var encoded = encodeMjpeg(scaled, dstW, dstH, qscale);
                encoded = JpegCleaner.clean(encoded);
                last = encoded;
                if (encoded.length <= MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES) {
                    return encoded;
                }
            }
        } finally {
            freeFrame(scaled);
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "image thumbnail exceeds size budget: bytes={0} budgetBytes={1} qscaleCap={2}",
                    last.length, MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES, QSCALE_THUMB_CAP);
        }
        return last;
    }

    /**
     * Reads the {@code "rotate"} key from the given stream's metadata dictionary, if present, and
     * clamps the value to one of {@code 0}, {@code 90}, {@code 180}, or {@code 270}.
     *
     * <p>{@code libavformat} normalises EXIF orientation tags into the {@code "rotate"} metadata key
     * on the demuxed stream when reading JPEG, HEIC, and TIFF input. The value is normalised into the
     * {@code 0..359} range and then clamped: any value that is not an exact multiple of 90 falls back
     * to {@code 0}, as does a missing dictionary, a missing key, or an unparseable value.
     *
     * @param stream the demuxed source stream pointer
     * @return the rotation in degrees clockwise; one of {@code 0}, {@code 90}, {@code 180},
     *         {@code 270}
     */
    private static int readOrientation(MemorySegment stream) {
        var metadata = AVStream.metadata(stream);
        if (metadata == null || metadata == MemorySegment.NULL) {
            return 0;
        }
        try (var arena = Arena.ofConfined()) {
            var entry = Ffmpeg.av_dict_get(metadata, arena.allocateFrom("rotate"),
                    MemorySegment.NULL, 0);
            if (entry == null || entry == MemorySegment.NULL) {
                return 0;
            }
            var entryTyped = entry.reinterpret(AVDictionaryEntry.layout().byteSize());
            var valuePtr = AVDictionaryEntry.value(entryTyped);
            if (valuePtr == null || valuePtr == MemorySegment.NULL) {
                return 0;
            }
            var value = valuePtr.reinterpret(Long.MAX_VALUE).getString(0L);
            int rotation;
            try {
                rotation = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
            rotation = ((rotation % 360) + 360) % 360;
            return switch (rotation) {
                case 90, 180, 270 -> rotation;
                default -> 0;
            };
        }
    }

    /**
     * Opens the buffered source through FFmpeg and returns the first decoded frame plus the demuxer
     * state needed for subsequent encode passes.
     *
     * <p>Drains the demuxer until the decoder emits one frame, then halts. Images are single-frame
     * inputs by definition; for multi-frame inputs that reach the image path (an animated GIF, for
     * instance) the first frame is taken. On any failure during open or decode the partially
     * initialised native resources are released through
     * {@link #freeOnFailure(MemorySegment, MemorySegment, MemorySegment, MemorySegment, AvioReadBuffer)}
     * before the exception propagates.
     *
     * @param arena   the shared arena that owns the AVIO bridge and any transient allocations
     * @param channel the source channel
     * @return the decoded source bundle holding the open contexts and the first decoded frame
     * @throws WhatsAppMediaException.Processing if the source cannot be probed, opened, or decoded
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
                if (Log.WARNING) LOGGER.log(Level.WARNING, "no video stream in image source");
                throw new WhatsAppMediaException.Processing("no video stream in source");
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
            return new DecodedSource(arena, bridge, formatCtx, codecCtx, packet, frame,
                    stream, streamIndex);
        } catch (RuntimeException e) {
            freeOnFailure(formatCtx, codecCtx, frame, packet, bridge);
            throw e;
        }
    }

    /**
     * Picks the first video stream out of the demuxed format context.
     *
     * @param formatCtx the open demuxer pointer
     * @return the stream index, or {@code -1} if no video stream is present
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
     * Returns the i-th stream pointer from the demuxer's stream array.
     *
     * @param formatCtx the open demuxer pointer
     * @param index     the zero-based stream index
     * @return the stream pointer reinterpreted with the {@code AVStream} layout
     */
    private static MemorySegment streamPointer(MemorySegment formatCtx, int index) {
        return AVFormatContext.streams(formatCtx)
                .getAtIndex(ValueLayout.ADDRESS, index)
                .reinterpret(AVStream.layout().byteSize());
    }

    /**
     * Allocates a fresh {@code AVFrame}.
     *
     * @return the allocated frame pointer
     */
    private static MemorySegment allocFrame() {
        return FFmpegError.requireNonNull("av_frame_alloc", Ffmpeg.av_frame_alloc());
    }

    /**
     * Frees the given frame via {@code av_frame_free}, tolerating {@code NULL} inputs.
     *
     * @param frame the frame pointer to free; {@code NULL} is allowed
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
     * Releases any half-initialised resources held during a failed open or decode attempt.
     *
     * <p>Each native pointer is freed only when non-{@code NULL}, in reverse allocation order, and
     * the AVIO bridge is closed last.
     *
     * @param formatCtx the demuxer context pointer, or {@code NULL}
     * @param codecCtx  the decoder context pointer, or {@code NULL}
     * @param frame     the decoder output frame pointer, or {@code NULL}
     * @param packet    the demuxer packet pointer, or {@code NULL}
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
     * Computes the largest pair of dimensions that fit inside a square of side {@code maxEdge} while
     * preserving the source aspect ratio.
     *
     * <p>Both output dimensions are at least 2 and rounded down to even values through
     * {@link #roundEven(int)}.
     *
     * @implNote
     * This implementation rounds to even dimensions because the {@code mjpeg} encoder uses chroma
     * subsampling that must align on a 2-pixel grid.
     *
     * @param srcW    source width in pixels
     * @param srcH    source height in pixels
     * @param maxEdge upper bound on either output dimension
     * @return the bound dimensions
     */
    private static Dimensions boundEdge(int srcW, int srcH, int maxEdge) {
        var longestEdge = Math.max(srcW, srcH);
        if (longestEdge <= maxEdge) {
            return new Dimensions(roundEven(srcW), roundEven(srcH));
        }
        var scale = (double) maxEdge / longestEdge;
        var dstW = Math.max(2, (int) Math.round(srcW * scale));
        var dstH = Math.max(2, (int) Math.round(srcH * scale));
        return new Dimensions(roundEven(dstW), roundEven(dstH));
    }

    /**
     * Rounds the given value down to the nearest even integer, with a floor of {@code 2}.
     *
     * @param value the value to round
     * @return the rounded value
     */
    private static int roundEven(int value) {
        var rounded = value - (value & 1);
        return rounded < 2 ? 2 : rounded;
    }

    /**
     * Holds a pair of pixel dimensions.
     *
     * @param width  width in pixels
     * @param height height in pixels
     */
    private record Dimensions(int width, int height) {
    }

    /**
     * Bundles the FFmpeg state shared between the demux/decode step and the subsequent encode passes.
     *
     * <p>Held by the pipeline only for the duration of a single {@link ImagePipeline#run} invocation.
     * Implements {@link AutoCloseable} so the orchestrator can wrap it in a try-with-resources block
     * and guarantee FFmpeg cleanup runs even when the encode passes throw.
     *
     * @implNote
     * This implementation owns the AVIO bridge backing the format context's read path. The shared
     * {@link Arena} is provided by the caller (the pipeline orchestrator) and confines every upcall
     * stub the bridge installs.
     */
    private static final class DecodedSource implements AutoCloseable {
        /**
         * Shared arena that owns the AVIO bridge and any transient allocations made by helper
         * methods.
         */
        final Arena arena;

        /**
         * AVIO bridge that backs the format context's read callback.
         */
        final AvioReadBuffer bridge;

        /**
         * Open {@code libavformat} demuxer pointer.
         */
        final MemorySegment formatCtx;

        /**
         * Open {@code libavcodec} decoder pointer for the picked video stream.
         */
        final MemorySegment codecCtx;

        /**
         * Reusable demuxer packet pointer.
         */
        final MemorySegment packet;

        /**
         * Pointer to the first decoded frame produced by the source.
         *
         * <p>Reused by every encode pass during a single {@link ImagePipeline#run}.
         */
        final MemorySegment frame;

        /**
         * Pointer to the picked video stream within the demuxer.
         */
        final MemorySegment stream;

        /**
         * Zero-based index of the picked video stream.
         */
        final int streamIndex;

        /**
         * Constructs a decoded source bundle.
         *
         * @param arena       the shared arena
         * @param bridge      the AVIO bridge
         * @param formatCtx   the open demuxer pointer
         * @param codecCtx    the open decoder pointer
         * @param packet      the demuxer packet pointer
         * @param frame       the first decoded frame pointer
         * @param stream      the picked stream pointer
         * @param streamIndex the picked stream index
         */
        DecodedSource(Arena arena, AvioReadBuffer bridge, MemorySegment formatCtx,
                      MemorySegment codecCtx, MemorySegment packet, MemorySegment frame,
                      MemorySegment stream, int streamIndex) {
            this.arena = arena;
            this.bridge = bridge;
            this.formatCtx = formatCtx;
            this.codecCtx = codecCtx;
            this.packet = packet;
            this.frame = frame;
            this.stream = stream;
            this.streamIndex = streamIndex;
        }

        /**
         * Releases the packet, frame, decoder context, and demuxer context held by this bundle, then
         * closes the AVIO bridge.
         *
         * <p>Each native pointer is freed only when non-{@code NULL}. Safe to call exactly once at the
         * end of a {@link ImagePipeline#run} invocation.
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
