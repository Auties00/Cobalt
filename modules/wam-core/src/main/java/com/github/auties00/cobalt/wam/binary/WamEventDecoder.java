package com.github.auties00.cobalt.wam.binary;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

import static com.github.auties00.cobalt.wam.binary.WamTags.*;

/**
 * A sink-agnostic decoder for the WhatsApp Metrics (WAM) custom binary
 * protocol, mirror of {@link WamEventEncoder}.
 *
 * <p>Two implementations are nested as private permitted subclasses:
 *
 * <ul>
 *   <li>{@code ByteArray} — reads from a byte array slice.
 *   <li>{@code Stream} — streams from an {@link InputStream}.
 * </ul>
 *
 * <p>Construct either via the static {@code fromBytes(...)} or
 * {@code fromStream(...)} factories; the {@code fromBufferedStream(...)} factories
 * wrap the source in a {@link BufferedInputStream} and take ownership of it, so
 * the decoder should be {@link #close() closed} to release it.
 *
 * <p>Tag bytes returned by {@link #readHeader} are packed into a single
 * {@code int} alongside the field identifier:
 * <pre>{@code
 *     bits  0-15: fieldId (uint16)
 *     bits 16-23: raw tag byte (role + LAST + WIDE_ID flags + value-type bits)
 * }</pre>
 *
 * Static helpers ({@link #fieldIdOf}, {@link #valueTypeOf},
 * {@link #isLast}) unpack the components.
 *
 * @see WamEventEncoder
 * @see WamTags
 */
public abstract sealed class WamEventDecoder
        implements AutoCloseable
        permits WamEventDecoder.ByteArray, WamEventDecoder.Stream {

    private static final VarHandle SHORT_HANDLE =
            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle INT_HANDLE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle LONG_HANDLE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * Constructs a new decoder. Subclasses should not perform any
     * initialisation that depends on the source being readable.
     */
    private WamEventDecoder() {
    }

    /**
     * Returns a decoder reading from the entire given byte array.
     *
     * @param source the source array, must not be {@code null}
     * @return a new byte-array-backed decoder
     */
    public static WamEventDecoder fromBytes(byte[] source) {
        return new ByteArray(source, 0, source.length);
    }

    /**
     * Returns a decoder reading from the given byte array slice
     * starting at {@code offset} for at most {@code length} bytes.
     *
     * @param source the source array, must not be {@code null}
     * @param offset the starting offset within the array
     * @param length the maximum number of bytes that may be read
     * @return a new byte-array-backed decoder
     */
    public static WamEventDecoder fromBytes(byte[] source, int offset, int length) {
        return new ByteArray(source, offset, length);
    }

    /**
     * Returns a decoder reading from the given input stream with no
     * size limit.
     *
     * <p>The decoder does not buffer; it reads tag and header bytes one at a
     * time from {@code source}. Prefer {@link #fromBufferedStream(InputStream)}
     * when the source is an unbuffered file, socket, or decompression stream.
     *
     * @param source the source stream, must not be {@code null}
     * @return a new stream-backed decoder
     */
    public static WamEventDecoder fromStream(InputStream source) {
        return new Stream(source, -1);
    }

    /**
     * Returns a decoder reading from the given input stream with the
     * given total byte budget.
     *
     * @param source the source stream, must not be {@code null}
     * @param length the byte budget; pass a negative value for no limit
     * @return a new stream-backed decoder
     */
    public static WamEventDecoder fromStream(InputStream source, int length) {
        return new Stream(source, length);
    }

    /**
     * Returns a decoder reading from the given input stream through a
     * {@link BufferedInputStream} of the default size, so the decoder's
     * byte-at-a-time header reads are served from memory rather than one
     * underlying read each.
     *
     * <p>The decoder takes ownership of {@code source}: {@link #close()} closes
     * the underlying stream.
     *
     * @param source the source stream, must not be {@code null}
     * @return a new buffered stream-backed decoder
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static WamEventDecoder fromBufferedStream(InputStream source) {
        Objects.requireNonNull(source, "source cannot be null");
        return new Stream(new BufferedInputStream(source), -1);
    }

    /**
     * Returns a decoder reading from the given input stream through a
     * {@link BufferedInputStream} of the given size.
     *
     * <p>The decoder takes ownership of {@code source}: {@link #close()} closes
     * the underlying stream.
     *
     * @param source     the source stream, must not be {@code null}
     * @param bufferSize the buffer size in bytes
     * @return a new buffered stream-backed decoder
     * @throws NullPointerException     if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is not positive
     */
    public static WamEventDecoder fromBufferedStream(InputStream source, int bufferSize) {
        Objects.requireNonNull(source, "source cannot be null");
        return new Stream(new BufferedInputStream(source, bufferSize), -1);
    }

    /**
     * Returns the number of bytes consumed so far.
     *
     * @return the number of bytes read
     */
    public abstract int read();

    /**
     * Returns whether at least one more byte is available from the
     * source.
     *
     * @return {@code true} if the next {@link #readHeader} call would
     *         succeed, {@code false} at EOF
     */
    public abstract boolean hasMore();

    /**
     * Reads a tag byte plus its field identifier and returns the pair
     * packed into an {@code int} (see class-level docs for layout).
     *
     * @return the packed header
     */
    public abstract int readHeader();

    /**
     * Reads an integer payload sized according to the value-type bits
     * of the given header.
     *
     * @param header the packed header returned by {@link #readHeader}
     * @return the decoded integer value
     */
    public abstract long readInt(int header);

    /**
     * Reads an 8-byte little-endian float64 payload.
     *
     * @param header the packed header returned by {@link #readHeader}
     * @return the decoded double value
     */
    public abstract double readFloat(int header);

    /**
     * Reads a length-prefixed UTF-8 string payload.
     *
     * @param header the packed header returned by {@link #readHeader}
     * @return the decoded string, never {@code null}
     */
    public abstract String readString(int header);

    /**
     * Skips the payload bytes for an unknown or unwanted field.
     *
     * @param header the packed header returned by {@link #readHeader}
     */
    public abstract void skip(int header);

    /**
     * Releases the source.
     *
     * <p>For a byte-array source this is a no-op. For a stream source it closes
     * the underlying {@link InputStream}, including the {@link BufferedInputStream}
     * interposed by {@link #fromBufferedStream(InputStream)}.
     *
     * @throws UncheckedIOException if closing the underlying stream fails
     */
    @Override
    public abstract void close();

    /**
     * Returns the field (or event) identifier from a packed header.
     *
     * @param header the packed header
     * @return the numeric identifier
     */
    public static int fieldIdOf(int header) {
        return header & 0xFFFF;
    }

    /**
     * Returns the value-type bits (upper nibble of the tag byte) from
     * a packed header.
     *
     * @param header the packed header
     * @return one of the {@code VALUE_*} constants in {@link WamTags}
     */
    public static int valueTypeOf(int header) {
        return (header >>> 16) & 0xF0;
    }

    /**
     * Returns whether the {@link WamTags#LAST} flag is set on the tag
     * byte.
     *
     * @param header the packed header
     * @return {@code true} if no more entries follow in this group
     */
    public static boolean isLast(int header) {
        return ((header >>> 16) & LAST) != 0;
    }

    /**
     * Reads a run of field entries into a map keyed by field identifier,
     * decoding each value purely from its wire representation.
     *
     * <p>The semantic type of a field cannot be recovered from the wire
     * (integers, booleans, timers, and enums share the integer forms), so
     * each value is stored as the matching {@link WamWireValue}: a
     * float64 becomes {@link WamWireValue.WamFloat}, a length-prefixed
     * string becomes {@link WamWireValue.WamString}, and every integer form
     * becomes {@link WamWireValue.WamInt}. Null-valued entries carry no
     * payload and are omitted from the result. The returned map is sorted
     * by field identifier so a subsequent {@link WamEventEncoder#writeEvent}
     * emits fields in a deterministic order.
     *
     * @param decoder   the source decoder positioned at the first field
     *                  header, must not be {@code null}
     * @param hasFields {@code true} if at least one field follows the event
     *                  marker; {@code false} for an empty event
     * @return a sorted, concurrent map of field identifier to decoded value
     */
    public static NavigableMap<Integer, WamWireValue> readFields(WamEventDecoder decoder, boolean hasFields) {
        var fields = new ConcurrentSkipListMap<Integer, WamWireValue>();
        while (hasFields) {
            var header = decoder.readHeader();
            var fieldId = fieldIdOf(header);
            WamWireValue value = switch (valueTypeOf(header)) {
                case VALUE_NULL -> null;
                case VALUE_FLOAT64 -> new WamWireValue.WamFloat(decoder.readFloat(header));
                case VALUE_STR8, VALUE_STR16, VALUE_STR32 -> new WamWireValue.WamString(decoder.readString(header));
                default -> new WamWireValue.WamInt(decoder.readInt(header));
            };
            if (value != null) {
                fields.put(fieldId, value);
            }
            hasFields = !isLast(header);
        }
        return fields;
    }

    /**
     * Packs a raw tag byte and a field identifier into the standard
     * header layout used by {@link #readHeader}.
     *
     * @param rawTag  the original tag byte
     * @param fieldId the resolved field identifier
     * @return the packed header
     */
    private static int packHeader(int rawTag, int fieldId) {
        return (fieldId & 0xFFFF) | ((rawTag & 0xFF) << 16);
    }

    /**
     * Byte-array-backed decoder using {@link VarHandle} loads for
     * multi-byte little-endian reads.
     */
    private static final class ByteArray extends WamEventDecoder {
        private final byte[] buffer;
        private final int start;
        private final int limit;
        private int offset;

        private ByteArray(byte[] buffer, int offset, int length) {
            Objects.requireNonNull(buffer, "buffer cannot be null");
            Objects.checkFromIndexSize(offset, length, buffer.length);
            this.buffer = buffer;
            this.start = offset;
            this.limit = offset + length;
            this.offset = offset;
        }

        @Override
        public int read() {
            return offset - start;
        }

        @Override
        public boolean hasMore() {
            return offset < limit;
        }

        @Override
        public int readHeader() {
            ensureAvailable(1);
            var rawTag = buffer[offset++] & 0xFF;
            int fieldId;
            if ((rawTag & WIDE_ID) != 0) {
                ensureAvailable(2);
                fieldId = (int) SHORT_HANDLE.get(buffer, offset) & 0xFFFF;
                offset += 2;
            } else {
                ensureAvailable(1);
                fieldId = buffer[offset++] & 0xFF;
            }
            return packHeader(rawTag, fieldId);
        }

        @Override
        public long readInt(int header) {
            return switch (valueTypeOf(header)) {
                case VALUE_INT_0 -> 0L;
                case VALUE_INT_1 -> 1L;
                case VALUE_INT8 -> {
                    ensureAvailable(1);
                    yield (long) buffer[offset++];
                }
                case VALUE_INT16 -> {
                    ensureAvailable(2);
                    var value = (short) SHORT_HANDLE.get(buffer, offset);
                    offset += 2;
                    yield (long) value;
                }
                case VALUE_INT32 -> {
                    ensureAvailable(4);
                    var value = (int) INT_HANDLE.get(buffer, offset);
                    offset += 4;
                    yield (long) value;
                }
                case VALUE_INT64 -> {
                    ensureAvailable(8);
                    var value = (long) LONG_HANDLE.get(buffer, offset);
                    offset += 8;
                    yield value;
                }
                case VALUE_FLOAT64 -> {
                    ensureAvailable(8);
                    var bits = (long) LONG_HANDLE.get(buffer, offset);
                    offset += 8;
                    yield (long) Double.longBitsToDouble(bits);
                }
                default -> throw new IllegalStateException(
                        "Cannot read int from value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            };
        }

        @Override
        public double readFloat(int header) {
            if (valueTypeOf(header) != VALUE_FLOAT64) {
                throw new IllegalStateException(
                        "Cannot read float from value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            }
            ensureAvailable(8);
            var bits = (long) LONG_HANDLE.get(buffer, offset);
            offset += 8;
            return Double.longBitsToDouble(bits);
        }

        @Override
        public String readString(int header) {
            var length = switch (valueTypeOf(header)) {
                case VALUE_STR8 -> {
                    ensureAvailable(1);
                    yield buffer[offset++] & 0xFF;
                }
                case VALUE_STR16 -> {
                    ensureAvailable(2);
                    var len = (int) SHORT_HANDLE.get(buffer, offset) & 0xFFFF;
                    offset += 2;
                    yield len;
                }
                case VALUE_STR32 -> {
                    ensureAvailable(4);
                    var len = (int) INT_HANDLE.get(buffer, offset);
                    offset += 4;
                    yield len;
                }
                default -> throw new IllegalStateException(
                        "Cannot read string from value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            };
            ensureAvailable(length);
            var value = new String(buffer, offset, length, StandardCharsets.UTF_8);
            offset += length;
            return value;
        }

        @Override
        public void skip(int header) {
            switch (valueTypeOf(header)) {
                case VALUE_NULL, VALUE_INT_0, VALUE_INT_1 -> {}
                case VALUE_INT8 -> advance(1);
                case VALUE_INT16 -> advance(2);
                case VALUE_INT32 -> advance(4);
                case VALUE_INT64, VALUE_FLOAT64 -> advance(8);
                case VALUE_STR8 -> {
                    ensureAvailable(1);
                    var len = buffer[offset++] & 0xFF;
                    advance(len);
                }
                case VALUE_STR16 -> {
                    ensureAvailable(2);
                    var len = (int) SHORT_HANDLE.get(buffer, offset) & 0xFFFF;
                    offset += 2;
                    advance(len);
                }
                case VALUE_STR32 -> {
                    ensureAvailable(4);
                    var len = (int) INT_HANDLE.get(buffer, offset);
                    offset += 4;
                    advance(len);
                }
                default -> throw new IllegalStateException(
                        "Unknown value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            }
        }

        private void advance(int n) {
            ensureAvailable(n);
            offset += n;
        }

        private void ensureAvailable(int n) {
            if (offset + n > limit) {
                throw new IndexOutOfBoundsException(
                        "WAM decode would underflow buffer: need " + n
                                + " more bytes at offset " + offset
                                + ", limit " + limit);
            }
        }

        @Override
        public void close() {
            // A byte-array source holds no resource to release.
        }
    }

    /**
     * Stream-backed decoder that uses an internal 8-byte scratch buffer
     * for staged little-endian reads.
     */
    private static final class Stream extends WamEventDecoder {
        private final InputStream in;
        private final byte[] scratch;
        private final int limit;
        private int read;
        private int peeked;

        private Stream(InputStream in, int limit) {
            this.in = Objects.requireNonNull(in, "in cannot be null");
            this.scratch = new byte[8];
            this.limit = limit;
            this.read = 0;
            this.peeked = -1;
        }

        @Override
        public int read() {
            return read;
        }

        @Override
        public boolean hasMore() {
            if (limit >= 0 && read >= limit) {
                return false;
            }
            if (peeked != -1) {
                return true;
            }
            try {
                peeked = in.read();
                return peeked != -1;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public int readHeader() {
            var rawTag = nextByte() & 0xFF;
            int fieldId;
            if ((rawTag & WIDE_ID) != 0) {
                readFully(scratch, 0, 2);
                fieldId = (int) SHORT_HANDLE.get(scratch, 0) & 0xFFFF;
            } else {
                fieldId = nextByte() & 0xFF;
            }
            return packHeader(rawTag, fieldId);
        }

        @Override
        public long readInt(int header) {
            return switch (valueTypeOf(header)) {
                case VALUE_INT_0 -> 0L;
                case VALUE_INT_1 -> 1L;
                case VALUE_INT8 -> (long) (byte) nextByte();
                case VALUE_INT16 -> {
                    readFully(scratch, 0, 2);
                    yield (long) (short) SHORT_HANDLE.get(scratch, 0);
                }
                case VALUE_INT32 -> {
                    readFully(scratch, 0, 4);
                    yield (long) (int) INT_HANDLE.get(scratch, 0);
                }
                case VALUE_INT64 -> {
                    readFully(scratch, 0, 8);
                    yield (long) LONG_HANDLE.get(scratch, 0);
                }
                case VALUE_FLOAT64 -> {
                    readFully(scratch, 0, 8);
                    yield (long) Double.longBitsToDouble((long) LONG_HANDLE.get(scratch, 0));
                }
                default -> throw new IllegalStateException(
                        "Cannot read int from value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            };
        }

        @Override
        public double readFloat(int header) {
            if (valueTypeOf(header) != VALUE_FLOAT64) {
                throw new IllegalStateException(
                        "Cannot read float from value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            }
            readFully(scratch, 0, 8);
            return Double.longBitsToDouble((long) LONG_HANDLE.get(scratch, 0));
        }

        @Override
        public String readString(int header) {
            var length = switch (valueTypeOf(header)) {
                case VALUE_STR8 -> nextByte() & 0xFF;
                case VALUE_STR16 -> {
                    readFully(scratch, 0, 2);
                    yield (int) SHORT_HANDLE.get(scratch, 0) & 0xFFFF;
                }
                case VALUE_STR32 -> {
                    readFully(scratch, 0, 4);
                    yield (int) INT_HANDLE.get(scratch, 0);
                }
                default -> throw new IllegalStateException(
                        "Cannot read string from value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            };
            var bytes = new byte[length];
            readFully(bytes, 0, length);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public void skip(int header) {
            switch (valueTypeOf(header)) {
                case VALUE_NULL, VALUE_INT_0, VALUE_INT_1 -> {}
                case VALUE_INT8 -> nextByte();
                case VALUE_INT16 -> readFully(scratch, 0, 2);
                case VALUE_INT32 -> readFully(scratch, 0, 4);
                case VALUE_INT64, VALUE_FLOAT64 -> readFully(scratch, 0, 8);
                case VALUE_STR8 -> {
                    var len = nextByte() & 0xFF;
                    skipFully(len);
                }
                case VALUE_STR16 -> {
                    readFully(scratch, 0, 2);
                    var len = (int) SHORT_HANDLE.get(scratch, 0) & 0xFFFF;
                    skipFully(len);
                }
                case VALUE_STR32 -> {
                    readFully(scratch, 0, 4);
                    var len = (int) INT_HANDLE.get(scratch, 0);
                    skipFully(len);
                }
                default -> throw new IllegalStateException(
                        "Unknown value-type 0x"
                                + Integer.toHexString(valueTypeOf(header)));
            }
        }

        private int nextByte() {
            ensureBudget(1);
            try {
                int b;
                if (peeked != -1) {
                    b = peeked;
                    peeked = -1;
                } else {
                    b = in.read();
                    if (b < 0) {
                        throw new UncheckedIOException(new EOFException("WAM stream ended early"));
                    }
                }
                read++;
                return b;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void readFully(byte[] dst, int offset, int length) {
            ensureBudget(length);
            try {
                var pos = 0;
                if (peeked != -1) {
                    dst[offset] = (byte) peeked;
                    peeked = -1;
                    pos = 1;
                    read++;
                }
                while (pos < length) {
                    var n = in.read(dst, offset + pos, length - pos);
                    if (n < 0) {
                        throw new UncheckedIOException(new EOFException("WAM stream ended early"));
                    }
                    pos += n;
                    read += n;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void skipFully(int n) {
            ensureBudget(n);
            try {
                var remaining = n;
                if (peeked != -1) {
                    peeked = -1;
                    remaining--;
                    read++;
                }
                while (remaining > 0) {
                    var skipped = in.skip(remaining);
                    if (skipped <= 0) {
                        if (in.read() < 0) {
                            throw new UncheckedIOException(new EOFException("WAM stream ended early"));
                        }
                        remaining--;
                        read++;
                    } else {
                        remaining -= (int) skipped;
                        read += (int) skipped;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void ensureBudget(int n) {
            if (limit >= 0 && read + n > limit) {
                throw new IndexOutOfBoundsException(
                        "WAM decode would exceed stream budget: need " + n
                                + " more bytes after " + read
                                + ", limit " + limit);
            }
        }

        @Override
        public void close() {
            try {
                in.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
