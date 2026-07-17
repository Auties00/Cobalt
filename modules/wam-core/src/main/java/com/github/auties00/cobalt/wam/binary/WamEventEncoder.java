package com.github.auties00.cobalt.wam.binary;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.NavigableMap;
import java.util.Objects;

import static com.github.auties00.cobalt.wam.binary.WamTags.*;

/**
 * A sink-agnostic encoder for the WhatsApp Metrics (WAM) custom binary
 * protocol.
 *
 * <p>Two implementations are nested as private permitted subclasses:
 *
 * <ul>
 *   <li>{@code ByteArray} — writes into a pre-allocated {@code byte[]},
 *       the fast path used by the upload buffer in {@code WamService}.
 *   <li>{@code Stream} — streams directly into an {@link OutputStream},
 *       used by the persistence path so that events can be written to
 *       a file without holding the full buffer in memory.
 * </ul>
 *
 * <p>Construct either via the static {@code toBytes(...)} or
 * {@code toStream(...)} factories; the {@code toBufferedStream(...)} factories
 * wrap the sink in a {@link BufferedOutputStream} and take ownership of it, so
 * the encoder must be {@link #close() closed} to flush and release it.
 *
 * <p>The event-layer convenience methods ({@link #writeEventMarker},
 * {@link #writeIntField}, {@link #writeBoolField},
 * {@link #writeStringField}, {@link #writeFloatField}) are declared
 * {@code final} on this class so that the generated {@code *Impl}
 * bodies need only call into one type regardless of sink.
 *
 * <p>Use {@link WamEventSizes} to compute the byte count required by an
 * encode operation when pre-allocating a fixed-size sink.
 *
 * @see WamEventSizes
 * @see WamEventDecoder
 * @see WamTags
 */
public abstract sealed class WamEventEncoder
        implements AutoCloseable
        permits WamEventEncoder.ByteArray, WamEventEncoder.Stream {

    private static final VarHandle SHORT_HANDLE =
            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle INT_HANDLE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle LONG_HANDLE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * Constructs a new encoder. Subclasses should not perform any
     * initialisation that depends on the sink being writable.
     */
    private WamEventEncoder() {
    }

    /**
     * Returns an encoder writing into the entire given byte array.
     *
     * @param destination the destination array, must not be {@code null}
     * @return a new byte-array-backed encoder
     */
    public static WamEventEncoder toBytes(byte[] destination) {
        return new ByteArray(destination, 0, destination.length);
    }

    /**
     * Returns an encoder writing into the given byte array slice
     * starting at {@code offset} for at most {@code length} bytes.
     *
     * @param destination the destination array, must not be {@code null}
     * @param offset      the starting offset within the array
     * @param length      the maximum number of bytes that may be written
     * @return a new byte-array-backed encoder
     */
    public static WamEventEncoder toBytes(byte[] destination, int offset, int length) {
        return new ByteArray(destination, offset, length);
    }

    /**
     * Returns an encoder writing into the given output stream with no
     * size limit.
     *
     * <p>The encoder does not buffer; each value is written straight to
     * {@code destination} in small chunks. Prefer {@link #toBufferedStream(OutputStream)}
     * when the destination is an unbuffered file, socket, or cipher stream.
     *
     * @param destination the destination stream, must not be {@code null}
     * @return a new stream-backed encoder
     */
    public static WamEventEncoder toStream(OutputStream destination) {
        return new Stream(destination, -1);
    }

    /**
     * Returns an encoder writing into the given output stream with the
     * given total byte budget.
     *
     * <p>Once {@code length} bytes have been written, any further write
     * will throw {@link IndexOutOfBoundsException} so that callers can
     * verify they wrote exactly the size they pre-computed via
     * {@link WamEventSizes}.
     *
     * @param destination the destination stream, must not be {@code null}
     * @param length      the byte budget; pass a negative value for no
     *                    limit
     * @return a new stream-backed encoder
     */
    public static WamEventEncoder toStream(OutputStream destination, int length) {
        return new Stream(destination, length);
    }

    /**
     * Returns an encoder writing into the given output stream through a
     * {@link BufferedOutputStream} of the default size, coalescing the
     * encoder's many small writes into block writes.
     *
     * <p>The encoder takes ownership of {@code destination}: {@link #close()}
     * flushes the buffer and closes the underlying stream, so the encoder must
     * be closed (ideally with try-with-resources) or buffered output is lost.
     *
     * @param destination the destination stream, must not be {@code null}
     * @return a new buffered stream-backed encoder
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public static WamEventEncoder toBufferedStream(OutputStream destination) {
        Objects.requireNonNull(destination, "destination cannot be null");
        return new Stream(new BufferedOutputStream(destination), -1);
    }

    /**
     * Returns an encoder writing into the given output stream through a
     * {@link BufferedOutputStream} of the given size.
     *
     * <p>The encoder takes ownership of {@code destination}: {@link #close()}
     * flushes the buffer and closes the underlying stream, so the encoder must
     * be closed (ideally with try-with-resources) or buffered output is lost.
     *
     * @param destination the destination stream, must not be {@code null}
     * @param bufferSize  the buffer size in bytes
     * @return a new buffered stream-backed encoder
     * @throws NullPointerException     if {@code destination} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is not positive
     */
    public static WamEventEncoder toBufferedStream(OutputStream destination, int bufferSize) {
        Objects.requireNonNull(destination, "destination cannot be null");
        return new Stream(new BufferedOutputStream(destination, bufferSize), -1);
    }

    /**
     * Returns the number of bytes written so far through this encoder.
     *
     * @return the number of bytes written
     */
    public abstract int written();

    /**
     * Writes a tag (flags byte + field identifier) into the sink.
     *
     * @param fieldId the numeric field or event identifier
     * @param flags   the pre-computed flags byte (role + value-type +
     *                {@link WamTags#LAST LAST})
     */
    public abstract void writeTag(int fieldId, int flags);

    /**
     * Writes an integer value entry (tag + payload) into the sink.
     *
     * @param fieldId the numeric field identifier
     * @param flags   the role and continuation flags
     * @param value   the integer value to encode
     */
    public abstract void writeInt(int fieldId, int flags, long value);

    /**
     * Writes a float (double) value entry into the sink.
     *
     * @param fieldId the numeric field identifier
     * @param flags   the role and continuation flags
     * @param value   the double-precision value to encode
     */
    public abstract void writeFloat(int fieldId, int flags, double value);

    /**
     * Writes a UTF-8 string value entry (tag + length prefix + payload)
     * into the sink.
     *
     * @param fieldId the numeric field identifier
     * @param flags   the role and continuation flags
     * @param value   the string to encode, must not be {@code null}
     */
    public abstract void writeString(int fieldId, int flags, String value);

    /**
     * Writes a verbatim slice of pre-encoded bytes into the sink.
     *
     * @param source the source array, must not be {@code null}
     * @param offset the starting offset within the source
     * @param length the number of bytes to copy
     */
    public abstract void writeRaw(byte[] source, int offset, int length);

    /**
     * Flushes any buffered output and releases the sink.
     *
     * <p>For a byte-array sink this is a no-op. For a stream sink it closes the
     * underlying {@link OutputStream}; when the encoder was created through
     * {@link #toBufferedStream(OutputStream)} this also flushes the buffer, so a
     * buffered encoder must be closed or its trailing output is lost.
     *
     * @throws UncheckedIOException if closing the underlying stream fails
     */
    @Override
    public abstract void close();

    /**
     * Writes a null value entry (tag-only) into the sink when the
     * role is {@link WamTags#GLOBAL}, otherwise emits nothing.
     *
     * <p>Matches {@code WAWebWamLibProtocol}'s shared write helper,
     * which uses the predicate {@code if (value == null) role ===
     * GLOBAL && writeTag(...);} — only the GLOBAL role's
     * dirty-tracking null-transition path emits bytes for a null
     * value. FIELD and EVENT roles with a null value are a no-op
     * so callers can defensively invoke {@code writeNull} regardless
     * of role without padding the wire output.
     *
     * @param fieldId the numeric field identifier
     * @param flags   the role and continuation flags
     *                (e.g. {@link WamTags#GLOBAL GLOBAL})
     */
    public final void writeNull(int fieldId, int flags) {
        if ((flags & 0x03) != GLOBAL) {
            return;
        }
        writeTag(fieldId, flags | VALUE_NULL);
    }

    /**
     * Writes an event marker (event id + negative weight payload) into
     * the sink.
     *
     * @param eventId   the numeric event identifier
     * @param weight    the sampling weight (written as {@code -weight})
     * @param hasFields {@code true} if at least one field follows
     */
    public final void writeEventMarker(int eventId, int weight, boolean hasFields) {
        writeInt(eventId, EVENT | (hasFields ? 0 : LAST), -weight);
    }

    /**
     * Writes an integer field entry into the sink.
     *
     * @param fieldId the numeric field identifier
     * @param value   the integer value
     * @param hasMore {@code true} if more fields follow in this event
     */
    public final void writeIntField(int fieldId, long value, boolean hasMore) {
        writeInt(fieldId, FIELD | (hasMore ? 0 : LAST), value);
    }

    /**
     * Writes a boolean field entry into the sink, encoded as integer
     * {@code 0} or {@code 1}.
     *
     * @param fieldId the numeric field identifier
     * @param value   the boolean value
     * @param hasMore {@code true} if more fields follow in this event
     */
    public final void writeBoolField(int fieldId, boolean value, boolean hasMore) {
        writeInt(fieldId, FIELD | (hasMore ? 0 : LAST), value ? 1 : 0);
    }

    /**
     * Writes a string field entry into the sink.
     *
     * @param fieldId the numeric field identifier
     * @param value   the string value, must not be {@code null}
     * @param hasMore {@code true} if more fields follow in this event
     */
    public final void writeStringField(int fieldId, String value, boolean hasMore) {
        writeString(fieldId, FIELD | (hasMore ? 0 : LAST), value);
    }

    /**
     * Writes a float field entry into the sink.
     *
     * @param fieldId the numeric field identifier
     * @param value   the double-precision value
     * @param hasMore {@code true} if more fields follow in this event
     */
    public final void writeFloatField(int fieldId, double value, boolean hasMore) {
        writeFloat(fieldId, FIELD | (hasMore ? 0 : LAST), value);
    }

    /**
     * Writes a complete event, marker and fields, from a sorted map of
     * decoded wire values.
     *
     * <p>The marker is written first with its {@link WamTags#LAST} flag set
     * when the field map is empty. Fields are then emitted in ascending
     * field-identifier order; the entry with the highest identifier carries
     * the {@code LAST} flag. Each value is dispatched by its
     * {@link WamWireValue} variant to the matching primitive writer.
     *
     * @param eventId the numeric event identifier
     * @param weight  the resolved sampling weight (written as {@code -weight})
     * @param fields  the fields to write, sorted by identifier; must not be
     *                {@code null}
     */
    public final void writeEvent(int eventId, int weight, NavigableMap<Integer, WamWireValue> fields) {
        var hasFields = !fields.isEmpty();
        writeEventMarker(eventId, weight, hasFields);
        if (!hasFields) {
            return;
        }
        var lastKey = fields.lastKey();
        for (var entry : fields.entrySet()) {
            int fieldId = entry.getKey();
            var hasMore = !entry.getKey().equals(lastKey);
            switch (entry.getValue()) {
                case WamWireValue.WamInt value -> writeIntField(fieldId, value.value(), hasMore);
                case WamWireValue.WamFloat value -> writeFloatField(fieldId, value.value(), hasMore);
                case WamWireValue.WamString value -> writeStringField(fieldId, value.value(), hasMore);
            }
        }
    }

    /**
     * Byte-array-backed encoder using {@link VarHandle} stores for
     * multi-byte little-endian writes.
     */
    private static final class ByteArray extends WamEventEncoder {
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
        public int written() {
            return offset - start;
        }

        @Override
        public void writeTag(int fieldId, int flags) {
            if (fieldId < 256) {
                ensureCapacity(2);
                buffer[offset++] = (byte) flags;
                buffer[offset++] = (byte) fieldId;
            } else {
                ensureCapacity(3);
                buffer[offset++] = (byte) (flags | WIDE_ID);
                SHORT_HANDLE.set(buffer, offset, (short) fieldId);
                offset += 2;
            }
        }

        @Override
        public void writeInt(int fieldId, int flags, long value) {
            if (value == 0) {
                writeTag(fieldId, flags | VALUE_INT_0);
            } else if (value == 1) {
                writeTag(fieldId, flags | VALUE_INT_1);
            } else if (value >= -128 && value < 128) {
                writeTag(fieldId, flags | VALUE_INT8);
                ensureCapacity(1);
                buffer[offset++] = (byte) value;
            } else if (value >= -32768 && value < 32768) {
                writeTag(fieldId, flags | VALUE_INT16);
                ensureCapacity(2);
                SHORT_HANDLE.set(buffer, offset, (short) value);
                offset += 2;
            } else if (value >= -2147483648L && value < 2147483648L) {
                writeTag(fieldId, flags | VALUE_INT32);
                ensureCapacity(4);
                INT_HANDLE.set(buffer, offset, (int) value);
                offset += 4;
            } else {
                writeFloat(fieldId, flags, (double) value);
            }
        }

        @Override
        public void writeFloat(int fieldId, int flags, double value) {
            writeTag(fieldId, flags | VALUE_FLOAT64);
            ensureCapacity(8);
            LONG_HANDLE.set(buffer, offset, Double.doubleToRawLongBits(value));
            offset += 8;
        }

        @Override
        public void writeString(int fieldId, int flags, String value) {
            var encoded = value.getBytes(StandardCharsets.UTF_8);
            var encodedLength = encoded.length;
            if (encodedLength < 256) {
                writeTag(fieldId, flags | VALUE_STR8);
                ensureCapacity(1 + encodedLength);
                buffer[offset++] = (byte) encodedLength;
            } else if (encodedLength < 65536) {
                writeTag(fieldId, flags | VALUE_STR16);
                ensureCapacity(2 + encodedLength);
                SHORT_HANDLE.set(buffer, offset, (short) encodedLength);
                offset += 2;
            } else {
                writeTag(fieldId, flags | VALUE_STR32);
                ensureCapacity(4 + encodedLength);
                INT_HANDLE.set(buffer, offset, encodedLength);
                offset += 4;
            }
            System.arraycopy(encoded, 0, buffer, offset, encodedLength);
            offset += encodedLength;
        }

        @Override
        public void writeRaw(byte[] source, int offset, int length) {
            ensureCapacity(length);
            System.arraycopy(source, offset, buffer, this.offset, length);
            this.offset += length;
        }

        private void ensureCapacity(int n) {
            if (offset + n > limit) {
                throw new IndexOutOfBoundsException(
                        "WAM encode would overflow buffer: need " + n
                                + " more bytes at offset " + offset
                                + ", limit " + limit);
            }
        }

        @Override
        public void close() {
            // A byte-array sink holds no resource to release.
        }
    }

    /**
     * Stream-backed encoder that uses an internal 8-byte scratch buffer
     * for staged little-endian writes.
     */
    private static final class Stream extends WamEventEncoder {
        private final OutputStream out;
        private final byte[] scratch;
        private final int limit;
        private int written;

        private Stream(OutputStream out, int limit) {
            this.out = Objects.requireNonNull(out, "out cannot be null");
            this.scratch = new byte[8];
            this.limit = limit;
            this.written = 0;
        }

        @Override
        public int written() {
            return written;
        }

        @Override
        public void writeTag(int fieldId, int flags) {
            try {
                if (fieldId < 256) {
                    ensureCapacity(2);
                    scratch[0] = (byte) flags;
                    scratch[1] = (byte) fieldId;
                    out.write(scratch, 0, 2);
                    written += 2;
                } else {
                    ensureCapacity(3);
                    scratch[0] = (byte) (flags | WIDE_ID);
                    SHORT_HANDLE.set(scratch, 1, (short) fieldId);
                    out.write(scratch, 0, 3);
                    written += 3;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void writeInt(int fieldId, int flags, long value) {
            try {
                if (value == 0) {
                    writeTag(fieldId, flags | VALUE_INT_0);
                } else if (value == 1) {
                    writeTag(fieldId, flags | VALUE_INT_1);
                } else if (value >= -128 && value < 128) {
                    writeTag(fieldId, flags | VALUE_INT8);
                    ensureCapacity(1);
                    scratch[0] = (byte) value;
                    out.write(scratch, 0, 1);
                    written += 1;
                } else if (value >= -32768 && value < 32768) {
                    writeTag(fieldId, flags | VALUE_INT16);
                    ensureCapacity(2);
                    SHORT_HANDLE.set(scratch, 0, (short) value);
                    out.write(scratch, 0, 2);
                    written += 2;
                } else if (value >= -2147483648L && value < 2147483648L) {
                    writeTag(fieldId, flags | VALUE_INT32);
                    ensureCapacity(4);
                    INT_HANDLE.set(scratch, 0, (int) value);
                    out.write(scratch, 0, 4);
                    written += 4;
                } else {
                    writeFloat(fieldId, flags, (double) value);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void writeFloat(int fieldId, int flags, double value) {
            try {
                writeTag(fieldId, flags | VALUE_FLOAT64);
                ensureCapacity(8);
                LONG_HANDLE.set(scratch, 0, Double.doubleToRawLongBits(value));
                out.write(scratch, 0, 8);
                written += 8;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void writeString(int fieldId, int flags, String value) {
            try {
                var encoded = value.getBytes(StandardCharsets.UTF_8);
                var encodedLength = encoded.length;
                if (encodedLength < 256) {
                    writeTag(fieldId, flags | VALUE_STR8);
                    ensureCapacity(1 + encodedLength);
                    scratch[0] = (byte) encodedLength;
                    out.write(scratch, 0, 1);
                    written += 1;
                } else if (encodedLength < 65536) {
                    writeTag(fieldId, flags | VALUE_STR16);
                    ensureCapacity(2 + encodedLength);
                    SHORT_HANDLE.set(scratch, 0, (short) encodedLength);
                    out.write(scratch, 0, 2);
                    written += 2;
                } else {
                    writeTag(fieldId, flags | VALUE_STR32);
                    ensureCapacity(4 + encodedLength);
                    INT_HANDLE.set(scratch, 0, encodedLength);
                    out.write(scratch, 0, 4);
                    written += 4;
                }
                out.write(encoded, 0, encodedLength);
                written += encodedLength;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void writeRaw(byte[] source, int offset, int length) {
            try {
                ensureCapacity(length);
                out.write(source, offset, length);
                written += length;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void ensureCapacity(int n) {
            if (limit >= 0 && written + n > limit) {
                throw new IndexOutOfBoundsException(
                        "WAM encode would exceed stream budget: need " + n
                                + " more bytes after " + written
                                + ", limit " + limit);
            }
        }

        @Override
        public void close() {
            try {
                out.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
