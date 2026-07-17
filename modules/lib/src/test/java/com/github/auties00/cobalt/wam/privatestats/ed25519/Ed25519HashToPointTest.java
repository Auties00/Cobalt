package com.github.auties00.cobalt.wam.privatestats.ed25519;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates {@link Ed25519HashToPoint} via algebraic properties: every output must lie on the
 * Ed25519 curve, lie in the prime-order subgroup, and be deterministic in its input. This pins the
 * math without a JS-bundle known-answer test; Ed25519LiveBundleKatTest separately pins
 * byte-identical agreement against vectors captured from the live WhatsApp Web bundle.
 */
class Ed25519HashToPointTest {
    private static final BigInteger P = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19));

    private static final BigInteger L =
            BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));

    private static final BigInteger D_BIG =
            BigInteger.valueOf(-121665).multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P);

    private static final int ITERATIONS = 16;

    @Test
    void outputIsOnCurve() {
        var rng = new Random(0xC0BA50L);
        for (var i = 0; i < ITERATIONS; i++) {
            var msg = new byte[32 + rng.nextInt(64)];
            rng.nextBytes(msg);
            var p = Ed25519HashToPoint.compute(msg);

            var x = affineX(p);
            var y = affineY(p);
            var lhs = y.multiply(y).subtract(x.multiply(x)).mod(P);
            var rhs = BigInteger.ONE.add(D_BIG.multiply(x).multiply(x).multiply(y).multiply(y)).mod(P);
            assertEquals(rhs, lhs, "hashToPoint output not on curve at iteration " + i);
        }
    }

    @Test
    void outputIsInPrimeOrderSubgroup() {
        var rng = new Random(0xC0BA51L);
        var lBytes = bigIntegerToScalar(L);
        var identity = packIdentity();
        for (var i = 0; i < ITERATIONS; i++) {
            var msg = new byte[32 + rng.nextInt(64)];
            rng.nextBytes(msg);
            var p = Ed25519HashToPoint.compute(msg);

            var lp = Ed25519Point.p3();
            Ed25519Point.scalarMult(lp, p, lBytes);

            var packed = new byte[32];
            Ed25519Point.pack(packed, lp);
            assertArrayEquals(identity, packed,
                    "hashToPoint output not in prime-order subgroup at iteration " + i);
        }
    }

    @Test
    void outputIsDeterministic() {
        var rng = new Random(0xC0BA52L);
        for (var i = 0; i < ITERATIONS; i++) {
            var msg = new byte[32 + rng.nextInt(64)];
            rng.nextBytes(msg);

            var first = Ed25519HashToPoint.compute(msg);
            var second = Ed25519HashToPoint.compute(msg);

            var firstPacked = new byte[32];
            var secondPacked = new byte[32];
            Ed25519Point.pack(firstPacked, first);
            Ed25519Point.pack(secondPacked, second);
            assertArrayEquals(firstPacked, secondPacked,
                    "hashToPoint not deterministic at iteration " + i);
        }
    }

    @Test
    void outputExercisesBothParityBranches() {
        var rng = new Random(0xC0BA53L);
        var iters = 64;
        var parityZero = 0;
        var parityOne = 0;
        for (var i = 0; i < iters; i++) {
            var msg = new byte[16 + rng.nextInt(32)];
            rng.nextBytes(msg);
            var p = Ed25519HashToPoint.compute(msg);
            var packed = new byte[32];
            Ed25519Point.pack(packed, p);
            if ((packed[31] & 0x80) != 0) {
                parityOne++;
            } else {
                parityZero++;
            }
        }
        assertEquals(true, parityZero > 0, "hashToPoint never produced parity-0 outputs");
        assertEquals(true, parityOne > 0, "hashToPoint never produced parity-1 outputs");
    }

    private static BigInteger affineX(long[][] p) {
        var inv = Ed25519Field.gf();
        Ed25519Field.inv25519(inv, p[2]);
        var x = Ed25519Field.gf();
        Ed25519Field.mul(x, p[0], inv);
        return packedToBigInteger(packField(x));
    }

    private static BigInteger affineY(long[][] p) {
        var inv = Ed25519Field.gf();
        Ed25519Field.inv25519(inv, p[2]);
        var y = Ed25519Field.gf();
        Ed25519Field.mul(y, p[1], inv);
        return packedToBigInteger(packField(y));
    }

    private static byte[] packField(long[] fe) {
        var out = new byte[32];
        Ed25519Field.pack25519(out, fe);
        return out;
    }

    private static BigInteger packedToBigInteger(byte[] le) {
        var be = new byte[le.length + 1];
        for (var i = 0; i < le.length; i++) {
            be[le.length - i] = le[i];
        }
        return new BigInteger(be);
    }

    private static byte[] bigIntegerToScalar(BigInteger k) {
        var be = k.mod(BigInteger.ONE.shiftLeft(256)).toByteArray();
        var out = new byte[32];
        for (var i = 0; i < be.length && i < 32; i++) {
            out[i] = be[be.length - 1 - i];
        }
        return out;
    }

    private static byte[] packIdentity() {
        var identity = Ed25519Point.p3();
        Ed25519Field.set25519(identity[0], Ed25519Field.gf());
        Ed25519Field.set25519(identity[1], Ed25519Field.gfFromSmall(1));
        Ed25519Field.set25519(identity[2], Ed25519Field.gfFromSmall(1));
        Ed25519Field.set25519(identity[3], Ed25519Field.gf());
        var out = new byte[32];
        Ed25519Point.pack(out, identity);
        return out;
    }

    @Test
    void usesSha512OfInput() throws NoSuchAlgorithmException {
        var rng = new Random(0xC0BA54L);
        for (var i = 0; i < ITERATIONS; i++) {
            var msg = new byte[16 + rng.nextInt(48)];
            rng.nextBytes(msg);
            var direct = MessageDigest.getInstance("SHA-512").digest(msg);
            var msgFlipped = msg.clone();
            msgFlipped[0] ^= 1;
            var directFlipped = MessageDigest.getInstance("SHA-512").digest(msgFlipped);

            assertEquals(false, Arrays.equals(direct, directFlipped),
                    "SHA-512 should differ for flipped input (sanity check)");

            var p1 = Ed25519HashToPoint.compute(msg);
            var p2 = Ed25519HashToPoint.compute(msgFlipped);
            var packed1 = new byte[32];
            var packed2 = new byte[32];
            Ed25519Point.pack(packed1, p1);
            Ed25519Point.pack(packed2, p2);
            assertEquals(false, Arrays.equals(packed1, packed2),
                    "hashToPoint must produce different outputs for flipped input at iteration " + i);
        }
    }
}
