package com.github.auties00.cobalt.wam.privatestats.ed25519;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes an arbitrary byte string to a point on the Ed25519
 * prime-order subgroup.
 *
 * <p>The composition is: SHA-512 of the message, an Elligator2 map onto
 * the Curve25519 Montgomery form, a birational lift to the
 * twisted-Edwards form, and a cofactor multiplication by {@code 8} to
 * land in the prime-order subgroup.
 *
 * <p>This is consumed only by
 * {@link com.github.auties00.cobalt.wam.privatestats.WamPrivateStatsTokenBlinder#blind};
 * it is not a general-purpose hash-to-curve. It predates RFC 9380 and
 * is WhatsApp's own composition, validated only against vectors captured
 * from the live WhatsApp Web bundle rather than an independent
 * specification.
 */
@WhatsAppWebModule(moduleName = "WACryptoEd25519")
public final class Ed25519HashToPoint {
    /**
     * Holds the canonical 32-byte little-endian encoding of
     * {@code sqrt(-1) mod p}, used as the alternate square-root branch
     * in {@link #sqrt}.
     */
    private static final byte[] SQRT_M1_BYTES = {
            (byte) 176, (byte) 160, (byte) 14, (byte) 74, (byte) 39, (byte) 27, (byte) 238, (byte) 196,
            (byte) 120, (byte) 228, (byte) 47, (byte) 173, (byte) 6, (byte) 24, (byte) 67, (byte) 47,
            (byte) 167, (byte) 215, (byte) 251, (byte) 61, (byte) 153, (byte) 0, (byte) 77, (byte) 43,
            (byte) 11, (byte) 223, (byte) 193, (byte) 79, (byte) 128, (byte) 36, (byte) 131, (byte) 43
    };

    /**
     * Holds the canonical 32-byte little-endian encoding of
     * {@code sqrt(-486664) mod p}, used as the scaling factor of the
     * Curve25519 to Ed25519 birational map in {@link #liftMontToP3}.
     */
    private static final byte[] SQRT_NEG_A_PLUS_2_BYTES = {
            (byte) 6, (byte) 126, (byte) 69, (byte) 255, (byte) 170, (byte) 4, (byte) 110, (byte) 204,
            (byte) 130, (byte) 26, (byte) 125, (byte) 75, (byte) 209, (byte) 211, (byte) 161, (byte) 197,
            (byte) 126, (byte) 79, (byte) 252, (byte) 3, (byte) 220, (byte) 8, (byte) 123, (byte) 210,
            (byte) 187, (byte) 6, (byte) 160, (byte) 96, (byte) 244, (byte) 237, (byte) 38, (byte) 15
    };

    /**
     * Holds the Curve25519 Montgomery curve parameter {@code A =
     * 486662}, encoded as 16 radix-{@code 2^16} limbs.
     */
    private static final long[] A_MONT = {
            0x6D06L, 0x7L, 0L, 0L,
            0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L
    };

    /**
     * Holds the field constant {@code 2}, materialised so
     * {@link #squareTimesTwo} can pass it directly to
     * {@link Ed25519Field#mul}.
     */
    private static final long[] TWO = {
            2L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L
    };

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private Ed25519HashToPoint() {
        throw new AssertionError("Ed25519HashToPoint is a utility class and must not be instantiated");
    }

    /**
     * Hashes a byte string to a point on the Ed25519 prime-order
     * subgroup.
     *
     * <p>The returned point is in extended-Edwards form and has the
     * cofactor-{@code 8} component mapped out, so it lies in the
     * prime-order subgroup.
     *
     * @implNote
     * This implementation extracts the SHA-512 sign bit from the high
     * bit of byte 31 of the digest, masks it off, unpacks the remainder
     * as a field element, runs {@link #elligator} to land on the
     * Curve25519 Montgomery form, lifts to Ed25519 with the extracted
     * sign via {@link #liftMontToP3}, and finally maps into the
     * prime-order subgroup via {@link #cofactorMul8}.
     *
     * @param message the message to hash; any length
     * @return a freshly allocated extended-Edwards point
     */
    public static long[][] compute(byte[] message) {
        var digest = sha512(message);
        var sign = (digest[31] & 0x80) >>> 7;
        digest[31] &= 0x7f;

        var u = Ed25519Field.gf();
        Ed25519Field.unpack25519(u, digest);

        var mont = Ed25519Field.gf();
        elligator(mont, u);

        var lifted = Ed25519Point.p3();
        liftMontToP3(lifted, mont, sign);

        var result = Ed25519Point.p3();
        cofactorMul8(result, lifted);
        return result;
    }

    /**
     * Computes the SHA-512 digest of the input message.
     *
     * @implNote
     * This implementation uses the JDK's {@link MessageDigest}
     * SHA-512 rather than a hand-ported hash; the two are byte-identical
     * for any input.
     *
     * @param message the message
     * @return a fresh 64-byte digest
     * @throws AssertionError if the JVM does not provide SHA-512
     */
    private static byte[] sha512(byte[] message) {
        try {
            var md = MessageDigest.getInstance("SHA-512");
            return md.digest(message);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-512 must be available on every JVM", e);
        }
    }

    /**
     * Computes {@code o = 2 * (t^2)}.
     *
     * @param o the destination
     * @param t the operand
     */
    private static void squareTimesTwo(long[] o, long[] t) {
        var sq = Ed25519Field.gf();
        Ed25519Field.square(sq, t);
        Ed25519Field.mul(o, TWO, sq);
    }

    /**
     * Evaluates the Curve25519 Montgomery polynomial at {@code t}:
     * {@code o = t^3 + A*t^2 + t} where {@code A = 486662}.
     *
     * <p>This is the right-hand side of the Curve25519 equation
     * {@code v^2 = u^3 + A*u^2 + u}. It is used in {@link #elligator} to
     * test whether a candidate {@code u} lies on the curve and in
     * {@link #liftMontToP3} to recover the {@code v} coordinate.
     *
     * @param o the destination
     * @param t the {@code u} coordinate at which to evaluate
     */
    private static void curve25519Polynomial(long[] o, long[] t) {
        var sq = Ed25519Field.gf();
        Ed25519Field.square(sq, t);
        var atimes = Ed25519Field.gf();
        Ed25519Field.mul(atimes, A_MONT, t);
        var sum = Ed25519Field.gf();
        Ed25519Field.add(sum, sq, atimes);
        var sumPlusOne = Ed25519Field.gf();
        Ed25519Field.add(sumPlusOne, sum, Ed25519Field.gfFromSmall(1));
        Ed25519Field.mul(o, t, sumPlusOne);
    }

    /**
     * Computes the canonical square root {@code o = sqrt(t)}, chosen so
     * that {@code o^2 = t}.
     *
     * <p>The caller must supply a quadratic residue; the result is
     * undefined otherwise.
     *
     * @implNote
     * This implementation uses the closed-form root for {@code p == 5
     * (mod 8)}: compute {@code c = t^((p+3)/8)} (delivered by
     * {@link Ed25519Field#pow2523} followed by a multiply); if
     * {@code c^2 == t} the result is {@code c}, otherwise it is
     * {@code c * sqrt(-1)}. The branch on {@code c^2 == t} is made
     * constant time via {@link Ed25519Point#neq25519} and
     * {@link Ed25519Field#sel25519}.
     *
     * @param o the destination
     * @param t the operand, assumed to be a quadratic residue
     */
    private static void sqrt(long[] o, long[] t) {
        var sqrtMinusOne = Ed25519Field.gf();
        Ed25519Field.unpack25519(sqrtMinusOne, SQRT_M1_BYTES);
        var u = Ed25519Field.gf();
        Ed25519Field.pow2523(u, t);
        var c = Ed25519Field.gf();
        Ed25519Field.mul(c, t, u);
        var d = Ed25519Field.gf();
        Ed25519Field.square(d, c);
        var alt = Ed25519Field.gf();
        Ed25519Field.mul(alt, c, sqrtMinusOne);
        Ed25519Field.sel25519(c, alt, Ed25519Point.neq25519(d, t));
        Ed25519Field.set25519(o, c);
    }

    /**
     * Computes the Edwards y-coordinate from a Montgomery
     * u-coordinate: {@code o = (t - 1) / (t + 1)}.
     *
     * @param o the destination Edwards y
     * @param t the Montgomery u
     */
    private static void montUToEdwardsY(long[] o, long[] t) {
        var minus = Ed25519Field.gf();
        Ed25519Field.sub(minus, t, Ed25519Field.gfFromSmall(1));
        var plus = Ed25519Field.gf();
        Ed25519Field.add(plus, t, Ed25519Field.gfFromSmall(1));
        var inv = Ed25519Field.gf();
        Ed25519Field.inv25519(inv, plus);
        Ed25519Field.mul(o, minus, inv);
    }

    /**
     * Negates a field element: {@code o = (-t) mod p}.
     *
     * @param o the destination
     * @param t the operand
     */
    private static void negate(long[] o, long[] t) {
        Ed25519Field.sub(o, Ed25519Field.gf(), t);
    }

    /**
     * Returns {@code 1} when {@code e} is a non-zero quadratic
     * non-residue modulo {@code p}, and {@code 0} otherwise (including
     * for zero).
     *
     * <p>Used by {@link #elligator} to select between the two
     * Elligator2 pre-images.
     *
     * @implNote
     * This implementation reads the Legendre symbol bit by raising
     * {@code e} to {@code (p-1)/2} and inspecting bit 0 of byte 31 of
     * the canonical encoding: set for {@code -1}, clear for {@code 1}.
     *
     * @param e the operand
     * @return {@code 0} for quadratic residues and zero, {@code 1} for
     *         non-residues
     */
    private static long isNonResidue(long[] e) {
        var l = Ed25519Field.gf();
        Ed25519Field.pow2523(l, e);
        var s = Ed25519Field.gf();
        Ed25519Field.square(s, l);
        var u = Ed25519Field.gf();
        Ed25519Field.square(u, s);
        var c = Ed25519Field.gf();
        Ed25519Field.mul(c, u, e);
        var d = Ed25519Field.gf();
        Ed25519Field.mul(d, c, e);
        var packed = new byte[Ed25519Field.BYTES];
        Ed25519Field.pack25519(packed, d);
        return packed[31] & 1L;
    }

    /**
     * Conditionally copies {@code src} into {@code dst} when
     * {@code bit == 1}, and leaves {@code dst} unchanged when
     * {@code bit == 0}.
     *
     * @implNote
     * This implementation uses a real {@code if} rather than a masked
     * select because every call site supplies a bit derived from public
     * inputs (the Legendre symbol bit of public field elements, the
     * SHA-512 sign bit of the public message digest, or a parity bit
     * derived from those); no secret data flows through this branch.
     *
     * @param dst the possibly-overwritten destination
     * @param src the source
     * @param bit must be exactly {@code 0} or {@code 1}
     */
    private static void setIfBitOne(long[] dst, long[] src, long bit) {
        if (bit == 1) {
            Ed25519Field.set25519(dst, src);
        }
    }

    /**
     * Applies the Elligator2 map: takes a 256-bit field element
     * {@code t} (with bit 255 cleared) and produces a Curve25519
     * Montgomery u-coordinate {@code o} on the curve.
     *
     * @implNote
     * This implementation computes {@code u = -A / (1 + 2*t^2)}; if
     * {@code u^3 + A*u^2 + u} is a quadratic residue the result is
     * {@code u}, otherwise the result is {@code -u - A}, the second
     * pre-image of the same quadratic-residue branch.
     *
     * @param o the destination Montgomery u
     * @param t the input field element
     */
    private static void elligator(long[] o, long[] t) {
        var twoTSq = Ed25519Field.gf();
        squareTimesTwo(twoTSq, t);
        var denom = Ed25519Field.gf();
        Ed25519Field.add(denom, twoTSq, Ed25519Field.gfFromSmall(1));
        var denomInv = Ed25519Field.gf();
        Ed25519Field.inv25519(denomInv, denom);
        var aOverDenom = Ed25519Field.gf();
        Ed25519Field.mul(aOverDenom, denomInv, A_MONT);
        var candidate = Ed25519Field.gf();
        negate(candidate, aOverDenom);

        var rhs = Ed25519Field.gf();
        curve25519Polynomial(rhs, candidate);
        var nonResidue = isNonResidue(rhs);

        var aIfNqr = Ed25519Field.gf();
        setIfBitOne(aIfNqr, A_MONT, nonResidue);
        var combined = Ed25519Field.gf();
        Ed25519Field.add(combined, candidate, aIfNqr);
        var negated = Ed25519Field.gf();
        negate(negated, combined);
        setIfBitOne(combined, negated, nonResidue);
        Ed25519Field.set25519(o, combined);
    }

    /**
     * Lifts a Curve25519 Montgomery u-coordinate to an Ed25519
     * extended-Edwards point with the requested sign.
     *
     * @implNote
     * This implementation computes {@code y = (u-1)/(u+1)} (the
     * standard birational map), recovers the corresponding {@code v} via
     * the Curve25519 polynomial and a square root, composes the Ed25519
     * x-coordinate as {@code u * sqrt(-486664) / v}, and conditionally
     * negates it so its parity matches the requested sign bit.
     *
     * @param p    the destination point
     * @param u    the Montgomery u
     * @param sign the sign bit ({@code 0} or {@code 1}) for the
     *             recovered Edwards x
     */
    private static void liftMontToP3(long[][] p, long[] u, long sign) {
        var sqrtNegAPlusTwo = Ed25519Field.gf();
        Ed25519Field.unpack25519(sqrtNegAPlusTwo, SQRT_NEG_A_PLUS_2_BYTES);

        var edwardsY = Ed25519Field.gf();
        montUToEdwardsY(edwardsY, u);

        var rhs = Ed25519Field.gf();
        curve25519Polynomial(rhs, u);
        var v = Ed25519Field.gf();
        sqrt(v, rhs);

        var uTimesK = Ed25519Field.gf();
        Ed25519Field.mul(uTimesK, u, sqrtNegAPlusTwo);
        var vInv = Ed25519Field.gf();
        Ed25519Field.inv25519(vInv, v);
        var edwardsX = Ed25519Field.gf();
        Ed25519Field.mul(edwardsX, uTimesK, vInv);

        var negated = Ed25519Field.gf();
        negate(negated, edwardsX);
        var parityMismatch = Ed25519Point.par25519(edwardsX) ^ sign;
        setIfBitOne(edwardsX, negated, parityMismatch);

        Ed25519Field.set25519(p[0], edwardsX);
        Ed25519Field.set25519(p[1], edwardsY);
        Ed25519Field.set25519(p[2], Ed25519Field.gfFromSmall(1));
        Ed25519Field.mul(p[3], p[0], p[1]);
    }

    /**
     * Multiplies a point by the Ed25519 cofactor {@code 8} via three
     * doublings.
     *
     * <p>Maps the output of {@link #liftMontToP3} into the prime-order
     * subgroup.
     *
     * @implNote
     * This implementation alternates between extended (4-element) and
     * projective (3-element) representations to avoid recomputing the
     * {@code T} coordinate when only the next double is needed; each
     * doubling uses the Hisil-Wong-Carter-Dawson formula via
     * {@link #applyDoublingFormula}.
     *
     * @param r the destination point
     * @param p the input point
     */
    private static void cofactorMul8(long[][] r, long[][] p) {
        var p3Tmp = Ed25519Point.p3();
        var p2Tmp = new long[][]{Ed25519Field.gf(), Ed25519Field.gf(), Ed25519Field.gf()};
        doubleP3ToP3(p3Tmp, p);
        projectFromP3(p2Tmp, p3Tmp);
        applyDoublingFormula(p3Tmp, p2Tmp);
        projectFromP3(p2Tmp, p3Tmp);
        applyDoublingFormula(p3Tmp, p2Tmp);
        finalizeFromP3(r, p3Tmp);
    }

    /**
     * Computes {@code dst = 2 * src} where both are extended-Edwards
     * points (4-element form), dropping to projective form internally to
     * invoke the doubling transform.
     *
     * @param dst the destination 4-element point
     * @param src the input 4-element point
     */
    private static void doubleP3ToP3(long[][] dst, long[][] src) {
        var threeTuple = new long[][]{Ed25519Field.gf(), Ed25519Field.gf(), Ed25519Field.gf()};
        Ed25519Field.set25519(threeTuple[0], src[0]);
        Ed25519Field.set25519(threeTuple[1], src[1]);
        Ed25519Field.set25519(threeTuple[2], src[2]);
        applyDoublingFormula(dst, threeTuple);
    }

    /**
     * Projects an extended-Edwards 4-element point into the 3-element
     * tuple {@code (X*T, Y*Z, Z*T)} consumed by
     * {@link #applyDoublingFormula}.
     *
     * @param dst the 3-element destination
     * @param src the 4-element input
     */
    private static void projectFromP3(long[][] dst, long[][] src) {
        Ed25519Field.mul(dst[0], src[0], src[3]);
        Ed25519Field.mul(dst[1], src[1], src[2]);
        Ed25519Field.mul(dst[2], src[2], src[3]);
    }

    /**
     * Finalises the cofactor-multiplication ladder by emitting the full
     * 4-element extended-Edwards form, recovering the {@code T}
     * coordinate as {@code X*Y}.
     *
     * @param dst the 4-element destination
     * @param src the 4-element input from the last doubling
     */
    private static void finalizeFromP3(long[][] dst, long[][] src) {
        Ed25519Field.mul(dst[0], src[0], src[3]);
        Ed25519Field.mul(dst[1], src[1], src[2]);
        Ed25519Field.mul(dst[2], src[2], src[3]);
        Ed25519Field.mul(dst[3], src[0], src[1]);
    }

    /**
     * Applies the Hisil-Wong-Carter-Dawson doubling formula on a
     * 3-element input tuple, producing a 4-element extended-Edwards
     * output.
     *
     * <p>This is the point-doubling transform, distinct from the field
     * multiplication {@link Ed25519Field#mul}.
     *
     * @param dst the 4-element destination
     * @param src the 3-element input
     */
    private static void applyDoublingFormula(long[][] dst, long[][] src) {
        Ed25519Field.square(dst[0], src[0]);
        Ed25519Field.square(dst[2], src[1]);
        squareTimesTwo(dst[3], src[2]);
        Ed25519Field.add(dst[1], src[0], src[1]);
        var tmp = Ed25519Field.gf();
        Ed25519Field.square(tmp, dst[1]);
        Ed25519Field.add(dst[1], dst[2], dst[0]);
        Ed25519Field.sub(dst[2], dst[2], dst[0]);
        Ed25519Field.sub(dst[0], tmp, dst[1]);
        Ed25519Field.sub(dst[3], dst[3], dst[2]);
    }
}
