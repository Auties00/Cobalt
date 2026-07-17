package com.github.auties00.cobalt.stanza.model;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An input stream source whose exact byte length is known up front.
 *
 * <p>Pairs a {@link Supplier} of {@link InputStream} with the exact number of bytes the stream
 * yields so a binary payload (a profile picture, a group picture, a newsletter picture) can be
 * streamed straight to a length-prefixed wire without first buffering it into a {@code byte[]}:
 * the {@linkplain #length() length} is advertised before the body is read, so an encoder can
 * emit the length prefix from metadata and then drain a fresh stream once at serialisation time.
 *
 * <p>The supplier must yield a fresh, readable stream of exactly {@link #length()} bytes on each
 * {@link #openStream()} call. Ownership of every opened stream passes to the caller, which must
 * close it; the {@link #toBytes()} and {@link #toBase64()} terminal operations open and close a
 * stream themselves.
 *
 * <p>On the wire this maps to a plain {@code BYTES} field: {@link #toBytes()} drains the stream
 * into the raw bytes and {@link #of(byte[])} wraps decoded bytes back into a replayable
 * {@code SizedInputStream}.
 */
public final class SizedInputStream {
    /**
     * Holds the 64 standard Base64 alphabet characters as ASCII bytes, indexed by their 6-bit
     * group value.
     */
    private static final byte[] BASE64_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the ASCII {@code =} padding byte appended to a final partial group.
     */
    private static final byte BASE64_PADDING = '=';

    /**
     * Holds the upper bound on the scratch read buffer used by {@link #toBase64()}; a payload
     * smaller than this is read through a buffer sized to the payload instead.
     */
    private static final int BASE64_BUFFER_SIZE = 8192;

    /**
     * Holds the supplier yielding a fresh stream over the payload on each call.
     */
    final Supplier<InputStream> supplier;

    /**
     * Holds the exact number of bytes {@link #supplier} yields.
     */
    final long length;

    /**
     * Constructs a sized stream from a supplier and its advertised length.
     *
     * @param supplier the supplier yielding a fresh stream over the payload; never {@code null}
     * @param length   the exact number of bytes the supplied stream yields; non-negative
     * @throws NullPointerException     if {@code supplier} is {@code null}
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public SizedInputStream(Supplier<InputStream> supplier, long length) {
        this.supplier = Objects.requireNonNull(supplier, "supplier cannot be null");
        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative: " + length);
        }
        this.length = length;
    }

    /**
     * Opens a fresh stream over the payload.
     *
     * <p>Invokes the underlying supplier; the returned stream yields exactly {@link #length()}
     * bytes and its ownership passes to the caller, which must close it.
     *
     * @return a fresh, readable stream over the payload
     */
    public InputStream openStream() {
        return supplier.get();
    }

    /**
     * Returns the exact number of bytes {@link #openStream()} yields.
     *
     * @return the payload length, in bytes
     */
    public long length() {
        return length;
    }

    /**
     * Drains the payload fully into a byte array.
     *
     * <p>Opens a fresh stream, reads it to exhaustion, and closes it. A supplier yielding a
     * {@code null} stream returns an empty {@code byte[0]}. This is also the wire-level
     * {@code BYTES} serializer.
     *
     * @return the payload bytes, never {@code null}
     * @throws UncheckedIOException if reading the stream fails
     */
    @ProtobufSerializer
    public byte[] toBytes() {
        try (var stream = supplier.get()) {
            return stream == null ? new byte[0] : stream.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read sized input stream", exception);
        }
    }

    /**
     * Encodes the payload into a standard Base64 (RFC 4648) string.
     *
     * <p>Encoding a stream through {@link java.util.Base64} first buffers the whole input into a
     * {@code byte[]} before producing the encoded {@link String}, peaking at roughly 2.3x the
     * payload size in memory. Because the {@link #length()} is known up front, this method instead
     * precomputes the exact output size, allocates the output once, and drains a fresh stream
     * through a small fixed scratch buffer of at most {@value #BASE64_BUFFER_SIZE} bytes, so the
     * raw payload is never held in a single array and the output never has to grow. The standard
     * alphabet is used with {@code =} padding and no line breaks, matching
     * {@link java.util.Base64#getEncoder()}.
     *
     * <p>The stream is opened and closed here; the read is capped at {@link #length()} bytes.
     *
     * @return the Base64 encoding of the payload, or the empty string when {@link #length()} is
     *         {@code 0}
     * @throws NullPointerException     if the supplier yields a {@code null} stream
     * @throws IllegalArgumentException if the stream ends before {@link #length()} bytes are read
     * @throws ArithmeticException      if the encoded length exceeds {@link Integer#MAX_VALUE}
     * @throws UncheckedIOException     if reading the stream fails
     */
    public String toBase64() {
        if (length == 0) {
            return "";
        }
        var outLength = Math.toIntExact(4 * ((length + 2) / 3));
        var out = new byte[outLength];
        var buffer = new byte[(int) Math.min(BASE64_BUFFER_SIZE, length)];
        var carry = new byte[3];
        var carryLength = 0;
        var outPosition = 0;
        var remaining = length;
        try (var input = Objects.requireNonNull(supplier.get(), "supplier yielded a null stream")) {
            int read;
            while (remaining > 0
                    && (read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                remaining -= read;
                var index = 0;
                // Complete a 1-2 byte group carried over from the previous read.
                while (carryLength > 0 && carryLength < 3 && index < read) {
                    carry[carryLength++] = buffer[index++];
                }
                if (carryLength == 3) {
                    outPosition = encodeBase64Group(carry, 0, 3, out, outPosition);
                    carryLength = 0;
                }
                // Bulk-encode every whole 3-byte group remaining in the buffer.
                var groupsEnd = index + ((read - index) / 3) * 3;
                while (index < groupsEnd) {
                    outPosition = encodeBase64Group(buffer, index, 3, out, outPosition);
                    index += 3;
                }
                // Stash the 0-2 trailing bytes for the next read or the final flush.
                while (index < read) {
                    carry[carryLength++] = buffer[index++];
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read sized input stream", exception);
        }
        if (remaining != 0) {
            throw new IllegalArgumentException(
                    "stream ended " + remaining + " bytes short of the declared length " + length);
        }
        if (carryLength > 0) {
            outPosition = encodeBase64Group(carry, 0, carryLength, out, outPosition);
        }
        return new String(out, 0, outPosition, StandardCharsets.US_ASCII);
    }

    /**
     * Deserializes a wire-level {@code BYTES} value into a replayable sized stream.
     *
     * <p>The decoded bytes are captured and the returned stream yields a fresh
     * {@link ByteArrayInputStream} over them on each {@link #openStream()} call. A {@code null}
     * input yields a {@code null} result so optional protobuf fields can remain absent after
     * decoding.
     *
     * @param wire the protobuf {@code BYTES} value, possibly {@code null}
     * @return a sized stream replaying {@code wire}, or {@code null} when {@code wire} is
     *         {@code null}
     */
    @ProtobufDeserializer
    public static SizedInputStream of(byte[] wire) {
        if (wire == null) {
            return null;
        }
        return new SizedInputStream(() -> new ByteArrayInputStream(wire), wire.length);
    }

    /**
     * Encodes a single 1-, 2-, or 3-byte group into four output bytes at {@code outPosition},
     * padding the tail with {@code =} for a partial group.
     *
     * @param source      the array holding the group
     * @param offset      the offset of the group within {@code source}
     * @param groupLength the group length, in {@code [1, 3]}
     * @param out         the output buffer
     * @param outPosition the write offset within {@code out}
     * @return the output position after the four written bytes
     */
    private static int encodeBase64Group(byte[] source, int offset, int groupLength, byte[] out, int outPosition) {
        var b0 = source[offset] & 0xFF;
        var b1 = groupLength > 1 ? source[offset + 1] & 0xFF : 0;
        var b2 = groupLength > 2 ? source[offset + 2] & 0xFF : 0;
        out[outPosition] = BASE64_ALPHABET[b0 >>> 2];
        out[outPosition + 1] = BASE64_ALPHABET[((b0 << 4) | (b1 >>> 4)) & 0x3F];
        out[outPosition + 2] = groupLength > 1 ? BASE64_ALPHABET[((b1 << 2) | (b2 >>> 6)) & 0x3F] : BASE64_PADDING;
        out[outPosition + 3] = groupLength > 2 ? BASE64_ALPHABET[b2 & 0x3F] : BASE64_PADDING;
        return outPosition + 4;
    }
}
