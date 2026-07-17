package com.github.auties00.cobalt.wam.privatestats.ed25519;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Validates {@link Ed25519Scalar#reduce} against {@link BigInteger} as the oracle: for any input
 * bytes the canonical reduction is {@code BigInteger(bytes).mod(L)}, compared as 32-byte
 * little-endian encodings on both sides. Each {@link Random} is seeded for reproducibility.
 */
class Ed25519ScalarTest {
    private static final BigInteger L =
            BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));

    private static final int ITERATIONS = 256;

    @Test
    void reduceMatchesOracle() {
        var rng = new Random(0xC0BA30L);
        for (var i = 0; i < ITERATIONS; i++) {
            var input = new byte[Ed25519Scalar.WIDE_BYTES];
            rng.nextBytes(input);
            var expected = bigIntegerToScalarBytes(littleEndianToBigInteger(input).mod(L));

            var buffer = new byte[Ed25519Scalar.WIDE_BYTES];
            System.arraycopy(input, 0, buffer, 0, Ed25519Scalar.WIDE_BYTES);
            Ed25519Scalar.reduce(buffer);

            var actual = new byte[Ed25519Scalar.SCALAR_BYTES];
            System.arraycopy(buffer, 0, actual, 0, Ed25519Scalar.SCALAR_BYTES);
            assertArrayEquals(expected, actual, "reduce disagreed at iteration " + i);
        }
    }

    @Test
    void reduceZeroesHighHalf() {
        var rng = new Random(0xC0BA31L);
        for (var i = 0; i < ITERATIONS; i++) {
            var buffer = new byte[Ed25519Scalar.WIDE_BYTES];
            rng.nextBytes(buffer);
            Ed25519Scalar.reduce(buffer);
            var highHalf = new byte[Ed25519Scalar.SCALAR_BYTES];
            System.arraycopy(buffer, Ed25519Scalar.SCALAR_BYTES, highHalf, 0,
                    Ed25519Scalar.SCALAR_BYTES);
            assertArrayEquals(new byte[Ed25519Scalar.SCALAR_BYTES], highHalf,
                    "reduce must zero the high 32 bytes at iteration " + i);
        }
    }

    @Test
    void reduceHandlesBoundaryInputs() {
        assertReducesTo(BigInteger.ZERO, BigInteger.ZERO);
        assertReducesTo(L.subtract(BigInteger.ONE), L.subtract(BigInteger.ONE));
        assertReducesTo(L, BigInteger.ZERO);
        assertReducesTo(L.add(BigInteger.ONE), BigInteger.ONE);

        var maxWide = BigInteger.ONE.shiftLeft(512).subtract(BigInteger.ONE);
        assertReducesTo(maxWide, maxWide.mod(L));
    }

    @Test
    void reduceIsIdentityOnCanonicalScalars() {
        var rng = new Random(0xC0BA32L);
        for (var i = 0; i < ITERATIONS; i++) {
            BigInteger s;
            do {
                s = new BigInteger(252, rng);
            } while (s.compareTo(L) >= 0);

            var expected = bigIntegerToScalarBytes(s);
            var buffer = new byte[Ed25519Scalar.WIDE_BYTES];
            System.arraycopy(expected, 0, buffer, 0, Ed25519Scalar.SCALAR_BYTES);
            Ed25519Scalar.reduce(buffer);

            var actual = new byte[Ed25519Scalar.SCALAR_BYTES];
            System.arraycopy(buffer, 0, actual, 0, Ed25519Scalar.SCALAR_BYTES);
            assertArrayEquals(expected, actual, "reduce should be identity on canonical s = " + s);
        }
    }

    private static void assertReducesTo(BigInteger input, BigInteger expected) {
        var buffer = new byte[Ed25519Scalar.WIDE_BYTES];
        var bytes = bigIntegerToWideBytes(input);
        System.arraycopy(bytes, 0, buffer, 0, Ed25519Scalar.WIDE_BYTES);
        Ed25519Scalar.reduce(buffer);

        var actual = new byte[Ed25519Scalar.SCALAR_BYTES];
        System.arraycopy(buffer, 0, actual, 0, Ed25519Scalar.SCALAR_BYTES);
        assertArrayEquals(bigIntegerToScalarBytes(expected), actual,
                "reduce(" + input + ") should equal " + expected);
    }

    private static BigInteger littleEndianToBigInteger(byte[] le) {
        var be = new byte[le.length + 1];
        for (var i = 0; i < le.length; i++) {
            be[le.length - i] = le[i];
        }
        return new BigInteger(be);
    }

    private static byte[] bigIntegerToScalarBytes(BigInteger x) {
        return bigIntegerToBytes(x, Ed25519Scalar.SCALAR_BYTES);
    }

    private static byte[] bigIntegerToWideBytes(BigInteger x) {
        return bigIntegerToBytes(x, Ed25519Scalar.WIDE_BYTES);
    }

    private static byte[] bigIntegerToBytes(BigInteger x, int width) {
        var be = x.toByteArray();
        var out = new byte[width];
        for (var i = 0; i < be.length && i < width; i++) {
            out[i] = be[be.length - 1 - i];
        }
        return out;
    }
}
