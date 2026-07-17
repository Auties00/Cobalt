package com.github.auties00.cobalt.calls.stream.video;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.ffmpeg.AVChannelLayout;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecParameters;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFrame;
import com.github.auties00.cobalt.util.ffmpeg.AVPacket;
import com.github.auties00.cobalt.util.ffmpeg.AVRational;
import com.github.auties00.cobalt.util.ffmpeg.AVStream;
import com.github.auties00.cobalt.util.ffmpeg.Ffmpeg;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegError;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegLoader;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;
import com.github.auties00.cobalt.calls.stream.audio.SilenceAudioOutput;
import com.github.auties00.cobalt.calls.stream.ffmpeg.FfmpegHttpAvio;
import com.github.auties00.cobalt.calls.stream.ffmpeg.FfmpegIoWatchdog;

/**
 * Provides the FFmpeg backed base of the demuxed media sources of a call, opening one input container once
 * and decoding both of its tracks so a single media file can drive the video and the audio of a call: the
 * video track becomes {@link VideoPixelFormat#I420 I420} frames at the input's detected geometry, and the
 * audio track becomes the call's 16 kHz mono frames.
 *
 * <p>This is the shared engine of its nested {@link File} (a local media file) and {@link Uri} (a media
 * stream addressed by URI) sources. A subclass supplies only how the input is opened, by overriding
 * {@link #open(Arena, FfmpegIoWatchdog)}; everything downstream of the opened demuxer is identical and lives
 * here. The container is opened and probed once. Its first video stream is mandatory, so an input with no
 * video stream is rejected at construction; its first audio stream is optional. The advertised geometry is
 * the input's own native video geometry, capped to {@code 1280} on the longer side and rounded to even.
 *
 * <p>A single background demux thread reads the container with {@code av_read_frame} and routes each packet
 * to the video or audio decoder by its stream index. Each decoded picture is scaled with libswscale to the
 * advertised {@link #width()} by {@link #height()} I420 geometry, stamped with its presentation timestamp
 * rescaled from the video stream time base to microseconds, and enqueued on a bounded video queue that
 * {@link #takeVideo()} drains. Each decoded audio frame is resampled with libswresample to 16 kHz mono
 * signed 16 bit PCM, sliced into 10 ms frames with subframe leftover carried across decodes so no samples
 * are lost, stamped with a running microsecond clock, and enqueued on a bounded audio prefetch queue that
 * {@link #takeAudio()} drains. Both queues are bounded, so a slow consumer applies backpressure to the demux
 * thread rather than letting it decode without limit.
 *
 * <p>When the container carries no audio track, the inherited audio face is served by a composed
 * {@link AudioOutput#fromSilence() silence} companion instead of the audio queue, so a video only media file
 * still drives a working audio plane of continuous silence rather than ending the call's audio.
 *
 * <p>The end of the input is signalled by flushing both decoders, draining their buffered frames, and
 * enqueuing an end marker on each queue, so {@link #takeVideo()} and {@link #takeAudio()} return {@code null}
 * once their queue is drained. A demux, decode, resample, or scale failure, including a read the watchdog
 * aborts, is recorded and an end marker is enqueued on both queues, so the pending {@link #takeVideo()} or
 * {@link #takeAudio()} rethrows it. {@link #shutdown()} stops the demux thread and releases the native
 * demuxer, decoders, scaler, and resampler.
 *
 * @implNote This implementation decodes ahead of both consumers on one demux thread rather than inline on the
 * engine's drain threads, because a single container feeds two independently paced consumers: the engine
 * pulls video and audio on separate threads and paces each to wall clock from the frames' presentation
 * timestamps, so a shared reader must read ahead into per plane buffers to keep both supplied across the
 * jitter of the demux, decode, resample, and scale steps. The two queues are sized to comparable wall clock
 * read ahead ({@link #VIDEO_PREFETCH_FRAMES} video frames against {@link #PREFETCH_FRAMES} audio frames) so
 * the single reader rarely blocks putting to one queue while the other starves; because the video read ahead
 * holds whole I420 pictures, each queued {@link VideoFrame} owns its own pixel buffer rather than borrowing a
 * reused one, so its bound trades memory for that decoupling. The queue sizing is the tuning point most worth
 * revisiting for a container with unusually skewed track interleaving.
 */
public abstract sealed class FfmpegVideoOutput implements VideoOutput
        permits FfmpegVideoOutput.File, FfmpegVideoOutput.Uri {
    /**
     * The logger for {@link FfmpegVideoOutput}.
     */
    private static final System.Logger LOGGER = Log.get(FfmpegVideoOutput.class);

    /**
     * Holds the advertised frame rate a decoded media source reports, since the engine paces to wall clock
     * from each frame's presentation timestamp rather than from a fixed rate.
     */
    private static final int DEFAULT_FPS = 30;

    /**
     * Holds the advertised initial encoder bitrate in bits per second.
     *
     * @implNote This implementation mirrors the encoder seed
     * {@link com.github.auties00.cobalt.calls.media.video.codec.VideoCodecParams#DEFAULT_INIT_TARGET_BITRATE},
     * the WhatsApp {@code vid_rc.max_init_bwe} value; it is advertised only.
     */
    private static final int DEFAULT_BITRATE_BPS = 350_000;

    /**
     * Holds the libswscale destination stride alignment in bytes.
     *
     * @implNote This implementation pads each destination plane's stride up to this boundary so libswscale's
     * SIMD write paths, which round each row up to their vector width, never spill past a plane or the
     * destination buffer for a width that is not itself a multiple of the vector width; {@code 32} covers the
     * AVX2 routines the bundled build may select.
     */
    private static final int SWS_DST_ALIGN = 32;

    /**
     * Holds the output audio sample rate, in Hz, that the call layer expects.
     *
     * @implNote This implementation uses 16000, the rate WhatsApp's Opus call configuration runs at, so the
     * resampled output feeds the encoder directly.
     */
    private static final int OUT_SAMPLE_RATE = 16_000;

    /**
     * Holds the number of output samples per emitted audio frame.
     *
     * @implNote This implementation uses 160, which is 10 ms at {@link #OUT_SAMPLE_RATE}, the frame cadence
     * the call layer consumes.
     */
    private static final int OUT_FRAME_SAMPLES = 160;

    /**
     * Holds the duration, in microseconds, advanced per emitted audio frame.
     *
     * @implNote This implementation uses 10000, which is 10 ms for a frame of {@link #OUT_FRAME_SAMPLES}
     * samples at {@link #OUT_SAMPLE_RATE}, in the {@link AudioFrame#ptsMicros()} microsecond clock.
     */
    private static final long OUT_FRAME_DURATION_MICROS = 10_000;

    /**
     * Holds the number of decoded 10 ms audio frames buffered ahead of the consumer.
     *
     * @implNote This implementation reads up to one second of audio ahead so the jitter of the demux, decode,
     * and resample steps, which is worst during the codec's cold start, never starves the call's paced sender.
     */
    private static final int PREFETCH_FRAMES = 100;

    /**
     * Holds the number of decoded video frames buffered ahead of the consumer.
     *
     * @implNote This implementation reads about one second of video ahead at {@link #DEFAULT_FPS}, comparable
     * to the {@link #PREFETCH_FRAMES} one second audio read ahead, so the single demux thread rarely blocks
     * putting a picture to a full video queue while the audio queue drains toward starvation. Each queued
     * frame owns a full I420 picture, so this bound also caps the source's video read ahead memory.
     */
    private static final int VIDEO_PREFETCH_FRAMES = 30;

    /**
     * Marks end of video inside {@link #videoQueue}, distinct from a real decoded frame and compared by
     * identity.
     */
    private static final VideoFrame VIDEO_END =
            new VideoFrame(new byte[6], VideoPixelFormat.I420, 2, 2, Long.MIN_VALUE);

    /**
     * Marks end of audio inside {@link #audioQueue}, distinct from a real decoded frame and compared by
     * identity.
     */
    private static final AudioFrame AUDIO_END = new AudioFrame(new short[0], Long.MIN_VALUE);

    /**
     * Holds the maximum time any single blocking demux read may take before the watchdog aborts it, or
     * {@code null} for an input that does not block and needs no read timeout.
     */
    private final Duration readTimeout;

    /**
     * Holds the timeout watchdog the opener installs and the demux loop arms, shared with the opener so a
     * stalled open or read aborts instead of blocking the demux thread forever.
     */
    private final FfmpegIoWatchdog watchdog;

    /**
     * Holds the arena owning every native allocation this source makes, closed when the source shuts down.
     */
    private final Arena arena;

    /**
     * Holds the libavformat demuxer context pointer, which owns the input handle.
     */
    private final MemorySegment formatCtx;

    /**
     * Holds the libavcodec video decoder context pointer.
     */
    private final MemorySegment videoCodecCtx;

    /**
     * Holds the libavcodec audio decoder context pointer, or {@link MemorySegment#NULL} when the container
     * has no audio stream and the {@link #audioCompanion} serves the audio face instead.
     */
    private final MemorySegment audioCodecCtx;

    /**
     * Holds the libswscale converter pointer that scales the decoded pixel format to
     * {@code AV_PIX_FMT_YUV420P}, built lazily and rebuilt when the frame geometry changes; touched only by
     * the demux thread.
     */
    private MemorySegment swsCtx;

    /**
     * Holds the libswresample context pointer that converts the decoded audio format to 16 kHz mono signed
     * 16 bit PCM, or {@code null} until the first decoded audio frame; touched only by the demux thread.
     *
     * <p>Set lazily on the first decoded {@link AVFrame} rather than in the constructor: many codecs (MP3
     * included) leave {@code codecCtx->ch_layout} and {@code codecCtx->sample_fmt} unset until the first frame
     * is produced, and seeding the resampler from those zeroed values yields a context that fails
     * {@code swr_convert} with {@code EINVAL} on every subsequent call.
     */
    private MemorySegment swrCtx;

    /**
     * Holds the reusable {@code AVPacket} pointer driven by the demux loop.
     */
    private final MemorySegment packet;

    /**
     * Holds the reusable video decoder output frame pointer that the video decoder writes into.
     */
    private final MemorySegment videoFrame;

    /**
     * Holds the reusable audio decoder output frame pointer that the audio decoder writes into, or
     * {@link MemorySegment#NULL} when the container has no audio stream.
     */
    private final MemorySegment audioFrame;

    /**
     * Holds the index of the video stream chosen from the container.
     */
    private final int videoStreamIndex;

    /**
     * Holds the index of the audio stream chosen from the container, or {@code -1} when it has no audio
     * stream.
     */
    private final int audioStreamIndex;

    /**
     * Holds the numerator of the video stream's time base, used to rescale presentation timestamps to
     * microseconds.
     */
    private final int timeBaseNum;

    /**
     * Holds the denominator of the video stream's time base, used to rescale presentation timestamps to
     * microseconds.
     */
    private final int timeBaseDen;

    /**
     * Holds the decoded source width the current {@link #swsCtx} converter was built for.
     */
    private int swsW;

    /**
     * Holds the decoded source height the current {@link #swsCtx} converter was built for.
     */
    private int swsH;

    /**
     * Holds the decoded source pixel format the current {@link #swsCtx} converter was built for.
     *
     * <p>When a later frame uses a different format, the converter is torn down and rebuilt.
     */
    private int swsFmt;

    /**
     * Holds the reusable libswscale destination plane buffer, or {@code null} until the first conversion.
     *
     * <p>Its geometry is the advertised {@link #width()} by {@link #height()} with each plane stride padded to
     * {@link #SWS_DST_ALIGN}, fixed for the source's life, so it is allocated once from {@link #arena} on the
     * first {@link #convertCurrentVideoFrame()} and reused by every later conversion rather than opening a
     * confined arena and allocating a full destination buffer for each frame. The tightly packed pixels each
     * converted frame carries are copied out of it into a fresh per frame array, so reusing this scratch does
     * not violate the borrow contract.
     */
    private MemorySegment scaleBuf;

    /**
     * Holds the reusable {@code uint8_t *dst[8]} destination plane pointer array handed to {@code sws_scale},
     * pointing into {@link #scaleBuf}; built once alongside it.
     */
    private MemorySegment scaleDstData;

    /**
     * Holds the reusable {@code int dstStride[8]} destination stride array handed to {@code sws_scale}; built
     * once alongside {@link #scaleBuf}.
     */
    private MemorySegment scaleDstStride;

    /**
     * Holds the padded Y plane stride, in bytes, of {@link #scaleBuf}.
     */
    private int scaleYStride;

    /**
     * Holds the padded chroma plane stride, in bytes, of {@link #scaleBuf}.
     */
    private int scaleCStride;

    /**
     * Holds the byte offset of the U plane within {@link #scaleBuf}, equal to its padded Y plane size.
     */
    private long scaleYPlaneBytes;

    /**
     * Holds the byte size of each padded chroma plane within {@link #scaleBuf}.
     */
    private long scaleCPlaneBytes;

    /**
     * Holds the reusable libswresample destination sample buffer, or {@code null} until the first resample;
     * touched only by the demux thread.
     *
     * <p>The per frame output capacity is effectively constant for a stream, so this buffer and its
     * {@link #resampleOutPtrs} pointer are allocated once from {@link #arena} on the first resample and reused
     * by every later resample rather than opening a per frame confined arena, growing only if a frame ever
     * needs more than the current {@link #resampleCapacity}.
     */
    private MemorySegment resampleBuf;

    /**
     * Holds the reusable {@code int16_t *out[1]} destination pointer array handed to {@code swr_convert},
     * pointing into {@link #resampleBuf}; built once and repointed whenever {@link #resampleBuf} grows.
     */
    private MemorySegment resampleOutPtrs;

    /**
     * Holds the sample capacity of {@link #resampleBuf}, used to decide whether a resample needs a larger
     * buffer.
     */
    private int resampleCapacity;

    /**
     * Holds the leftover samples from the last audio decode that did not fill a 10 ms frame.
     *
     * <p>Carried into the next decode and prepended before the new samples are sliced, so subframe audio is
     * never dropped mid stream. Touched only by the demux thread.
     */
    private short[] leftover = new short[0];

    /**
     * Holds the presentation timestamp, in microseconds, of the next emitted audio frame, advanced by
     * {@link #OUT_FRAME_DURATION_MICROS} (10 ms) per frame. Touched only by the demux thread.
     */
    private long audioPtsMicros;

    /**
     * Holds the advertised frame width in pixels, the input's detected native width capped to {@code 1280} on
     * the longer side and rounded to even.
     */
    private final int width;

    /**
     * Holds the advertised frame height in pixels, the input's detected native height capped and rounded to
     * even.
     */
    private final int height;

    /**
     * Holds the silence source serving the inherited audio face when the container has no audio stream, or
     * {@code null} when the container carries audio that the {@link #audioQueue} serves instead.
     */
    private final SilenceAudioOutput audioCompanion;

    /**
     * Holds the failure that ended the background demux, surfaced from {@link #takeVideo()} or
     * {@link #takeAudio()}, or {@code null} while the demux is healthy.
     *
     * <p>Set on the demux thread when a demux, decode, resample, or scale step fails (including a watchdog
     * timeout) and read on the consumer threads, so it is {@code volatile}; it lets the take methods
     * distinguish a failed source, which throws, from a cleanly drained one, which returns {@code null}.
     */
    private volatile RuntimeException decodeFailure;

    /**
     * Holds decoded video frames produced by the background {@link #demuxThread} and consumed by
     * {@link #takeVideo()}, bounding the read ahead to {@link #VIDEO_PREFETCH_FRAMES} frames.
     */
    private final BlockingQueue<VideoFrame> videoQueue =
            new ArrayBlockingQueue<>(VIDEO_PREFETCH_FRAMES + 1);

    /**
     * Holds decoded 10 ms audio frames produced by the background {@link #demuxThread} and consumed by
     * {@link #takeAudio()}, bounding the read ahead to {@link #PREFETCH_FRAMES} frames; unused when the
     * container has no audio stream and the {@link #audioCompanion} serves the audio face.
     */
    private final BlockingQueue<AudioFrame> audioQueue =
            new ArrayBlockingQueue<>(PREFETCH_FRAMES + 1);

    /**
     * Holds the background thread that demuxes the input and decodes both tracks ahead of the consumers,
     * started once at the end of the constructor.
     */
    private final Thread demuxThread;

    /**
     * Guards {@link #shutdown()} so the teardown runs at most once, and read by the demux loop to end once the
     * source is closed.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Opens and probes the input demuxer, returning its context.
     *
     * <p>Called once from the constructor, before any field derived from the input is set.
     *
     * @implSpec An implementation opens the demuxer with {@code avformat_open_input}, probes it with
     * {@code avformat_find_stream_info}, and returns the opened {@code AVFormatContext} pointer reinterpreted
     * to the size of {@link AVFormatContext#layout()}. A network implementation installs the watchdog on the
     * context before opening and arms it around each blocking call; a file implementation ignores the
     * watchdog.
     * @param arena    the source's lifetime arena that owns the native allocations the open makes
     * @param watchdog the timeout watchdog to install and arm around blocking calls, or ignore for an input
     *                 that does not block
     * @return the opened and probed {@code AVFormatContext} pointer, reinterpreted to its layout size
     * @throws IllegalStateException if the input cannot be opened or probed
     */
    protected abstract MemorySegment open(Arena arena, FfmpegIoWatchdog watchdog);

    /**
     * Opens the input, detects its video geometry, and prepares the demuxer, both decoders, and the scaler
     * chain.
     *
     * <p>Ensures the FFmpeg libraries are loaded, allocates the lifetime arena and the timeout watchdog, opens
     * and probes the input through {@link #open(Arena, FfmpegIoWatchdog)}, picks its first video stream and
     * opens a decoder for it, picks its first audio stream and opens a decoder for it when one exists (falling
     * back to a silence companion when it does not), advertises the video stream's native geometry capped to
     * {@code 1280} on the longer side and rounded to even, and starts the background demux thread. If any step
     * fails the native allocations are freed and the arena is closed before the exception propagates, so a
     * failed construction leaks no native resource.
     *
     * @param readTimeout the maximum time a single blocking demux read may take, or {@code null} for an input
     *                    that does not block and needs no read timeout
     * @throws IllegalStateException if the input cannot be opened, has no video stream, or a decoder cannot be
     *                               initialized
     */
    public FfmpegVideoOutput(Duration readTimeout) {
        FFmpegLoader.ensureLoaded();
        this.readTimeout = readTimeout;
        this.arena = Arena.ofShared();
        this.watchdog = new FfmpegIoWatchdog(arena);
        var localVideoCodecCtx = MemorySegment.NULL;
        var localAudioCodecCtx = MemorySegment.NULL;
        var localPacket = MemorySegment.NULL;
        var localVideoFrame = MemorySegment.NULL;
        var localAudioFrame = MemorySegment.NULL;
        SilenceAudioOutput localCompanion = null;
        try {
            var openedCtx = open(arena, watchdog);
            var vIndex = pickVideoStream(openedCtx);
            if (vIndex < 0) {
                throw new IllegalStateException("no video stream in input");
            }
            var vStream = streamPointer(openedCtx, vIndex);
            var tb = AVStream.time_base(vStream);
            var tbNum = AVRational.num(tb);
            var tbDen = AVRational.den(tb);
            var vParams = AVStream.codecpar(vStream);
            var nativeWidth = AVCodecParameters.width(vParams);
            var nativeHeight = AVCodecParameters.height(vParams);
            var vCodecId = AVCodecParameters.codec_id(vParams);
            var vCodec = FFmpegError.requireNonNull("avcodec_find_decoder(" + vCodecId + ")",
                    Ffmpeg.avcodec_find_decoder(vCodecId));
            localVideoCodecCtx = FFmpegError.requireNonNull("avcodec_alloc_context3",
                    Ffmpeg.avcodec_alloc_context3(vCodec));
            FFmpegError.check("avcodec_parameters_to_context",
                    Ffmpeg.avcodec_parameters_to_context(localVideoCodecCtx, vParams));
            FFmpegError.check("avcodec_open2",
                    Ffmpeg.avcodec_open2(localVideoCodecCtx, vCodec, MemorySegment.NULL));

            var aIndex = pickAudioStream(openedCtx);
            if (aIndex >= 0) {
                var aStream = streamPointer(openedCtx, aIndex);
                var aParams = AVStream.codecpar(aStream);
                var aCodecId = AVCodecParameters.codec_id(aParams);
                var aCodec = FFmpegError.requireNonNull("avcodec_find_decoder(" + aCodecId + ")",
                        Ffmpeg.avcodec_find_decoder(aCodecId));
                localAudioCodecCtx = FFmpegError.requireNonNull("avcodec_alloc_context3",
                        Ffmpeg.avcodec_alloc_context3(aCodec));
                FFmpegError.check("avcodec_parameters_to_context",
                        Ffmpeg.avcodec_parameters_to_context(localAudioCodecCtx, aParams));
                FFmpegError.check("avcodec_open2",
                        Ffmpeg.avcodec_open2(localAudioCodecCtx, aCodec, MemorySegment.NULL));
                localAudioFrame = FFmpegError.requireNonNull("av_frame_alloc", Ffmpeg.av_frame_alloc());
            } else {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "input has no audio stream, falling back to silence companion");
                }
                localCompanion = new SilenceAudioOutput();
            }

            localPacket = FFmpegError.requireNonNull("av_packet_alloc", Ffmpeg.av_packet_alloc());
            localVideoFrame = FFmpegError.requireNonNull("av_frame_alloc", Ffmpeg.av_frame_alloc());
            var capped = capGeometry(nativeWidth, nativeHeight);
            this.formatCtx = openedCtx;
            this.videoCodecCtx = localVideoCodecCtx;
            this.audioCodecCtx = localAudioCodecCtx;
            this.packet = localPacket;
            this.videoFrame = localVideoFrame;
            this.audioFrame = localAudioFrame;
            this.videoStreamIndex = vIndex;
            this.audioStreamIndex = aIndex;
            this.audioCompanion = localCompanion;
            this.timeBaseNum = tbNum;
            this.timeBaseDen = tbDen;
            this.width = capped[0];
            this.height = capped[1];
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "ffmpeg media source opened, {0}x{1}, audio={2}",
                        capped[0], capped[1], aIndex >= 0);
            }
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to open ffmpeg media source", e);
            freePointer(localVideoFrame, Ffmpeg::av_frame_free);
            freePointer(localAudioFrame, Ffmpeg::av_frame_free);
            freePointer(localPacket, Ffmpeg::av_packet_free);
            freePointer(localVideoCodecCtx, Ffmpeg::avcodec_free_context);
            freePointer(localAudioCodecCtx, Ffmpeg::avcodec_free_context);
            if (localCompanion != null) {
                localCompanion.shutdown();
            }
            arena.close();
            throw e;
        }
        this.demuxThread = Thread.ofPlatform()
                .name("ffmpeg-media-demuxer")
                .daemon(true)
                .start(this::demuxLoop);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the next decoded picture from the read ahead {@link #videoQueue} that the background
     * {@link #demuxThread} fills, blocking only if the demuxer has not yet produced it. Returns {@code null}
     * once the input is fully drained and the queue is exhausted, or once {@link #shutdown()} has ended the
     * source. Throws if the background demux failed, including when a blocking read timed out.
     *
     * @return {@inheritDoc}
     * @throws IllegalStateException if the background demux, decode, or scale failed, or a blocking read
     *                               exceeded the configured timeout
     * @implNote This implementation reads from a queue the demux thread fills ahead of the consumer rather
     * than decoding inline, since one container feeds two independently paced consumers; each queued frame
     * owns its own pixel buffer, so a frame is safe to hold until the engine copies it into the codec.
     */
    @Override
    public VideoFrame takeVideo() {
        if (closed.get()) {
            return null;
        }
        try {
            var next = videoQueue.take();
            if (next == VIDEO_END) {
                var failure = decodeFailure;
                if (failure != null) {
                    throw failure;
                }
                return null;
            }
            return next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the next decoded 10 ms frame from the read ahead {@link #audioQueue} that the background
     * {@link #demuxThread} fills, blocking only if the demuxer has not yet produced it. When the container has
     * no audio stream, the {@link #audioCompanion} silence source serves this face instead. Returns
     * {@code null} once the input is fully drained and the queue is exhausted, or once {@link #shutdown()} has
     * ended the source. Throws if the background demux failed, including when a blocking read timed out.
     *
     * @return {@inheritDoc}
     * @throws IllegalStateException if the background demux, decode, or resample failed, or a blocking read
     *                               exceeded the configured timeout
     */
    @Override
    public AudioFrame takeAudio() {
        if (audioCompanion != null) {
            return audioCompanion.takeAudio();
        }
        if (closed.get()) {
            return null;
        }
        try {
            var next = audioQueue.take();
            if (next == AUDIO_END) {
                var failure = decodeFailure;
                if (failure != null) {
                    throw failure;
                }
                return null;
            }
            return next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A demuxed media source produces its frames on the demux thread and ignores application writes, so
     * this does nothing.
     *
     * @param frame the frame that would be written; ignored
     */
    @Override
    public void writeVideo(VideoFrame frame) {
    }

    /**
     * {@inheritDoc}
     *
     * <p>A demuxed media source produces its frames on the demux thread and ignores application writes, so
     * this does nothing. When the container has no audio stream the write is forwarded to the
     * {@link #audioCompanion}, whose write is likewise a no op.
     *
     * @param frame the frame that would be written; ignored
     */
    @Override
    public void writeAudio(AudioFrame frame) {
        if (audioCompanion != null) {
            audioCompanion.writeAudio(frame);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @implNote This implementation returns {@code false}: decoded media, whether the container's audio track
     * or the silence companion's fill, is already clean line level audio that the engine encodes without the
     * acoustic conditioning a live microphone capture needs.
     */
    @Override
    public boolean isLiveCapture() {
        if (audioCompanion != null) {
            return audioCompanion.isLiveCapture();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the source ended, cancels the timeout watchdog so a parked demux read returns at once, shuts
     * down the silence companion when one is serving the audio face, stops and joins the background demux
     * thread so no FFmpeg call is in flight, then frees the libswscale converter, the libswresample context,
     * both decoder output frames, the demuxer packet, both decoder contexts, and the demuxer context, and
     * closes the owning arena. Guards each pointer against {@code null} and a zero address, so the call is
     * idempotent.
     *
     * @implNote This implementation joins the demux thread before freeing any native context the thread
     * touches, so no scaler, resampler, decoder, or demuxer context is released while a decode is still
     * running; the join establishes the happens before edge that makes the thread's writes to the lazily
     * built {@link #swsCtx} and {@link #swrCtx} visible here.
     */
    @Override
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "shutting down ffmpeg media source");
        watchdog.cancel();
        if (audioCompanion != null) {
            audioCompanion.shutdown();
        }
        var demux = demuxThread;
        if (demux != null) {
            demux.interrupt();
            try {
                demux.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try (arena) {
            if (swsCtx != null && swsCtx.address() != 0L) {
                Ffmpeg.sws_freeContext(swsCtx);
            }
            freePointer(swrCtx, Ffmpeg::swr_free);
            freePointer(videoFrame, Ffmpeg::av_frame_free);
            freePointer(audioFrame, Ffmpeg::av_frame_free);
            freePointer(packet, Ffmpeg::av_packet_free);
            freePointer(videoCodecCtx, Ffmpeg::avcodec_free_context);
            freePointer(audioCodecCtx, Ffmpeg::avcodec_free_context);
            freePointer(formatCtx, Ffmpeg::avformat_close_input);
            watchdog.closeIo();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int width() {
        return width;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int height() {
        return height;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @implNote This implementation advertises the fixed default frame rate; the engine paces the outbound
     * video to wall clock from each frame's presentation timestamp rather than from this rate.
     */
    @Override
    public int fps() {
        return DEFAULT_FPS;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @implNote This implementation advertises the WhatsApp initial bitrate; the engine seeds the encoder from
     * its own rate controller rather than from this advertised field.
     */
    @Override
    public int bitrateBps() {
        return DEFAULT_BITRATE_BPS;
    }

    /**
     * Demuxes the input and decodes both tracks ahead of the consumers, feeding {@link #videoQueue} and
     * {@link #audioQueue} until the input drains or the source shuts down.
     *
     * <p>Runs on the background {@link #demuxThread}: all FFmpeg demux, decode, resample, and scale calls
     * happen here, on a single thread, while {@link #takeVideo()} and {@link #takeAudio()} only read the
     * buffered results. Reads one packet at a time, arming the watchdog around each blocking read when a read
     * timeout is configured, routes it to the matching decoder, and blocks on a full queue to bound the read
     * ahead. On end of input it flushes both decoders, drains their buffered frames, and enqueues an end
     * marker on each queue. On a demux, decode, resample, or scale failure it records the failure in
     * {@link #decodeFailure} and enqueues an end marker on both queues so a waiting take unblocks and rethrows
     * it. Exits when the input drains, when {@link #shutdown()} sets the closed flag, or when interrupted.
     */
    private void demuxLoop() {
        try {
            while (!closed.get()) {
                if (readTimeout != null) {
                    watchdog.arm(readTimeout);
                }
                var read = Ffmpeg.av_read_frame(formatCtx, packet);
                if (readTimeout != null) {
                    watchdog.disarm();
                }
                if (read < 0) {
                    if (readTimeout != null && watchdog.fired()) {
                        if (Log.ERROR) {
                            LOGGER.log(Level.ERROR, "input read timed out after {0}ms",
                                    readTimeout.toMillis());
                        }
                        throw new IllegalStateException("input read timed out after " + readTimeout);
                    }
                    var failure = watchdog.readFailure();
                    if (failure != null) {
                        if (Log.ERROR) LOGGER.log(Level.ERROR, "input read failed: {0}", failure);
                        throw new IllegalStateException("input read failed: " + failure);
                    }
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "input drained cleanly, ending demux");
                    drainToEnd();
                    return;
                }
                try {
                    routePacket();
                } finally {
                    Ffmpeg.av_packet_unref(packet);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "demux thread failed", e);
            decodeFailure = e;
            enqueueFailureEnd();
        }
    }

    /**
     * Routes the current {@link #packet} to the video or audio decoder by its stream index, draining the
     * frames it produces.
     *
     * <p>A packet of the chosen video stream is sent to the video decoder and its pictures drained to the
     * video queue; a packet of the chosen audio stream, when the container has one, is sent to the audio
     * decoder and its frames drained to the audio queue; a packet of any other stream is ignored.
     *
     * @throws InterruptedException  if interrupted while blocking to enqueue a decoded frame
     * @throws IllegalStateException if a decoder reports a hard failure
     */
    private void routePacket() throws InterruptedException {
        var idx = AVPacket.stream_index(packet);
        if (idx == videoStreamIndex) {
            var sent = Ffmpeg.avcodec_send_packet(videoCodecCtx, packet);
            if (sent < 0 && !FFmpegError.isAgain(sent)) {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "avcodec_send_packet(video) failed: {0}",
                            FFmpegError.describe(sent));
                }
                throw new IllegalStateException("avcodec_send_packet(video) failed: "
                        + FFmpegError.describe(sent));
            }
            drainVideoDecoder();
        } else if (audioStreamIndex >= 0 && idx == audioStreamIndex) {
            var sent = Ffmpeg.avcodec_send_packet(audioCodecCtx, packet);
            if (sent < 0 && !FFmpegError.isAgain(sent)) {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "avcodec_send_packet(audio) failed: {0}",
                            FFmpegError.describe(sent));
                }
                throw new IllegalStateException("avcodec_send_packet(audio) failed: "
                        + FFmpegError.describe(sent));
            }
            drainAudioDecoder(false);
        }
    }

    /**
     * Flushes both decoders at end of input, drains their buffered frames, and enqueues an end marker on each
     * active queue.
     *
     * <p>Sends the null flush packet to the video decoder, drains its remaining pictures, and enqueues the
     * video end marker; when the container has an audio stream it does the same for the audio decoder, letting
     * the resampler's tail flush too. When the container has no audio stream the audio face is served by the
     * never ending {@link #audioCompanion}, so no audio end marker is enqueued.
     *
     * @throws InterruptedException  if interrupted while blocking to enqueue a decoded frame or an end marker
     * @throws IllegalStateException if a decoder or the resampler reports a hard failure
     */
    private void drainToEnd() throws InterruptedException {
        Ffmpeg.avcodec_send_packet(videoCodecCtx, MemorySegment.NULL);
        drainVideoDecoder();
        videoQueue.put(VIDEO_END);
        if (audioStreamIndex >= 0) {
            Ffmpeg.avcodec_send_packet(audioCodecCtx, MemorySegment.NULL);
            drainAudioDecoder(true);
            audioQueue.put(AUDIO_END);
        }
    }

    /**
     * Enqueues an end marker on each active queue after a failure, so a waiting take unblocks and rethrows the
     * recorded {@link #decodeFailure}.
     *
     * <p>Called from the demux loop's failure path, where the failure has already been recorded. Restores the
     * thread's interrupt status and returns if interrupted while blocking to enqueue.
     */
    private void enqueueFailureEnd() {
        try {
            videoQueue.put(VIDEO_END);
            if (audioStreamIndex >= 0) {
                audioQueue.put(AUDIO_END);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Pulls every available picture out of the video decoder, converts each to {@link VideoPixelFormat#I420
     * I420} at the advertised geometry, and enqueues it on the video queue.
     *
     * <p>Returns once the decoder needs more input or is fully drained. Each picture is converted into its own
     * pixel buffer and the decoder frame is unreferenced before the potentially blocking enqueue, so the
     * decoder's native picture is released promptly.
     *
     * @throws InterruptedException  if interrupted while blocking to enqueue a converted frame
     * @throws IllegalStateException if the decoder reports a hard failure, or the frame has unsupported
     *                               dimensions, or the scale fails
     */
    private void drainVideoDecoder() throws InterruptedException {
        while (true) {
            var got = Ffmpeg.avcodec_receive_frame(videoCodecCtx, videoFrame);
            if (FFmpegError.isAgain(got) || FFmpegError.isEof(got)) {
                return;
            }
            if (got < 0) {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "avcodec_receive_frame(video) failed: {0}",
                            FFmpegError.describe(got));
                }
                throw new IllegalStateException("avcodec_receive_frame(video) failed: "
                        + FFmpegError.describe(got));
            }
            VideoFrame converted;
            try {
                converted = convertCurrentVideoFrame();
            } finally {
                Ffmpeg.av_frame_unref(videoFrame);
            }
            videoQueue.put(converted);
        }
    }

    /**
     * Pulls every available frame out of the audio decoder, resamples each to 16 kHz mono signed 16 bit PCM,
     * and enqueues it as 10 ms frames on the audio queue.
     *
     * <p>When in flush mode and the decoder signals end of output, the resampler's internal buffer is flushed
     * too so no tail samples are lost.
     *
     * @param flush whether the decoder is in flush mode (after end of input)
     * @throws InterruptedException  if interrupted while blocking to enqueue a resampled frame
     * @throws IllegalStateException if the decoder or the resampler reports a hard failure
     */
    private void drainAudioDecoder(boolean flush) throws InterruptedException {
        while (true) {
            var got = Ffmpeg.avcodec_receive_frame(audioCodecCtx, audioFrame);
            if (FFmpegError.isAgain(got) || (flush && FFmpegError.isEof(got))) {
                if (flush && FFmpegError.isEof(got)) {
                    flushResampler();
                }
                return;
            }
            if (got < 0) {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "avcodec_receive_frame(audio) failed: {0}",
                            FFmpegError.describe(got));
                }
                throw new IllegalStateException("avcodec_receive_frame(audio) failed: "
                        + FFmpegError.describe(got));
            }
            try {
                resampleAndQueue();
            } finally {
                Ffmpeg.av_frame_unref(audioFrame);
            }
        }
    }

    /**
     * Converts the current {@link #videoFrame} to {@link VideoPixelFormat#I420 I420} at the advertised
     * geometry and wraps it in a {@link VideoFrame} with its presentation timestamp rescaled to microseconds.
     *
     * <p>Rejects decoded frames whose dimensions are below {@code 2} or odd, since I420's half resolution
     * chroma planes require even dimensions. Rebuilds the converter when the decoded geometry changes, scales
     * the three planes through the reusable {@link #scaleBuf} scratch (see
     * {@link #ensureScaleScratch(int, int)}) into a fresh tightly packed I420 buffer sized for the advertised
     * {@link #width()} by {@link #height()} geometry the engine encodes at, and rescales the frame's best
     * effort timestamp from the video stream time base to a microsecond value clamped to at least zero.
     *
     * @return the converted frame at the advertised geometry
     * @throws IllegalStateException if the decoded frame has unsupported dimensions or the scale fails
     * @implNote This implementation allocates the returned frame's pixel buffer afresh on every call rather
     * than lending one reused buffer, because a converted frame is queued and outlives the next conversion, so
     * it must own its pixels. It scales the decoded picture to the advertised geometry here, in the source,
     * because the source already owns an {@code sws} context; WhatsApp likewise scales the decoded media file
     * picture to the negotiated encode resolution before encoding, so the advertised and encoded resolutions
     * stay identical and the engine's encoder, built at the advertised geometry, never rejects a native
     * resolution frame. The libswscale destination is a scratch of the source's lifetime reused across frames,
     * and its plane strides are padded to {@link #SWS_DST_ALIGN} so the SIMD write paths cannot spill past a
     * plane; the scaled planes are copied out of it into the fresh buffer, stripping the padding.
     */
    private VideoFrame convertCurrentVideoFrame() {
        var w = AVFrame.width(videoFrame);
        var h = AVFrame.height(videoFrame);
        var srcFmt = AVFrame.format(videoFrame);
        if (w < 2 || h < 2 || (w & 1) != 0 || (h & 1) != 0) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "decoded frame has unsupported dimensions {0}x{1}", w, h);
            }
            throw new IllegalStateException(
                    "decoded frame has unsupported dimensions " + w + "x" + h);
        }
        var dstW = width;
        var dstH = height;
        rebuildSwsIfNeeded(w, h, srcFmt);
        ensureScaleScratch(dstW, dstH);

        var ySize = dstW * dstH;
        var uvSize = (dstW / 2) * (dstH / 2);
        var pixels = new byte[ySize + 2 * uvSize];

        var produced = Ffmpeg.sws_scale(swsCtx,
                AVFrame.data(videoFrame), AVFrame.linesize(videoFrame),
                0, h, scaleDstData, scaleDstStride);
        if (produced < 0) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sws_scale failed, code={0}", produced);
            throw new IllegalStateException("sws_scale failed: " + produced);
        }
        copyPlaneRows(scaleBuf, 0L, scaleYStride, dstW, dstH, pixels, 0);
        copyPlaneRows(scaleBuf, scaleYPlaneBytes, scaleCStride, dstW / 2, dstH / 2, pixels, ySize);
        copyPlaneRows(scaleBuf, scaleYPlaneBytes + scaleCPlaneBytes, scaleCStride,
                dstW / 2, dstH / 2, pixels, ySize + uvSize);

        var ptsRaw = AVFrame.best_effort_timestamp(videoFrame);
        var ptsMicros = (timeBaseDen == 0)
                ? 0L
                : Math.max(0L, ptsRaw * 1_000_000L * timeBaseNum / timeBaseDen);
        return new VideoFrame(pixels, VideoPixelFormat.I420, dstW, dstH, ptsMicros);
    }

    /**
     * Allocates the reusable libswscale destination scratch once, on the first conversion.
     *
     * <p>The advertised destination geometry is fixed for the source's life, so {@link #scaleBuf} and its
     * companion {@link #scaleDstData} plane pointer and {@link #scaleDstStride} stride arrays are built once
     * from {@link #arena} and reused by every later conversion, rather than opening a confined arena and
     * allocating a full destination buffer for each frame. Each destination plane's stride is padded to
     * {@link #SWS_DST_ALIGN} so libswscale's SIMD write paths cannot spill past a plane. A later call does
     * nothing once the scratch exists.
     *
     * @param dstW the advertised destination width in pixels
     * @param dstH the advertised destination height in pixels
     */
    private void ensureScaleScratch(int dstW, int dstH) {
        if (scaleBuf != null) {
            return;
        }
        var yStride = alignUp(dstW, SWS_DST_ALIGN);
        var cStride = alignUp(dstW / 2, SWS_DST_ALIGN);
        var yPlane = (long) yStride * dstH;
        var cPlane = (long) cStride * (dstH / 2);
        var buf = arena.allocate(yPlane + 2L * cPlane + SWS_DST_ALIGN);
        var dstData = arena.allocate(8L * ValueLayout.ADDRESS.byteSize());
        var dstStride = arena.allocate(8L * Integer.BYTES);
        dstData.setAtIndex(ValueLayout.ADDRESS, 0L, buf);
        dstData.setAtIndex(ValueLayout.ADDRESS, 1L, buf.asSlice(yPlane));
        dstData.setAtIndex(ValueLayout.ADDRESS, 2L, buf.asSlice(yPlane + cPlane));
        dstStride.setAtIndex(ValueLayout.JAVA_INT, 0L, yStride);
        dstStride.setAtIndex(ValueLayout.JAVA_INT, 1L, cStride);
        dstStride.setAtIndex(ValueLayout.JAVA_INT, 2L, cStride);
        this.scaleBuf = buf;
        this.scaleDstData = dstData;
        this.scaleDstStride = dstStride;
        this.scaleYStride = yStride;
        this.scaleCStride = cStride;
        this.scaleYPlaneBytes = yPlane;
        this.scaleCPlaneBytes = cPlane;
    }

    /**
     * Copies one plane from a strided native buffer into a tightly packed region of a byte array.
     *
     * <p>A plane whose row stride already equals its width is contiguous and copied in a single bulk transfer;
     * otherwise each row is copied separately to strip the stride padding, since {@link VideoFrame} carries
     * planes with no padding between rows.
     *
     * @param src       the native buffer holding the plane
     * @param srcOffset the byte offset of the plane's first row within {@code src}
     * @param srcStride the plane's row stride in bytes
     * @param planeW    the plane's width in bytes per row
     * @param planeH    the plane's height in rows
     * @param dst       the destination byte array
     * @param dstOffset the byte offset of the plane within {@code dst}
     */
    private static void copyPlaneRows(MemorySegment src, long srcOffset, int srcStride,
                                      int planeW, int planeH, byte[] dst, int dstOffset) {
        if (srcStride == planeW) {
            MemorySegment.copy(src, ValueLayout.JAVA_BYTE, srcOffset, dst, dstOffset, planeW * planeH);
            return;
        }
        for (var row = 0; row < planeH; row++) {
            MemorySegment.copy(src, ValueLayout.JAVA_BYTE, srcOffset + (long) row * srcStride,
                    dst, dstOffset + row * planeW, planeW);
        }
    }

    /**
     * Rounds a value up to the next multiple of the given power of two alignment.
     *
     * @param value the value to round
     * @param align the alignment, a power of two
     * @return the smallest multiple of {@code align} not below {@code value}
     */
    private static int alignUp(int value, int align) {
        return (value + align - 1) & ~(align - 1);
    }

    /**
     * Rebuilds the libswscale converter when the decoded frame dimensions or pixel format change between
     * frames.
     *
     * <p>Returns immediately when the current converter already matches the requested source geometry;
     * otherwise frees the old converter and builds one scaling from the decoded {@code (w, h, fmt)} triple to
     * {@code AV_PIX_FMT_YUV420P} at the advertised {@link #width()} by {@link #height()} geometry.
     *
     * @param w   the decoded source width
     * @param h   the decoded source height
     * @param fmt the decoded source pixel format
     * @throws IllegalStateException if the converter cannot be built
     */
    private void rebuildSwsIfNeeded(int w, int h, int fmt) {
        if (swsCtx != null && swsCtx.address() != 0L
                && w == swsW && h == swsH && fmt == swsFmt) {
            return;
        }
        if (swsCtx != null && swsCtx.address() != 0L) {
            Ffmpeg.sws_freeContext(swsCtx);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "(re)building sws scaler {0}x{1} -> {2}x{3}", w, h, width, height);
        }
        this.swsCtx = FFmpegError.requireNonNull(
                "sws_getContext",
                Ffmpeg.sws_getContext(w, h, fmt, width, height, Ffmpeg.AV_PIX_FMT_YUV420P(),
                        Ffmpeg.SWS_BILINEAR(),
                        MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL));
        this.swsW = w;
        this.swsH = h;
        this.swsFmt = fmt;
    }

    /**
     * Resamples the current {@link #audioFrame} to 16 kHz mono signed 16 bit PCM and enqueues the output as
     * 10 ms frames.
     *
     * <p>Builds the resampler lazily from the first decoded frame, sizes the output buffer from the input
     * sample count rescaled to the output rate plus a small margin, runs one libswresample conversion, and
     * slices the result into ready frames. Frames with no samples are skipped.
     *
     * @throws InterruptedException  if interrupted while blocking to enqueue a resampled frame
     * @throws IllegalStateException if the resample fails
     */
    private void resampleAndQueue() throws InterruptedException {
        var inSamples = AVFrame.nb_samples(audioFrame);
        if (inSamples <= 0) {
            return;
        }
        if (swrCtx == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building libswresample context from first decoded frame");
            swrCtx = buildResampler(arena, audioFrame);
        }
        var outCapacity = (int) (((long) inSamples * OUT_SAMPLE_RATE
                                  / Math.max(1, AVFrame.sample_rate(audioFrame))) + 256);
        ensureResampleScratch(outCapacity);
        var inPtrs = AVFrame.extended_data(audioFrame);
        var produced = Ffmpeg.swr_convert(swrCtx, resampleOutPtrs, outCapacity, inPtrs, inSamples);
        if (produced < 0) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "swr_convert failed: {0}", FFmpegError.describe(produced));
            throw new IllegalStateException("swr_convert failed: " + FFmpegError.describe(produced));
        }
        queueResampled(resampleBuf, produced);
    }

    /**
     * Allocates the reusable libswresample destination scratch, growing it only when a frame needs more.
     *
     * <p>The output capacity is effectively constant across a stream, so {@link #resampleBuf} and
     * {@link #resampleOutPtrs} are built once from {@link #arena} and reused by every resample rather than a
     * per frame confined arena. A frame whose capacity exceeds the current {@link #resampleCapacity} grows the
     * buffer and repoints {@link #resampleOutPtrs}; the superseded allocation lives until the source's arena
     * closes.
     *
     * @param capacitySamples the number of output samples the current resample must be able to hold
     */
    private void ensureResampleScratch(int capacitySamples) {
        if (resampleOutPtrs == null) {
            resampleOutPtrs = arena.allocate(ValueLayout.ADDRESS);
        }
        if (resampleBuf == null || resampleCapacity < capacitySamples) {
            resampleBuf = arena.allocate((long) capacitySamples * Short.BYTES);
            resampleOutPtrs.set(ValueLayout.ADDRESS, 0L, resampleBuf);
            resampleCapacity = capacitySamples;
        }
    }

    /**
     * Flushes any samples libswresample is still holding in its internal buffer and enqueues them.
     *
     * <p>Called when the demuxer hits end of input so the resampler's tail is not lost.
     *
     * @throws InterruptedException  if interrupted while blocking to enqueue a flushed frame
     * @throws IllegalStateException if the flush conversion fails
     */
    private void flushResampler() throws InterruptedException {
        if (swrCtx == null) {
            return;
        }
        var outCapacity = OUT_FRAME_SAMPLES * 4;
        ensureResampleScratch(outCapacity);
        var produced = Ffmpeg.swr_convert(swrCtx, resampleOutPtrs, outCapacity, MemorySegment.NULL, 0);
        if (produced < 0) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "swr_convert(flush) failed: {0}", FFmpegError.describe(produced));
            }
            throw new IllegalStateException("swr_convert(flush) failed: "
                    + FFmpegError.describe(produced));
        }
        queueResampled(resampleBuf, produced);
    }

    /**
     * Slices freshly resampled samples into 10 ms frames, stamps each with the running presentation
     * timestamp, and enqueues them on the audio queue.
     *
     * <p>Prepends the {@link #leftover} carry from the previous decode, splits the combined samples into whole
     * chunks of {@link #OUT_FRAME_SAMPLES} samples each into a fresh array, advances {@link #audioPtsMicros}
     * by {@link #OUT_FRAME_DURATION_MICROS} per chunk, and stores any remainder as the new leftover carry for
     * the next call. Blocks on a full queue, applying backpressure to the demux thread.
     *
     * @param outBuf   the resampler output buffer
     * @param produced the number of samples produced into {@code outBuf}
     * @throws InterruptedException if interrupted while blocking to enqueue a frame
     */
    private void queueResampled(MemorySegment outBuf, int produced) throws InterruptedException {
        var total = leftover.length + produced;
        var combined = new short[total];
        System.arraycopy(leftover, 0, combined, 0, leftover.length);
        MemorySegment.copy(outBuf, ValueLayout.JAVA_SHORT_UNALIGNED, 0L,
                combined, leftover.length, produced);

        var chunks = total / OUT_FRAME_SAMPLES;
        for (var i = 0; i < chunks; i++) {
            var chunk = new short[OUT_FRAME_SAMPLES];
            System.arraycopy(combined, i * OUT_FRAME_SAMPLES, chunk, 0, OUT_FRAME_SAMPLES);
            var pts = audioPtsMicros;
            audioPtsMicros += OUT_FRAME_DURATION_MICROS;
            audioQueue.put(new AudioFrame(chunk, pts));
        }
        var remainder = total - chunks * OUT_FRAME_SAMPLES;
        if (remainder > 0) {
            leftover = new short[remainder];
            System.arraycopy(combined, chunks * OUT_FRAME_SAMPLES, leftover, 0, remainder);
        } else {
            leftover = new short[0];
        }
    }

    /**
     * Builds a libswresample context that converts the decoder's native format to 16 kHz mono signed 16 bit
     * PCM.
     *
     * <p>Reads the decoder's channel layout, sample format, and sample rate as the source parameters and
     * targets a default mono layout at {@link #OUT_SAMPLE_RATE}, then initializes the context.
     *
     * @param arena the lifetime arena that owns the allocations
     * @param frame the decoded frame whose channel layout, sample format, and sample rate are read as the
     *              source parameters
     * @return the initialized {@code SwrContext} pointer
     * @throws IllegalStateException if the resampler cannot be allocated or initialized
     */
    private static MemorySegment buildResampler(Arena arena, MemorySegment frame) {
        var swrPtr = arena.allocate(ValueLayout.ADDRESS);
        var monoLayout = arena.allocate(AVChannelLayout.layout());
        Ffmpeg.av_channel_layout_default(monoLayout, 1);
        var srcChLayout = AVFrame.ch_layout(frame);
        var srcFmt = AVFrame.format(frame);
        var srcRate = AVFrame.sample_rate(frame);

        FFmpegError.check("swr_alloc_set_opts2",
                Ffmpeg.swr_alloc_set_opts2(swrPtr,
                        monoLayout, Ffmpeg.AV_SAMPLE_FMT_S16(), OUT_SAMPLE_RATE,
                        srcChLayout, srcFmt, srcRate,
                        0, MemorySegment.NULL));
        var swr = swrPtr.get(ValueLayout.ADDRESS, 0L);
        FFmpegError.check("swr_init", Ffmpeg.swr_init(swr));
        return swr;
    }

    /**
     * Frees a native pointer held behind a single indirection, guarding against a {@code null} or zero
     * address.
     *
     * <p>Wraps the pointer in a temporary {@code T**} the way each libav* free expects and invokes the free.
     *
     * @param ptr  the native pointer to free, or a {@code null} or zero segment to skip
     * @param free the libav* free that takes the address of the pointer
     */
    private static void freePointer(MemorySegment ptr, Consumer<MemorySegment> free) {
        if (ptr == null || ptr.address() == 0L) {
            return;
        }
        try (var local = Arena.ofConfined()) {
            var pp = local.allocate(ValueLayout.ADDRESS);
            pp.set(ValueLayout.ADDRESS, 0L, ptr);
            free.accept(pp);
        }
    }

    /**
     * Returns the index of the first video stream in the container, or {@code -1} when none exists.
     *
     * @param formatCtx the demuxer context
     * @return the video stream index, or {@code -1} if the container has no video stream
     */
    private static int pickVideoStream(MemorySegment formatCtx) {
        var n = AVFormatContext.nb_streams(formatCtx);
        var streamsArr = AVFormatContext.streams(formatCtx)
                .reinterpret((long) n * ValueLayout.ADDRESS.byteSize());
        for (var i = 0; i < n; i++) {
            var stream = streamsArr.getAtIndex(ValueLayout.ADDRESS, i)
                    .reinterpret(AVStream.layout().byteSize());
            var params = AVStream.codecpar(stream);
            if (AVCodecParameters.codec_type(params) == Ffmpeg.AVMEDIA_TYPE_VIDEO()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first audio stream in the container, or {@code -1} when none exists.
     *
     * @param formatCtx the demuxer context
     * @return the audio stream index, or {@code -1} if the container has no audio stream
     */
    private static int pickAudioStream(MemorySegment formatCtx) {
        var n = AVFormatContext.nb_streams(formatCtx);
        var streamsArr = AVFormatContext.streams(formatCtx)
                .reinterpret((long) n * ValueLayout.ADDRESS.byteSize());
        for (var i = 0; i < n; i++) {
            var stream = streamsArr.getAtIndex(ValueLayout.ADDRESS, i)
                    .reinterpret(AVStream.layout().byteSize());
            var params = AVStream.codecpar(stream);
            if (AVCodecParameters.codec_type(params) == Ffmpeg.AVMEDIA_TYPE_AUDIO()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the {@code AVStream} pointer at the given index in the container.
     *
     * @param formatCtx the demuxer context
     * @param index     the stream index
     * @return the stream pointer at that index
     */
    private static MemorySegment streamPointer(MemorySegment formatCtx, int index) {
        var n = AVFormatContext.nb_streams(formatCtx);
        var streamsArr = AVFormatContext.streams(formatCtx)
                .reinterpret((long) n * ValueLayout.ADDRESS.byteSize());
        return streamsArr.getAtIndex(ValueLayout.ADDRESS, index)
                .reinterpret(AVStream.layout().byteSize());
    }

    /**
     * Caps a native pixel geometry to the engine's maximum encoded resolution, preserving aspect ratio.
     *
     * <p>Returns the geometry rounded down to even when neither dimension exceeds {@code 1280}; otherwise
     * scales the longer dimension down to {@code 1280} and the shorter dimension proportionally, then rounds
     * both down to the nearest even value of at least {@code 2}. H264 requires even dimensions, and capping the
     * longer side bounds the encode cost of a high resolution source while keeping its aspect ratio, so a 16:9
     * input is advertised as 16:9 rather than squished to a fixed 4:3 default.
     *
     * @param width  the native pixel width
     * @param height the native pixel height
     * @return a two element array of the capped, even {@code [width, height]}
     */
    private static int[] capGeometry(int width, int height) {
        if (Math.max(width, height) <= 1280) {
            return new int[]{evenDown(width), evenDown(height)};
        }
        int cappedWidth;
        int cappedHeight;
        if (width >= height) {
            cappedWidth = 1280;
            cappedHeight = (int) Math.round((double) height * 1280 / width);
        } else {
            cappedHeight = 1280;
            cappedWidth = (int) Math.round((double) width * 1280 / height);
        }
        return new int[]{evenDown(cappedWidth), evenDown(cappedHeight)};
    }

    /**
     * Rounds a dimension down to the nearest even value of at least {@code 2}.
     *
     * @param value the dimension to round
     * @return the largest even value not exceeding {@code value}, or {@code 2} when that would be below
     *         {@code 2}
     */
    private static int evenDown(int value) {
        return Math.max(2, value & ~1);
    }

    /**
     * Transmits the video and audio tracks of a local media file.
     *
     * <p>Opens the file through libavformat with no read timeout, since a local read does not stall.
     */
    public static final class File extends FfmpegVideoOutput {
        /**
         * Holds the media file to open, assigned before the base constructor runs so {@link #open} can read
         * it.
         */
        private final Path path;

        /**
         * Opens the given media file, detects its native video geometry, and prepares the shared demux,
         * decode, resample, and scale pipeline.
         *
         * @param path the media file to open
         * @throws NullPointerException  if {@code path} is {@code null}
         * @throws IllegalStateException if the file cannot be opened or has no video stream
         */
        public File(Path path) {
            this.path = path;
            super(null);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Opens the file through libavformat's file protocol; a local read does not stall, so the watchdog
         * is ignored.
         *
         * @param arena    {@inheritDoc}
         * @param watchdog {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        protected MemorySegment open(Arena arena, FfmpegIoWatchdog watchdog) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opening local media file {0}", path);
            return FfmpegHttpAvio.openFile(arena, path);
        }
    }

    /**
     * Transmits the video and audio tracks of a media stream addressed by a URI, bounding every blocking
     * demux call by a timeout.
     *
     * <p>Opens {@code file}, {@code http}, and {@code https} URIs through {@link FfmpegHttpAvio}, which serves
     * HTTP over a JDK connection so the native library carries no network or TLS code.
     */
    public static final class Uri extends FfmpegVideoOutput {
        /**
         * Holds the media stream to open, assigned before the base constructor runs so {@link #open} can read
         * it.
         */
        private final URI uri;

        /**
         * Holds the timeout for each operation, assigned before the base constructor runs so {@link #open} can
         * read it.
         */
        private final Duration ioTimeout;

        /**
         * Opens the given URI, detects its native video geometry, and prepares the shared demux, decode,
         * resample, and scale pipeline, bounding every blocking demux call with the given timeout.
         *
         * @param uri       the media stream to open
         * @param ioTimeout the maximum time any single connect, probe, or read may block; must be positive
         * @throws NullPointerException     if {@code uri} or {@code ioTimeout} is {@code null}
         * @throws IllegalArgumentException if {@code ioTimeout} is not positive, or the scheme is not
         *                                  permitted
         * @throws IllegalStateException    if the stream cannot be opened or has no video stream
         */
        public Uri(URI uri, Duration ioTimeout) {
            this.uri = uri;
            this.ioTimeout = ioTimeout;
            super(ioTimeout);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Opens the URI through {@link FfmpegHttpAvio}, installing and arming the watchdog around the
         * blocking connect and probe.
         *
         * @param arena    {@inheritDoc}
         * @param watchdog {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        protected MemorySegment open(Arena arena, FfmpegIoWatchdog watchdog) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "opening media uri, scheme={0} host={1} timeout={2}ms",
                        uri.getScheme(), uri.getHost(), ioTimeout.toMillis());
            }
            return FfmpegHttpAvio.openUri(arena, watchdog, uri, ioTimeout);
        }
    }
}
