package com.github.auties00.cobalt.util;

import com.github.auties00.cobalt.telemetry.log.Log;
import it.auties.protobuf.stream.ProtobufOutputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link ProtobufOutputStream} that writes its wire bytes through an internal write-behind buffer.
 *
 * <p>The stream-backed encoder shipped with the protobuf library issues a separate underlying
 * {@link OutputStream#write} for every tag, varint, and field it emits; it does no buffering of its
 * own. When the sink is a file, socket, or cipher stream, that turns the encode of a single message
 * into many small writes, one syscall each. This subclass keeps the exact same encoding contract but
 * accumulates those writes in a {@code bufferSize}-byte block that is flushed in one underlying write
 * when it fills, collapsing the small writes into block writes.
 *
 * <p>The buffer is integrated directly rather than by wrapping a {@link java.io.BufferedOutputStream},
 * so there is no second decorator object and no per-write locking. Writes longer than the buffer
 * bypass it and go straight to the sink to avoid a redundant copy. Because the buffer holds bytes
 * that have not yet reached the sink, the encoder must be {@link #close() closed} (ideally with
 * try-with-resources) to flush the trailing block, or its tail output is lost.
 *
 * <p>This implementation performs no locking and is therefore suitable only for single-threaded
 * sequential encoding; it must not be shared across threads.
 *
 * @apiNote
 * Construct one in place of {@link ProtobufOutputStream#toStream(OutputStream)} when encoding to an
 * I/O sink, for example
 * {@snippet :
 * try (var output = new BufferedProtobufOutputStream(file)) {
 *     SomeSpec.encode(value, output);
 * }
 * }
 * There is nothing to gain over {@link ProtobufOutputStream#toBytes(int)} when the result is wanted
 * as an in-memory array; reserve this for file, network, and cipher sinks.
 */
// TODO: Delete me when we migrate to Daedalus
public final class BufferedProtobufOutputStream extends ProtobufOutputStream<OutputStream> implements Closeable {
    /**
     * The logger for {@link BufferedProtobufOutputStream}.
     */
    private static final System.Logger LOGGER = Log.get(BufferedProtobufOutputStream.class);

    /**
     * The buffer size used by {@link #BufferedProtobufOutputStream(OutputStream)} and {@link #BufferedProtobufOutputStream(Path)} when no
     * explicit size is given, matching the conventional 8 KiB block size of the JDK stream decorators.
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The sink the buffer is flushed to and that {@link #close()} closes.
     */
    private final OutputStream outputStream;

    /**
     * The write-behind buffer accumulating bytes until it fills or the stream is closed.
     */
    private final byte[] buffer;

    /**
     * The number of bytes currently held in {@link #buffer}, starting at index zero.
     */
    private int position;

    /**
     * Creates a buffered protobuf encoder over the given sink using {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param outputStream the sink to write to
     * @throws NullPointerException if {@code outputStream} is {@code null}
     */
    public BufferedProtobufOutputStream(OutputStream outputStream) {
        this(outputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a buffered protobuf encoder over the given sink with an explicit buffer size.
     *
     * @param outputStream the sink to write to
     * @param bufferSize   the buffer size in bytes
     * @throws NullPointerException     if {@code outputStream} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is not positive
     */
    public BufferedProtobufOutputStream(OutputStream outputStream, int bufferSize) {
        Objects.requireNonNull(outputStream, "outputStream cannot be null");
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive: " + bufferSize);
        }
        this.outputStream = outputStream;
        this.buffer = new byte[bufferSize];
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opening buffered protobuf output stream, bufferSize={0}", bufferSize);
    }

    /**
     * Creates a buffered protobuf encoder writing to the file at {@code path}, opened for writing
     * with the default options, using {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param path the file to write to
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException          if the file cannot be opened for writing
     */
    public BufferedProtobufOutputStream(Path path) throws IOException {
        this(Files.newOutputStream(Objects.requireNonNull(path, "path cannot be null")));
    }

    /**
     * Creates a buffered protobuf encoder writing to the file at {@code path}, opened for writing
     * with the default options, using an explicit buffer size.
     *
     * @param path       the file to write to
     * @param bufferSize the buffer size in bytes
     * @throws NullPointerException     if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is not positive
     * @throws IOException              if the file cannot be opened for writing
     */
    public BufferedProtobufOutputStream(Path path, int bufferSize) throws IOException {
        this(Files.newOutputStream(Objects.requireNonNull(path, "path cannot be null")), bufferSize);
    }

    /**
     * Appends a single byte, flushing the buffer first when it is full.
     *
     * @param entry the byte to write
     * @throws UncheckedIOException if flushing the buffer to the sink fails
     */
    @Override
    public void writeRaw(byte entry) {
        try {
            if (position == buffer.length) {
                flushBuffer();
            }
            buffer[position++] = entry;
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "buffered protobuf output stream write failed", exception);
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Appends the entire given array.
     *
     * @param entry the bytes to write
     * @throws UncheckedIOException if flushing the buffer to the sink fails
     */
    @Override
    public void writeRaw(byte[] entry) {
        writeRaw(entry, 0, entry.length);
    }

    /**
     * Appends a slice of the given array, bypassing the buffer for slices at least as large as it to
     * avoid a redundant copy.
     *
     * @param entry  the source array
     * @param offset the starting offset within the source
     * @param length the number of bytes to write
     * @throws UncheckedIOException if flushing the buffer to the sink fails
     */
    @Override
    public void writeRaw(byte[] entry, int offset, int length) {
        try {
            if (length >= buffer.length) {
                flushBuffer();
                outputStream.write(entry, offset, length);
                return;
            }
            if (length > buffer.length - position) {
                flushBuffer();
            }
            System.arraycopy(entry, offset, buffer, position, length);
            position += length;
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "buffered protobuf output stream write failed for length=" + length, exception);
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Appends the remaining bytes of the given buffer without consuming it, copying in buffer-sized
     * chunks and flushing between them.
     *
     * @param entry the source buffer, read from its current position to its limit
     * @throws UncheckedIOException if flushing the buffer to the sink fails
     */
    @Override
    public void writeRaw(ByteBuffer entry) {
        try {
            var sourcePosition = entry.position();
            var remaining = entry.remaining();
            var written = 0;
            while (written < remaining) {
                if (position == buffer.length) {
                    flushBuffer();
                }
                var chunk = Math.min(remaining - written, buffer.length - position);
                entry.get(sourcePosition + written, buffer, position, chunk);
                position += chunk;
                written += chunk;
            }
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "buffered protobuf output stream write failed", exception);
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Returns the underlying sink.
     *
     * @return the sink; note that bytes buffered but not yet flushed are not visible on it until
     * {@link #close()}
     */
    @Override
    public OutputStream toOutput() {
        return outputStream;
    }

    /**
     * Flushes the buffered bytes and closes the underlying sink.
     *
     * @throws IOException if flushing or closing the underlying stream fails
     */
    @Override
    public void close() throws IOException {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing buffered protobuf output stream");
        try {
            flushBuffer();
            outputStream.close();
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "buffered protobuf output stream close failed", exception);
            throw exception;
        }
    }

    /**
     * Writes the buffered bytes to the sink and resets the buffer.
     *
     * @throws IOException if the underlying write fails
     */
    private void flushBuffer() throws IOException {
        if (position > 0) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "flushing buffered protobuf output stream, bytes={0}", position);
            outputStream.write(buffer, 0, position);
            position = 0;
        }
    }
}
