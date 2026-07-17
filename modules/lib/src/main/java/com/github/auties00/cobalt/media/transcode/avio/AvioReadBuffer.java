package com.github.auties00.cobalt.media.transcode.avio;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.ffmpeg.AVIOContext;
import com.github.auties00.cobalt.util.ffmpeg.FFmpegError;
import com.github.auties00.cobalt.util.ffmpeg.Ffmpeg;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.channels.SeekableByteChannel;

/**
 * Bridges libavformat reads onto a {@link SeekableByteChannel} through native upcalls.
 *
 * <p>Each instance owns one native AVIO buffer (allocated via {@code av_malloc} and handed to
 * {@code avio_alloc_context}, which may grow or replace it at runtime) and two upcall stubs,
 * {@code read_packet} and {@code seek}, confined to the constructor's {@link Arena}. libavformat
 * invokes those stubs whenever it needs more input or repositions the stream; the bridge forwards
 * each call to the backing channel. The bridge implements {@link AutoCloseable} so the owning
 * pipeline can release the AVIO context with try-with-resources, while the backing
 * {@link SeekableByteChannel} remains the caller's to close.
 *
 * <p>The upcalls cannot propagate exceptions across the native boundary, so a channel-level
 * {@link IOException} is reported to libavformat as {@code AVERROR_EOF} and stashed in
 * {@link #lastError()}. The owning pipeline inspects {@link #lastError()} after the FFmpeg call
 * returns and converts a stashed error into a
 * {@link com.github.auties00.cobalt.exception.linked.WhatsAppMediaException.Processing}.
 *
 * @implNote This implementation wraps the native AVIO buffer slice as a direct
 * {@link java.nio.ByteBuffer} via {@link MemorySegment#asByteBuffer()} and hands it straight to
 * {@link SeekableByteChannel#read(java.nio.ByteBuffer)}, so disk bytes land in FFmpeg's native
 * buffer with one syscall and no Java-heap intermediation. The seek upcall delegates to
 * {@link SeekableByteChannel#position(long)} and {@link SeekableByteChannel#size()} and supports
 * the POSIX whences {@link #SEEK_SET}, {@link #SEEK_CUR}, {@link #SEEK_END} plus the
 * {@link #AVSEEK_SIZE} flag from {@code libavformat/avio.h}.
 */
public final class AvioReadBuffer implements AutoCloseable {
    /** The logger for {@link AvioReadBuffer}. */
    private static final System.Logger LOGGER = Log.get(AvioReadBuffer.class);

    /**
     * Describes the {@code read_packet} upcall signature {@code int (*)(void *opaque, uint8_t *buf, int buf_size)}.
     *
     * <p>Returns a {@code JAVA_INT} byte count and takes the opaque pointer, the destination buffer
     * pointer, and the buffer size, matching the function-pointer slot {@code avio_alloc_context}
     * expects for reads.
     */
    private static final FunctionDescriptor READ_PACKET_DESCRIPTOR = FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT);

    /**
     * Describes the {@code seek} upcall signature {@code int64_t (*)(void *opaque, int64_t offset, int whence)}.
     *
     * <p>Returns a {@code JAVA_LONG} position and takes the opaque pointer, the target offset, and
     * the whence selector, matching the function-pointer slot {@code avio_alloc_context} expects for
     * seeks.
     */
    private static final FunctionDescriptor SEEK_DESCRIPTOR = FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT);

    /**
     * Holds the POSIX {@code SEEK_SET} whence, which sets the position to {@code offset}.
     */
    private static final int SEEK_SET = 0;

    /**
     * Holds the POSIX {@code SEEK_CUR} whence, which adds {@code offset} to the current position.
     */
    private static final int SEEK_CUR = 1;

    /**
     * Holds the POSIX {@code SEEK_END} whence, which sets the position to {@code size + offset}.
     */
    private static final int SEEK_END = 2;

    /**
     * Holds the {@code AVSEEK_SIZE} flag, which requests the underlying size instead of a seek.
     *
     * @implNote This implementation uses {@code 0x10000}, the value defined for {@code AVSEEK_SIZE}
     * in {@code libavformat/avio.h}. libavformat ORs the flag into the whence word, so the seek
     * upcall masks the low byte off before matching a POSIX whence.
     */
    private static final int AVSEEK_SIZE = 0x10000;

    /**
     * Holds the default AVIO read buffer size in bytes.
     *
     * @implNote This implementation uses {@code 4096}, the buffer size FFmpeg's own custom-IO
     * example programs allocate.
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Holds the source channel that the read and seek upcalls delegate to.
     */
    private final SeekableByteChannel channel;

    /**
     * Holds the native buffer handed to {@code avio_alloc_context} as the AVIO read buffer.
     */
    private final MemorySegment avioBuffer;

    /**
     * Holds the AVIOContext pointer, which the owning pipeline installs on its {@code AVFormatContext}
     * through the {@code AVFormatContext.pb} field.
     */
    private final MemorySegment ioContext;

    /**
     * Holds the last {@link IOException} thrown from the channel, or {@code null} when none occurred.
     *
     * <p>Declared {@code volatile} because the upcall stub may run on whichever thread libavformat
     * drives the demuxer from, while the owning pipeline reads the field after the FFmpeg call
     * returns.
     */
    private volatile IOException lastError;

    /**
     * Guards against a second {@link #close()} freeing the AVIO context twice.
     */
    private boolean closed;

    /**
     * Constructs a read bridge backed by the given channel using {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param arena   the arena confining the upcall stubs
     * @param channel the source channel, which this bridge does not close
     */
    public AvioReadBuffer(Arena arena, SeekableByteChannel channel) {
        this(arena, channel, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs a read bridge backed by the given channel with an explicit buffer size.
     *
     * <p>Allocates the native AVIO buffer, binds the {@link #readPacket} and {@link #seek} upcall
     * stubs to {@code this} within {@code arena}, and creates the AVIOContext in read-only mode.
     *
     * @param arena      the arena confining the upcall stubs
     * @param channel    the source channel, which this bridge does not close
     * @param bufferSize the AVIO buffer size in bytes, which must be positive
     */
    public AvioReadBuffer(Arena arena, SeekableByteChannel channel, int bufferSize) {
        this.channel = channel;
        this.avioBuffer = FFmpegError.requireNonNull("av_malloc(AVIO read buffer)",
                Ffmpeg.av_malloc(bufferSize)).reinterpret(bufferSize);
        MethodHandle readHandle;
        MethodHandle seekHandle;
        try {
            readHandle = MethodHandles.lookup().bind(this, "readPacket",
                    MethodType.methodType(int.class, MemorySegment.class,
                            MemorySegment.class, int.class));
            seekHandle = MethodHandles.lookup().bind(this, "seek",
                    MethodType.methodType(long.class, MemorySegment.class,
                            long.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("AvioReadBuffer.{readPacket,seek} must exist", e);
        }
        var readStub = Linker.nativeLinker().upcallStub(readHandle, READ_PACKET_DESCRIPTOR, arena);
        var seekStub = Linker.nativeLinker().upcallStub(seekHandle, SEEK_DESCRIPTOR, arena);
        this.ioContext = FFmpegError.requireNonNull("avio_alloc_context(read)",
                Ffmpeg.avio_alloc_context(avioBuffer, bufferSize, 0,
                        MemorySegment.NULL, readStub, MemorySegment.NULL, seekStub));
    }

    /**
     * Returns the AVIOContext pointer for this bridge.
     *
     * @return the AVIOContext pointer
     */
    public MemorySegment ioContext() {
        return ioContext;
    }

    /**
     * Returns the last {@link IOException} thrown from the underlying channel, or {@code null}.
     *
     * <p>Because the read and seek upcalls cannot throw across the native boundary, a channel-level
     * I/O error is reported to libavformat as {@code AVERROR_EOF} and stashed here. The owning
     * pipeline inspects this value after FFmpeg returns and converts a non-null result into a
     * {@link com.github.auties00.cobalt.exception.linked.WhatsAppMediaException.Processing}.
     *
     * @return the last channel exception, or {@code null} if none occurred
     */
    public IOException lastError() {
        return lastError;
    }

    /**
     * Reads up to {@code bufSize} bytes from the channel into the libavformat-supplied buffer.
     *
     * <p>libavformat invokes this through the {@code read_packet} upcall stub installed in the AVIO
     * context whenever it needs more input. The method reinterprets {@code buf} as the native AVIO
     * buffer slice, exposes it as a direct {@link java.nio.ByteBuffer}, and reads straight into it
     * so disk bytes land in FFmpeg's buffer with no Java-heap copy. A non-positive channel read is
     * reported as end-of-input. An {@link IOException} is stashed in {@link #lastError()} and also
     * reported to libavformat as end-of-input.
     *
     * @param opaque  the opaque pointer, unused because the bridge captures {@code this}
     * @param buf     the destination buffer libavformat provided
     * @param bufSize the maximum number of bytes to copy
     * @return the number of bytes read, or {@code AVERROR_EOF} on end-of-input or I/O error
     */
    @SuppressWarnings("unused")
    int readPacket(MemorySegment opaque, MemorySegment buf, int bufSize) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "avio read: bufSize={0}", bufSize);
        try {
            var dst = buf.reinterpret(bufSize).asByteBuffer();
            dst.limit(bufSize);
            var read = channel.read(dst);
            return read > 0 ? read : Ffmpeg.AVERROR_EOF();
        } catch (IOException e) {
            lastError = e;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "avio read failed: bufSize=" + bufSize, e);
            return Ffmpeg.AVERROR_EOF();
        }
    }

    /**
     * Repositions the channel or reports its size in response to a libavformat seek.
     *
     * <p>libavformat invokes this through the {@code seek} upcall stub. When the {@link #AVSEEK_SIZE}
     * flag is set, the method returns the channel size without moving the position; this supports
     * demuxers (including MP4 and M4A) that probe the stream length and seek back to a trailing
     * metadata atom during {@code avformat_open_input}. Otherwise it masks the low byte of
     * {@code whence}, resolves the target absolute position against {@link #SEEK_SET},
     * {@link #SEEK_CUR}, or {@link #SEEK_END}, rejects an out-of-bounds or unrecognised request by
     * returning {@code -1}, and applies the position through
     * {@link SeekableByteChannel#position(long)}. An {@link IOException} is stashed in
     * {@link #lastError()} and reported as {@code -1}.
     *
     * @param opaque the opaque pointer, unused
     * @param offset the target offset, whose interpretation depends on {@code whence}
     * @param whence one of {@link #SEEK_SET}, {@link #SEEK_CUR}, {@link #SEEK_END}, or a word
     *               containing the {@link #AVSEEK_SIZE} flag
     * @return the new absolute position, the channel size when {@link #AVSEEK_SIZE} is set, or
     *         {@code -1} on an out-of-bounds, unrecognised, or failed seek
     */
    @SuppressWarnings("unused")
    long seek(MemorySegment opaque, long offset, int whence) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "avio seek: offset={0} whence={1}", offset, whence);
        try {
            if ((whence & AVSEEK_SIZE) != 0) {
                return channel.size();
            }
            var size = channel.size();
            var newPosition = switch (whence & 0xFF) {
                case SEEK_SET -> offset;
                case SEEK_CUR -> channel.position() + offset;
                case SEEK_END -> size + offset;
                default -> -1L;
            };
            if (newPosition < 0 || newPosition > size) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "avio seek out of bounds: target={0} size={1}", newPosition, size);
                }
                return -1L;
            }
            channel.position(newPosition);
            return newPosition;
        } catch (IOException e) {
            lastError = e;
            if (Log.WARNING) LOGGER.log(Level.WARNING, "avio seek failed: offset=" + offset, e);
            return -1L;
        }
    }

    /**
     * Releases the AVIO context and frees the backing native buffer.
     *
     * <p>Frees the AVIO buffer pointer through {@code av_freep} and the AVIO context through
     * {@code avio_context_free}, reading the live buffer pointer back from the context because
     * libavformat may have grown or replaced the original allocation. The call is idempotent and a
     * no-op once the context has already been released. The underlying channel is not closed.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (ioContext == MemorySegment.NULL) {
            return;
        }
        try (var local = Arena.ofConfined()) {
            var bufferPtr = AVIOContext.buffer(ioContext);
            if (bufferPtr != MemorySegment.NULL) {
                var pp = local.allocate(ValueLayout.ADDRESS);
                pp.set(ValueLayout.ADDRESS, 0L, bufferPtr);
                Ffmpeg.av_freep(pp);
            }
            var pp = local.allocate(ValueLayout.ADDRESS);
            pp.set(ValueLayout.ADDRESS, 0L, ioContext);
            Ffmpeg.avio_context_free(pp);
        }
    }
}
