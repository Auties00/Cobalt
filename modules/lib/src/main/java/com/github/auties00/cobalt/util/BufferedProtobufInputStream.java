package com.github.auties00.cobalt.util;

import com.github.auties00.cobalt.telemetry.log.Log;
import it.auties.protobuf.model.ProtobufString;
import it.auties.protobuf.stream.ProtobufInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link ProtobufInputStream} that reads its wire bytes through an internal read-ahead buffer.
 *
 * <p>The stream-backed decoder shipped with the protobuf library issues one underlying
 * {@link InputStream#read()} per byte for every tag and variable-length integer it parses; its only
 * buffer is a tiny ring sized for varint rewind, not for read-ahead. When the wrapped stream is a
 * file, socket, cipher, or decompression stream, that turns the decode of a single message into
 * thousands of one-byte syscalls or inflate steps. This subclass keeps the exact same decoding
 * contract but serves those byte reads from a block that is refilled {@code bufferSize} bytes at a
 * time, collapsing the per-byte traffic into one underlying read per buffer.
 *
 * <p>A sub-stream opened for a length-delimited region must drain the same pre-fetched bytes the
 * originating stream already buffered rather than re-reading them, so the buffer and its cursor live
 * on a single owner: the {@link #root}. The root is the stream created through a public constructor;
 * every sub-stream holds a reference back to it and routes its byte reads through it, leaving only
 * the per-region bookkeeping ({@link #length}, {@link #position}) private to each instance. The
 * cursor is three plain {@code int}s on the root ({@link #bufferPosition}, {@link #bufferLimit},
 * {@link #bufferMark}). Bulk length-delimited reads still bypass the buffer and land straight in the
 * destination array, and the bounded varint rewind used by the fast-path integer parser is preserved
 * across refills.
 *
 * <p>This implementation performs no locking and is therefore suitable only for single-threaded
 * sequential decoding; it must not be shared across threads.
 *
 * @apiNote
 * Construct one in place of {@link ProtobufInputStream#fromStream(InputStream)} when decoding from an
 * I/O or decompression stream, for example
 * {@snippet :
 * try (var input = new BufferedProtobufInputStream(path)) {
 *     return SomeSpec.decode(input);
 * }
 * }
 * There is nothing to gain over {@link ProtobufInputStream#fromBytes(byte[])} when the payload is
 * already fully in memory; reserve this for file, network, cipher, and decompression sources.
 */
// TODO: Delete me when we migrate to Daedalus
public final class BufferedProtobufInputStream extends ProtobufInputStream {
    /**
     * The logger for {@link BufferedProtobufInputStream}.
     */
    private static final System.Logger LOGGER = Log.get(BufferedProtobufInputStream.class);

    /**
     * The buffer size used by the constructors that take no explicit size, matching the conventional
     * 8 KiB block size of the JDK stream decorators.
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The smallest accepted buffer size; chosen comfortably above {@link #MAX_REWIND_DISTANCE} so a
     * refill can always retain a full rewind window and still read at least one fresh byte.
     */
    private static final int MINIMUM_BUFFER_SIZE = 64;

    /**
     * The largest rewind distance the protobuf decoder ever requests, equal to the maximum encoded
     * length of a 64-bit varint; refills retain at most this many already-consumed bytes so a
     * pending rewind can replay them.
     */
    private static final int MAX_REWIND_DISTANCE = 10;

    /**
     * The buffer and cursor owner: {@code this} for a root stream, the originating root for a
     * sub-stream. All byte reads route through it so sub-streams share one underlying cursor.
     */
    private final BufferedProtobufInputStream root;

    /**
     * The wrapped stream, meaningful on the {@link #root}; {@link #close()} closes it.
     */
    private final InputStream inputStream;

    /**
     * The read-ahead buffer, meaningful on the {@link #root}.
     */
    private final byte[] buffer;

    /**
     * The index of the next unread byte in {@link #buffer}; maintained on the {@link #root} only.
     */
    private int bufferPosition;

    /**
     * The number of valid bytes in {@link #buffer}; bytes in {@code [bufferPosition, bufferLimit)}
     * are unread. Maintained on the {@link #root} only.
     */
    private int bufferLimit;

    /**
     * The buffer index a {@link #rewind()} returns to, or {@code -1} when no live mark exists.
     * Maintained on the {@link #root} only.
     */
    private int bufferMark;

    /**
     * Whether {@link #close()} closes the underlying stream; {@code true} for a root stream,
     * {@code false} for a sub-stream.
     */
    private final boolean autoclose;

    /**
     * The byte length of this logical extent, or {@code -1} for an unbounded root stream; a
     * sub-stream carries the size of the length-delimited region it was opened for.
     */
    private final long length;

    /**
     * The number of bytes consumed from this logical extent, tracked only when {@link #length} is
     * bounded so {@link #isFinished()} can detect the end of a sub-message.
     */
    private long position;

    /**
     * The value of {@link #position} captured by the most recent {@link #mark()}, restored by
     * {@link #rewind()}.
     */
    private long markedPosition;

    /**
     * Creates a buffered protobuf stream over the given source using {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param inputStream the stream to decode from
     * @throws NullPointerException if {@code inputStream} is {@code null}
     */
    public BufferedProtobufInputStream(InputStream inputStream) {
        this(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a buffered protobuf stream over the given source with an explicit buffer size.
     *
     * @param inputStream the stream to decode from
     * @param bufferSize  the read-ahead buffer size in bytes
     * @throws NullPointerException     if {@code inputStream} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is below {@link #MINIMUM_BUFFER_SIZE}
     */
    public BufferedProtobufInputStream(InputStream inputStream, int bufferSize) {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        if (bufferSize < MINIMUM_BUFFER_SIZE) {
            throw new IllegalArgumentException("bufferSize must be at least " + MINIMUM_BUFFER_SIZE + ": " + bufferSize);
        }
        this.root = this;
        this.inputStream = inputStream;
        this.buffer = new byte[bufferSize];
        this.bufferMark = -1;
        this.autoclose = true;
        this.length = -1;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opening buffered protobuf input stream, bufferSize={0}", bufferSize);
    }

    /**
     * Creates a buffered protobuf stream reading the file at {@code path}, opened with the default
     * options, using {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param path the file to decode from
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException          if the file cannot be opened for reading
     */
    public BufferedProtobufInputStream(Path path) throws IOException {
        this(Files.newInputStream(Objects.requireNonNull(path, "path cannot be null")));
    }

    /**
     * Creates a buffered protobuf stream reading the file at {@code path}, opened with the default
     * options, using an explicit buffer size.
     *
     * @param path       the file to decode from
     * @param bufferSize the read-ahead buffer size in bytes
     * @throws NullPointerException     if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is below {@link #MINIMUM_BUFFER_SIZE}
     * @throws IOException              if the file cannot be opened for reading
     */
    public BufferedProtobufInputStream(Path path, int bufferSize) throws IOException {
        this(Files.newInputStream(Objects.requireNonNull(path, "path cannot be null")), bufferSize);
    }

    /**
     * Creates a sub-stream bounded to {@code length} bytes, sharing the given root's buffer.
     *
     * @param root   the buffer and cursor owner to route byte reads through
     * @param length the byte length of the length-delimited region
     */
    private BufferedProtobufInputStream(BufferedProtobufInputStream root, long length) {
        this.root = root;
        this.inputStream = root.inputStream;
        this.buffer = root.buffer;
        this.bufferMark = -1;
        this.autoclose = false;
        this.length = length;
    }

    /**
     * Returns the next wire byte, refilling the root's buffer when it is drained and advancing the
     * bounded-extent counter when this is a sub-stream.
     *
     * @return the next byte, or {@code (byte) -1} at end of stream
     * @throws UncheckedIOException if the underlying read fails
     */
    @Override
    protected byte readByte() {
        if (length != -1) {
            position++;
        }
        return (byte) root.nextByte();
    }

    /**
     * Reads exactly {@code size} bytes into a fresh buffer.
     *
     * @param size the number of bytes to read
     * @return a {@link ByteBuffer} wrapping the bytes read
     */
    @Override
    protected ByteBuffer readBytes(int size) {
        if (length != -1) {
            position += size;
        }
        return ByteBuffer.wrap(root.readFully(size));
    }

    /**
     * Reads exactly {@code size} bytes as a lazily decoded string.
     *
     * @param size the number of bytes to read
     * @return the lazily decoded {@link ProtobufString.Lazy}
     */
    @Override
    protected ProtobufString.Lazy readString(int size) {
        if (length != -1) {
            position += size;
        }
        return ProtobufString.lazy(root.readFully(size), 0, size);
    }

    /**
     * Records the current position so a subsequent {@link #rewind()} can return to it.
     *
     * @implSpec The protobuf fast-path integer parser marks before speculatively reading a varint
     * and rewinds when it overflows, never more than {@link #MAX_REWIND_DISTANCE} bytes; this
     * implementation relies on that bound, so a rewind spanning more bytes than a refill retains is
     * not supported.
     */
    @Override
    protected void mark() {
        markedPosition = position;
        root.markBuffer();
    }

    /**
     * Returns the cursor to the position captured by the most recent {@link #mark()}.
     *
     * @throws InvalidMarkException if no live mark exists
     */
    @Override
    protected void rewind() {
        position = markedPosition;
        root.rewindBuffer();
    }

    /**
     * Reports whether the extent is fully consumed.
     *
     * @return {@code true} if no more bytes remain in this extent
     * @throws UncheckedIOException if the underlying read fails
     * @implNote A bounded sub-stream compares its consumed count against its length without touching
     * the buffer; an unbounded root stream peeks the buffer for a further byte, refilling but not
     * consuming.
     */
    @Override
    protected boolean isFinished() {
        if (length != -1) {
            return position >= length;
        }
        return root.bufferExhausted();
    }

    /**
     * Opens a sub-stream over the next {@code size} bytes, sharing this stream's root buffer.
     *
     * @param size the byte length of the length-delimited region
     * @return a sub-stream bounded to {@code size} bytes
     * @implNote The sub-stream routes byte reads through the same {@link #root}, so reading it drains
     * the same underlying cursor; this stream charges the region against its own extent up front so it
     * resumes correctly once the caller finishes the sub-stream.
     */
    @Override
    protected ProtobufInputStream subStream(int size) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "opening protobuf sub-stream, size={0}", size);
        if (length != -1) {
            position += size;
        }
        return new BufferedProtobufInputStream(root, size);
    }

    /**
     * Closes the underlying stream when this is a root stream; a sub-stream close is a no-op.
     *
     * @throws IOException if closing the underlying stream fails
     */
    @Override
    public void close() throws IOException {
        if (autoclose) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing buffered protobuf input stream");
            inputStream.close();
        }
    }

    /**
     * Returns the next byte as an unsigned value, refilling from the underlying stream when the
     * buffer is drained. Invoked on the {@link #root}.
     *
     * @return the next byte in {@code [0, 255]}, or {@code -1} at end of stream
     * @throws UncheckedIOException if the underlying read fails
     */
    private int nextByte() {
        try {
            if (bufferPosition >= bufferLimit && !fill()) {
                return -1;
            }
            return buffer[bufferPosition++] & 0xFF;
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "buffered protobuf input stream read failed", exception);
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Reads exactly {@code size} bytes, draining the buffer first and then reading any remainder
     * straight from the underlying stream to avoid copying large fields through the buffer. Invoked
     * on the {@link #root}.
     *
     * @param size the number of bytes to read
     * @return a new array of exactly {@code size} bytes
     * @throws IllegalArgumentException if {@code size} is negative
     * @throws UncheckedIOException     if the stream ends early or the underlying read fails
     */
    private byte[] readFully(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative: " + size);
        }
        try {
            var result = new byte[size];
            var fromBuffer = Math.min(size, bufferLimit - bufferPosition);
            if (fromBuffer > 0) {
                System.arraycopy(buffer, bufferPosition, result, 0, fromBuffer);
                bufferPosition += fromBuffer;
            }
            bufferMark = -1;
            var total = fromBuffer;
            while (total < size) {
                var read = inputStream.read(result, total, size - total);
                if (read == -1) {
                    throw new IOException("Unexpected end of stream: read " + total + ", expected " + size);
                }
                total += read;
            }
            return result;
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "buffered protobuf input stream read failed for size=" + size, exception);
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Marks the current buffer position as the target of the next {@link #rewindBuffer()}. Invoked
     * on the {@link #root}.
     */
    private void markBuffer() {
        bufferMark = bufferPosition;
    }

    /**
     * Returns the buffer cursor to the marked position. Invoked on the {@link #root}.
     *
     * @throws InvalidMarkException if no live mark exists
     */
    private void rewindBuffer() {
        if (bufferMark == -1) {
            throw new InvalidMarkException();
        }
        bufferPosition = bufferMark;
    }

    /**
     * Reports whether the underlying stream has no further bytes, refilling to peek without
     * consuming. Invoked on the {@link #root}.
     *
     * @return {@code true} if no more bytes can be read
     * @throws UncheckedIOException if the underlying read fails
     */
    private boolean bufferExhausted() {
        try {
            return bufferPosition >= bufferLimit && !fill();
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "buffered protobuf input stream end-of-stream check failed", exception);
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Refills the buffer with the next block, retaining the bounded rewind window. Invoked on the
     * {@link #root}.
     *
     * <p>Up to {@link #MAX_REWIND_DISTANCE} already-consumed bytes immediately before the cursor are
     * shifted to the front so a pending varint rewind can still replay them; older bytes are
     * discarded. A mark that falls outside the retained window is invalidated, which is safe because
     * the decoder never rewinds further than a single varint. Bounding the retained span keeps a long
     * run of buffer-bypassing reads from pinning the whole buffer.
     *
     * @return {@code true} if at least one fresh byte was read, {@code false} at end of stream
     * @throws IOException if the underlying read fails
     */
    private boolean fill() throws IOException {
        var keep = Math.min(bufferPosition, MAX_REWIND_DISTANCE);
        var start = bufferPosition - keep;
        if (start > 0) {
            System.arraycopy(buffer, start, buffer, 0, bufferLimit - start);
            bufferLimit -= start;
            bufferPosition -= start;
            bufferMark = bufferMark >= start ? bufferMark - start : -1;
        }
        var read = inputStream.read(buffer, bufferLimit, buffer.length - bufferLimit);
        if (read <= 0) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "buffer refill reached end of stream");
            return false;
        }
        bufferLimit += read;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "refilled buffer, read={0} bytes", read);
        return true;
    }
}
