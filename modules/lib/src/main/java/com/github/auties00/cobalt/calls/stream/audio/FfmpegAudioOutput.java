package com.github.auties00.cobalt.calls.stream.audio;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.ffmpeg.AVChannelLayout;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecParameters;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFrame;
import com.github.auties00.cobalt.util.ffmpeg.AVPacket;
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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.ffmpeg.FfmpegHttpAvio;
import com.github.auties00.cobalt.calls.stream.ffmpeg.FfmpegIoWatchdog;

/**
 * Provides the FFmpeg based foundation of the demuxed media audio sources of a call, decoding the audio
 * track of an input into the call's 16 kHz mono frames ahead of the consumer into a jitter buffer.
 *
 * <p>This is the shared engine of its nested {@link File} (a local media file) and {@link Uri} (a media
 * stream addressed by URI) sources. A subclass supplies only how the input is opened, by overriding
 * {@link #open(Arena, FfmpegIoWatchdog)}; everything downstream of the opened demuxer is identical and
 * lives here: picking the first audio stream, opening its decoder, decoding ahead of the consumer on a
 * background thread, resampling each decoded frame to 16 kHz mono signed 16 bit PCM, slicing it into 10 ms
 * frames, and buffering them for the consumer.
 *
 * <p>A background thread decodes the input ahead of the consumer into a bounded read ahead buffer, and each
 * {@link #takeAudio()} returns one buffered 10 ms frame of 160 samples as soon as one is available; the source
 * does not pace itself, since the engine's send seam is the wall clock cadence gate at the wire. Decoding
 * ahead keeps that paced sender supplied across the jitter of the demux, decode, and resample steps, which
 * is otherwise worst at the codec's cold start and would starve the sender into choppy playback. Subframe
 * audio is never lost: the leftover samples from a decode that does not land on a 10 ms boundary are carried
 * into the next decode. When the input ends, the trailing partial frame (if any) is dropped and
 * {@link #takeAudio()} returns {@code null} to signal end of stream. {@link #shutdown()} stops the decoder thread
 * and releases the native demuxer, decoder, and resampler.
 *
 * <p>Every blocking demux operation is bounded by the optional read timeout passed to the constructor.
 * A local file source passes {@code null}, since a file read does not stall; a network source passes its
 * timeout, and an {@code av_read_frame} that the {@link FfmpegIoWatchdog} aborts is surfaced as an
 * {@link IllegalStateException} from {@link #takeAudio()} rather than being mistaken for a clean end of input.
 *
 * @implNote This implementation decodes ahead into a one second read ahead buffer and stamps each frame with
 * the {@link AudioFrame#ptsMicros()} microsecond clock the engine's send seam paces transmission against,
 * matching the cadence a live microphone capture would deliver.
 */
public abstract sealed class FfmpegAudioOutput implements AudioOutput
        permits FfmpegAudioOutput.File, FfmpegAudioOutput.Uri {
    /**
     * The logger for {@link FfmpegAudioOutput}.
     */
    private static final System.Logger LOGGER = Log.get(FfmpegAudioOutput.class);

    /**
     * Holds the output sample rate, in Hz, that the call layer expects.
     *
     * @implNote This implementation uses 16000, the rate WhatsApp's Opus call configuration runs at, so
     * the resampled output feeds the encoder directly.
     */
    private static final int OUT_SAMPLE_RATE = 16_000;

    /**
     * Holds the number of output samples per emitted frame.
     *
     * @implNote This implementation uses 160, which is 10 ms at {@link #OUT_SAMPLE_RATE}, the frame
     * cadence the call layer consumes.
     */
    private static final int OUT_FRAME_SAMPLES = 160;

    /**
     * Holds the duration, in microseconds, advanced per emitted frame.
     *
     * @implNote This implementation uses 10000, which is 10 ms for a frame of {@link #OUT_FRAME_SAMPLES}
     * samples at {@link #OUT_SAMPLE_RATE}, in the {@link AudioFrame#ptsMicros()} microsecond clock.
     */
    private static final long OUT_FRAME_DURATION_MICROS = 10_000;

    /**
     * Holds the number of decoded 10 ms frames buffered ahead of the consumer.
     *
     * @implNote This implementation reads up to one second ahead so the jitter of the demux, decode, and
     * resample steps, which is worst during the codec's cold start, never starves the call's paced sender.
     * The buffer fills during the seconds of call setup that precede the first {@link #takeAudio()}, so the read
     * ahead latency is hidden behind connection establishment.
     */
    private static final int PREFETCH_FRAMES = 100;

    /**
     * Marks end of stream inside {@link #prefetch}, distinct from a real decoded frame.
     */
    private static final AudioFrame END = new AudioFrame(new short[0], Long.MIN_VALUE);

    /**
     * Guards {@link #shutdown()} so the teardown runs at most once, and read by the decode loop to end once
     * the source is closed.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Holds the maximum time any single blocking demux read may take before the watchdog aborts it, or
     * {@code null} for an input that does not block and needs no read timeout.
     */
    private final Duration readTimeout;

    /**
     * Holds the timeout watchdog the opener installs and the read loop arms, shared with the opener so a
     * stalled open or read aborts instead of blocking the call's decode thread forever.
     */
    private final FfmpegIoWatchdog watchdog;

    /**
     * Holds the arena owning every libav* allocation this stream makes, closed when the stream shuts
     * down.
     */
    private final Arena arena;

    /**
     * Holds the libavformat demuxer context pointer, which owns the input handle.
     */
    private final MemorySegment formatCtx;

    /**
     * Holds the libavcodec decoder context pointer, which owns the codec's internal buffers.
     */
    private final MemorySegment codecCtx;

    /**
     * Holds the libswresample context pointer that converts the decoded format to 16 kHz mono signed
     * 16 bit PCM.
     *
     * <p>Set lazily on the first decoded {@link AVFrame} rather than in the constructor: many codecs
     * (MP3 included) leave {@code codecCtx->ch_layout} and {@code codecCtx->sample_fmt} unset until the
     * first frame is produced, and seeding the resampler from those zeroed values yields a context that
     * fails {@code swr_convert} with {@code EINVAL} on every subsequent call.
     */
    private MemorySegment swrCtx;

    /**
     * Holds the reusable libswresample destination sample buffer, or {@code null} until the first resample.
     *
     * <p>The per frame output capacity is effectively constant for a stream (the codec's frame size and the
     * source sample rate are fixed), so this buffer and its {@link #resampleOutPtrs} pointer are allocated
     * once from {@link #arena} on the first resample and reused by every later resample rather than opening a
     * per frame confined arena, growing only if a frame ever needs more than the current
     * {@link #resampleCapacity}.
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
     * Holds the reusable {@code AVPacket} pointer driven by the demuxer read loop.
     */
    private final MemorySegment packet;

    /**
     * Holds the reusable {@code AVFrame} pointer that the decoder writes into.
     */
    private final MemorySegment frame;

    /**
     * Holds the index of the audio stream chosen from the container.
     */
    private final int streamIndex;

    /**
     * Holds the queue of decoded and resampled 10 ms frames not yet emitted by {@link #takeAudio()}.
     */
    private final Deque<short[]> readyFrames = new ArrayDeque<>();

    /**
     * Holds the leftover samples from the last decode that did not fill a 10 ms frame.
     *
     * <p>Carried into the next decode and prepended before the new samples are sliced, so subframe audio
     * is never dropped mid stream.
     */
    private short[] leftover = new short[0];

    /**
     * Holds the presentation timestamp, in microseconds, of the next emitted frame, advanced by
     * {@link #OUT_FRAME_DURATION_MICROS} (10 ms) per frame.
     */
    private long ptsMicros;

    /**
     * Holds whether the demuxer has reached end of stream and the decoder has been flushed.
     */
    private boolean drained;

    /**
     * Holds the failure that ended the background decode, surfaced from {@link #takeAudio()}, or {@code null}
     * while the decode is healthy.
     *
     * <p>Set on the decoder thread when a demux, decode, or resample step fails (including a watchdog
     * timeout) and read on the pump thread, so it is {@code volatile}; it lets {@link #takeAudio()} distinguish
     * a failed stream, which throws, from a cleanly drained one, which returns {@code null}.
     */
    private volatile RuntimeException decodeFailure;

    /**
     * Holds decoded 10 ms frames produced by the background {@link #decoderThread} and consumed by
     * {@link #takeAudio()}, bounding the read ahead to {@link #PREFETCH_FRAMES} frames.
     */
    private final BlockingQueue<AudioFrame> prefetch = new ArrayBlockingQueue<>(PREFETCH_FRAMES + 1);

    /**
     * Holds the background thread that decodes the input ahead of the consumer, started once at the end of
     * the constructor.
     */
    private final Thread decoderThread;

    /**
     * Opens and probes the input demuxer, returning its context.
     *
     * <p>Called once from the constructor. A subclass opens the demuxer with {@code avformat_open_input},
     * probes it with {@code avformat_find_stream_info}, and returns the opened {@code AVFormatContext}
     * pointer reinterpreted to {@link AVFormatContext#layout()}'s size; a network subclass installs the
     * watchdog on the context before opening and arms it around each blocking call, while a file subclass
     * ignores the watchdog.
     *
     * @param arena    the source's lifetime arena that owns the native allocations the open makes
     * @param watchdog the timeout watchdog to install and arm around blocking calls, or ignore for an
     *                 input that does not block
     * @return the opened and probed {@code AVFormatContext} pointer, reinterpreted to its layout size
     * @throws IllegalStateException if the input cannot be opened or probed
     */
    protected abstract MemorySegment open(Arena arena, FfmpegIoWatchdog watchdog);

    /**
     * Opens the input and prepares the demuxer, decoder, and resampler chain.
     *
     * <p>Ensures the FFmpeg libraries are loaded, allocates the lifetime arena and the timeout watchdog,
     * opens and probes the input through {@link #open(Arena, FfmpegIoWatchdog)}, picks its first audio
     * stream, opens a decoder for that stream's codec, and starts the background decode thread. If any step
     * fails the arena is closed before the exception propagates, so a failed construction leaks no native
     * resources.
     *
     * @param readTimeout the maximum time a single blocking demux read may take, or {@code null} for an
     *                    input that does not block and needs no read timeout
     * @throws IllegalStateException if the input cannot be opened, has no audio stream, or its decoder
     *                               cannot be initialized
     */
    public FfmpegAudioOutput(Duration readTimeout) {
        FFmpegLoader.ensureLoaded();
        this.readTimeout = readTimeout;
        this.arena = Arena.ofShared();
        this.watchdog = new FfmpegIoWatchdog(arena);
        try {
            this.formatCtx = open(arena, watchdog);
            this.streamIndex = pickAudioStream(formatCtx);
            if (streamIndex < 0) {
                throw new IllegalStateException("no audio stream in input");
            }
            var stream = streamPointer(formatCtx, streamIndex);
            var params = AVStream.codecpar(stream);
            var codecId = AVCodecParameters.codec_id(params);
            var codec = FFmpegError.requireNonNull(
                    "avcodec_find_decoder(" + codecId + ")",
                    Ffmpeg.avcodec_find_decoder(codecId));
            this.codecCtx = FFmpegError.requireNonNull(
                    "avcodec_alloc_context3",
                    Ffmpeg.avcodec_alloc_context3(codec));
            FFmpegError.check("avcodec_parameters_to_context",
                    Ffmpeg.avcodec_parameters_to_context(codecCtx, params));
            FFmpegError.check("avcodec_open2",
                    Ffmpeg.avcodec_open2(codecCtx, codec, MemorySegment.NULL));

            this.packet = FFmpegError.requireNonNull("av_packet_alloc", Ffmpeg.av_packet_alloc());
            this.frame = FFmpegError.requireNonNull("av_frame_alloc", Ffmpeg.av_frame_alloc());
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "audio source open failed", e);
            arena.close();
            throw e;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "audio source opened: stream={0} readTimeout={1}",
                    streamIndex, readTimeout);
        }
        this.decoderThread = Thread.ofPlatform()
                .name("ffmpeg-audio-decoder")
                .daemon(true)
                .start(this::decodeLoop);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A demuxed media source produces its frames inside {@link #takeAudio()} from the decoder and ignores
     * application writes, so this does nothing.
     *
     * @param frame the frame that would be written; ignored
     */
    @Override
    public void writeAudio(AudioFrame frame) {
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @implNote This implementation returns {@code false}: decoded media is already clean line level audio
     * that the engine encodes without the acoustic conditioning a live microphone capture needs.
     */
    @Override
    public boolean isLiveCapture() {
        return false;
    }

    /**
     * Decodes the input ahead of the consumer, feeding {@link #prefetch} until the stream drains or shuts
     * down.
     *
     * <p>Runs on the background {@link #decoderThread}: all FFmpeg decode calls happen here, on a single
     * thread, while {@link #takeAudio()} only reads the buffered result. Blocks on a full buffer to bound the
     * read ahead and exits when the input drains, when {@link #shutdown()} sets the closed flag, when
     * interrupted, or when a demux, decode, or resample step fails. A failure is recorded in
     * {@link #decodeFailure} and an end marker is enqueued so a waiting {@link #takeAudio()} unblocks and
     * rethrows it.
     */
    private void decodeLoop() {
        try {
            while (!closed.get()) {
                var next = decodeNext();
                if (next == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio input drained");
                    prefetch.put(END);
                    return;
                }
                prefetch.put(next);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "audio decode failed", e);
            decodeFailure = e;
            try {
                prefetch.put(END);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the next decoded 10 ms frame from the read ahead {@link #prefetch} buffer that the
     * background {@link #decoderThread} fills, blocking only if the decoder has not yet produced the next
     * frame. Returns {@code null} once the input is fully drained and the buffer is exhausted, or once
     * {@link #shutdown()} has ended the stream. Throws if the background decode failed, including when a
     * blocking read timed out.
     *
     * @return {@inheritDoc}
     * @throws IllegalStateException if the background demux, decode, or resample failed, or a blocking read
     *                               exceeded the configured timeout
     * @implNote This implementation reads from a buffer the decoder fills ahead of the consumer rather
     * than decoding inline, so a slow or bursty demux, decode, or resample step never stalls the call's
     * paced sender; without it the peer's jitter buffer underruns on every decode hiccup and the audio is
     * choppy. Each frame carries its running presentation timestamp, which the engine's send seam uses to
     * pace transmission to wall clock; the source itself does not pace, so it returns a buffered frame as
     * soon as one is available.
     */
    @Override
    public AudioFrame takeAudio() {
        if (closed.get()) {
            return null;
        }
        try {
            var next = prefetch.take();
            if (next == END) {
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
     * Drives the demux, decode, and resample pipeline until a 10 ms frame is ready, then returns it with the
     * next presentation timestamp and advances the timestamp by 10 ms.
     *
     * <p>Called only from the background {@link #decoderThread}. Returns {@code null} once the input is
     * fully drained and no further frames remain, or once {@link #shutdown()} has set the closed flag.
     *
     * @return the next decoded frame, or {@code null} at end of stream
     */
    private AudioFrame decodeNext() {
        while (readyFrames.isEmpty() && !drained && !closed.get()) {
            pump();
        }
        if (readyFrames.isEmpty()) {
            return null;
        }
        var pcm = readyFrames.pollFirst();
        var pts = ptsMicros;
        ptsMicros += OUT_FRAME_DURATION_MICROS;
        return new AudioFrame(pcm, pts);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the source ended, cancels the timeout watchdog so a parked demux read returns at once,
     * stops and joins the background decoder so no FFmpeg call is in flight, then frees the decoder output
     * frame, the demuxer packet, the resampler, the decoder context, and the demuxer context, and closes
     * the owning arena. Guards each pointer against {@code null} and a zero address, so the call is
     * idempotent.
     */
    @Override
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "audio source shutdown");
        watchdog.cancel();
        var decoder = decoderThread;
        if (decoder != null) {
            decoder.interrupt();
            try {
                decoder.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try (arena) {
            if (frame != null && frame.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, frame);
                    Ffmpeg.av_frame_free(pp);
                }
            }
            if (packet != null && packet.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, packet);
                    Ffmpeg.av_packet_free(pp);
                }
            }
            if (swrCtx != null && swrCtx.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, swrCtx);
                    Ffmpeg.swr_free(pp);
                }
            }
            if (codecCtx != null && codecCtx.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, codecCtx);
                    Ffmpeg.avcodec_free_context(pp);
                }
            }
            if (formatCtx != null && formatCtx.address() != 0L) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, formatCtx);
                    Ffmpeg.avformat_close_input(pp);
                }
            }
            watchdog.closeIo();
        }
    }

    /**
     * Drives one round of demux, decode, and resample.
     *
     * <p>Reads one packet, arming the watchdog around the read when a read timeout is configured; on
     * end of input it flushes the decoder and marks the stream drained, and on a read the watchdog aborts it
     * throws so {@link #takeAudio()} surfaces the timeout. Otherwise it feeds packets belonging to the chosen
     * audio stream to the decoder and drains the resulting frames.
     *
     * @throws IllegalStateException if a configured read timeout aborts the demux read, or the decoder
     *                               reports a hard failure
     */
    private void pump() {
        if (readTimeout != null) {
            watchdog.arm(readTimeout);
        }
        var read = Ffmpeg.av_read_frame(formatCtx, packet);
        if (readTimeout != null) {
            watchdog.disarm();
        }
        if (read < 0) {
            if (readTimeout != null && watchdog.fired()) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "audio input read timed out after {0}", readTimeout);
                }
                throw new IllegalStateException("input read timed out after " + readTimeout);
            }
            var failure = watchdog.readFailure();
            if (failure != null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "audio input read failed: {0}", failure);
                throw new IllegalStateException("input read failed: " + failure);
            }
            Ffmpeg.avcodec_send_packet(codecCtx, MemorySegment.NULL);
            drainDecoder(true);
            drained = true;
            return;
        }
        try {
            var idx = AVPacket.stream_index(packet);
            if (idx != streamIndex) {
                return;
            }
            var sent = Ffmpeg.avcodec_send_packet(codecCtx, packet);
            if (sent < 0 && !FFmpegError.isAgain(sent)) {
                var description = FFmpegError.describe(sent);
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "avcodec_send_packet failed: {0}", description);
                }
                throw new IllegalStateException("avcodec_send_packet failed: " + description);
            }
            drainDecoder(false);
        } finally {
            Ffmpeg.av_packet_unref(packet);
        }
    }

    /**
     * Pulls every available frame out of the decoder, resamples each to 16 kHz mono signed 16 bit PCM, and
     * queues it for emission.
     *
     * <p>When in flush mode and the decoder signals end of output, the resampler's internal buffer is
     * flushed too so no tail samples are lost.
     *
     * @param flush whether the decoder is in flush mode (after end of input)
     * @throws IllegalStateException if the decoder reports a hard failure
     */
    private void drainDecoder(boolean flush) {
        while (true) {
            var got = Ffmpeg.avcodec_receive_frame(codecCtx, frame);
            if (FFmpegError.isAgain(got) || (flush && FFmpegError.isEof(got))) {
                if (flush && FFmpegError.isEof(got)) {
                    flushResampler();
                }
                return;
            }
            if (got < 0) {
                throw new IllegalStateException("avcodec_receive_frame failed: "
                        + FFmpegError.describe(got));
            }
            try {
                resampleAndQueue();
            } finally {
                Ffmpeg.av_frame_unref(frame);
            }
        }
    }

    /**
     * Resamples the current {@link #frame} to 16 kHz mono signed 16 bit PCM and queues the output as 10 ms
     * frames.
     *
     * <p>Sizes the output buffer from the input sample count rescaled to the output rate plus a small
     * margin, runs one libswresample conversion, and slices the result into ready frames. Frames with no
     * samples are skipped.
     *
     * @throws IllegalStateException if the resample fails
     */
    private void resampleAndQueue() {
        var inSamples = AVFrame.nb_samples(frame);
        if (inSamples <= 0) {
            return;
        }
        if (swrCtx == null) {
            swrCtx = buildResampler(arena, frame);
        }
        var outCapacity = (int) (((long) inSamples * OUT_SAMPLE_RATE
                                  / Math.max(1, AVFrame.sample_rate(frame))) + 256);
        ensureResampleScratch(outCapacity);
        var inPtrs = AVFrame.extended_data(frame);
        var produced = Ffmpeg.swr_convert(swrCtx, resampleOutPtrs, outCapacity, inPtrs, inSamples);
        if (produced < 0) {
            throw new IllegalStateException("swr_convert failed: " + FFmpegError.describe(produced));
        }
        queueResampled(resampleBuf, produced);
    }

    /**
     * Allocates the reusable libswresample destination scratch, growing it only when a frame needs more.
     *
     * <p>The output capacity is effectively constant across a stream, so {@link #resampleBuf} and
     * {@link #resampleOutPtrs} are built once from {@link #arena} and reused by every resample rather than a
     * per frame confined arena. A frame whose capacity exceeds the current {@link #resampleCapacity} grows
     * the buffer and repoints {@link #resampleOutPtrs}; the superseded allocation lives until the source's
     * arena closes.
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
     * Flushes any samples libswresample is still holding in its internal buffer and queues them.
     *
     * <p>Called when the demuxer hits end of input so the resampler's tail is not lost.
     *
     * @throws IllegalStateException if the flush conversion fails
     */
    private void flushResampler() {
        if (swrCtx == null) {
            return;
        }
        var outCapacity = OUT_FRAME_SAMPLES * 4;
        ensureResampleScratch(outCapacity);
        var produced = Ffmpeg.swr_convert(swrCtx, resampleOutPtrs, outCapacity, MemorySegment.NULL, 0);
        if (produced < 0) {
            throw new IllegalStateException("swr_convert(flush) failed: "
                    + FFmpegError.describe(produced));
        }
        queueResampled(resampleBuf, produced);
    }

    /**
     * Slices freshly resampled samples into 10 ms frames and pushes them onto the ready queue.
     *
     * <p>Prepends the {@link #leftover} carry from the previous decode, splits the combined samples into
     * whole chunks of {@link #OUT_FRAME_SAMPLES} samples, and stores any remainder as the new leftover carry
     * for the next call.
     *
     * @param outBuf   the resampler output buffer
     * @param produced the number of samples produced into {@code outBuf}
     */
    private void queueResampled(MemorySegment outBuf, int produced) {
        var total = leftover.length + produced;
        var combined = new short[total];
        System.arraycopy(leftover, 0, combined, 0, leftover.length);
        MemorySegment.copy(outBuf, ValueLayout.JAVA_SHORT_UNALIGNED, 0L,
                combined, leftover.length, produced);

        var chunks = total / OUT_FRAME_SAMPLES;
        for (var i = 0; i < chunks; i++) {
            var chunk = new short[OUT_FRAME_SAMPLES];
            System.arraycopy(combined, i * OUT_FRAME_SAMPLES, chunk, 0, OUT_FRAME_SAMPLES);
            readyFrames.addLast(chunk);
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
     * Builds a libswresample context that converts the decoder's native format to 16 kHz mono signed
     * 16 bit PCM.
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
     * Transmits the audio track of a local media file.
     *
     * <p>Opens the file through libavformat with no read timeout, since a local read does not stall.
     */
    public static final class File extends FfmpegAudioOutput {
        /**
         * Holds the media file to open, assigned before the base constructor runs so {@link #open} can read
         * it.
         */
        private final Path path;

        /**
         * Opens the given media file and prepares the shared demux, decode, and resample chain.
         *
         * @param path the media file to open
         * @throws NullPointerException  if {@code path} is {@code null}
         * @throws IllegalStateException if the file cannot be opened or has no audio stream
         */
        public File(Path path) {
            this.path = path;
            super(null);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Opens the file through libavformat's file protocol; a local read does not stall, so the
         * watchdog is ignored.
         *
         * @param arena    {@inheritDoc}
         * @param watchdog {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        protected MemorySegment open(Arena arena, FfmpegIoWatchdog watchdog) {
            return FfmpegHttpAvio.openFile(arena, path);
        }
    }

    /**
     * Transmits the audio track of a media stream addressed by a URI, bounding every blocking demux call by
     * a timeout.
     *
     * <p>Opens {@code file}, {@code http}, and {@code https} URIs through {@link FfmpegHttpAvio}, which
     * serves HTTP over a JDK connection so the native library carries no network or TLS code.
     */
    public static final class Uri extends FfmpegAudioOutput {
        /**
         * Holds the media stream to open, assigned before the base constructor runs so {@link #open} can
         * read it.
         */
        private final URI uri;

        /**
         * Holds the per operation timeout, assigned before the base constructor runs so {@link #open} can
         * read it.
         */
        private final Duration ioTimeout;

        /**
         * Opens the given URI and prepares the shared demux, decode, and resample chain, bounding every
         * blocking demux call with the given timeout.
         *
         * @param uri       the media stream to open
         * @param ioTimeout the maximum time any single connect, probe, or read may block; must be positive
         * @throws NullPointerException     if {@code uri} or {@code ioTimeout} is {@code null}
         * @throws IllegalArgumentException if {@code ioTimeout} is not positive, or the scheme is not
         *                                  permitted
         * @throws IllegalStateException    if the stream cannot be opened or has no audio stream
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
            return FfmpegHttpAvio.openUri(arena, watchdog, uri, ioTimeout);
        }
    }
}
