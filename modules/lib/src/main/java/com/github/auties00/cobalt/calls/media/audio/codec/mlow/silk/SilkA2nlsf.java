package com.github.auties00.cobalt.calls.media.audio.codec.mlow.silk;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Converts monic whitening filter coefficients into normalized line spectral frequencies (NLSFs) in integer
 * fixed point.
 *
 * <p>This is the encoder inverse of {@link SilkNlsf2a}: where {@link SilkNlsf2a} maps an NLSF vector to
 * prediction filter coefficients, this class takes Q16 monic whitening filter coefficients and finds the NLSFs
 * by locating the roots of two Chebyshev domain symmetric polynomials. The filter is split into an even
 * polynomial {@code P} and an odd polynomial {@code Q} (the {@code z = -1} root goes to {@code P} and the
 * {@code z = 1} root to {@code Q}, both divided out for even order), each polynomial is transformed from the
 * {@code cos(n*f)} basis to the {@code cos(f)^n} basis, and the roots of the two polynomials, which strictly
 * interlace, are bracketed and bisected along the cosine table grid to recover the line spectral frequencies
 * in Q15.
 *
 * <p>The whole pipeline runs in fixed point so the result is reproducible across platforms: every shift,
 * rounding, table lookup, and bandwidth expansion fallback is exact integer arithmetic. The single public
 * entry point {@link #a2nlsf(short[], int[], int)} writes the Q15 NLSFs and, as a documented side effect, may
 * modify the input coefficient array in place when a root search fails and bandwidth expansion is applied.
 *
 * <p>Q format conventions. The input coefficients {@code aQ16} are Q16 (the MLow float bridge supplies
 * {@code round(-A[i + 1] * 2^16)}, one bit lower than the Q17 coefficients {@link SilkNlsf2a} emits). The even
 * and odd polynomials {@code P} and {@code Q} are carried in Q16 as well. {@link #evalPoly(int[], int, int)}
 * evaluates a Q16 polynomial at a Q12 cosine point, internally lifting the point to Q16, and returns the value
 * in Q16. The output NLSFs are Q15 in {@code [0, 2^15 - 1]}, formed as {@code (k << 8) + ffrac} where
 * {@code k} indexes the {@value #LSF_COS_TAB_SZ} entry cosine table and {@code ffrac} is the interpolated
 * subgrid fraction.
 *
 * <p>The fixed point helper macros are reproduced as private methods with the exact integer arithmetic of
 * their definitions: {@link #rshiftRound(int, int)} rounds by {@code (((a >> (s - 1)) + 1) >> 1)} (special
 * cased at {@code s == 1}); {@link #smlaww(int, int, int)} returns {@code a + ((b * c) >> 16)} of a 64 bit
 * product; {@link #addRshift(int, int, int)} is {@code a + (b >> shift)}; and
 * {@link #bwExpander32(int[], int, int)} applies the geometric chirp bandwidth expansion. All arithmetic is
 * done in {@code int}/{@code long} two's complement.
 *
 * <p>Scope is the SMPL 16 kHz, 60 ms, mono path with filter order 16 ({@code dd == 8}); the general order
 * arithmetic is carried so the conversion stays a faithful copy of the reference. This type is stateless and
 * thread safe.
 *
 * @implNote This implementation keeps the reference's unrolled order 8 polynomial evaluation as a single
 * rolled loop: the multiply accumulate order is identical, so the integer result is bit identical. The cosine
 * table {@link #LSF_COS_TAB_Q12} is identical to the table in {@link SilkNlsf2a}. Two convergence fallbacks are
 * reproduced exactly: progressive bandwidth expansion on each failed table sweep, and a flat white spectrum
 * escape after {@value #MAX_ITERATIONS} expansions.
 */
public final class SilkA2nlsf {
    /**
     * The logger for {@link SilkA2nlsf}.
     */
    private static final System.Logger LOGGER = Log.get(SilkA2nlsf.class);

    /**
     * Number of binary division refinement steps per root.
     *
     * <p>After a sign change is bracketed between two cosine table grid points the root is bisected this many
     * times to refine the subgrid fraction. This must be no higher than {@code 16 - log2(LSF_COS_TAB_SZ)}, that
     * is {@code 16 - 7 == 9}; the value used is 3.
     */
    private static final int BIN_DIV_STEPS = 3;

    /**
     * Maximum number of bandwidth expansion retries before falling back to a white spectrum.
     */
    private static final int MAX_ITERATIONS = 16;

    /**
     * Size of the cosine approximation table.
     *
     * <p>The table holds this many entries plus one guard entry so the root sweep can read both
     * {@code table[k - 1]} and {@code table[k]} for any {@code k} in {@code [1, 128]}.
     */
    private static final int LSF_COS_TAB_SZ = 128;

    /**
     * Maximum prediction filter order the scratch arrays are sized for.
     *
     * <p>MLow uses order 16; this larger bound is kept so the ported scratch buffers match the reference sizes.
     */
    private static final int MAX_ORDER_LPC = 24;

    /**
     * Largest 16 bit signed value, the clamp ceiling applied to each encoded Q15 NLSF.
     */
    private static final int INT16_MAX = 0x7FFF;

    /**
     * Cosine approximation table in Q12.
     *
     * <p>Entry {@code i} is {@code 2^12 * cos(pi * i / 128)} rounded to an even integer; the trailing guard
     * entry mirrors entry {@code 0} negated. The root sweep reads consecutive entries as the Q12 evaluation
     * points {@code xlo} and {@code xhi} that bracket each polynomial sign change.
     */
    private static final int[] LSF_COS_TAB_Q12 = {
            8192, 8190, 8182, 8170,
            8152, 8130, 8104, 8072,
            8034, 7994, 7946, 7896,
            7840, 7778, 7714, 7644,
            7568, 7490, 7406, 7318,
            7226, 7128, 7026, 6922,
            6812, 6698, 6580, 6458,
            6332, 6204, 6070, 5934,
            5792, 5648, 5502, 5352,
            5198, 5040, 4880, 4718,
            4552, 4382, 4212, 4038,
            3862, 3684, 3502, 3320,
            3136, 2948, 2760, 2570,
            2378, 2186, 1990, 1794,
            1598, 1400, 1202, 1002,
            802, 602, 402, 202,
            0, -202, -402, -602,
            -802, -1002, -1202, -1400,
            -1598, -1794, -1990, -2186,
            -2378, -2570, -2760, -2948,
            -3136, -3320, -3502, -3684,
            -3862, -4038, -4212, -4382,
            -4552, -4718, -4880, -5040,
            -5198, -5352, -5502, -5648,
            -5792, -5934, -6070, -6204,
            -6332, -6458, -6580, -6698,
            -6812, -6922, -7026, -7128,
            -7226, -7318, -7406, -7490,
            -7568, -7644, -7714, -7778,
            -7840, -7896, -7946, -7994,
            -8034, -8072, -8104, -8130,
            -8152, -8170, -8182, -8190,
            -8192
    };

    /**
     * Prevents instantiation of this static holder.
     *
     * <p>All conversion routines are static; the class carries no state.
     */
    private SilkA2nlsf() {
    }

    /**
     * Transforms a polynomial in place from the {@code cos(n*f)} basis to the {@code cos(f)^n} basis.
     *
     * <p>Applies the Chebyshev change of basis by repeated in place subtraction so a polynomial expressed
     * against {@code cos(n*f)} terms becomes expressed against powers of {@code cos(f)}, the form
     * {@link #evalPoly(int[], int, int)} expects. The arithmetic is exact integer subtraction with a single
     * left shift by one.
     *
     * @param p  the polynomial coefficients, modified in place, indices {@code 0..dd}
     * @param dd the polynomial order, half the filter order
     */
    private static void transPoly(int[] p, int dd) {
        for (var k = 2; k <= dd; k++) {
            for (var n = dd; n > k; n--) {
                p[n - 2] -= p[n];
            }
            p[k - 2] -= p[k] << 1;
        }
    }

    /**
     * Evaluates a Q16 polynomial at a Q12 point and returns the value in Q16.
     *
     * <p>Runs Horner's scheme from the highest coefficient down, lifting the Q12 evaluation point to Q16 once
     * and using {@link #smlaww(int, int, int)} for each multiply accumulate so the running value stays in Q16.
     *
     * @param p  the polynomial coefficients in Q16, indices {@code 0..dd}
     * @param x  the evaluation point in Q12
     * @param dd the polynomial order, half the filter order
     * @return the polynomial value at {@code x} in Q16
     */
    private static int evalPoly(int[] p, int x, int dd) {
        // A single rolled loop runs the same multiply accumulate sequence as the order 8 unrolled form and
        // produces identical integer results.
        var y32 = p[dd];
        var xQ16 = x << 4;
        for (var n = dd - 1; n >= 0; n--) {
            y32 = smlaww(p[n], y32, xQ16);
        }
        return y32;
    }

    /**
     * Builds the even and odd transformed polynomials from the Q16 filter coefficients.
     *
     * <p>Splits the monic whitening filter into its even part {@code P} and odd part {@code Q} in the
     * {@code cos(n*f)} basis, divides out the {@code z = -1} root from {@code P} and the {@code z = 1} root
     * from {@code Q} (always present for even filter order), and maps both polynomials to the {@code cos(f)^n}
     * basis with {@link #transPoly(int[], int)}. The two leading coefficients are set to {@code 1 << 16} (Q16
     * one). The buffers {@code p} and {@code q} are written for indices {@code 0..dd}.
     *
     * @param aQ16 the monic whitening filter coefficients in Q16, {@code 2 * dd} entries
     * @param p    the even polynomial output in Q16, indices {@code 0..dd} written
     * @param q    the odd polynomial output in Q16, indices {@code 0..dd} written
     * @param dd   the polynomial order, half the filter order
     */
    private static void init(int[] aQ16, int[] p, int[] q, int dd) {
        p[dd] = 1 << 16;
        q[dd] = 1 << 16;
        for (var k = 0; k < dd; k++) {
            p[k] = -aQ16[dd - k - 1] - aQ16[dd + k];
            q[k] = -aQ16[dd - k - 1] + aQ16[dd + k];
        }

        for (var k = dd; k > 0; k--) {
            p[k - 1] -= p[k];
            q[k - 1] += q[k];
        }

        transPoly(p, dd);
        transPoly(q, dd);
    }

    /**
     * Computes the normalized line spectral frequencies from monic whitening filter coefficients.
     *
     * <p>Builds the even and odd transformed polynomials with {@link #init(int[], int[], int[], int)} and finds
     * their interlacing roots by sweeping the {@value #LSF_COS_TAB_SZ} entry cosine grid, alternating between
     * the {@code P} and {@code Q} polynomials. Each detected sign change is refined with
     * {@value #BIN_DIV_STEPS} binary divisions and a final linear interpolation, then encoded as the Q15 NLSF
     * {@code min((k << 8) + ffrac, INT16_MAX)}. If a full sweep fails to find all roots, the filter is bandwidth
     * expanded by a progressively larger chirp ({@link #bwExpander32(int[], int, int)}) and the sweep restarts;
     * after {@value #MAX_ITERATIONS} expansions the NLSFs are set to a flat white spectrum. The input
     * {@code aQ16} array is modified in place by any bandwidth expansion pass.
     *
     * @param nlsf the output normalized line spectral frequencies in Q15, {@code d} entries written
     * @param aQ16 the monic whitening filter coefficients in Q16, {@code d} entries, modified in place when
     *             bandwidth expansion is applied
     * @param d    the filter order; must be even
     */
    public static void a2nlsf(short[] nlsf, int[] aQ16, int d) {
        var dd = d >> 1;

        var p = new int[MAX_ORDER_LPC / 2 + 1];
        var q = new int[MAX_ORDER_LPC / 2 + 1];
        var pq = new int[][]{p, q};

        init(aQ16, p, q, dd);

        var poly = p;

        var xlo = LSF_COS_TAB_Q12[0];
        var ylo = evalPoly(poly, xlo, dd);

        int rootIx;
        if (ylo < 0) {
            nlsf[0] = 0;
            poly = q;
            ylo = evalPoly(poly, xlo, dd);
            rootIx = 1;
        } else {
            rootIx = 0;
        }
        var k = 1;
        var i = 0;
        var thr = 0;
        while (true) {
            var xhi = LSF_COS_TAB_Q12[k];
            var yhi = evalPoly(poly, xhi, dd);

            if ((ylo <= 0 && yhi >= thr) || (ylo >= 0 && yhi <= -thr)) {
                thr = yhi == 0 ? 1 : 0;

                var ffrac = -256;
                for (var m = 0; m < BIN_DIV_STEPS; m++) {
                    var xmid = rshiftRound(xlo + xhi, 1);
                    var ymid = evalPoly(poly, xmid, dd);

                    if ((ylo <= 0 && ymid >= 0) || (ylo >= 0 && ymid <= 0)) {
                        xhi = xmid;
                        yhi = ymid;
                    } else {
                        xlo = xmid;
                        ylo = ymid;
                        ffrac = addRshift(ffrac, 128, m);
                    }
                }

                if (Math.abs(ylo) < 65536) {
                    var den = ylo - yhi;
                    var nom = (ylo << (8 - BIN_DIV_STEPS)) + (den >> 1);
                    if (den != 0) {
                        ffrac += nom / den;
                    }
                } else {
                    ffrac += ylo / ((ylo - yhi) >> (8 - BIN_DIV_STEPS));
                }
                nlsf[rootIx] = (short) Math.min((k << 8) + ffrac, INT16_MAX);

                rootIx++;
                if (rootIx >= d) {
                    break;
                }
                poly = pq[rootIx & 1];

                xlo = LSF_COS_TAB_Q12[k - 1];
                ylo = (1 - (rootIx & 2)) << 12;
            } else {
                k++;
                xlo = xhi;
                ylo = yhi;
                thr = 0;

                if (k > LSF_COS_TAB_SZ) {
                    i++;
                    if (i > MAX_ITERATIONS) {
                        if (Log.WARNING) {
                            LOGGER.log(Level.WARNING,
                                    "a2nlsf root search exhausted {0} bandwidth-expansion retries, falling back to flat spectrum, order={1}",
                                    MAX_ITERATIONS, d);
                        }
                        nlsf[0] = (short) ((1 << 15) / (d + 1));
                        for (k = 1; k < d; k++) {
                            nlsf[k] = (short) (nlsf[k - 1] + nlsf[0]);
                        }
                        return;
                    }

                    bwExpander32(aQ16, d, 65536 - (1 << i));

                    init(aQ16, p, q, dd);
                    poly = p;
                    xlo = LSF_COS_TAB_Q12[0];
                    ylo = evalPoly(poly, xlo, dd);
                    if (ylo < 0) {
                        nlsf[0] = 0;
                        poly = q;
                        ylo = evalPoly(poly, xlo, dd);
                        rootIx = 1;
                    } else {
                        rootIx = 0;
                    }
                    k = 1;
                }
            }
        }
    }

    /**
     * Bandwidth expands a 32 bit AR filter in place.
     *
     * <p>Each coefficient is multiplied by a chirp factor in Q16 that geometrically decays from
     * {@code chirpQ16} toward zero, pulling the filter poles inward toward the unit circle so the root search
     * can converge. The decay update uses {@link #smulww(int, int)} and {@link #rshiftRound(int, int)} so the
     * integer trajectory matches the reference.
     *
     * @param ar       the AR filter without the leading {@code 1}, modified in place, {@code d} entries
     * @param d        the filter length
     * @param chirpQ16 the initial chirp factor in Q16
     */
    private static void bwExpander32(int[] ar, int d, int chirpQ16) {
        var chirpMinusOneQ16 = chirpQ16 - 65536;
        for (var i = 0; i < d - 1; i++) {
            ar[i] = smulww(chirpQ16, ar[i]);
            chirpQ16 += rshiftRound(chirpQ16 * chirpMinusOneQ16, 16);
        }
        ar[d - 1] = smulww(chirpQ16, ar[d - 1]);
    }

    /**
     * Returns {@code a >> shift} with round to nearest on a 32 bit operand.
     *
     * <p>For {@code shift == 1} this is {@code (a >> 1) + (a & 1)}; otherwise it is
     * {@code (((a >> (shift - 1)) + 1) >> 1)}. The arithmetic right shift matches signed shifting.
     *
     * @param a     the value to shift
     * @param shift the right shift amount; at least one
     * @return the rounded shifted value
     */
    private static int rshiftRound(int a, int shift) {
        return shift == 1 ? (a >> 1) + (a & 1) : ((a >> (shift - 1)) + 1) >> 1;
    }

    /**
     * Returns {@code a + (b >> shift)}.
     *
     * <p>The right shift is a plain arithmetic shift with {@code shift >= 0}.
     *
     * @param a     the accumulator
     * @param b     the value to shift before adding
     * @param shift the right shift amount; nonnegative
     * @return {@code a + (b >> shift)}
     */
    private static int addRshift(int a, int b, int shift) {
        return a + (b >> shift);
    }

    /**
     * Returns {@code a + ((b * c) >> 16)} of the 64 bit product.
     *
     * <p>The product {@code b * c} is formed in 64 bits, right shifted by 16, narrowed to {@code int}, and
     * added to {@code a}.
     *
     * @param a the accumulator
     * @param b the first factor
     * @param c the second factor
     * @return the accumulated value
     */
    private static int smlaww(int a, int b, int c) {
        return a + (int) (((long) b * c) >> 16);
    }

    /**
     * Returns {@code (a * b) >> 16} of the 64 bit product.
     *
     * @param a the first factor
     * @param b the second factor
     * @return the 64 bit product right shifted by 16, narrowed to {@code int}
     */
    private static int smulww(int a, int b) {
        return (int) (((long) a * b) >> 16);
    }
}
