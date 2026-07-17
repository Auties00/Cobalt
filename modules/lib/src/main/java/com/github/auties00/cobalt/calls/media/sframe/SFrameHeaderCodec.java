package com.github.auties00.cobalt.calls.media.sframe;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Encodes and decodes the SFrame trailer: the LEB128 key id and counter and the closing length byte
 * that together identify which key sealed a frame and which per frame nonce it used.
 *
 * <p>The SFrame trailer is a suffix appended after the ciphertext and authentication tag. Its byte
 * order is {@code [keyId varint][counter varint][length byte]}: the key id is written and read first,
 * the counter second, and a single closing byte equal to the trailer's total length is last. A
 * decoder therefore reads the final byte to learn the trailer length, walks back that many bytes to
 * the start of the trailer, then LEB128-decodes the key id followed by the counter. The whole trailer,
 * including the length byte, is the associated data the cipher authenticates.
 *
 * <p>The key id and counter are LEB128: little endian base 128 with a high bit continuation flag, at
 * most ten bytes for a 64-bit value. The key id is always written and read before the counter. The
 * trailer length therefore ranges from {@value #MIN_TRAILER_LENGTH} (a one byte key id, a one byte
 * counter, and the length byte) up to {@value #MAX_TRAILER_LENGTH}.
 */
public final class SFrameHeaderCodec {
    /**
     * The logger for {@link SFrameHeaderCodec}.
     */
    private static final System.Logger LOGGER = Log.get(SFrameHeaderCodec.class);

    /**
     * Holds the maximum number of bytes a LEB128-encoded 64-bit value can occupy.
     *
     * <p>Ten bytes carry {@code 70} bits, enough for any 64-bit value; the native decoder caps its
     * scan at this length.
     */
    public static final int MAX_VARINT_LENGTH = 10;

    /**
     * Holds the continuation bit mask of a LEB128 byte; a set high bit means another byte follows.
     */
    private static final int CONTINUATION_BIT = 0x80;

    /**
     * Holds the payload mask of a LEB128 byte: the low seven value bits.
     */
    private static final int PAYLOAD_MASK = 0x7F;

    /**
     * Holds the number of value bits each LEB128 byte carries.
     */
    private static final int PAYLOAD_BITS = 7;

    /**
     * Holds the smallest valid trailer length, in bytes: a one byte key id, a one byte counter, and
     * the length byte.
     */
    public static final int MIN_TRAILER_LENGTH = 3;

    /**
     * Holds the largest valid trailer length, in bytes, the native decoder accepts ({@code 0x15}).
     */
    public static final int MAX_TRAILER_LENGTH = 0x15;

    /**
     * Holds the maximum scratch size needed to assemble a trailer: two ten byte varints and the
     * length byte.
     */
    private static final int MAX_TRAILER_SCRATCH = 2 * MAX_VARINT_LENGTH + 1;

    /**
     * Prevents instantiation of this stateless codec holder.
     */
    private SFrameHeaderCodec() {
        throw new AssertionError("SFrameHeaderCodec is not instantiable");
    }

    /**
     * Encodes {@code value} as an LEB128 varint into {@code dst} starting at {@code offset} and
     * returns the number of bytes written.
     *
     * <p>The value is treated as unsigned: each step emits the low seven bits with the continuation
     * bit set while more than seven value bits remain, then emits the final byte with the
     * continuation bit clear.
     *
     * @param dst    the destination buffer, large enough for up to {@value #MAX_VARINT_LENGTH} bytes
     *               at {@code offset}
     * @param offset the index to start writing at
     * @param value  the value to encode, interpreted as unsigned
     * @return the number of bytes written, between {@code 1} and {@value #MAX_VARINT_LENGTH}
     * @throws NullPointerException      if {@code dst} is {@code null}
     * @throws IndexOutOfBoundsException if the encoding does not fit in {@code dst} at {@code offset}
     */
    public static int writeVarint(byte[] dst, int offset, long value) {
        Objects.requireNonNull(dst, "dst cannot be null");
        var cursor = offset;
        while (Long.compareUnsigned(value, PAYLOAD_MASK) > 0) {
            dst[cursor++] = (byte) ((value & PAYLOAD_MASK) | CONTINUATION_BIT);
            value >>>= PAYLOAD_BITS;
        }
        dst[cursor++] = (byte) value;
        return cursor - offset;
    }

    /**
     * Decodes an LEB128 varint from {@code src} starting at {@code offset}, scanning at most
     * {@code limit} bytes, and reports the decoded value and the bytes consumed.
     *
     * <p>The decoder accumulates seven value bits per byte until it reads a byte with the
     * continuation bit clear, capping its scan at {@value #MAX_VARINT_LENGTH} bytes. A varint with no
     * terminating byte within {@code limit} or within {@value #MAX_VARINT_LENGTH} bytes is reported as
     * a zero consumed length, which the caller treats as a malformed trailer.
     *
     * @param src    the source buffer
     * @param offset the index to start reading from
     * @param limit  the maximum number of bytes that may be read
     * @param out    a single element holder receiving the decoded value on success
     * @return the number of bytes consumed, or {@code 0} if the varint is truncated or too long
     * @throws NullPointerException      if {@code src} or {@code out} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} or {@code limit} addresses bytes outside
     *                                   {@code src}
     */
    public static int readVarint(byte[] src, int offset, int limit, long[] out) {
        Objects.requireNonNull(src, "src cannot be null");
        Objects.requireNonNull(out, "out cannot be null");
        var bound = Math.min(limit, MAX_VARINT_LENGTH);
        var value = 0L;
        var shift = 0;
        for (var i = 0; i < bound; i++) {
            var current = src[offset + i];
            value |= (long) (current & PAYLOAD_MASK) << shift;
            if ((current & CONTINUATION_BIT) == 0) {
                out[0] = value;
                return i + 1;
            }
            shift += PAYLOAD_BITS;
        }
        if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe trailer varint truncated or too long, offset={0} limit={1}", offset, limit);
        return 0;
    }

    /**
     * Builds the SFrame trailer {@code [keyId varint][counter varint][length byte]} for the given key
     * id and counter.
     *
     * <p>The key id is encoded first, the counter second, and a closing byte equal to the trailer's
     * total length last; the returned array's final byte therefore equals its length.
     *
     * @param keyId   the key id, interpreted as unsigned
     * @param counter the per frame counter, interpreted as unsigned
     * @return the trailer bytes, whose last byte equals their total length
     */
    public static byte[] writeTrailer(long keyId, long counter) {
        var scratch = new byte[MAX_TRAILER_SCRATCH];
        var cursor = 0;
        cursor += writeVarint(scratch, cursor, keyId);
        cursor += writeVarint(scratch, cursor, counter);
        var trailerLength = cursor + 1;
        scratch[cursor] = (byte) trailerLength;
        var trailer = new byte[trailerLength];
        System.arraycopy(scratch, 0, trailer, 0, trailerLength);
        return trailer;
    }

    /**
     * Reads the trailer length from the final byte of a received frame and validates it.
     *
     * <p>The native decoder reads {@code frame[frameLength - 1]} as the trailer length and rejects a
     * value outside {@code [}{@value #MIN_TRAILER_LENGTH}{@code , }{@value #MAX_TRAILER_LENGTH}{@code ]}
     * or larger than the frame itself.
     *
     * @param frame       the received frame bytes (ciphertext, tag, then trailer)
     * @param frameLength the number of valid bytes in {@code frame}
     * @return the trailer length in bytes, or {@code -1} if it is absent or out of range
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    public static int readTrailerLength(byte[] frame, int frameLength) {
        Objects.requireNonNull(frame, "frame cannot be null");
        if (frameLength < MIN_TRAILER_LENGTH) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe frame shorter than minimum trailer length, frameLength={0}", frameLength);
            return -1;
        }
        var trailerLength = frame[frameLength - 1] & 0xFF;
        if (trailerLength < MIN_TRAILER_LENGTH || trailerLength > MAX_TRAILER_LENGTH
                || trailerLength > frameLength) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe trailer length out of range, trailerLength={0} frameLength={1}", trailerLength, frameLength);
            return -1;
        }
        return trailerLength;
    }

    /**
     * Decodes the key id and counter from a trailer located at {@code trailerStart} in {@code frame}.
     *
     * <p>The trailer spans {@code trailerLength} bytes ending with the length byte; this reads the key
     * id varint then the counter varint and requires their combined length plus the closing byte to
     * equal {@code trailerLength} exactly, rejecting a trailer whose declared length does not match
     * its varint content.
     *
     * @param frame         the received frame bytes
     * @param trailerStart  the index of the first trailer byte (the start of the key id varint)
     * @param trailerLength the declared trailer length, including the closing length byte
     * @return the decoded {@link Trailer}, or {@code null} if the varints are malformed or their
     *         lengths do not reconcile with {@code trailerLength}
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    public static Trailer readTrailer(byte[] frame, int trailerStart, int trailerLength) {
        Objects.requireNonNull(frame, "frame cannot be null");
        var keyIdOut = new long[1];
        var keyIdLength = readVarint(frame, trailerStart, trailerLength - 1, keyIdOut);
        if (keyIdLength == 0) {
            return null;
        }
        var counterOut = new long[1];
        var counterLength = readVarint(
                frame, trailerStart + keyIdLength, trailerLength - 1 - keyIdLength, counterOut);
        if (counterLength == 0 || keyIdLength + counterLength + 1 != trailerLength) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "sframe trailer length mismatch, declared={0} keyIdLen={1} counterLen={2}",
                        trailerLength, keyIdLength, counterLength);
            }
            return null;
        }
        return new Trailer(keyIdOut[0], counterOut[0]);
    }

    /**
     * Carries the key id and counter decoded from an SFrame trailer.
     *
     * @param keyId   the key id naming which SFrame key sealed the frame
     * @param counter the per frame counter selecting the nonce
     */
    public record Trailer(long keyId, long counter) {
    }
}
