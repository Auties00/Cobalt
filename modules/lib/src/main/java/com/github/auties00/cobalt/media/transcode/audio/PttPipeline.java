package com.github.auties00.cobalt.media.transcode.audio;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.util.ffmpeg.AVChannelLayout;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecContext;
import com.github.auties00.cobalt.util.ffmpeg.AVCodecParameters;
import com.github.auties00.cobalt.util.ffmpeg.AVFormatContext;
import com.github.auties00.cobalt.util.ffmpeg.AVFrame;
import com.github.auties00.cobalt.util.ffmpeg.AVOutputFormat;
import com.github.auties00.cobalt.util.ffmpeg.AVPacket;
import com.github.auties00.cobalt.util.ffmpeg.AVStream;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegError;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegLoader;
import com.github.auties00.cobalt.util.ffmpeg.Ffmpeg;
import com.github.auties00.cobalt.media.transcode.MediaTranscoderService;
import com.github.auties00.cobalt.media.transcode.avio.AvioReadBuffer;
import com.github.auties00.cobalt.media.transcode.avio.AvioWriteBuffer;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.media.AudioMessage;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.SeekableByteChannel;

/**
 * Decodes a source audio stream and re-encodes it as a WhatsApp voice note
 * (mono Opus inside an OGG container) plus the amplitude waveform that ships
 * in the message protobuf.
 *
 * <p>This is the voice-note branch of the upload transcoder. The output mirrors
 * the encode profile WhatsApp's mobile recorders ship: {@value #PTT_BITRATE} bps
 * Opus, {@value #PTT_FRAME_MS} ms frames, mono, {@value #PTT_SAMPLE_RATE} Hz
 * sampling. The waveform is computed by {@link WaveformGenerator} on the
 * resampled PCM before encoding.
 *
 * @implNote
 * This implementation drives FFmpeg end-to-end: libavformat demux, libswresample
 * to {@code FLT} {@value #PTT_SAMPLE_RATE} Hz mono, libopus encode, libavformat
 * OGG mux. The resulting bytes are a complete OGG-Opus container with no embedded
 * timestamps or chapter markers. The waveform is built from a {@code short[]}
 * quantisation of the same resampled buffer.
 *
 * @see WaveformGenerator
 */
public final class PttPipeline {
    /** The logger for {@link PttPipeline}. */
    private static final System.Logger LOGGER = Log.get(PttPipeline.class);

    /**
     * Holds the target sample rate for the encoded voice note in Hz, matching
     * the mobile recorders' wire-format profile.
     */
    private static final int PTT_SAMPLE_RATE = 16_000;

    /**
     * Holds the target channel count for the encoded voice note (mono).
     */
    private static final int PTT_CHANNELS = 1;

    /**
     * Holds the target bitrate for the encoded voice note in bits per second.
     */
    private static final int PTT_BITRATE = 16_000;

    /**
     * Holds the frame duration in milliseconds.
     *
     * @implNote
     * This implementation uses {@code 20} because Opus accepts only 5, 10, 20,
     * 40, or 60 ms frames and {@code 20} matches the mobile recorders' default.
     */
    private static final int PTT_FRAME_MS = 20;

    /**
     * Holds the frame size in samples at {@link #PTT_SAMPLE_RATE} for a
     * {@link #PTT_FRAME_MS}-millisecond frame.
     */
    private static final int PTT_FRAME_SAMPLES = PTT_SAMPLE_RATE * PTT_FRAME_MS / 1000;

    /**
     * Holds the {@code AV_CH_LAYOUT_MONO} constant value from
     * {@code <libavutil/channel_layout.h>}.
     */
    private static final long AV_CH_LAYOUT_MONO = 0x4L;

    /**
     * Constructs the pipeline.
     *
     * <p>The parent {@link MediaTranscoderService} owns the single instance.
     */
    public PttPipeline() {
    }

    /**
     * Transcodes the source audio into the voice-note wire format,
     * applies codec-derived metadata to {@code provider}, and returns the
     * encoded payload stream.
     *
     * <p>Decodes and resamples the source to {@value #PTT_SAMPLE_RATE} Hz mono,
     * generates the amplitude waveform from the resampled PCM, and encodes the
     * PCM as OGG-Opus. The provider is mutated in place: when {@code provider}
     * is an {@link AudioMessage} the
     * {@link AudioMessage#setMimetype(String) mimetype},
     * {@link MediaProvider#setMediaSize(long) mediaSize},
     * {@link AudioMessage#setSeconds(Integer) seconds}, and
     * {@link AudioMessage#setWaveform(byte[]) waveform} fields are populated and
     * {@link AudioMessage#setPtt(Boolean) ptt} is set to {@code true}; every
     * other {@link MediaProvider} variant receives only the common
     * {@link MediaProvider#setMediaSize(long) mediaSize} update. The reported
     * duration is clamped to a minimum of one second.
     *
     * @param provider the upload target; codec-derived fields are applied
     *                 to this instance
     * @param source   the raw audio channel; not closed by this method
     * @return the encoded OGG-Opus payload
     * @throws WhatsAppMediaException.Processing if demuxing, resampling,
     *         encoding, or muxing fails
     */
    public MediaPayload run(MediaProvider provider, SeekableByteChannel source)
            throws WhatsAppMediaException.Processing {
        FFmpegLoader.ensureLoaded();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ptt transcode start");
        try (var arena = Arena.ofShared()) {
            var pcm = decodeAndResample(arena, source);
            var durationSeconds = Math.max(1, pcm.length / PTT_SAMPLE_RATE);
            var s16 = floatToS16(pcm);
            var waveform = WaveformGenerator.generate(s16);
            var oggOpus = encodeOggOpus(arena, pcm);
            provider.setMediaSize(oggOpus.length);
            if (provider instanceof AudioMessage audio) {
                audio.setMimetype("audio/ogg; codecs=opus");
                audio.setSeconds(durationSeconds);
                audio.setWaveform(waveform);
                audio.setPtt(Boolean.TRUE);
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "ptt transcode done: samples={0} durationSeconds={1} encodedBytes={2}",
                        pcm.length, durationSeconds, oggOpus.length);
            }
            return new MediaPayload.OfBytes(oggOpus);
        } catch (WhatsAppMediaException.Processing e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "ptt transcode failed", e);
            throw e;
        }
    }

    /**
     * Demuxes the source, decodes the first audio stream, and resamples
     * every produced frame to {@link #PTT_SAMPLE_RATE} Hz mono float.
     *
     * @param arena   the shared arena owning the AVIO bridge
     * @param channel the source channel
     * @return a contiguous float buffer of every decoded PCM sample
     * @throws WhatsAppMediaException.Processing if any stage fails
     */
    private static float[] decodeAndResample(Arena arena, SeekableByteChannel channel)
            throws WhatsAppMediaException.Processing {
        var bridge = new AvioReadBuffer(arena, channel);
        var formatCtx = MemorySegment.NULL;
        var codecCtx = MemorySegment.NULL;
        var swr = MemorySegment.NULL;
        var frame = MemorySegment.NULL;
        var packet = MemorySegment.NULL;
        try {
            formatCtx = openInput(arena, bridge);
            var audioStream = pickStream(formatCtx, Ffmpeg.AVMEDIA_TYPE_AUDIO());
            if (audioStream < 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "no audio stream in ptt source");
                throw new WhatsAppMediaException.Processing("no audio stream in voice-note source");
            }
            var stream = streamPointer(formatCtx, audioStream);
            var params = AVStream.codecpar(stream);
            var codecId = AVCodecParameters.codec_id(params);
            var codec = FFmpegError.requireNonNull("avcodec_find_decoder(" + codecId + ")",
                    Ffmpeg.avcodec_find_decoder(codecId));
            codecCtx = FFmpegError.requireNonNull("avcodec_alloc_context3",
                    Ffmpeg.avcodec_alloc_context3(codec));
            FFmpegError.check("avcodec_parameters_to_context",
                    Ffmpeg.avcodec_parameters_to_context(codecCtx, params));
            FFmpegError.check("avcodec_open2",
                    Ffmpeg.avcodec_open2(codecCtx, codec, MemorySegment.NULL));
            swr = openResampler(arena, codecCtx);
            packet = FFmpegError.requireNonNull("av_packet_alloc", Ffmpeg.av_packet_alloc());
            frame = FFmpegError.requireNonNull("av_frame_alloc", Ffmpeg.av_frame_alloc());
            var pcm = new GrowingFloatBuffer();
            var eof = false;
            while (!eof) {
                var read = Ffmpeg.av_read_frame(formatCtx, packet);
                if (read < 0) {
                    Ffmpeg.avcodec_send_packet(codecCtx, MemorySegment.NULL);
                    eof = true;
                } else if (AVPacket.stream_index(packet) != audioStream) {
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
                while (true) {
                    var got = Ffmpeg.avcodec_receive_frame(codecCtx, frame);
                    if (FFmpegError.isAgain(got)) {
                        break;
                    }
                    if (FFmpegError.isEof(got)) {
                        eof = true;
                        break;
                    }
                    if (got < 0) {
                        throw new WhatsAppMediaException.Processing(
                                "avcodec_receive_frame failed: " + FFmpegError.describe(got));
                    }
                    drainResampler(arena, swr, frame, pcm);
                    Ffmpeg.av_frame_unref(frame);
                }
            }
            drainResamplerFlush(arena, swr, pcm);
            return pcm.toArray();
        } finally {
            if (frame != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, frame);
                    Ffmpeg.av_frame_free(pp);
                }
            }
            if (packet != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, packet);
                    Ffmpeg.av_packet_free(pp);
                }
            }
            if (swr != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, swr);
                    Ffmpeg.swr_free(pp);
                }
            }
            if (codecCtx != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, codecCtx);
                    Ffmpeg.avcodec_free_context(pp);
                }
            }
            if (formatCtx != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, formatCtx);
                    Ffmpeg.avformat_close_input(pp);
                }
            }
            bridge.close();
        }
    }

    /**
     * Allocates a {@code SwrContext} configured to resample from the
     * decoder's native format to {@link #PTT_SAMPLE_RATE} Hz mono float.
     *
     * @param arena    the arena for any transient strings
     * @param codecCtx the open decoder context
     * @return the initialised resampler
     */
    private static MemorySegment openResampler(Arena arena, MemorySegment codecCtx) {
        var swrPp = arena.allocate(ValueLayout.ADDRESS);
        var outLayout = AVChannelLayout.allocate(arena);
        Ffmpeg.av_channel_layout_default(outLayout, PTT_CHANNELS);
        FFmpegError.check("swr_alloc_set_opts2",
                Ffmpeg.swr_alloc_set_opts2(swrPp, outLayout,
                        Ffmpeg.AV_SAMPLE_FMT_FLT(), PTT_SAMPLE_RATE,
                        AVCodecContext.ch_layout(codecCtx),
                        AVCodecContext.sample_fmt(codecCtx),
                        AVCodecContext.sample_rate(codecCtx),
                        0, MemorySegment.NULL));
        var swr = FFmpegError.requireNonNull("swr_alloc_set_opts2 out", swrPp.get(ValueLayout.ADDRESS, 0L));
        FFmpegError.check("swr_init", Ffmpeg.swr_init(swr));
        return swr;
    }

    /**
     * Runs one batch of decoded samples through the resampler and appends
     * the produced floats to the growing accumulator.
     *
     * @param arena the arena for transient allocations
     * @param swr   the initialised resampler
     * @param frame the decoded source frame
     * @param sink  the growing accumulator that collects resampled output
     */
    private static void drainResampler(Arena arena, MemorySegment swr,
                                        MemorySegment frame, GrowingFloatBuffer sink) {
        var inSamples = AVFrame.nb_samples(frame);
        var outBufferSamples = (int) ((long) inSamples * PTT_SAMPLE_RATE
                                      / Math.max(1, AVFrame.sample_rate(frame))) + 1024;
        var outBuf = arena.allocate((long) outBufferSamples * Float.BYTES * PTT_CHANNELS);
        var outData = arena.allocate(ValueLayout.ADDRESS);
        outData.set(ValueLayout.ADDRESS, 0L, outBuf);
        var produced = FFmpegError.check("swr_convert",
                Ffmpeg.swr_convert(swr, outData, outBufferSamples,
                        AVFrame.extended_data(frame), inSamples));
        for (var i = 0; i < produced; i++) {
            sink.append(outBuf.getAtIndex(ValueLayout.JAVA_FLOAT, i));
        }
    }

    /**
     * Flushes the resampler at end-of-input.
     *
     * @param arena the arena for transient allocations
     * @param swr   the initialised resampler
     * @param sink  the growing accumulator
     */
    private static void drainResamplerFlush(Arena arena, MemorySegment swr,
                                             GrowingFloatBuffer sink) {
        while (true) {
            var outBufferSamples = 1024;
            var outBuf = arena.allocate((long) outBufferSamples * Float.BYTES * PTT_CHANNELS);
            var outData = arena.allocate(ValueLayout.ADDRESS);
            outData.set(ValueLayout.ADDRESS, 0L, outBuf);
            var produced = FFmpegError.check("swr_convert(flush)",
                    Ffmpeg.swr_convert(swr, outData, outBufferSamples,
                            MemorySegment.NULL, 0));
            if (produced <= 0) {
                return;
            }
            for (var i = 0; i < produced; i++) {
                sink.append(outBuf.getAtIndex(ValueLayout.JAVA_FLOAT, i));
            }
        }
    }

    /**
     * Encodes the resampled PCM as an OGG-Opus container into a growing
     * heap buffer and returns the accumulated bytes.
     *
     * <p>The PCM is split into {@link #PTT_FRAME_SAMPLES}-sample frames,
     * zero-padded on the final partial frame, fed to libopus, and interleaved
     * into the OGG muxer. A heap-backed output bridge is used because voice
     * notes are KB-scale at {@value #PTT_BITRATE} bps Opus, so the buffer never
     * climbs above sub-MB and its cost is negligible against concurrent uploads.
     *
     * @implNote
     * This implementation backs the output bridge with a heap buffer rather
     * than a temporary file because the OGG muxer writes strictly forward and
     * never backseeks during muxing, so no seekable sink is required.
     *
     * @param arena the shared arena owning the output bridge
     * @param pcm   the float PCM samples at {@link #PTT_SAMPLE_RATE}
     * @return the muxed OGG-Opus bytes
     * @throws WhatsAppMediaException.Processing if encoder or muxer
     *         setup fails
     */
    private static byte[] encodeOggOpus(Arena arena, float[] pcm)
            throws WhatsAppMediaException.Processing {
        var sink = AvioWriteBuffer.ofHeap(arena);
        var fmtCtx = MemorySegment.NULL;
        var encCtx = MemorySegment.NULL;
        var encFrame = MemorySegment.NULL;
        var encPacket = MemorySegment.NULL;
        try {
            var fmtCtxPp = arena.allocate(ValueLayout.ADDRESS);
            FFmpegError.check("avformat_alloc_output_context2(ogg)",
                    Ffmpeg.avformat_alloc_output_context2(fmtCtxPp, MemorySegment.NULL,
                            arena.allocateFrom("ogg"), MemorySegment.NULL));
            fmtCtx = fmtCtxPp.get(ValueLayout.ADDRESS, 0L)
                    .reinterpret(AVFormatContext.layout().byteSize());
            AVFormatContext.pb(fmtCtx, sink.ioContext());
            var encoder = FFmpegError.requireNonNull("avcodec_find_encoder_by_name(libopus)",
                    Ffmpeg.avcodec_find_encoder_by_name(arena.allocateFrom("libopus")));
            encCtx = FFmpegError.requireNonNull("avcodec_alloc_context3(libopus)",
                    Ffmpeg.avcodec_alloc_context3(encoder));
            AVCodecContext.sample_rate(encCtx, PTT_SAMPLE_RATE);
            AVCodecContext.sample_fmt(encCtx, Ffmpeg.AV_SAMPLE_FMT_FLT());
            AVCodecContext.bit_rate(encCtx, PTT_BITRATE);
            var encLayout = AVChannelLayout.allocate(arena);
            Ffmpeg.av_channel_layout_default(encLayout, PTT_CHANNELS);
            Ffmpeg.av_channel_layout_copy(AVCodecContext.ch_layout(encCtx), encLayout);
            var oneOne = arena.allocate(8);
            oneOne.set(ValueLayout.JAVA_INT, 0L, 1);
            oneOne.set(ValueLayout.JAVA_INT, 4L, PTT_SAMPLE_RATE);
            AVCodecContext.time_base(encCtx, oneOne);
            var output = AVFormatContext.oformat(fmtCtx);
            if ((AVOutputFormat.flags(output) & 0x40 /* AVFMT_GLOBALHEADER */) != 0) {
                AVCodecContext.flags(encCtx,
                        AVCodecContext.flags(encCtx) | Ffmpeg.AV_CODEC_FLAG_GLOBAL_HEADER());
            }
            FFmpegError.check("avcodec_open2(libopus)",
                    Ffmpeg.avcodec_open2(encCtx, encoder, MemorySegment.NULL));
            var stream = FFmpegError.requireNonNull("avformat_new_stream",
                    Ffmpeg.avformat_new_stream(fmtCtx, encoder));
            FFmpegError.check("avcodec_parameters_from_context",
                    Ffmpeg.avcodec_parameters_from_context(AVStream.codecpar(stream), encCtx));
            var streamTb = arena.allocate(8);
            streamTb.set(ValueLayout.JAVA_INT, 0L, 1);
            streamTb.set(ValueLayout.JAVA_INT, 4L, PTT_SAMPLE_RATE);
            AVStream.time_base(stream, streamTb);
            FFmpegError.check("avformat_write_header",
                    Ffmpeg.avformat_write_header(fmtCtx, MemorySegment.NULL));
            encFrame = FFmpegError.requireNonNull("av_frame_alloc(enc)",
                    Ffmpeg.av_frame_alloc());
            encPacket = FFmpegError.requireNonNull("av_packet_alloc(enc)",
                    Ffmpeg.av_packet_alloc());
            AVFrame.format(encFrame, Ffmpeg.AV_SAMPLE_FMT_FLT());
            Ffmpeg.av_channel_layout_copy(AVFrame.ch_layout(encFrame), encLayout);
            AVFrame.sample_rate(encFrame, PTT_SAMPLE_RATE);
            AVFrame.nb_samples(encFrame, PTT_FRAME_SAMPLES);
            FFmpegError.check("av_frame_get_buffer",
                    Ffmpeg.av_frame_get_buffer(encFrame, 0));
            long pts = 0;
            var cursor = 0;
            while (cursor < pcm.length) {
                var copy = Math.min(PTT_FRAME_SAMPLES, pcm.length - cursor);
                FFmpegError.check("av_frame_make_writable",
                        Ffmpeg.av_frame_make_writable(encFrame));
                AVFrame.nb_samples(encFrame, PTT_FRAME_SAMPLES);
                AVFrame.pts(encFrame, pts);
                var frameData = AVFrame.data(encFrame)
                        .getAtIndex(ValueLayout.ADDRESS, 0)
                        .reinterpret((long) PTT_FRAME_SAMPLES * Float.BYTES);
                for (var i = 0; i < copy; i++) {
                    frameData.setAtIndex(ValueLayout.JAVA_FLOAT, i, pcm[cursor + i]);
                }
                for (var i = copy; i < PTT_FRAME_SAMPLES; i++) {
                    frameData.setAtIndex(ValueLayout.JAVA_FLOAT, i, 0.0f);
                }
                writeFrameToMuxer(fmtCtx, encCtx, encFrame, encPacket);
                cursor += copy;
                pts += PTT_FRAME_SAMPLES;
            }
            writeFrameToMuxer(fmtCtx, encCtx, MemorySegment.NULL, encPacket);
            FFmpegError.check("av_write_trailer", Ffmpeg.av_write_trailer(fmtCtx));
            return sink.toByteArray();
        } finally {
            if (encPacket != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, encPacket);
                    Ffmpeg.av_packet_free(pp);
                }
            }
            if (encFrame != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, encFrame);
                    Ffmpeg.av_frame_free(pp);
                }
            }
            if (encCtx != MemorySegment.NULL) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, encCtx);
                    Ffmpeg.avcodec_free_context(pp);
                }
            }
            if (fmtCtx != MemorySegment.NULL) {
                Ffmpeg.avformat_free_context(fmtCtx);
            }
            sink.close();
        }
    }

    /**
     * Sends one frame to the encoder, pulls every produced packet, and
     * writes them to the muxer.
     *
     * @param fmtCtx the output format context
     * @param encCtx the open encoder
     * @param frame  the input frame, or {@code NULL} for end-of-stream
     * @param packet a reusable packet for the muxer write
     * @throws WhatsAppMediaException.Processing if the encode or write
     *         fails
     */
    private static void writeFrameToMuxer(MemorySegment fmtCtx, MemorySegment encCtx,
                                           MemorySegment frame, MemorySegment packet)
            throws WhatsAppMediaException.Processing {
        var sent = Ffmpeg.avcodec_send_frame(encCtx, frame);
        if (sent < 0 && !FFmpegError.isEof(sent)) {
            throw new WhatsAppMediaException.Processing(
                    "avcodec_send_frame failed: " + FFmpegError.describe(sent));
        }
        while (true) {
            var got = Ffmpeg.avcodec_receive_packet(encCtx, packet);
            if (FFmpegError.isAgain(got) || FFmpegError.isEof(got)) {
                return;
            }
            if (got < 0) {
                throw new WhatsAppMediaException.Processing(
                        "avcodec_receive_packet failed: " + FFmpegError.describe(got));
            }
            AVPacket.stream_index(packet, 0);
            var written = Ffmpeg.av_interleaved_write_frame(fmtCtx, packet);
            Ffmpeg.av_packet_unref(packet);
            if (written < 0) {
                throw new WhatsAppMediaException.Processing(
                        "av_interleaved_write_frame failed: " + FFmpegError.describe(written));
            }
        }
    }

    /**
     * Quantises a float PCM buffer to signed 16-bit samples.
     *
     * @param pcm the float samples in {@code [-1.0, 1.0]}
     * @return a fresh short array of the same length
     */
    private static short[] floatToS16(float[] pcm) {
        var out = new short[pcm.length];
        for (var i = 0; i < pcm.length; i++) {
            var v = pcm[i];
            if (v > 1.0f) v = 1.0f;
            else if (v < -1.0f) v = -1.0f;
            out[i] = (short) Math.round(v * Short.MAX_VALUE);
        }
        return out;
    }

    /**
     * Opens the source via the supplied AVIO bridge.
     *
     * @param arena  the shared arena
     * @param bridge the AVIO read bridge
     * @return the open demuxer
     */
    private static MemorySegment openInput(Arena arena, AvioReadBuffer bridge) {
        var formatCtx = FFmpegError.requireNonNull("avformat_alloc_context",
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
        return formatCtx;
    }

    /**
     * Returns the index of the first stream of the given media type.
     *
     * @param formatCtx the open demuxer
     * @param mediaType the {@code AVMEDIA_TYPE_*} value to search for
     * @return the stream index or {@code -1} if no matching stream exists
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
     * Returns the i-th stream pointer.
     *
     * @param formatCtx the open demuxer
     * @param index     the zero-based stream index
     * @return the stream pointer
     */
    private static MemorySegment streamPointer(MemorySegment formatCtx, int index) {
        return AVFormatContext.streams(formatCtx)
                .getAtIndex(ValueLayout.ADDRESS, index)
                .reinterpret(AVStream.layout().byteSize());
    }

    /**
     * Accumulates resampled PCM in an append-only growable primitive float
     * buffer.
     *
     * <p>The voice-note pipeline does not know the source duration in advance,
     * so the resampled buffer must grow as the demuxer delivers frames. A
     * {@link java.util.ArrayList} of boxed floats would work, but the
     * autoboxing cost dominates for typical voice-note lengths; this helper
     * trades the boxing for a small amortised resize cost.
     *
     * @implNote
     * This implementation doubles the backing-array capacity each time the
     * buffer fills, and {@link #toArray()} compacts to the exact size on
     * extraction.
     */
    private static final class GrowingFloatBuffer {
        /**
         * Holds the initial capacity in floats.
         */
        private static final int INITIAL_CAPACITY = 4096;

        /**
         * Holds the current backing array.
         */
        private float[] data = new float[INITIAL_CAPACITY];

        /**
         * Holds the number of populated elements.
         */
        private int size;

        /**
         * Appends one float to the buffer, doubling capacity if necessary.
         *
         * @param value the value to append
         */
        void append(float value) {
            if (size == data.length) {
                var grown = new float[data.length * 2];
                System.arraycopy(data, 0, grown, 0, size);
                data = grown;
            }
            data[size++] = value;
        }

        /**
         * Returns a fresh float array sized exactly to the appended
         * content.
         *
         * @return the compacted float array
         */
        float[] toArray() {
            var out = new float[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }
}
