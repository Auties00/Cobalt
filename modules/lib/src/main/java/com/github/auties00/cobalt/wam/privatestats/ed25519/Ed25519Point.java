package com.github.auties00.cobalt.wam.privatestats.ed25519;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.security.MessageDigest;

/**
 * Performs Edwards-curve point arithmetic on Ed25519, layered on
 * {@link Ed25519Field}.
 *
 * <p>Points are held in extended twisted-Edwards coordinates as a
 * 4-element array of {@link Ed25519Field#LIMBS}-limb field elements
 * {@code {X, Y, Z, T}} satisfying {@code x = X/Z}, {@code y = Y/Z}, and
 * {@code x*y = T/Z}.
 *
 * <p>Every operation is constant time with respect to point coordinates
 * and scalar bits; branches on point or scalar data are implemented as
 * masked {@link Ed25519Field#sel25519} swaps rather than data-dependent
 * control flow.
 */
@WhatsAppWebModule(moduleName = "WACryptoEd25519")
@WhatsAppWebModule(moduleName = "WACryptoPrimitives")
public final class Ed25519Point {
    /**
     * Holds the Edwards-curve constant {@code d = -121665 / 121666 (mod
     * p)}, encoded as 16 little-endian radix-{@code 2^16} limbs.
     */
    private static final long[] D = {
            0x78a3, 0x1359, 0x4dca, 0x75eb,
            0xd8ab, 0x4141, 0x0a4d, 0x0070,
            0xe898, 0x7779, 0x4079, 0x8cc7,
            0xfe73, 0x2b6f, 0x6cee, 0x5203
    };

    /**
     * Holds the constant {@code 2 * d}, used in the extended-Edwards
     * addition formula in {@link #add}.
     */
    private static final long[] D2 = {
            0xf159, 0x26b2, 0x9b94, 0xebd6,
            0xb156, 0x8283, 0x149a, 0x00e0,
            0xd130, 0xeef3, 0x80f2, 0x198e,
            0xfce7, 0x56df, 0xd9dc, 0x2406
    };

    /**
     * Holds the affine x-coordinate of the Ed25519 base point
     * {@code B}, encoded as 16 little-endian radix-{@code 2^16} limbs.
     */
    private static final long[] BASE_X = {
            0xd51a, 0x8f25, 0x2d60, 0xc956,
            0xa7b2, 0x9525, 0xc760, 0x692c,
            0xdc5c, 0xfdd6, 0xe231, 0xc0a4,
            0x53fe, 0xcd6e, 0x36d3, 0x2169
    };

    /**
     * Holds the affine y-coordinate of the Ed25519 base point
     * {@code B}, equal to {@code 4/5 (mod p)}.
     */
    private static final long[] BASE_Y = {
            0x6658, 0x6666, 0x6666, 0x6666,
            0x6666, 0x6666, 0x6666, 0x6666,
            0x6666, 0x6666, 0x6666, 0x6666,
            0x6666, 0x6666, 0x6666, 0x6666
    };

    /**
     * Holds the field element {@code sqrt(-1) mod p}, used in
     * {@link #unpackNeg} as the alternate square-root branch.
     */
    private static final long[] I = {
            0xa0b0, 0x4a0e, 0x1b27, 0xc4ee,
            0xe478, 0xad2f, 0x1806, 0x2f43,
            0xd7a7, 0x3dfb, 0x0099, 0x2b4d,
            0xdf0b, 0x4fc1, 0x2480, 0x2b83
    };

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private Ed25519Point() {
        throw new AssertionError("Ed25519Point is a utility class and must not be instantiated");
    }

    /**
     * Allocates a fresh extended-Edwards point.
     *
     * <p>The four field elements are zero-initialised; the caller fills
     * them before use. This is the standard scratch allocation made
     * before invoking {@link #scalarMult}, {@link #scalarMultBase}, or
     * {@link #unpack}.
     *
     * @return a new {@code long[4][LIMBS]} array of zero-initialised
     *         field elements
     */
    public static long[][] p3() {
        return new long[][]{
                Ed25519Field.gf(),
                Ed25519Field.gf(),
                Ed25519Field.gf(),
                Ed25519Field.gf()
        };
    }

    /**
     * Adds two extended-Edwards points in place: {@code p = p + q}.
     *
     * <p>The right operand {@code q} is read only; the result
     * overwrites {@code p}.
     *
     * @implNote
     * This implementation uses the unified Hisil-Wong-Carter-Dawson
     * addition formula on extended twisted-Edwards coordinates; the
     * formula is complete, so it is correct even when {@code p} and
     * {@code q} are equal, and no branch depends on that case.
     *
     * @param p the destination point (mutated)
     * @param q the right operand (read only)
     */
    public static void add(long[][] p, long[][] q) {
        var a = Ed25519Field.gf();
        var b = Ed25519Field.gf();
        var c = Ed25519Field.gf();
        var d = Ed25519Field.gf();
        var e = Ed25519Field.gf();
        var f = Ed25519Field.gf();
        var g = Ed25519Field.gf();
        var h = Ed25519Field.gf();
        var t = Ed25519Field.gf();

        Ed25519Field.sub(a, p[1], p[0]);
        Ed25519Field.sub(t, q[1], q[0]);
        Ed25519Field.mul(a, a, t);
        Ed25519Field.add(b, p[0], p[1]);
        Ed25519Field.add(t, q[0], q[1]);
        Ed25519Field.mul(b, b, t);
        Ed25519Field.mul(c, p[3], q[3]);
        Ed25519Field.mul(c, c, D2);
        Ed25519Field.mul(d, p[2], q[2]);
        Ed25519Field.add(d, d, d);
        Ed25519Field.sub(e, b, a);
        Ed25519Field.sub(f, d, c);
        Ed25519Field.add(g, d, c);
        Ed25519Field.add(h, b, a);

        Ed25519Field.mul(p[0], e, f);
        Ed25519Field.mul(p[1], h, g);
        Ed25519Field.mul(p[2], g, f);
        Ed25519Field.mul(p[3], e, h);
    }

    /**
     * Performs a constant-time conditional swap of two points: when
     * {@code b == 1} swaps {@code p} and {@code q} coordinate by
     * coordinate, otherwise leaves both unchanged.
     *
     * <p>Each coordinate is swapped through the masked
     * {@link Ed25519Field#sel25519} primitive, so no branch depends on
     * {@code b}. This drives the constant-time bit handling inside the
     * Montgomery ladder of {@link #scalarMult}.
     *
     * @param p the first point
     * @param q the second point
     * @param b the swap bit; must be exactly {@code 0} or {@code 1}
     */
    public static void cswap(long[][] p, long[][] q, long b) {
        for (var i = 0; i < 4; i++) {
            Ed25519Field.sel25519(p[i], q[i], b);
        }
    }

    /**
     * Returns the parity of the canonical 32-byte encoding of a field
     * element, the least-significant bit of byte 0.
     *
     * <p>The parity bit is packed into the high bit of byte 31 by
     * {@link #pack} to record the sign of an Edwards x-coordinate, and
     * read back during decoding.
     *
     * @param a the field element
     * @return {@code 0} or {@code 1}
     */
    public static int par25519(long[] a) {
        var d = new byte[Ed25519Field.BYTES];
        Ed25519Field.pack25519(d, a);
        return d[0] & 1;
    }

    /**
     * Performs a constant-time inequality test on two field elements.
     *
     * <p>Compares the canonical encodings of {@code a} and {@code b} and
     * reports whether they differ, without an early-exit branch on the
     * compared bytes.
     *
     * @implNote
     * This implementation compares the canonical 32-byte encodings via
     * {@link MessageDigest#isEqual}, which is documented constant time.
     *
     * @param a the left field element
     * @param b the right field element
     * @return {@code 0} if the canonical encodings match, {@code 1}
     *         otherwise
     */
    public static int neq25519(long[] a, long[] b) {
        var c = new byte[Ed25519Field.BYTES];
        var d = new byte[Ed25519Field.BYTES];
        Ed25519Field.pack25519(c, a);
        Ed25519Field.pack25519(d, b);
        return MessageDigest.isEqual(c, d) ? 0 : 1;
    }

    /**
     * Encodes a point in compressed Edwards form: 32 bytes holding the
     * y-coordinate, with the sign of x packed into the high bit of the
     * last byte.
     *
     * <p>The projective coordinates are first normalised to affine form
     * by dividing through by {@code Z}, then the y-coordinate is
     * serialised and the sign of x is recorded in bit 255 via
     * {@link #par25519}.
     *
     * @param r the 32-byte destination buffer
     * @param p the point to encode
     */
    public static void pack(byte[] r, long[][] p) {
        var tx = Ed25519Field.gf();
        var ty = Ed25519Field.gf();
        var zi = Ed25519Field.gf();
        Ed25519Field.inv25519(zi, p[2]);
        Ed25519Field.mul(tx, p[0], zi);
        Ed25519Field.mul(ty, p[1], zi);
        Ed25519Field.pack25519(r, ty);
        r[31] = (byte) (r[31] ^ (par25519(tx) << 7));
    }

    /**
     * Decodes a 32-byte compressed Edwards point and stores its
     * negation into {@code r}.
     *
     * <p>The "negation on decode" convention follows NaCl/SUPERCOP:
     * Ed25519 verification uses {@code -A} where it would mathematically
     * use {@code A}, so the public unpack routine pre-negates. Callers
     * that want the actual decoded point must negate the X coordinate
     * afterwards, as {@link #unpack} does. The y-coordinate is recovered
     * from the low 255 bits and the x-coordinate from its packed sign
     * bit; a candidate that does not satisfy the curve equation is
     * rejected.
     *
     * @param r the destination point
     * @param p the 32-byte compressed encoding
     * @return {@code 0} on success, {@code -1} when {@code p} does not
     *         encode a valid curve point
     */
    public static int unpackNeg(long[][] r, byte[] p) {
        var t = Ed25519Field.gf();
        var chk = Ed25519Field.gf();
        var num = Ed25519Field.gf();
        var den = Ed25519Field.gf();
        var den2 = Ed25519Field.gf();
        var den4 = Ed25519Field.gf();
        var den6 = Ed25519Field.gf();

        Ed25519Field.set25519(r[2], Ed25519Field.gfFromSmall(1));
        Ed25519Field.unpack25519(r[1], p);
        Ed25519Field.square(num, r[1]);
        Ed25519Field.mul(den, num, D);
        Ed25519Field.sub(num, num, r[2]);
        Ed25519Field.add(den, r[2], den);

        Ed25519Field.square(den2, den);
        Ed25519Field.square(den4, den2);
        Ed25519Field.mul(den6, den4, den2);
        Ed25519Field.mul(t, den6, num);
        Ed25519Field.mul(t, t, den);

        Ed25519Field.pow2523(t, t);
        Ed25519Field.mul(t, t, num);
        Ed25519Field.mul(t, t, den);
        Ed25519Field.mul(t, t, den);
        Ed25519Field.mul(r[0], t, den);

        Ed25519Field.square(chk, r[0]);
        Ed25519Field.mul(chk, chk, den);
        if (neq25519(chk, num) != 0) {
            Ed25519Field.mul(r[0], r[0], I);
        }

        Ed25519Field.square(chk, r[0]);
        Ed25519Field.mul(chk, chk, den);
        if (neq25519(chk, num) != 0) {
            return -1;
        }

        if (par25519(r[0]) == ((p[31] & 0xff) >> 7)) {
            var zero = Ed25519Field.gf();
            Ed25519Field.sub(r[0], zero, r[0]);
        }

        Ed25519Field.mul(r[3], r[0], r[1]);
        return 0;
    }

    /**
     * Decodes a 32-byte compressed Edwards point as the actual encoded
     * point, undoing the negation that {@link #unpackNeg} performs.
     *
     * @implNote
     * This implementation calls {@link #unpackNeg} into a scratch point
     * and then negates X and T in place, since Edwards negation maps
     * {@code (X, Y, Z, T)} to {@code (-X, Y, Z, -T)}.
     *
     * @param r the destination point
     * @param p the 32-byte compressed encoding
     * @return {@code 0} on success, {@code -1} when {@code p} does not
     *         encode a valid curve point
     */
    public static int unpack(long[][] r, byte[] p) {
        var tmp = p3();
        var status = unpackNeg(tmp, p);
        if (status != 0) {
            return -1;
        }
        var zero = Ed25519Field.gf();
        Ed25519Field.sub(r[0], zero, tmp[0]);
        Ed25519Field.set25519(r[1], tmp[1]);
        Ed25519Field.set25519(r[2], tmp[2]);
        Ed25519Field.sub(r[3], zero, tmp[3]);
        return 0;
    }

    /**
     * Performs variable-base scalar multiplication: {@code p = s * q}
     * where {@code s} is a 32-byte little-endian scalar.
     *
     * <p>The {@code q} input is used as scratch space by the ladder and
     * is overwritten; callers must pass a copy of any point they want to
     * keep.
     *
     * @implNote
     * This implementation runs a constant-time Montgomery ladder over
     * the 256 bits of {@code s}, using {@link #cswap} to mask the
     * bit-dependent swap so no branch reveals a scalar bit.
     *
     * @param p the destination point
     * @param q the base point (mutated as scratch space)
     * @param s the scalar, 32 bytes little endian
     */
    public static void scalarMult(long[][] p, long[][] q, byte[] s) {
        Ed25519Field.set25519(p[0], Ed25519Field.gf());
        Ed25519Field.set25519(p[1], Ed25519Field.gfFromSmall(1));
        Ed25519Field.set25519(p[2], Ed25519Field.gfFromSmall(1));
        Ed25519Field.set25519(p[3], Ed25519Field.gf());
        for (var i = 255; i >= 0; --i) {
            var b = ((s[i >>> 3] & 0xff) >> (i & 7)) & 1;
            cswap(p, q, b);
            add(q, p);
            add(p, p);
            cswap(p, q, b);
        }
    }

    /**
     * Performs fixed-base scalar multiplication: {@code p = s * B} where
     * {@code B} is the Ed25519 base point and {@code s} is a 32-byte
     * little-endian scalar.
     *
     * @implNote
     * This implementation materialises the base point in extended
     * coordinates and delegates to {@link #scalarMult}. A precomputed
     * comb table would be faster but is deliberately avoided to keep the
     * layout identical to the reference for diff-validation.
     *
     * @param p the destination point
     * @param s the scalar, 32 bytes little endian
     */
    public static void scalarMultBase(long[][] p, byte[] s) {
        var q = p3();
        Ed25519Field.set25519(q[0], BASE_X);
        Ed25519Field.set25519(q[1], BASE_Y);
        Ed25519Field.set25519(q[2], Ed25519Field.gfFromSmall(1));
        Ed25519Field.mul(q[3], BASE_X, BASE_Y);
        scalarMult(p, q, s);
    }
}
