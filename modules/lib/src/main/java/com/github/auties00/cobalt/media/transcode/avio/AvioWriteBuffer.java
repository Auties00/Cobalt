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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * Bridges libavformat muxer output onto a Java sink through native upcalls.
 *
 * <p>Two sealed variants cover the muxer shapes Cobalt drives. {@link HeapBuffer} accumulates output
 * in a growing heap {@code byte[]} and registers no read or seek callback, which fits append-only
 * muxers whose output is small enough to live in heap, such as OGG voice notes. {@link FileSystem}
 * pipes output through a {@link FileChannel} on a temporary file and registers read and seek
 * callbacks too, which supports muxers that backseek during {@code av_write_trailer}, such as the
 * M4A {@code ipod} trailer-atom rewrite and the MP4 {@code +faststart} {@code moov} hoist. Instances
 * are obtained through the {@link #ofHeap(Arena)} and {@link #ofFileSystem(Arena)} factories rather
 * than constructed directly.
 *
 * <p>The upcalls cannot propagate exceptions across the native boundary, so a sink I/O error is
 * reported to libavformat as a negative status and stashed in {@link #lastError()}. The owning
 * pipeline inspects {@link #lastError()} after FFmpeg returns and converts a non-null result into a
 * {@link com.github.auties00.cobalt.exception.linked.WhatsAppMediaException.Processing}.
 *
 * @implNote This implementation binds the upcall stubs to {@code this}, so the virtual dispatch of
 * the abstract {@link #writePacket} and the overridable {@link #readPacket} and {@link #seek}
 * routes each native call to the concrete variant at runtime. The file-backed variant wraps the
 * native AVIO buffer slice as a direct {@link java.nio.ByteBuffer} via
 * {@link MemorySegment#asByteBuffer()} so disk and FFmpeg's native buffer exchange bytes with one
 * syscall and no Java-heap copy; the heap variant instead copies once from the native buffer into
 * its backing array, which is acceptable because its callers cap their output at sub-megabyte
 * sizes. The AVIO context is created with the read and seek slots set to {@code NULL} for a
 * non-seekable variant, so libavformat never invokes the no-op defaults inherited from this class.
 */
public sealed abstract class AvioWriteBuffer implements AutoCloseable
        permits AvioWriteBuffer.HeapBuffer, AvioWriteBuffer.FileSystem {
    /** The logger for {@link AvioWriteBuffer}. */
    private static final System.Logger LOGGER = Log.get(AvioWriteBuffer.class);

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
     * Describes the {@code write_packet} upcall signature {@code int (*)(void *opaque, const uint8_t *buf, int buf_size)}.
     *
     * <p>Returns a {@code JAVA_INT} byte count and takes the opaque pointer, the source buffer
     * pointer, and the buffer size, matching the function-pointer slot {@code avio_alloc_context}
     * expects for writes.
     */
    private static final FunctionDescriptor WRITE_PACKET_DESCRIPTOR = FunctionDescriptor.of(
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
     * Holds the default AVIO buffer size in bytes.
     *
     * @implNote This implementation uses {@code 4096}, the buffer size FFmpeg's own custom-IO
     * example programs allocate.
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Holds the native buffer handed to {@code avio_alloc_context} as the AVIO write buffer.
     */
    protected final MemorySegment avioBuffer;

    /**
     * Holds the AVIOContext pointer, which the owning pipeline installs on its {@code AVFormatContext}
     * through the {@code AVFormatContext.pb} field.
     */
    protected final MemorySegment ioContext;

    /**
     * Holds the last {@link IOException} thrown from the underlying sink, or {@code null} when none
     * occurred.
     *
     * <p>Declared {@code volatile} because the upcall stub may run on whichever thread libavformat
     * drives the muxer from, while the owning pipeline reads the field after the FFmpeg call
     * returns.
     */
    protected volatile IOException lastError;

    /**
     * Guards against a second {@link #close()} freeing the AVIO context twice.
     */
    private boolean closed;

    /**
     * Initialises the AVIO context for a subclass, binding the upcall stubs to {@code this}.
     *
     * <p>Allocates the native AVIO buffer and binds the {@code write_packet} stub to
     * {@link #writePacket}. When {@code seekable} is {@code true} it also binds the
     * {@code read_packet} and {@code seek} stubs to {@link #readPacket} and {@link #seek};
     * otherwise those AVIO context slots are left {@code NULL} so libavformat never invokes them.
     * The bound stubs dispatch through the subclass overrides, so each native call resolves to the
     * concrete variant. On any failure after the buffer is allocated but before the context is
     * created, the buffer is freed so no native memory leaks.
     *
     * @param arena      the arena confining the upcall stubs
     * @param bufferSize the AVIO buffer size in bytes, which must be positive
     * @param seekable   {@code true} to register {@link #readPacket} and {@link #seek} alongside
     *                   {@link #writePacket}
     */
    private AvioWriteBuffer(Arena arena, int bufferSize, boolean seekable) {
        MemorySegment allocatedBuffer = null;
        MemorySegment allocatedContext = null;
        try {
            allocatedBuffer = FFmpegError.requireNonNull("av_malloc(AVIO write buffer)",
                    Ffmpeg.av_malloc(bufferSize)).reinterpret(bufferSize);
            MethodHandle writeHandle;
            MethodHandle readHandle = null;
            MethodHandle seekHandle = null;
            try {
                writeHandle = MethodHandles.lookup().bind(this, "writePacket",
                        MethodType.methodType(int.class, MemorySegment.class,
                                MemorySegment.class, int.class));
                if (seekable) {
                    readHandle = MethodHandles.lookup().bind(this, "readPacket",
                            MethodType.methodType(int.class, MemorySegment.class,
                                    MemorySegment.class, int.class));
                    seekHandle = MethodHandles.lookup().bind(this, "seek",
                            MethodType.methodType(long.class, MemorySegment.class,
                                    long.class, int.class));
                }
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new AssertionError("AvioWriteBuffer.{writePacket,readPacket,seek} must exist", e);
            }
            var writeStub = Linker.nativeLinker().upcallStub(writeHandle, WRITE_PACKET_DESCRIPTOR, arena);
            var readStub = seekable
                    ? Linker.nativeLinker().upcallStub(readHandle, READ_PACKET_DESCRIPTOR, arena)
                    : MemorySegment.NULL;
            var seekStub = seekable
                    ? Linker.nativeLinker().upcallStub(seekHandle, SEEK_DESCRIPTOR, arena)
                    : MemorySegment.NULL;
            allocatedContext = FFmpegError.requireNonNull("avio_alloc_context(write)",
                    Ffmpeg.avio_alloc_context(allocatedBuffer, bufferSize, 1,
                            MemorySegment.NULL, readStub, writeStub, seekStub));
        } catch (Throwable t) {
            if (allocatedBuffer != null && allocatedContext == null) {
                try (var local = Arena.ofConfined()) {
                    var pp = local.allocate(ValueLayout.ADDRESS);
                    pp.set(ValueLayout.ADDRESS, 0L, allocatedBuffer);
                    Ffmpeg.av_freep(pp);
                }
            }
            throw t;
        }
        this.avioBuffer = allocatedBuffer;
        this.ioContext = allocatedContext;
    }

    /**
     * Returns a heap-backed write bridge using {@link #DEFAULT_BUFFER_SIZE}.
     *
     * <p>Suits pipelines whose output fits comfortably in heap and is produced by an append-only
     * muxer, such as OGG voice notes.
     *
     * @param arena the arena confining the upcall stubs
     * @return the heap-backed bridge
     */
    public static HeapBuffer ofHeap(Arena arena) {
        return ofHeap(arena, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Returns a heap-backed write bridge with an explicit AVIO buffer size.
     *
     * @param arena      the arena confining the upcall stubs
     * @param bufferSize the AVIO buffer size in bytes, which must be positive
     * @return the heap-backed bridge
     */
    public static HeapBuffer ofHeap(Arena arena, int bufferSize) {
        return new HeapBuffer(arena, bufferSize);
    }

    /**
     * Returns a file-backed write bridge using {@link #DEFAULT_BUFFER_SIZE}.
     *
     * <p>Creates a fresh temporary file to back the sink. Suits pipelines whose muxer backseeks,
     * such as M4A {@code ipod} and MP4 {@code +faststart}, or whose output is too large to live in
     * heap.
     *
     * @param arena the arena confining the upcall stubs
     * @return the file-backed bridge
     * @throws IOException if the temp file cannot be created or opened
     */
    public static FileSystem ofFileSystem(Arena arena) throws IOException {
        return ofFileSystem(arena, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Returns a file-backed write bridge with an explicit AVIO buffer size.
     *
     * <p>Creates a temporary file and opens it for read and write before constructing the bridge. On
     * any failure after the file is created, the channel is closed and the file is deleted so no
     * temp file leaks.
     *
     * @param arena      the arena confining the upcall stubs
     * @param bufferSize the AVIO buffer size in bytes, which must be positive
     * @return the file-backed bridge
     * @throws IOException if the temp file cannot be created or opened
     */
    public static FileSystem ofFileSystem(Arena arena, int bufferSize) throws IOException {
        var path = Files.createTempFile(FileSystem.TEMP_FILE_PREFIX, FileSystem.TEMP_FILE_SUFFIX);
        FileChannel channel = null;
        try {
            channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mux temp file created: bufferSize={0}", bufferSize);
            return new FileSystem(arena, bufferSize, path, channel);
        } catch (Throwable t) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "mux temp file setup failed", t);
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                }
            }
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to delete mux temp file after setup failure", e);
            }
            throw t;
        }
    }

    /**
     * Returns the AVIOContext pointer for this bridge.
     *
     * @return the AVIOContext pointer
     */
    public final MemorySegment ioContext() {
        return ioContext;
    }

    /**
     * Returns the last {@link IOException} thrown from the underlying sink, or {@code null}.
     *
     * <p>Because the upcalls cannot throw across the native boundary, a sink I/O error is reported
     * to libavformat as a negative status and stashed here. The owning pipeline inspects this value
     * after FFmpeg returns and converts a non-null result into a
     * {@link com.github.auties00.cobalt.exception.linked.WhatsAppMediaException.Processing}.
     *
     * @return the last sink exception, or {@code null} if none occurred
     */
    public final IOException lastError() {
        return lastError;
    }

    /**
     * Routes the libavformat-supplied native bytes to the subclass sink.
     *
     * <p>libavformat invokes this through the {@code write_packet} upcall stub whenever it flushes
     * muxer output.
     *
     * @implSpec An implementation writes the {@code bufSize} bytes at {@code buf} to its backing
     * sink and returns the number of bytes accepted, or a negative {@code AVERROR_*} status on
     * failure. An implementation must not propagate an exception across the native boundary; it
     * stashes a sink {@link IOException} in {@link #lastError} and reports a negative status
     * instead.
     *
     * @param opaque  the opaque pointer, unused
     * @param buf     the source buffer
     * @param bufSize the number of bytes to write
     * @return the number of bytes written, or a negative {@code AVERROR_*} on failure
     */
    abstract int writePacket(MemorySegment opaque, MemorySegment buf, int bufSize);

    /**
     * Reads bytes back from the sink in response to a libavformat read; non-seekable variants
     * report end-of-input.
     *
     * <p>libavformat invokes this through the {@code read_packet} upcall stub only for a seekable
     * variant whose AVIO context registered the read slot. The default implementation immediately
     * reports end-of-input and exists so the non-seekable variant never needs to override it.
     *
     * @implSpec A seekable variant reads up to {@code bufSize} bytes from its sink into {@code buf}
     * and returns the byte count, or {@code AVERROR_EOF} on end-of-input or I/O error. The default
     * returns {@code AVERROR_EOF} and is never reached for a non-seekable variant because its read
     * slot is left {@code NULL}.
     *
     * @param opaque  the opaque pointer, unused
     * @param buf     the destination buffer
     * @param bufSize the maximum number of bytes to copy
     * @return {@code AVERROR_EOF} for the default implementation
     */
    int readPacket(MemorySegment opaque, MemorySegment buf, int bufSize) {
        return Ffmpeg.AVERROR_EOF();
    }

    /**
     * Repositions the sink in response to a libavformat seek; non-seekable variants report failure.
     *
     * <p>libavformat invokes this through the {@code seek} upcall stub only for a seekable variant
     * whose AVIO context registered the seek slot. The default implementation reports failure and
     * exists so the non-seekable variant never needs to override it.
     *
     * @implSpec A seekable variant interprets {@code whence} as a POSIX whence, or returns the sink
     * size when the {@link #AVSEEK_SIZE} flag is set, and returns the new absolute position or
     * {@code -1} on failure. The default returns {@code -1} and is never reached for a non-seekable
     * variant because its seek slot is left {@code NULL}.
     *
     * @param opaque the opaque pointer, unused
     * @param offset the target offset
     * @param whence the seek mode
     * @return {@code -1} for the default implementation
     */
    long seek(MemorySegment opaque, long offset, int whence) {
        return -1L;
    }

    /**
     * Releases the AVIO context, frees the backing native buffer, and releases subclass storage.
     *
     * <p>Frees the AVIO buffer pointer through {@code av_freep} and the AVIO context through
     * {@code avio_context_free}, reading the live buffer pointer back from the context because
     * libavformat may have grown or replaced the original allocation, then invokes
     * {@link #releaseBackingStorage()}. The call is idempotent: a second invocation returns without
     * touching native memory.
     */
    @Override
    public final void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (ioContext != MemorySegment.NULL) {
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
        releaseBackingStorage();
    }

    /**
     * Releases any type-specific backing storage held by the subclass.
     *
     * <p>Invoked by {@link #close()} after the AVIO context is freed. The default does nothing
     * because {@link HeapBuffer} has no native storage to release.
     *
     * @implSpec {@link FileSystem} overrides this to close its channel and delete its temp file
     * unless ownership was previously transferred via {@link FileSystem#release()}; an override must
     * tolerate being called after a successful {@link FileSystem#release()}.
     */
    protected void releaseBackingStorage() {
    }

    /**
     * Accumulates muxer output in a growing heap {@code byte[]}.
     *
     * <p>Suits append-only muxers such as OGG whose total output fits comfortably in heap. The
     * accumulated bytes are exposed through {@link #toByteArray()} once the caller has invoked
     * {@code av_write_trailer}. Instances are obtained through {@link AvioWriteBuffer#ofHeap(Arena)}.
     */
    public static final class HeapBuffer extends AvioWriteBuffer {
        /**
         * Holds the initial backing buffer capacity in bytes.
         *
         * @implNote This implementation uses {@code 16 * 1024}, a starting size large enough that
         * sub-megabyte voice-note output rarely needs more than a few geometric doublings.
         */
        private static final int INITIAL_BACKING_CAPACITY = 16 * 1024;

        /**
         * Holds every byte the muxer has written, growing geometrically as needed.
         */
        private byte[] backing = new byte[INITIAL_BACKING_CAPACITY];

        /**
         * Holds the logical size of the accumulated output in bytes, which never decreases.
         */
        private int size;

        /**
         * Constructs the heap-backed bridge.
         *
         * @param arena      the arena confining the upcall stubs
         * @param bufferSize the AVIO buffer size in bytes, which must be positive
         */
        private HeapBuffer(Arena arena, int bufferSize) {
            super(arena, bufferSize, false);
        }

        /**
         * Returns the bytes the muxer has accumulated so far as a fresh, exactly-sized array.
         *
         * <p>Callers usually invoke this after {@code av_write_trailer}. Because subsequent muxer
         * writes append to the same internal buffer, a later call returns a snapshot that includes
         * the trailing bytes.
         *
         * @return a fresh array of every accumulated byte
         */
        public byte[] toByteArray() {
            return Arrays.copyOf(backing, size);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation copies the native AVIO bytes into the heap backing array,
         * growing it geometrically on demand. A non-positive {@code bufSize} is accepted as a no-op.
         */
        @Override
        @SuppressWarnings("unused")
        int writePacket(MemorySegment opaque, MemorySegment buf, int bufSize) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "avio heap write: bufSize={0}", bufSize);
            if (bufSize <= 0) {
                return 0;
            }
            ensureCapacity(size + bufSize);
            var data = buf.reinterpret(bufSize);
            MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0L, backing, size, bufSize);
            size += bufSize;
            return bufSize;
        }

        /**
         * Grows {@link #backing} so it can hold at least {@code required} bytes.
         *
         * <p>Returns without reallocating when the current array already satisfies the request;
         * otherwise doubles the capacity until it does and copies the accumulated bytes across.
         *
         * @param required the new minimum capacity in bytes
         */
        private void ensureCapacity(int required) {
            if (required <= backing.length) {
                return;
            }
            var next = backing.length;
            while (next < required) {
                next *= 2;
            }
            var grown = new byte[next];
            System.arraycopy(backing, 0, grown, 0, size);
            backing = grown;
        }
    }

    /**
     * Pipes muxer output through a {@link FileChannel} on a temporary file.
     *
     * <p>Suits muxers that backseek during {@code av_write_trailer}, such as M4A {@code ipod} and
     * MP4 {@code +faststart}. The bridge owns the temp file: {@link #close()} deletes it unless
     * ownership was transferred via {@link #release()}. After a successful mux the owning pipeline
     * calls {@link #release()} to detach the path and stream the file from the upload layer.
     * Instances are obtained through {@link AvioWriteBuffer#ofFileSystem(Arena)}.
     */
    public static final class FileSystem extends AvioWriteBuffer {
        /**
         * Holds the prefix for the temp file name.
         */
        private static final String TEMP_FILE_PREFIX = "cobalt-mux-";

        /**
         * Holds the suffix for the temp file name.
         */
        private static final String TEMP_FILE_SUFFIX = ".tmp";

        /**
         * Holds the temp file backing this bridge, or {@code null} once {@link #release()} has
         * transferred ownership to the caller.
         */
        private Path path;

        /**
         * Holds the open file channel used for read, write, and seek.
         */
        private final FileChannel channel;

        /**
         * Constructs the file-backed bridge with a pre-validated temp file and open channel.
         *
         * @param arena      the arena confining the upcall stubs
         * @param bufferSize the AVIO buffer size in bytes, which must be positive
         * @param path       the temp file
         * @param channel    the open file channel
         */
        private FileSystem(Arena arena, int bufferSize, Path path, FileChannel channel) {
            super(arena, bufferSize, true);
            this.path = path;
            this.channel = channel;
        }

        /**
         * Detaches the temp file from this bridge's cleanup and returns its path to the caller.
         *
         * <p>Invoked by the owning pipeline after {@code av_write_trailer} completes successfully.
         * Forces pending writes to disk, closes the channel, clears the bridge's reference to the
         * path, and returns it; the caller then owns the file and is responsible for deleting it. A
         * subsequent {@link #close()} frees the AVIO context but does not touch the file.
         *
         * @return the temp file path
         * @throws IllegalStateException if the file has already been released
         * @throws IOException           if the channel cannot be flushed or closed
         */
        public Path release() throws IOException {
            if (path == null) {
                throw new IllegalStateException("AvioWriteBuffer.FileSystem already released");
            }
            channel.force(true);
            channel.close();
            var released = path;
            path = null;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mux temp file released to caller");
            return released;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation hands the native AVIO buffer slice to the channel as a
         * direct {@link java.nio.ByteBuffer} so muxer bytes land on disk with no Java-heap copy,
         * looping until the slice is drained or a short write stalls. A non-positive {@code bufSize}
         * is accepted as a no-op. A sink {@link IOException} is stashed in {@link #lastError} and
         * reported as {@code AVERROR_EOF}.
         */
        @Override
        @SuppressWarnings("unused")
        int writePacket(MemorySegment opaque, MemorySegment buf, int bufSize) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "avio file write: bufSize={0}", bufSize);
            if (bufSize <= 0) {
                return 0;
            }
            try {
                var src = buf.reinterpret(bufSize).asByteBuffer();
                src.limit(bufSize);
                var total = 0;
                while (src.hasRemaining()) {
                    var n = channel.write(src);
                    if (n <= 0) {
                        break;
                    }
                    total += n;
                }
                return total;
            } catch (IOException e) {
                lastError = e;
                if (Log.WARNING) LOGGER.log(Level.WARNING, "avio file write failed: bufSize=" + bufSize, e);
                return Ffmpeg.AVERROR_EOF();
            }
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation reinterprets {@code buf} as a direct
         * {@link java.nio.ByteBuffer} and reads from the channel into it. The MP4 muxer's
         * {@code +faststart} path requires it: the muxer seeks to position 0 and reads the entire
         * payload back so it can rewrite the file with the {@code moov} atom at the front. A
         * sink {@link IOException} is stashed in {@link #lastError} and reported as
         * {@code AVERROR_EOF}.
         */
        @Override
        @SuppressWarnings("unused")
        int readPacket(MemorySegment opaque, MemorySegment buf, int bufSize) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "avio file read: bufSize={0}", bufSize);
            try {
                var dst = buf.reinterpret(bufSize).asByteBuffer();
                dst.limit(bufSize);
                var read = channel.read(dst);
                return read > 0 ? read : Ffmpeg.AVERROR_EOF();
            } catch (IOException e) {
                lastError = e;
                if (Log.WARNING) LOGGER.log(Level.WARNING, "avio file read failed: bufSize=" + bufSize, e);
                return Ffmpeg.AVERROR_EOF();
            }
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation reports the channel size when
         * {@link AvioWriteBuffer#AVSEEK_SIZE} is set; otherwise it masks the low byte of
         * {@code whence}, resolves the target absolute position against
         * {@link AvioWriteBuffer#SEEK_SET}, {@link AvioWriteBuffer#SEEK_CUR}, or
         * {@link AvioWriteBuffer#SEEK_END}, rejects a negative or unrecognised target by returning
         * {@code -1}, and applies the position through {@link FileChannel#position(long)}. A sink
         * {@link IOException} is stashed in {@link #lastError} and reported as {@code -1}.
         */
        @Override
        @SuppressWarnings("unused")
        long seek(MemorySegment opaque, long offset, int whence) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "avio file seek: offset={0} whence={1}", offset, whence);
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
                if (newPosition < 0) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "avio file seek out of bounds: target={0}", newPosition);
                    }
                    return -1L;
                }
                channel.position(newPosition);
                return newPosition;
            } catch (IOException e) {
                lastError = e;
                if (Log.WARNING) LOGGER.log(Level.WARNING, "avio file seek failed: offset=" + offset, e);
                return -1L;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @implNote This implementation closes the channel and deletes the temp file, but only when
         * {@link #release()} has not already transferred ownership; otherwise it returns without
         * touching the file the caller now owns.
         */
        @Override
        protected void releaseBackingStorage() {
            if (path == null) {
                return;
            }
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to close mux temp file channel", e);
                }
            }
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to delete mux temp file on cleanup", e);
            }
            path = null;
        }
    }
}
