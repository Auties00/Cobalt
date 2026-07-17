package com.github.auties00.cobalt.media.transcode.video;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.util.ffmpeg.AVChannelLayout;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecContext;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecParameters;
import com.github.auties00.cobalt.util.ffmpeg.AVFilterContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFilterInOut;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFrame;
import com.github.auties00.cobalt.util.ffmpeg.AVOutputFormat;
import com.github.auties00.cobalt.util.ffmpeg.AVPacket;
import com.github.auties00.cobalt.util.ffmpeg.AVRational;
import com.github.auties00.cobalt.util.ffmpeg.AVStream;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegError;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegLoader;
import com.github.auties00.cobalt.util.ffmpeg.Ffmpeg;
import com.github.auties00.cobalt.media.transcode.MediaTranscoderService;
import com.github.auties00.cobalt.media.transcode.avio.AvioReadBuffer;
import com.github.auties00.cobalt.media.transcode.avio.AvioWriteBuffer;
import com.github.auties00.cobalt.media.transcode.image.ImagePipeline;
import com.github.auties00.cobalt.media.transcode.image.JpegCleaner;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decodes a source video and re-encodes it as a WhatsApp-compatible MP4 with H.264 video and AAC
 * audio, plus an in-band JPEG thumbnail.
 *
 * <p>The video branch of the upload transcoder targets H.264 baseline level 3.1 so the result
 * plays on every WhatsApp client without falling back to software decode, and AAC stereo audio at
 * {@value #AUDIO_SAMPLE_RATE} Hz. The output MP4 is muxed with {@code +faststart} so the moov atom
 * lands at the head of the file and playback can begin before the full download completes.
 *
 * @implNote This implementation drives FFmpeg end-to-end: libavformat demux of both the video and
 * audio streams, libavfilter for the {@code scale} chain on the video path, libswresample for the
 * audio resample, libopenh264 for H.264 encoding, the native FFmpeg AAC encoder, and the
 * libavformat MP4 muxer with {@code +faststart}. The thumbnail is the first decoded video frame run
 * through the same {@code mjpeg} encoder {@link ImagePipeline} uses, with baseline level 3.1 chosen
 * because higher H.264 levels are not universally hardware-decodable across WhatsApp's client
 * matrix.
 */
public final class VideoPipeline {
    /** The logger for {@link VideoPipeline}. */
    private static final System.Logger LOGGER = Log.get(VideoPipeline.class);

    /**
     * Holds the maximum vertical resolution in pixels for the
     * {@link SettingsSyncAction.MediaQualitySetting#STANDARD} quality preset.
     *
     * <p>A source taller than this value is downscaled to this height with the width adjusted to
     * preserve the aspect ratio; the {@link SettingsSyncAction.MediaQualitySetting#HD} preset
     * applies no height cap.
     */
    private static final int MAX_HEIGHT_STANDARD = 720;

    /**
     * Holds the target H.264 bitrate in bits per second for the
     * {@link SettingsSyncAction.MediaQualitySetting#STANDARD} quality preset.
     */
    private static final long VIDEO_BITRATE_STANDARD = 1_000_000;

    /**
     * Holds the target H.264 bitrate in bits per second for the
     * {@link SettingsSyncAction.MediaQualitySetting#HD} quality preset.
     */
    private static final long VIDEO_BITRATE_HD = 3_000_000;

    /**
     * Holds the target AAC bitrate in bits per second for the
     * {@link SettingsSyncAction.MediaQualitySetting#STANDARD} quality preset.
     */
    private static final long AUDIO_BITRATE_STANDARD = 128_000;

    /**
     * Holds the target AAC bitrate in bits per second for the
     * {@link SettingsSyncAction.MediaQualitySetting#HD} quality preset.
     */
    private static final long AUDIO_BITRATE_HD = 192_000;

    /**
     * Holds the audio sample rate in Hz of the encoded output.
     *
     * <p>The audio resampler converts every source stream to this rate regardless of its native
     * sample rate.
     */
    private static final int AUDIO_SAMPLE_RATE = 48_000;

    /**
     * Holds the audio channel count of the encoded output, fixed to stereo.
     *
     * <p>The audio resampler downmixes or upmixes the source layout to this channel count.
     */
    private static final int AUDIO_CHANNELS = 2;

    /**
     * Holds the maximum edge length in pixels for the video thumbnail.
     *
     * <p>The thumbnail's longest dimension is scaled down to this value while preserving the aspect
     * ratio; a thumbnail already within this bound is left at its source dimensions.
     */
    private static final int VIDEO_THUMB_MAX_EDGE = 480;

    /**
     * Holds the MJPEG quantization scale applied when encoding the thumbnail.
     *
     * @implNote This implementation multiplies the qscale by {@code FF_QP2LAMBDA} before setting it
     * as the encoder's global quality, matching the lambda-domain quality contract of the FFmpeg
     * MJPEG encoder.
     */
    private static final int THUMB_QSCALE = 4;

    /**
     * Holds the {@code AVFMT_GLOBALHEADER} output-format flag.
     *
     * <p>When the muxer reports this flag the encoders must emit their extradata in the file header
     * rather than inline, so the global-header codec flag is set on each encoder context.
     *
     * @implNote This implementation hardcodes {@code 0x40} from {@code libavformat/avformat.h}; the
     * jextract bindings do not expose this constant as a generated accessor.
     */
    private static final int AVFMT_GLOBALHEADER = 0x40;

    /**
     * Holds the fallback AAC frame size in samples used before the encoder reports its own.
     *
     * <p>The native FFmpeg AAC encoder produces frames of this size, so this value seeds the
     * encoder input frame until {@code openAudioEncoder} replaces it with the encoder-reported size
     * when that is positive.
     *
     * @implNote This implementation uses {@code 1024}, the fixed frame size of the native FFmpeg AAC
     * encoder.
     */
    private static final int DEFAULT_AAC_FRAME_SAMPLES = 1024;

    /**
     * Constructs the pipeline.
     *
     * <p>The parent {@link MediaTranscoderService} owns the single instance and dispatches every
     * video upload through it.
     */
    public VideoPipeline() {
    }

    /**
     * Transcodes the source video, applies codec-derived metadata to {@code provider}, and returns
     * the encoded payload stream.
     *
     * <p>The {@code provider} is mutated in place: when it is a {@link VideoMessage} its mimetype,
     * media size, width, height, duration in seconds, and JPEG thumbnail are populated from the
     * encoded output; every other {@link MediaProvider} variant receives only the common media-size
     * update via {@link MediaProvider#setMediaSize(long)}. The {@code source} channel is read but
     * not closed by this method. The {@code quality} preset selects the video and audio bitrates and
     * whether the height cap applies.
     *
     * @param provider the upload target; codec-derived fields are applied to this instance
     * @param source   the raw video channel, which is not closed by this method
     * @param quality  the quality preset selecting video and audio bitrate
     * @return the encoded MP4 payload
     * @throws WhatsAppMediaException.Processing if any stage of the demux, decode, encode, or mux
     *         pipeline fails
     */
    public MediaPayload run(MediaProvider provider, SeekableByteChannel source,
                            SettingsSyncAction.MediaQualitySetting quality)
            throws WhatsAppMediaException.Processing {
        FFmpegLoader.ensureLoaded();
        var hd = quality == SettingsSyncAction.MediaQualitySetting.HD;
        var videoBitrate = hd ? VIDEO_BITRATE_HD : VIDEO_BITRATE_STANDARD;
        var audioBitrate = hd ? AUDIO_BITRATE_HD : AUDIO_BITRATE_STANDARD;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "video transcode starting, quality={0}, videoBitrate={1}, audioBitrate={2}",
                    quality, videoBitrate, audioBitrate);
        }
        try (var arena = Arena.ofShared()) {
            return new Run(arena, source, hd, videoBitrate, audioBitrate).execute(provider);
        }
    }

    /**
     * Holds the one-shot state for a single transcode invocation, including every libav resource
     * that needs cleanup on completion or failure.
     *
     * <p>An instance lives only for the duration of {@link #execute(MediaProvider)}. The shared
     * {@link Arena} is owned by the caller; this run owns every FFmpeg pointer it allocates and
     * releases them in {@link #cleanup()} on every exit path, whether the transcode succeeds or
     * throws.
     */
    private static final class Run {
        /**
         * Holds the shared arena backing the AVIO bridges and every transient FFmpeg argument
         * allocated during the run.
         */
        final Arena arena;

        /**
         * Holds the raw source video channel that the demuxer reads from.
         */
        final SeekableByteChannel sourceChannel;

        /**
         * Indicates whether the {@link SettingsSyncAction.MediaQualitySetting#HD} quality preset is
         * in effect for this run.
         */
        final boolean hd;

        /**
         * Holds the target H.264 bitrate in bits per second selected from the quality preset.
         */
        final long videoBitrate;

        /**
         * Holds the target AAC bitrate in bits per second selected from the quality preset.
         */
        final long audioBitrate;

        /**
         * Holds the AVIO bridge that feeds the source channel to the demuxer.
         */
        AvioReadBuffer inputBridge;

        /**
         * Holds the AVIO bridge that drains the muxer to a temporary file.
         */
        AvioWriteBuffer.FileSystem outputBridge;

        /**
         * Holds the open input demuxer context, or {@link MemorySegment#NULL} before it is opened.
         */
        MemorySegment inFmtCtx = MemorySegment.NULL;

        /**
         * Holds the open output muxer context, or {@link MemorySegment#NULL} before it is opened.
         */
        MemorySegment outFmtCtx = MemorySegment.NULL;

        /**
         * Holds the open video decoder context, or {@link MemorySegment#NULL} before it is opened.
         */
        MemorySegment videoDecCtx = MemorySegment.NULL;

        /**
         * Holds the open audio decoder context, or {@link MemorySegment#NULL} when the source has no
         * audio stream.
         */
        MemorySegment audioDecCtx = MemorySegment.NULL;

        /**
         * Holds the open H.264 encoder context, or {@link MemorySegment#NULL} before it is opened.
         */
        MemorySegment videoEncCtx = MemorySegment.NULL;

        /**
         * Holds the open AAC encoder context, or {@link MemorySegment#NULL} when the source has no
         * audio stream.
         */
        MemorySegment audioEncCtx = MemorySegment.NULL;

        /**
         * Holds the scale filter graph applied to every decoded video frame.
         */
        MemorySegment filterGraph = MemorySegment.NULL;

        /**
         * Holds the buffer source filter context inside {@link #filterGraph} that decoded frames are
         * pushed into.
         */
        MemorySegment filterSrcCtx = MemorySegment.NULL;

        /**
         * Holds the buffer sink filter context inside {@link #filterGraph} that scaled frames are
         * pulled from.
         */
        MemorySegment filterSinkCtx = MemorySegment.NULL;

        /**
         * Holds the audio resampler context, or {@link MemorySegment#NULL} when the source has no
         * audio stream.
         */
        MemorySegment swr = MemorySegment.NULL;

        /**
         * Holds the reusable packet that the demuxer reads each source packet into.
         */
        MemorySegment demuxPacket = MemorySegment.NULL;

        /**
         * Holds the reusable frame that each decoder writes its decoded output into.
         */
        MemorySegment decFrame = MemorySegment.NULL;

        /**
         * Holds the reusable frame that the filter graph writes its scaled output into.
         */
        MemorySegment filterFrame = MemorySegment.NULL;

        /**
         * Holds the reusable AAC encoder input frame that resampled samples are copied into.
         */
        MemorySegment audioEncFrame = MemorySegment.NULL;

        /**
         * Holds the reusable packet that each encoder writes its encoded output into.
         */
        MemorySegment encPacket = MemorySegment.NULL;

        /**
         * Holds the index of the picked video stream in the input.
         */
        int videoStreamIndex = -1;

        /**
         * Holds the index of the picked audio stream in the input, or {@code -1} when the source has
         * no audio stream.
         */
        int audioStreamIndex = -1;

        /**
         * Holds the output stream index of the muxed video stream.
         */
        int videoOutIndex = -1;

        /**
         * Holds the output stream index of the muxed audio stream, or {@code -1} when no audio is
         * present.
         */
        int audioOutIndex = -1;

        /**
         * Holds a reference to the first decoded and scaled video frame, retained for the thumbnail
         * pass.
         *
         * <p>It remains {@link MemorySegment#NULL} until the first frame clears the filter graph;
         * when the source decodes no frame at all it stays {@link MemorySegment#NULL} and no
         * thumbnail is produced.
         */
        MemorySegment thumbnailFrame = MemorySegment.NULL;

        /**
         * Holds the encoded video width in pixels after the resolution policy is applied.
         */
        int encodedWidth;

        /**
         * Holds the encoded video height in pixels after the resolution policy is applied.
         */
        int encodedHeight;

        /**
         * Holds the monotonically increasing presentation timestamp assigned to each encoded video
         * frame.
         */
        long videoPts;

        /**
         * Holds the running presentation timestamp, in samples, assigned to each encoded audio
         * frame.
         */
        long audioPts;

        /**
         * Holds the source video frame rate numerator.
         *
         * @implNote This implementation defaults to {@code 30} so a malformed or absent source
         * frame rate still yields a usable encoder time base.
         */
        int frameRateNum = 30;

        /**
         * Holds the source video frame rate denominator.
         */
        int frameRateDen = 1;

        /**
         * Holds the AAC frame size in samples.
         *
         * <p>It is seeded with {@link #DEFAULT_AAC_FRAME_SAMPLES} and replaced with the encoder's
         * reported frame size once the AAC encoder is opened and reports a positive value.
         */
        int aacFrameSize = DEFAULT_AAC_FRAME_SAMPLES;

        /**
         * Constructs the run state from the caller-supplied arena, source, and resolved bitrates.
         *
         * @param arena         the shared arena that backs every allocation made during the run
         * @param sourceChannel the source video channel
         * @param hd            whether the {@link SettingsSyncAction.MediaQualitySetting#HD} preset
         *                      is in effect
         * @param videoBitrate  the target H.264 bitrate in bits per second
         * @param audioBitrate  the target AAC bitrate in bits per second
         */
        Run(Arena arena, SeekableByteChannel sourceChannel, boolean hd,
            long videoBitrate, long audioBitrate) {
            this.arena = arena;
            this.sourceChannel = sourceChannel;
            this.hd = hd;
            this.videoBitrate = videoBitrate;
            this.audioBitrate = audioBitrate;
        }

        /**
         * Runs the full transcode, applies the codec-derived metadata to {@code provider}, and
         * returns the encoded payload stream.
         *
         * <p>The pipeline opens the input, output, encoders, filter graph, and resampler, writes the
         * MP4 header, runs the transcode loop, flushes the encoders, and writes the trailer. On a
         * successful run the output length and a {@link VideoMessage}'s dimensions, duration, and
         * thumbnail are applied to {@code provider}; on any failure the partially written output
         * file is deleted. Every libav resource is released through {@link #cleanup()} on both the
         * success and failure paths.
         *
         * @param provider the upload target that receives the metadata updates
         * @return the encoded MP4 payload
         * @throws WhatsAppMediaException.Processing if any stage of the pipeline fails
         */
        MediaPayload execute(MediaProvider provider) throws WhatsAppMediaException.Processing {
            var success = false;
            Path outputPath = null;
            try {
                openInput();
                openOutput();
                openVideoEncoder();
                if (audioStreamIndex >= 0) {
                    openAudioEncoder();
                }
                buildFilterGraph();
                if (audioStreamIndex >= 0) {
                    openResampler();
                }
                writeHeader();
                transcodeLoop();
                flushEncoders();
                FFmpegError.check("av_write_trailer", Ffmpeg.av_write_trailer(outFmtCtx));
                var durationSeconds = Math.max(1, computeDurationSeconds());
                try {
                    outputPath = outputBridge.release();
                } catch (IOException e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "video transcode failed to flush output", e);
                    throw new WhatsAppMediaException.Processing("failed to flush video output", e);
                }
                var outputLength = 0L;
                try {
                    outputLength = Files.size(outputPath);
                } catch (IOException e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "video transcode failed to size output", e);
                    throw new WhatsAppMediaException.Processing("failed to size video output", e);
                }
                var thumbnail = encodeThumbnail();
                provider.setMediaSize(outputLength);
                if (provider instanceof VideoMessage video) {
                    video.setMimetype("video/mp4");
                    video.setWidth(encodedWidth);
                    video.setHeight(encodedHeight);
                    video.setSeconds(durationSeconds);
                    if (thumbnail != null) {
                        video.setJpegThumbnail(thumbnail);
                    }
                }
                success = true;
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "video transcode finished, width={0}, height={1}, seconds={2}, bytes={3}, thumbnail={4}",
                            encodedWidth, encodedHeight, durationSeconds, outputLength, thumbnail != null);
                }
                return new MediaPayload.OfPath(outputPath, outputLength, true);
            } finally {
                if (!success && outputPath != null) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "video transcode failed, discarding partial output");
                    try {
                        Files.deleteIfExists(outputPath);
                    } catch (IOException ignored) {
                    }
                }
                cleanup();
            }
        }

        /**
         * Opens the input demuxer, picks the first video and audio streams, and opens their
         * decoders.
         *
         * <p>The demuxer reads through the {@link AvioReadBuffer} bridge rather than a file path.
         * The first video stream is mandatory; its absence is a processing failure. The first audio
         * stream is optional and, when present, its decoder is opened too. The source average frame
         * rate is captured for the encoder time base when it is well-formed, otherwise the defaults
         * in {@link #frameRateNum} and {@link #frameRateDen} are kept.
         *
         * @throws WhatsAppMediaException.Processing if the demuxer cannot be opened, stream info
         *         cannot be found, the source has no video stream, or a decoder cannot be opened
         */
        void openInput() throws WhatsAppMediaException.Processing {
            inputBridge = new AvioReadBuffer(arena, sourceChannel);
            inFmtCtx = FFmpegError.requireNonNull("avformat_alloc_context",
                    Ffmpeg.avformat_alloc_context());
            AVFormatContext.pb(inFmtCtx, inputBridge.ioContext());
            var formatPp = arena.allocate(ValueLayout.ADDRESS);
            formatPp.set(ValueLayout.ADDRESS, 0L, inFmtCtx);
            FFmpegError.check("avformat_open_input",
                    Ffmpeg.avformat_open_input(formatPp, MemorySegment.NULL,
                            MemorySegment.NULL, MemorySegment.NULL));
            inFmtCtx = formatPp.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFormatContext.layout().byteSize());
            FFmpegError.check("avformat_find_stream_info",
                    Ffmpeg.avformat_find_stream_info(inFmtCtx, MemorySegment.NULL));
            videoStreamIndex = pickStream(inFmtCtx, Ffmpeg.AVMEDIA_TYPE_VIDEO());
            if (videoStreamIndex < 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "video transcode input has no video stream");
                throw new WhatsAppMediaException.Processing("no video stream in video source");
            }
            audioStreamIndex = pickStream(inFmtCtx, Ffmpeg.AVMEDIA_TYPE_AUDIO());
            videoDecCtx = openDecoder(streamPointer(inFmtCtx, videoStreamIndex));
            var videoStream = streamPointer(inFmtCtx, videoStreamIndex);
            var avgFr = AVStream.avg_frame_rate(videoStream);
            var num = AVRational.num(avgFr);
            var den = AVRational.den(avgFr);
            if (num > 0 && den > 0) {
                frameRateNum = num;
                frameRateDen = den;
            }
            if (audioStreamIndex >= 0) {
                audioDecCtx = openDecoder(streamPointer(inFmtCtx, audioStreamIndex));
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "video transcode input opened, videoStream={0}, hasAudio={1}, frameRate={2}/{3}",
                        videoStreamIndex, audioStreamIndex >= 0, frameRateNum, frameRateDen);
            }
        }

        /**
         * Opens a libavcodec decoder matching the codec parameters of the given input stream.
         *
         * <p>The decoder is looked up by the stream's codec id, allocated, populated from the
         * stream's codec parameters, and opened.
         *
         * @param stream the input stream pointer whose codec parameters drive decoder selection
         * @return the open decoder context
         * @throws WhatsAppMediaException.Processing if no decoder is found or the decoder cannot be
         *         allocated, configured, or opened
         */
        MemorySegment openDecoder(MemorySegment stream) throws WhatsAppMediaException.Processing {
            var params = AVStream.codecpar(stream);
            var codecId = AVCodecParameters.codec_id(params);
            var codec = FFmpegError.requireNonNull("avcodec_find_decoder(" + codecId + ")",
                    Ffmpeg.avcodec_find_decoder(codecId));
            var ctx = FFmpegError.requireNonNull("avcodec_alloc_context3",
                    Ffmpeg.avcodec_alloc_context3(codec));
            FFmpegError.check("avcodec_parameters_to_context",
                    Ffmpeg.avcodec_parameters_to_context(ctx, params));
            FFmpegError.check("avcodec_open2",
                    Ffmpeg.avcodec_open2(ctx, codec, MemorySegment.NULL));
            return ctx;
        }

        /**
         * Allocates the MP4 output muxer wired to the file-system AVIO bridge with {@code +faststart}
         * set.
         *
         * <p>The muxer writes through the {@link AvioWriteBuffer.FileSystem} bridge rather than a
         * file path. The {@code movflags=+faststart} option is set on the muxer's private data when
         * that private data is present, so the moov atom is relocated to the head of the file.
         *
         * @throws WhatsAppMediaException.Processing if the output bridge cannot be opened or the
         *         output context cannot be allocated or configured
         */
        void openOutput() throws WhatsAppMediaException.Processing {
            try {
                outputBridge = AvioWriteBuffer.ofFileSystem(arena);
            } catch (IOException e) {
                throw new WhatsAppMediaException.Processing("failed to open video output", e);
            }
            var fmtCtxPp = arena.allocate(ValueLayout.ADDRESS);
            FFmpegError.check("avformat_alloc_output_context2(mp4)",
                    Ffmpeg.avformat_alloc_output_context2(fmtCtxPp, MemorySegment.NULL,
                            arena.allocateFrom("mp4"), MemorySegment.NULL));
            outFmtCtx = fmtCtxPp.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFormatContext.layout().byteSize());
            AVFormatContext.pb(outFmtCtx, outputBridge.ioContext());
            var priv = AVFormatContext.priv_data(outFmtCtx);
            if (priv != null && priv != MemorySegment.NULL) {
                FFmpegError.check("av_opt_set(movflags=+faststart)",
                        Ffmpeg.av_opt_set(priv,
                                arena.allocateFrom("movflags"),
                                arena.allocateFrom("+faststart"),
                                Ffmpeg.AV_OPT_SEARCH_CHILDREN()));
            }
        }

        /**
         * Configures and opens the H.264 encoder via libopenh264 and registers its output stream.
         *
         * <p>The encoded dimensions are derived from the source dimensions according to the active
         * quality preset and stored in {@link #encodedWidth} and {@link #encodedHeight}. The encoder
         * is configured for {@code yuv420p}, the selected bitrate, the source-derived frame rate and
         * time base, baseline profile, and level 3.1, then opened and attached as a new output
         * stream whose index is recorded in {@link #videoOutIndex}.
         *
         * @implNote This implementation applies a resolution policy: the
         * {@link SettingsSyncAction.MediaQualitySetting#HD} preset keeps the source dimensions
         * rounded to even values, while the {@link SettingsSyncAction.MediaQualitySetting#STANDARD}
         * preset caps the height at {@link #MAX_HEIGHT_STANDARD} and rescales the width to preserve
         * the aspect ratio. Both dimensions are forced even via {@link #roundEven(int)} because
         * {@code yuv420p} chroma subsampling requires even width and height. The global-header codec
         * flag is set only when the muxer reports {@link #AVFMT_GLOBALHEADER}.
         *
         * @throws WhatsAppMediaException.Processing if the encoder is not found or cannot be
         *         allocated, configured, opened, or attached as an output stream
         */
        void openVideoEncoder() throws WhatsAppMediaException.Processing {
            var srcW = AVCodecContext.width(videoDecCtx);
            var srcH = AVCodecContext.height(videoDecCtx);
            if (hd) {
                encodedWidth = roundEven(srcW);
                encodedHeight = roundEven(srcH);
            } else if (srcH > MAX_HEIGHT_STANDARD) {
                encodedHeight = MAX_HEIGHT_STANDARD;
                encodedWidth = roundEven((int) Math.round((double) srcW * MAX_HEIGHT_STANDARD / srcH));
            } else {
                encodedHeight = roundEven(srcH);
                encodedWidth = roundEven(srcW);
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "video encoder resolution {0}x{1} -> {2}x{3}, hd={4}",
                        srcW, srcH, encodedWidth, encodedHeight, hd);
            }
            var encoder = FFmpegError.requireNonNull("avcodec_find_encoder_by_name(libopenh264)",
                    Ffmpeg.avcodec_find_encoder_by_name(arena.allocateFrom("libopenh264")));
            videoEncCtx = FFmpegError.requireNonNull("avcodec_alloc_context3(h264)",
                    Ffmpeg.avcodec_alloc_context3(encoder));
            AVCodecContext.width(videoEncCtx, encodedWidth);
            AVCodecContext.height(videoEncCtx, encodedHeight);
            AVCodecContext.pix_fmt(videoEncCtx, Ffmpeg.AV_PIX_FMT_YUV420P());
            AVCodecContext.bit_rate(videoEncCtx, videoBitrate);
            var tb = arena.allocate(8);
            tb.set(ValueLayout.JAVA_INT, 0L, frameRateDen);
            tb.set(ValueLayout.JAVA_INT, 4L, frameRateNum);
            AVCodecContext.time_base(videoEncCtx, tb);
            var fr = arena.allocate(8);
            fr.set(ValueLayout.JAVA_INT, 0L, frameRateNum);
            fr.set(ValueLayout.JAVA_INT, 4L, frameRateDen);
            AVCodecContext.framerate(videoEncCtx, fr);
            if ((AVOutputFormat.flags(AVFormatContext.oformat(outFmtCtx)) & AVFMT_GLOBALHEADER) != 0) {
                AVCodecContext.flags(videoEncCtx,
                        AVCodecContext.flags(videoEncCtx) | Ffmpeg.AV_CODEC_FLAG_GLOBAL_HEADER());
            }
            FFmpegError.check("av_opt_set(profile=baseline)",
                    Ffmpeg.av_opt_set(videoEncCtx,
                            arena.allocateFrom("profile"),
                            arena.allocateFrom("baseline"),
                            Ffmpeg.AV_OPT_SEARCH_CHILDREN()));
            FFmpegError.check("av_opt_set(level=3.1)",
                    Ffmpeg.av_opt_set(videoEncCtx,
                            arena.allocateFrom("level"),
                            arena.allocateFrom("3.1"),
                            Ffmpeg.AV_OPT_SEARCH_CHILDREN()));
            FFmpegError.check("avcodec_open2(libopenh264)",
                    Ffmpeg.avcodec_open2(videoEncCtx, encoder, MemorySegment.NULL));
            var stream = FFmpegError.requireNonNull("avformat_new_stream(video)",
                    Ffmpeg.avformat_new_stream(outFmtCtx, encoder));
            FFmpegError.check("avcodec_parameters_from_context(video)",
                    Ffmpeg.avcodec_parameters_from_context(AVStream.codecpar(stream), videoEncCtx));
            AVStream.time_base(stream, tb);
            videoOutIndex = AVStream.index(stream);
        }

        /**
         * Configures and opens the native FFmpeg AAC encoder and registers its output stream.
         *
         * <p>The encoder is configured for {@link #AUDIO_SAMPLE_RATE} Hz, planar float samples, the
         * selected bitrate, and the default {@link #AUDIO_CHANNELS}-channel layout, then opened. Its
         * reported frame size, when positive, replaces {@link #aacFrameSize}. The encoder is
         * attached as a new output stream whose index is recorded in {@link #audioOutIndex}. The
         * global-header codec flag is set only when the muxer reports {@link #AVFMT_GLOBALHEADER}.
         *
         * @throws WhatsAppMediaException.Processing if the encoder is not found or cannot be
         *         allocated, configured, opened, or attached as an output stream
         */
        void openAudioEncoder() throws WhatsAppMediaException.Processing {
            var encoder = FFmpegError.requireNonNull("avcodec_find_encoder(AAC)",
                    Ffmpeg.avcodec_find_encoder(Ffmpeg.AV_CODEC_ID_AAC()));
            audioEncCtx = FFmpegError.requireNonNull("avcodec_alloc_context3(aac)",
                    Ffmpeg.avcodec_alloc_context3(encoder));
            AVCodecContext.sample_rate(audioEncCtx, AUDIO_SAMPLE_RATE);
            AVCodecContext.sample_fmt(audioEncCtx, Ffmpeg.AV_SAMPLE_FMT_FLTP());
            AVCodecContext.bit_rate(audioEncCtx, audioBitrate);
            var encLayout = AVChannelLayout.allocate(arena);
            Ffmpeg.av_channel_layout_default(encLayout, AUDIO_CHANNELS);
            Ffmpeg.av_channel_layout_copy(AVCodecContext.ch_layout(audioEncCtx), encLayout);
            var tb = arena.allocate(8);
            tb.set(ValueLayout.JAVA_INT, 0L, 1);
            tb.set(ValueLayout.JAVA_INT, 4L, AUDIO_SAMPLE_RATE);
            AVCodecContext.time_base(audioEncCtx, tb);
            if ((AVOutputFormat.flags(AVFormatContext.oformat(outFmtCtx)) & AVFMT_GLOBALHEADER) != 0) {
                AVCodecContext.flags(audioEncCtx,
                        AVCodecContext.flags(audioEncCtx) | Ffmpeg.AV_CODEC_FLAG_GLOBAL_HEADER());
            }
            FFmpegError.check("avcodec_open2(aac)",
                    Ffmpeg.avcodec_open2(audioEncCtx, encoder, MemorySegment.NULL));
            var fs = AVCodecContext.frame_size(audioEncCtx);
            if (fs > 0) {
                aacFrameSize = fs;
            }
            var stream = FFmpegError.requireNonNull("avformat_new_stream(audio)",
                    Ffmpeg.avformat_new_stream(outFmtCtx, encoder));
            FFmpegError.check("avcodec_parameters_from_context(audio)",
                    Ffmpeg.avcodec_parameters_from_context(AVStream.codecpar(stream), audioEncCtx));
            AVStream.time_base(stream, tb);
            audioOutIndex = AVStream.index(stream);
        }

        /**
         * Builds the scale filter graph that drives every decoded video frame to the encoded
         * dimensions and {@code yuv420p}.
         *
         * <p>The graph wires a buffer source configured from the decoder's dimensions, pixel format,
         * and time base through a {@code scale} plus {@code format=yuv420p} chain to a buffer sink.
         * The source and sink filter contexts are recorded in {@link #filterSrcCtx} and
         * {@link #filterSinkCtx}.
         *
         * @implNote This implementation uses the {@code bicubic} scaler flag, which trades speed for
         * higher resampling quality on the downscale path.
         *
         * @throws WhatsAppMediaException.Processing if the graph cannot be allocated, the buffer
         *         filters cannot be created, or the chain cannot be parsed or configured
         */
        void buildFilterGraph() throws WhatsAppMediaException.Processing {
            filterGraph = FFmpegError.requireNonNull("avfilter_graph_alloc",
                    Ffmpeg.avfilter_graph_alloc());
            var srcArgs = String.format(
                    "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=1/1",
                    AVCodecContext.width(videoDecCtx),
                    AVCodecContext.height(videoDecCtx),
                    AVCodecContext.pix_fmt(videoDecCtx),
                    frameRateDen, frameRateNum);
            var bufferFilter = FFmpegError.requireNonNull("avfilter_get_by_name(buffer)",
                    Ffmpeg.avfilter_get_by_name(arena.allocateFrom("buffer")));
            var sinkFilter = FFmpegError.requireNonNull("avfilter_get_by_name(buffersink)",
                    Ffmpeg.avfilter_get_by_name(arena.allocateFrom("buffersink")));
            var srcPp = arena.allocate(ValueLayout.ADDRESS);
            var sinkPp = arena.allocate(ValueLayout.ADDRESS);
            FFmpegError.check("avfilter_graph_create_filter(in)",
                    Ffmpeg.avfilter_graph_create_filter(srcPp, bufferFilter,
                            arena.allocateFrom("in"), arena.allocateFrom(srcArgs),
                            MemorySegment.NULL, filterGraph));
            FFmpegError.check("avfilter_graph_create_filter(out)",
                    Ffmpeg.avfilter_graph_create_filter(sinkPp, sinkFilter,
                            arena.allocateFrom("out"), MemorySegment.NULL,
                            MemorySegment.NULL, filterGraph));
            filterSrcCtx = srcPp.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFilterContext.layout().byteSize());
            filterSinkCtx = sinkPp.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFilterContext.layout().byteSize());
            var chain = String.format("scale=%d:%d:flags=bicubic,format=yuv420p",
                    encodedWidth, encodedHeight);
            var outputs = FFmpegError.requireNonNull("avfilter_inout_alloc(outputs)",
                    Ffmpeg.avfilter_inout_alloc());
            var inputs = FFmpegError.requireNonNull("avfilter_inout_alloc(inputs)",
                    Ffmpeg.avfilter_inout_alloc());
            AVFilterInOut.name(outputs, Ffmpeg.av_strdup(arena.allocateFrom("in")));
            AVFilterInOut.filter_ctx(outputs, filterSrcCtx);
            AVFilterInOut.pad_idx(outputs, 0);
            AVFilterInOut.next(outputs, MemorySegment.NULL);
            AVFilterInOut.name(inputs, Ffmpeg.av_strdup(arena.allocateFrom("out")));
            AVFilterInOut.filter_ctx(inputs, filterSinkCtx);
            AVFilterInOut.pad_idx(inputs, 0);
            AVFilterInOut.next(inputs, MemorySegment.NULL);
            var outputsPp = arena.allocate(ValueLayout.ADDRESS);
            var inputsPp = arena.allocate(ValueLayout.ADDRESS);
            outputsPp.set(ValueLayout.ADDRESS, 0L, outputs);
            inputsPp.set(ValueLayout.ADDRESS, 0L, inputs);
            try {
                FFmpegError.check("avfilter_graph_parse_ptr",
                        Ffmpeg.avfilter_graph_parse_ptr(filterGraph, arena.allocateFrom(chain),
                                inputsPp, outputsPp, MemorySegment.NULL));
                FFmpegError.check("avfilter_graph_config",
                        Ffmpeg.avfilter_graph_config(filterGraph, MemorySegment.NULL));
            } finally {
                Ffmpeg.avfilter_inout_free(inputsPp);
                Ffmpeg.avfilter_inout_free(outputsPp);
            }
        }

        /**
         * Sets up the audio resampler that converts the decoded audio to the encoder's format.
         *
         * <p>The resampler is configured to convert from the audio decoder's channel layout, sample
         * format, and sample rate to the default {@link #AUDIO_CHANNELS}-channel layout, planar
         * float samples, and {@link #AUDIO_SAMPLE_RATE} Hz, then initialized.
         *
         * @throws WhatsAppMediaException.Processing if the resampler cannot be allocated, configured,
         *         or initialized
         */
        void openResampler() throws WhatsAppMediaException.Processing {
            var swrPp = arena.allocate(ValueLayout.ADDRESS);
            var outLayout = AVChannelLayout.allocate(arena);
            Ffmpeg.av_channel_layout_default(outLayout, AUDIO_CHANNELS);
            FFmpegError.check("swr_alloc_set_opts2",
                    Ffmpeg.swr_alloc_set_opts2(swrPp, outLayout,
                            Ffmpeg.AV_SAMPLE_FMT_FLTP(), AUDIO_SAMPLE_RATE,
                            AVCodecContext.ch_layout(audioDecCtx),
                            AVCodecContext.sample_fmt(audioDecCtx),
                            AVCodecContext.sample_rate(audioDecCtx),
                            0, MemorySegment.NULL));
            swr = FFmpegError.requireNonNull("swr_alloc_set_opts2 out",
                    swrPp.get(ValueLayout.ADDRESS, 0L));
            FFmpegError.check("swr_init", Ffmpeg.swr_init(swr));
        }

        /**
         * Allocates the reusable packets and frames and writes the MP4 file header.
         *
         * <p>The demuxer packet, decoder frame, filter frame, and encoder packet are allocated for
         * reuse across the transcode loop. When the source has audio the AAC encoder input frame is
         * allocated and configured for planar float samples, the default channel layout,
         * {@link #AUDIO_SAMPLE_RATE} Hz, and {@link #aacFrameSize} samples, with its sample buffer
         * reserved. The MP4 header is then written.
         *
         * @throws WhatsAppMediaException.Processing if any packet or frame cannot be allocated, the
         *         audio frame buffer cannot be reserved, or the header cannot be written
         */
        void writeHeader() throws WhatsAppMediaException.Processing {
            demuxPacket = FFmpegError.requireNonNull("av_packet_alloc(demux)",
                    Ffmpeg.av_packet_alloc());
            decFrame = FFmpegError.requireNonNull("av_frame_alloc(dec)",
                    Ffmpeg.av_frame_alloc());
            filterFrame = FFmpegError.requireNonNull("av_frame_alloc(filter)",
                    Ffmpeg.av_frame_alloc());
            encPacket = FFmpegError.requireNonNull("av_packet_alloc(enc)",
                    Ffmpeg.av_packet_alloc());
            if (audioEncCtx != MemorySegment.NULL) {
                audioEncFrame = FFmpegError.requireNonNull("av_frame_alloc(audio enc)",
                        Ffmpeg.av_frame_alloc());
                AVFrame.format(audioEncFrame, Ffmpeg.AV_SAMPLE_FMT_FLTP());
                var layout = AVChannelLayout.allocate(arena);
                Ffmpeg.av_channel_layout_default(layout, AUDIO_CHANNELS);
                Ffmpeg.av_channel_layout_copy(AVFrame.ch_layout(audioEncFrame), layout);
                AVFrame.sample_rate(audioEncFrame, AUDIO_SAMPLE_RATE);
                AVFrame.nb_samples(audioEncFrame, aacFrameSize);
                FFmpegError.check("av_frame_get_buffer(audio enc)",
                        Ffmpeg.av_frame_get_buffer(audioEncFrame, 0));
            }
            FFmpegError.check("avformat_write_header",
                    Ffmpeg.avformat_write_header(outFmtCtx, MemorySegment.NULL));
        }

        /**
         * Runs the main demux, decode, encode, and mux loop until the source is exhausted.
         *
         * <p>Each iteration reads one source packet and routes it to the matching decoder; packets
         * belonging to neither the picked video nor the picked audio stream are dropped. When the
         * demuxer reaches end of input both decoders are flushed with a {@code NULL} packet and the
         * loop terminates. After each read both decoders are drained so produced frames flow through
         * the filter graph, resampler, and encoders to the muxer.
         *
         * @throws WhatsAppMediaException.Processing if a packet cannot be sent to a decoder for a
         *         reason other than back-pressure, or any downstream drain fails
         */
        void transcodeLoop() throws WhatsAppMediaException.Processing {
            var eof = false;
            while (!eof) {
                var read = Ffmpeg.av_read_frame(inFmtCtx, demuxPacket);
                if (read < 0) {
                    if (videoDecCtx != MemorySegment.NULL) {
                        Ffmpeg.avcodec_send_packet(videoDecCtx, MemorySegment.NULL);
                    }
                    if (audioDecCtx != MemorySegment.NULL) {
                        Ffmpeg.avcodec_send_packet(audioDecCtx, MemorySegment.NULL);
                    }
                    eof = true;
                } else {
                    var idx = AVPacket.stream_index(demuxPacket);
                    if (idx == videoStreamIndex) {
                        var sent = Ffmpeg.avcodec_send_packet(videoDecCtx, demuxPacket);
                        Ffmpeg.av_packet_unref(demuxPacket);
                        if (sent < 0 && !FFmpegError.isAgain(sent)) {
                            throw new WhatsAppMediaException.Processing(
                                    "video send_packet failed: " + FFmpegError.describe(sent));
                        }
                    } else if (idx == audioStreamIndex && audioDecCtx != MemorySegment.NULL) {
                        var sent = Ffmpeg.avcodec_send_packet(audioDecCtx, demuxPacket);
                        Ffmpeg.av_packet_unref(demuxPacket);
                        if (sent < 0 && !FFmpegError.isAgain(sent)) {
                            throw new WhatsAppMediaException.Processing(
                                    "audio send_packet failed: " + FFmpegError.describe(sent));
                        }
                    } else {
                        Ffmpeg.av_packet_unref(demuxPacket);
                        continue;
                    }
                }
                drainVideoDecoder();
                if (audioDecCtx != MemorySegment.NULL) {
                    drainAudioDecoder();
                }
            }
        }

        /**
         * Drains every frame the video decoder has produced and routes each through the filter graph
         * and encoder.
         *
         * <p>Each decoded frame is pushed into the filter graph and every scaled frame the sink
         * yields is assigned a monotonically increasing presentation timestamp and sent to the H.264
         * encoder. The first scaled frame is additionally retained in {@link #thumbnailFrame} for the
         * later thumbnail pass. The method returns when the decoder reports back-pressure or end of
         * stream.
         *
         * @throws WhatsAppMediaException.Processing if a frame cannot be received from the decoder,
         *         added to the filter graph, or pulled from the sink for a reason other than
         *         back-pressure or end of stream
         */
        void drainVideoDecoder() throws WhatsAppMediaException.Processing {
            while (true) {
                var got = Ffmpeg.avcodec_receive_frame(videoDecCtx, decFrame);
                if (FFmpegError.isAgain(got) || FFmpegError.isEof(got)) {
                    return;
                }
                if (got < 0) {
                    throw new WhatsAppMediaException.Processing(
                            "video receive_frame failed: " + FFmpegError.describe(got));
                }
                FFmpegError.check("av_buffersrc_add_frame_flags",
                        Ffmpeg.av_buffersrc_add_frame_flags(filterSrcCtx, decFrame, 0));
                while (true) {
                    var gotFiltered = Ffmpeg.av_buffersink_get_frame(filterSinkCtx, filterFrame);
                    if (FFmpegError.isAgain(gotFiltered) || FFmpegError.isEof(gotFiltered)) {
                        break;
                    }
                    if (gotFiltered < 0) {
                        throw new WhatsAppMediaException.Processing(
                                "buffersink_get_frame failed: " + FFmpegError.describe(gotFiltered));
                    }
                    if (thumbnailFrame == MemorySegment.NULL) {
                        thumbnailFrame = Ffmpeg.av_frame_alloc();
                        if (thumbnailFrame != MemorySegment.NULL) {
                            Ffmpeg.av_frame_ref(thumbnailFrame, filterFrame);
                        }
                    }
                    AVFrame.pts(filterFrame, videoPts++);
                    pushFrameAndDrain(videoEncCtx, filterFrame, videoOutIndex);
                    Ffmpeg.av_frame_unref(filterFrame);
                }
                Ffmpeg.av_frame_unref(decFrame);
            }
        }

        /**
         * Drains every frame the audio decoder has produced and routes each through the resampler
         * and AAC encoder.
         *
         * <p>Each decoded audio frame is resampled and encoded. The method returns when the decoder
         * reports back-pressure or end of stream.
         *
         * @throws WhatsAppMediaException.Processing if a frame cannot be received from the decoder
         *         for a reason other than back-pressure or end of stream, or the resample or encode
         *         fails
         */
        void drainAudioDecoder() throws WhatsAppMediaException.Processing {
            while (true) {
                var got = Ffmpeg.avcodec_receive_frame(audioDecCtx, decFrame);
                if (FFmpegError.isAgain(got) || FFmpegError.isEof(got)) {
                    return;
                }
                if (got < 0) {
                    throw new WhatsAppMediaException.Processing(
                            "audio receive_frame failed: " + FFmpegError.describe(got));
                }
                resampleAndEncodeAudio(decFrame);
                Ffmpeg.av_frame_unref(decFrame);
            }
        }

        /**
         * Resamples one decoded audio frame and dispatches the produced samples to the AAC encoder.
         *
         * <p>A per-channel planar output buffer sized to {@link #aacFrameSize} samples is allocated
         * and filled by the resampler; when the conversion yields any samples they are written to the
         * encoder input frame and dispatched.
         *
         * @param inFrame the decoded source frame to resample
         * @throws WhatsAppMediaException.Processing if the resample fails or the produced samples
         *         cannot be encoded
         */
        void resampleAndEncodeAudio(MemorySegment inFrame) throws WhatsAppMediaException.Processing {
            var outBufSamples = aacFrameSize;
            var channels = AUDIO_CHANNELS;
            var outDataPtrs = arena.allocate((long) channels * ValueLayout.ADDRESS.byteSize());
            for (var c = 0; c < channels; c++) {
                var plane = arena.allocate((long) outBufSamples * Float.BYTES);
                outDataPtrs.setAtIndex(ValueLayout.ADDRESS, c, plane);
            }
            var produced = FFmpegError.check("swr_convert",
                    Ffmpeg.swr_convert(swr, outDataPtrs, outBufSamples,
                            AVFrame.extended_data(inFrame), AVFrame.nb_samples(inFrame)));
            if (produced > 0) {
                writeAudioFrame(outDataPtrs, produced, channels);
            }
        }

        /**
         * Copies resampled samples into the AAC encoder frame, advances the audio timestamp, and
         * dispatches the frame to the encoder.
         *
         * <p>The encoder input frame is resized to the produced sample count, stamped with the
         * running {@link #audioPts}, made writable, and filled by copying each channel's planar data.
         * The audio timestamp is then advanced by the sample count and the frame is encoded.
         *
         * @param outDataPtrs the per-channel data pointer table holding the resampled samples
         * @param samples     the number of produced samples per channel
         * @param channels    the channel count
         * @throws WhatsAppMediaException.Processing if the frame cannot be made writable or the
         *         encode fails
         */
        void writeAudioFrame(MemorySegment outDataPtrs, int samples, int channels)
                throws WhatsAppMediaException.Processing {
            AVFrame.nb_samples(audioEncFrame, samples);
            AVFrame.pts(audioEncFrame, audioPts);
            FFmpegError.check("av_frame_make_writable(audio)",
                    Ffmpeg.av_frame_make_writable(audioEncFrame));
            for (var c = 0; c < channels; c++) {
                var dstPlane = AVFrame.data(audioEncFrame).getAtIndex(ValueLayout.ADDRESS, c);
                var srcPlane = outDataPtrs.getAtIndex(ValueLayout.ADDRESS, c)
                        .reinterpret((long) samples * Float.BYTES);
                MemorySegment.copy(srcPlane, 0L,
                        dstPlane.reinterpret((long) samples * Float.BYTES), 0L,
                        (long) samples * Float.BYTES);
            }
            audioPts += samples;
            pushFrameAndDrain(audioEncCtx, audioEncFrame, audioOutIndex);
        }

        /**
         * Sends one frame to the encoder and drains every produced packet to the muxer.
         *
         * <p>Each produced packet is tagged with the output stream index, has its timestamps
         * rescaled to the muxer stream's time base, and is interleaved-written to the muxer. The
         * method returns when the encoder reports back-pressure or end of stream. Passing
         * {@link MemorySegment#NULL} as the frame flushes the encoder.
         *
         * @param encCtx   the encoder context
         * @param frame    the frame to encode, or {@link MemorySegment#NULL} to flush the encoder
         * @param outIndex the muxer stream index to tag produced packets with
         * @throws WhatsAppMediaException.Processing if the frame cannot be sent, a packet cannot be
         *         received for a reason other than back-pressure or end of stream, or a packet cannot
         *         be written
         */
        void pushFrameAndDrain(MemorySegment encCtx, MemorySegment frame, int outIndex)
                throws WhatsAppMediaException.Processing {
            var sent = Ffmpeg.avcodec_send_frame(encCtx, frame);
            if (sent < 0 && !FFmpegError.isAgain(sent) && !FFmpegError.isEof(sent)) {
                throw new WhatsAppMediaException.Processing(
                        "send_frame failed: " + FFmpegError.describe(sent));
            }
            while (true) {
                var got = Ffmpeg.avcodec_receive_packet(encCtx, encPacket);
                if (FFmpegError.isAgain(got) || FFmpegError.isEof(got)) {
                    return;
                }
                if (got < 0) {
                    throw new WhatsAppMediaException.Processing(
                            "receive_packet failed: " + FFmpegError.describe(got));
                }
                AVPacket.stream_index(encPacket, outIndex);
                rescalePacketTimestamps(encCtx, outIndex);
                var written = Ffmpeg.av_interleaved_write_frame(outFmtCtx, encPacket);
                Ffmpeg.av_packet_unref(encPacket);
                if (written < 0) {
                    throw new WhatsAppMediaException.Processing(
                            "av_interleaved_write_frame failed: " + FFmpegError.describe(written));
                }
            }
        }

        /**
         * Rescales the current encoder packet's timestamps from the encoder time base to the muxer
         * stream's time base.
         *
         * @param encCtx   the encoder context whose time base the packet timestamps start in
         * @param outIndex the muxer stream index whose time base the packet timestamps are rescaled
         *                 to
         */
        void rescalePacketTimestamps(MemorySegment encCtx, int outIndex) {
            var stream = AVFormatContext.streams(outFmtCtx)
                    .getAtIndex(ValueLayout.ADDRESS, outIndex)
                    .reinterpret(AVStream.layout().byteSize());
            Ffmpeg.av_packet_rescale_ts(encPacket,
                    AVCodecContext.time_base(encCtx), AVStream.time_base(stream));
        }

        /**
         * Flushes both encoders by sending a {@link MemorySegment#NULL} frame to each.
         *
         * <p>The video encoder is always flushed; the audio encoder is flushed only when the source
         * has an audio stream. Flushing drains any buffered packets to the muxer.
         *
         * @throws WhatsAppMediaException.Processing if either encoder cannot be flushed or its
         *         trailing packets cannot be written
         */
        void flushEncoders() throws WhatsAppMediaException.Processing {
            pushFrameAndDrain(videoEncCtx, MemorySegment.NULL, videoOutIndex);
            if (audioEncCtx != MemorySegment.NULL) {
                pushFrameAndDrain(audioEncCtx, MemorySegment.NULL, audioOutIndex);
            }
        }

        /**
         * Encodes the retained first video frame as an MJPEG thumbnail.
         *
         * <p>The thumbnail is downscaled so its longest edge is at most {@link #VIDEO_THUMB_MAX_EDGE}
         * pixels while preserving the aspect ratio; a frame already within that bound keeps its
         * dimensions. Both dimensions are forced even via {@link #roundEven(int)}. The scaled frame
         * is encoded with the {@code mjpeg} encoder and the bytes are passed through
         * {@link JpegCleaner#clean(byte[])}. When no frame was ever decoded the method returns
         * {@code null}.
         *
         * @return the cleaned JPEG bytes, or {@code null} when no frame was ever decoded
         * @throws WhatsAppMediaException.Processing if the thumbnail cannot be scaled, encoded, or
         *         cleaned
         */
        byte[] encodeThumbnail() throws WhatsAppMediaException.Processing {
            if (thumbnailFrame == MemorySegment.NULL) {
                return null;
            }
            var srcW = AVFrame.width(thumbnailFrame);
            var srcH = AVFrame.height(thumbnailFrame);
            var longest = Math.max(srcW, srcH);
            int dstW;
            int dstH;
            if (longest <= VIDEO_THUMB_MAX_EDGE) {
                dstW = roundEven(srcW);
                dstH = roundEven(srcH);
            } else {
                var scale = (double) VIDEO_THUMB_MAX_EDGE / longest;
                dstW = roundEven((int) Math.round(srcW * scale));
                dstH = roundEven((int) Math.round(srcH * scale));
            }
            var thumbScaled = scaleThumbnail(thumbnailFrame, dstW, dstH);
            try {
                var jpeg = encodeMjpeg(thumbScaled, dstW, dstH);
                return JpegCleaner.clean(jpeg);
            } finally {
                freeFrame(thumbScaled);
            }
        }

        /**
         * Runs a one-shot scale filter graph that resizes the thumbnail source frame.
         *
         * <p>A throwaway filter graph wires a buffer source configured from the source frame through
         * a {@code scale} plus {@code format=yuvj420p} chain to a buffer sink, pushes the source
         * frame, and pulls one scaled frame. The graph is freed before returning; the returned frame
         * is owned by the caller, which must free it. On a sink error the partially allocated output
         * frame is freed before the failure is reported.
         *
         * @implNote This implementation targets {@code yuvj420p} rather than {@code yuv420p} because
         * the JPEG encoder expects the full-range JPEG color space, and uses the {@code bicubic}
         * scaler flag for higher resampling quality.
         *
         * @param srcFrame the source frame to scale
         * @param dstW     the target width in pixels
         * @param dstH     the target height in pixels
         * @return the scaled frame, owned by the caller
         * @throws WhatsAppMediaException.Processing if the graph cannot be built or configured or no
         *         scaled frame can be pulled from the sink
         */
        MemorySegment scaleThumbnail(MemorySegment srcFrame, int dstW, int dstH)
                throws WhatsAppMediaException.Processing {
            var graph = FFmpegError.requireNonNull("avfilter_graph_alloc(thumb)",
                    Ffmpeg.avfilter_graph_alloc());
            try {
                var srcArgs = String.format(
                        "video_size=%dx%d:pix_fmt=%d:time_base=1/1:pixel_aspect=1/1",
                        AVFrame.width(srcFrame), AVFrame.height(srcFrame),
                        AVFrame.format(srcFrame));
                var bufferFilter = FFmpegError.requireNonNull("avfilter_get_by_name(buffer)",
                        Ffmpeg.avfilter_get_by_name(arena.allocateFrom("buffer")));
                var sinkFilter = FFmpegError.requireNonNull("avfilter_get_by_name(buffersink)",
                        Ffmpeg.avfilter_get_by_name(arena.allocateFrom("buffersink")));
                var srcPp = arena.allocate(ValueLayout.ADDRESS);
                var sinkPp = arena.allocate(ValueLayout.ADDRESS);
                FFmpegError.check("avfilter_graph_create_filter(in)",
                        Ffmpeg.avfilter_graph_create_filter(srcPp, bufferFilter,
                                arena.allocateFrom("in"), arena.allocateFrom(srcArgs),
                                MemorySegment.NULL, graph));
                FFmpegError.check("avfilter_graph_create_filter(out)",
                        Ffmpeg.avfilter_graph_create_filter(sinkPp, sinkFilter,
                                arena.allocateFrom("out"), MemorySegment.NULL,
                                MemorySegment.NULL, graph));
                var srcCtx = srcPp.get(ValueLayout.ADDRESS, 0L)
                        .reinterpret(AVFilterContext.layout().byteSize());
                var sinkCtx = sinkPp.get(ValueLayout.ADDRESS, 0L)
                        .reinterpret(AVFilterContext.layout().byteSize());
                var chain = String.format("scale=%d:%d:flags=bicubic,format=yuvj420p",
                        dstW, dstH);
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
                        Ffmpeg.av_buffersrc_add_frame_flags(srcCtx, srcFrame, 0));
                var outFrame = FFmpegError.requireNonNull("av_frame_alloc(thumb)",
                        Ffmpeg.av_frame_alloc());
                var got = Ffmpeg.av_buffersink_get_frame(sinkCtx, outFrame);
                if (got < 0) {
                    freeFrame(outFrame);
                    throw new WhatsAppMediaException.Processing(
                            "thumb buffersink_get_frame failed: " + FFmpegError.describe(got));
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
         * Encodes a single {@code yuvj420p} frame with the {@code mjpeg} encoder and returns the
         * encoded packet bytes.
         *
         * <p>A throwaway MJPEG encoder is configured for the given dimensions, {@code yuvj420p}, and
         * a fixed-quality qscale, then opened. The frame is sent followed by an end-of-stream
         * {@code NULL} frame, one packet is received, and its bytes are copied out. The encoder and
         * packet are freed before returning.
         *
         * @implNote This implementation derives the global quality from {@link #THUMB_QSCALE}
         * multiplied by {@code FF_QP2LAMBDA} under the {@code AV_CODEC_FLAG_QSCALE} flag, the
         * lambda-domain encoding the FFmpeg MJPEG encoder expects.
         *
         * @param frame the input frame to encode
         * @param w     the frame width in pixels
         * @param h     the frame height in pixels
         * @return the encoded JPEG bytes
         * @throws WhatsAppMediaException.Processing if the encoder cannot be found, allocated,
         *         opened, fed, or drained
         */
        byte[] encodeMjpeg(MemorySegment frame, int w, int h)
                throws WhatsAppMediaException.Processing {
            var codec = FFmpegError.requireNonNull("avcodec_find_encoder(MJPEG)",
                    Ffmpeg.avcodec_find_encoder(Ffmpeg.AV_CODEC_ID_MJPEG()));
            var ctx = FFmpegError.requireNonNull("avcodec_alloc_context3(mjpeg)",
                    Ffmpeg.avcodec_alloc_context3(codec));
            try (var local = Arena.ofConfined()) {
                AVCodecContext.width(ctx, w);
                AVCodecContext.height(ctx, h);
                AVCodecContext.pix_fmt(ctx, Ffmpeg.AV_PIX_FMT_YUVJ420P());
                var tb = local.allocate(8);
                tb.set(ValueLayout.JAVA_INT, 0L, 1);
                tb.set(ValueLayout.JAVA_INT, 4L, 1);
                AVCodecContext.time_base(ctx, tb);
                AVCodecContext.flags(ctx,
                        AVCodecContext.flags(ctx) | Ffmpeg.AV_CODEC_FLAG_QSCALE());
                AVCodecContext.global_quality(ctx, THUMB_QSCALE * Ffmpeg.FF_QP2LAMBDA());
                FFmpegError.check("avcodec_open2(mjpeg)",
                        Ffmpeg.avcodec_open2(ctx, codec, MemorySegment.NULL));
                AVFrame.pts(frame, 0);
                FFmpegError.check("avcodec_send_frame(thumb)",
                        Ffmpeg.avcodec_send_frame(ctx, frame));
                FFmpegError.check("avcodec_send_frame(thumb EOF)",
                        Ffmpeg.avcodec_send_frame(ctx, MemorySegment.NULL));
                var packet = FFmpegError.requireNonNull("av_packet_alloc(thumb)",
                        Ffmpeg.av_packet_alloc());
                try {
                    var got = Ffmpeg.avcodec_receive_packet(ctx, packet);
                    if (got < 0) {
                        throw new WhatsAppMediaException.Processing(
                                "thumb receive_packet failed: " + FFmpegError.describe(got));
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
         * Computes the encoded duration in whole seconds.
         *
         * <p>The demuxer's reported duration is used when positive; otherwise the duration is derived
         * from the encoded video presentation timestamp and frame rate. The result is floored at one
         * second so a source too short to round to a whole second still reports a usable duration.
         *
         * @implNote This implementation reads the demuxer duration in {@code AV_TIME_BASE} units of
         * {@code 1_000_000} per second, the FFmpeg convention for {@code AVFormatContext.duration}.
         *
         * @return the duration in whole seconds, with a one-second floor
         */
        int computeDurationSeconds() {
            var durTb = AVFormatContext.duration(inFmtCtx);
            if (durTb > 0) {
                return (int) Math.max(1, durTb / 1_000_000L);
            }
            if (videoPts > 0 && frameRateNum > 0) {
                return (int) Math.max(1, (videoPts * frameRateDen) / frameRateNum);
            }
            return 1;
        }

        /**
         * Releases every libav resource owned by the run, tolerating partially initialized state.
         *
         * <p>Frames, packets, the filter graph, the resampler, the encoders, the decoders, the
         * muxer and demuxer contexts, and the AVIO bridges are each freed only when they were
         * allocated, so this method is safe to call on any exit path including a failure during
         * setup. It is invoked from the {@code finally} block of {@link #execute(MediaProvider)}.
         */
        void cleanup() {
            freeFrame(thumbnailFrame);
            freeFrame(filterFrame);
            freeFrame(audioEncFrame);
            freeFrame(decFrame);
            freePointer(encPacket, Ffmpeg::av_packet_free);
            freePointer(demuxPacket, Ffmpeg::av_packet_free);
            if (filterGraph != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, filterGraph);
                    Ffmpeg.avfilter_graph_free(pp);
                }
            }
            freePointer(swr, Ffmpeg::swr_free);
            freePointer(audioEncCtx, Ffmpeg::avcodec_free_context);
            freePointer(videoEncCtx, Ffmpeg::avcodec_free_context);
            freePointer(audioDecCtx, Ffmpeg::avcodec_free_context);
            freePointer(videoDecCtx, Ffmpeg::avcodec_free_context);
            if (outFmtCtx != MemorySegment.NULL) {
                Ffmpeg.avformat_free_context(outFmtCtx);
            }
            freePointer(inFmtCtx, Ffmpeg::avformat_close_input);
            if (outputBridge != null) {
                outputBridge.close();
            }
            if (inputBridge != null) {
                inputBridge.close();
            }
        }
    }

    /**
     * Returns the index of the first stream of the given media type in the demuxer, or {@code -1}
     * when no such stream is present.
     *
     * <p>Streams are scanned in their declared order and the first whose codec type matches is
     * returned, so this method realizes the pipeline's first-stream selection policy.
     *
     * @param formatCtx the open demuxer to scan
     * @param mediaType the {@code AVMEDIA_TYPE_*} value to match
     * @return the index of the first matching stream, or {@code -1} when none matches
     */
    private static int pickStream(MemorySegment formatCtx, int mediaType) {
        var n = AVFormatContext.nb_streams(formatCtx);
        var streams = AVFormatContext.streams(formatCtx);
        for (var i = 0; i < n; i++) {
            var stream = streams.getAtIndex(ValueLayout.ADDRESS, i)
                    .reinterpret(AVStream.layout().byteSize());
            var params = AVStream.codecpar(stream);
            if (AVCodecParameters.codec_type(params) == mediaType) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the stream pointer at the given index in the demuxer, reinterpreted to the
     * {@code AVStream} layout.
     *
     * @param formatCtx the open demuxer to read from
     * @param index     the zero-based stream index
     * @return the stream pointer at the index
     */
    private static MemorySegment streamPointer(MemorySegment formatCtx, int index) {
        return AVFormatContext.streams(formatCtx)
                .getAtIndex(ValueLayout.ADDRESS, index)
                .reinterpret(AVStream.layout().byteSize());
    }

    /**
     * Frees an {@link AVFrame} pointer, tolerating {@code null} and {@link MemorySegment#NULL}.
     *
     * @param frame the frame pointer to free, ignored when {@code null} or {@link MemorySegment#NULL}
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
     * Calls a libav free function on a pointer slot when the pointer is non-{@code null} and
     * non-{@link MemorySegment#NULL}.
     *
     * <p>The pointer is written into a freshly allocated pointer-to-pointer slot and the supplied
     * {@link FreePointer} is invoked on that slot, matching the libav convention of passing the
     * address of the pointer so the callee can clear it.
     *
     * @param ptr   the pointer to free, ignored when {@code null} or {@link MemorySegment#NULL}
     * @param freer the libav free function to invoke on the pointer slot
     */
    private static void freePointer(MemorySegment ptr, FreePointer freer) {
        if (ptr == null || ptr == MemorySegment.NULL) {
            return;
        }
        try (var local = Arena.ofConfined()) {
            var pp = local.allocate(ValueLayout.ADDRESS);
            pp.set(ValueLayout.ADDRESS, 0L, ptr);
            freer.free(pp);
        }
    }

    /**
     * Rounds a value down to the nearest even integer and floors the result at {@code 2}.
     *
     * @implNote This implementation clears the low bit to round down to even and clamps the result
     * to at least {@code 2} because the {@code yuv420p} and {@code yuvj420p} pixel formats require
     * even, non-zero width and height.
     *
     * @param value the value to round
     * @return the value rounded down to even, never below {@code 2}
     */
    private static int roundEven(int value) {
        var rounded = value - (value & 1);
        return Math.max(rounded, 2);
    }

    /**
     * Abstracts one of libav's pointer-slot free functions so {@link #freePointer(MemorySegment,
     * FreePointer)} can release any libav resource through a single helper.
     */
    @FunctionalInterface
    private interface FreePointer {
        /**
         * Calls the underlying libav free function on the given pointer-to-pointer slot.
         *
         * @implSpec Implementations must invoke a libav free function that accepts the address of a
         * pointer and may clear the pointed-to value.
         *
         * @param pp the pointer-to-pointer slot holding the resource to free
         */
        void free(MemorySegment pp);
    }
}
