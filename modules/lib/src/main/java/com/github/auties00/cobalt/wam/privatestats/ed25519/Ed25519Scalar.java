package com.github.auties00.cobalt.wam.privatestats.ed25519;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Reduces arbitrary-length unsigned integers modulo the Ed25519 group
 * order {@code L = 2^252 + 27742317777372353535851937790883648493}.
 *
 * <p>Brings a 64-byte hash output, or any wide intermediate scalar,
 * into the canonical {@code [0, L)} range required for scalar
 * multiplication on the prime-order subgroup. Without this reduction
 * scalar multiplication is still defined but no longer constant time
 * and may leak information through subgroup-component drift.
 *
 * @implNote
 * This implementation backs the work buffer with {@code long[]} rather
 * than the JavaScript reference's {@code Float64Array} because
 * intermediate values can go negative; Java's arithmetic right shift
 * sign-extends correctly to match the reference {@code Math.floor(v /
 * 256)} on {@code Number} values.
 */
@WhatsAppWebModule(moduleName = "WACryptoPrimitives")
public final class Ed25519Scalar {
    /**
     * Holds the Ed25519 group order {@code L} as 32 little-endian
     * bytes.
     *
     * <p>Bytes 0..15 hold the
     * {@code 27742317777372353535851937790883648493} tail, bytes 16..30
     * are zero, and byte 31 holds the {@code 2^252} contribution
     * ({@code 0x10}).
     */
    private static final long[] L = {
            0xedL, 0xd3L, 0xf5L, 0x5cL, 0x1aL, 0x63L, 0x12L, 0x58L,
            0xd6L, 0x9cL, 0xf7L, 0xa2L, 0xdeL, 0xf9L, 0xdeL, 0x14L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0x10L
    };

    /**
     * Holds the number of bytes in a canonical scalar.
     */
    public static final int SCALAR_BYTES = 32;

    /**
     * Holds the number of bytes in a wide pre-reduction buffer, the
     * full width of a SHA-512 digest.
     */
    public static final int WIDE_BYTES = 64;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private Ed25519Scalar() {
        throw new AssertionError("Ed25519Scalar is a utility class and must not be instantiated");
    }

    /**
     * Reduces a 64-element work buffer modulo {@code L} and writes the
     * canonical 32-byte little-endian result to {@code r}.
     *
     * <p>Each entry of {@code x} must be an integer-valued {@code long}
     * in the byte range {@code [0, 256)} on input. The work buffer is
     * mutated in place during the carry propagation, with intermediate
     * entries going negative before the final byte extraction
     * renormalises them.
     *
     * @param r the 32-byte destination buffer
     * @param x the 64-element work buffer (mutated in place)
     */
    public static void modL(byte[] r, long[] x) {
        long carry;
        int j;
        for (var i = 63; i >= 32; --i) {
            carry = 0;
            for (j = i - 32; j < i - 12; ++j) {
                x[j] += carry - 16 * x[i] * L[j - (i - 32)];
                carry = (x[j] + 128) >> 8;
                x[j] -= carry << 8;
            }
            x[j] += carry;
            x[i] = 0;
        }
        carry = 0;
        for (j = 0; j < 32; j++) {
            x[j] += carry - (x[31] >> 4) * L[j];
            carry = x[j] >> 8;
            x[j] &= 0xffL;
        }
        for (j = 0; j < 32; j++) {
            x[j] -= carry * L[j];
        }
        for (var i = 0; i < 32; i++) {
            x[i + 1] += x[i] >> 8;
            r[i] = (byte) (x[i] & 0xffL);
        }
    }

    /**
     * Reduces a 64-byte little-endian buffer in place: overwrites the
     * first 32 bytes with the canonical scalar and zeroes the high 32
     * bytes.
     *
     * <p>This is the post-SHA-512 step of the EdDSA challenge
     * derivation, turning a full-width digest into a canonical scalar.
     *
     * @param r the 64-byte buffer (mutated in place)
     */
    public static void reduce(byte[] r) {
        var x = new long[WIDE_BYTES];
        for (var i = 0; i < WIDE_BYTES; i++) {
            x[i] = r[i] & 0xffL;
        }
        for (var i = 0; i < WIDE_BYTES; i++) {
            r[i] = 0;
        }
        modL(r, x);
    }
}
