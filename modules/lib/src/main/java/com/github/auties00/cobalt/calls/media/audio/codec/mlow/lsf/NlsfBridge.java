package com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.silk.SilkNlsf2a;

/**
 * Converts MLow normalized line spectral frequencies (NLSFs) into float linear prediction filter coefficients.
 *
 * <p>The MLow decoder carries its line spectral frequencies as {@code float} radians in {@code (0, SMPL_PI)},
 * but the conversion to linear prediction coefficients runs through SILK's integer fixed point core (the rest
 * of MLow is built with fast math float arithmetic that would perturb the bit exact integer pipeline). This
 * class is the bridge: it quantizes the float NLSFs to Q15 {@code int16}, runs the integer core
 * {@link SilkNlsf2a#nlsf2a32(int[], short[], int)}, and converts the resulting {@code QA+1} (Q17) integer
 * coefficients back to {@code float}.
 *
 * <p>The float result {@code a} is the monic prediction filter with {@code a[0] == 1} followed by the
 * {@code d} negated, scaled integer coefficients: {@code a[i + 1] = -a32_QA1[i] / 2^17}. The separate float
 * bandwidth expansion stabilization the MLow decoder applies downstream is not part of this bridge.
 *
 * <p>The integer intermediates are bit exact against the native decoder. The float coefficients match the
 * native single precision result to within IEEE-754 rounding of the one division per coefficient; the scaling
 * constant {@code 1 / 2^17} is an exact power of two, so the only rounding is the integer to float widening
 * and the multiply, which both round identically to the reference cast and multiply.
 *
 * <p>Scope is the SMPL 16 kHz / 60 ms / mono low band path with order {@value #LPC_ORDER}. Any order up to
 * {@value #LPC_ORDER} is accepted and a larger one short circuits; this bridge exposes the order 16 conversion
 * the low band decode drives, plus a general order entry point for the order 4 high band case kept for
 * symmetry. This type is stateless and thread safe.
 *
 * @implNote This implementation quantizes each NLSF by multiplying by {@link #SCALE1_NUM} and then dividing by
 * {@link #SMPL_PI}, a {@code float} multiply followed by a {@code float} divide, rather than multiplying by a
 * precomputed reciprocal. The two orders disagree at the rare exact halfway boundary (multiply then divide
 * rounds up, the reciprocal rounds down), and that single {@code int16} flip propagates through the integer
 * polynomial, so the numerator {@link #SCALE1_NUM} and the divisor {@link #SMPL_PI} are kept as separate
 * constants. {@link Math#round(float)} rounds half up, which matches the native round half away from zero for
 * the non negative in band values. The back conversion multiplies by {@link #SCALE2}, which equals
 * {@code 1 / (1 << (QA + 1))} with {@code QA == 16}.
 */
public final class NlsfBridge {
    /**
     * Linear prediction order of the MLow short term filter and the length of the NLSF vector.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Upper band edge of the NLSF range, the single precision value used to scale radians into the Q15
     * quantization domain.
     *
     * <p>This constant is load bearing because it sets the {@code int16} quantization boundary; the exact
     * single precision value must be preserved so the boundary lands identically to the native decoder.
     */
    private static final float SMPL_PI = 3.1415926535897f;

    /**
     * Numerator of the radians to Q15 scale, {@code 32768.0f}.
     *
     * <p>The quantization evaluates {@code nlsf[i] * 32768.0f / SMPL_PI} left to right as
     * {@code (nlsf[i] * 32768.0f) / SMPL_PI}: a {@code float} multiply followed by a {@code float} divide, not
     * a multiply by a precomputed reciprocal. The two orders round differently at the {@code int16} boundary
     * (the exact halfway case rounds up under multiply then divide and down under the reciprocal), which flips
     * an {@code int16} root and then the whole integer polynomial; this numerator is kept separate from
     * {@link #SMPL_PI} so {@link #nlsf2a(float[], int)} reproduces that operation order exactly.
     */
    private static final float SCALE1_NUM = 32768.0f;

    /**
     * Scale mapping a {@code QA+1} (Q17) integer coefficient back to {@code float},
     * {@code 1 / (1 << (QA + 1))} with {@code QA == 16}.
     *
     * <p>This is an exact power of two ({@code 1 / 131072}), so the back conversion introduces only the single
     * rounding of the integer to float multiply.
     */
    private static final float SCALE2 = 1.0f / (1 << (16 + 1));

    /**
     * Prevents instantiation of this static utility holder.
     *
     * <p>The conversion is a pure function with no state.
     */
    private NlsfBridge() {
    }

    /**
     * Converts an order {@value #LPC_ORDER} float NLSF vector to float linear prediction coefficients.
     *
     * <p>Equivalent to {@link #nlsf2a(float[], int)} with {@code d} fixed at {@value #LPC_ORDER}; provided as
     * the common case the low band decode path uses.
     *
     * @param nlsf the normalized line spectral frequencies in radians, {@value #LPC_ORDER} entries in
     *             {@code (0, SMPL_PI)}
     * @return a freshly allocated monic filter of {@value #LPC_ORDER}{@code  + 1} entries, {@code a[0] == 1}
     */
    public static float[] nlsf2a(float[] nlsf) {
        return nlsf2a(nlsf, LPC_ORDER);
    }

    /**
     * Converts a float NLSF vector to float linear prediction coefficients.
     *
     * <p>Quantizes each float NLSF to a Q15 {@code int16} by {@code round(nlsf[i] * 32768.0f / SMPL_PI)} (the
     * multiply then divide order documented on {@link #SCALE1_NUM}), runs the integer core
     * {@link SilkNlsf2a#nlsf2a32(int[], short[], int)} to obtain the {@code QA+1} coefficients, and writes the
     * monic float filter: {@code a[0] = 1} and {@code a[i + 1] = -a32_QA1[i] * SCALE2}. An order above
     * {@value #LPC_ORDER} short circuits with no conversion, returning a filter of all zeros (its {@code a[0]}
     * left at zero).
     *
     * @param nlsf the normalized line spectral frequencies in radians, at least {@code d} entries in
     *             {@code (0, SMPL_PI)}
     * @param d    the filter order; must be {@code 4}, {@code 10}, or {@code 16} for a converted result, and
     *             at most {@value #LPC_ORDER}
     * @return a freshly allocated monic filter of {@code d + 1} entries with {@code a[0] == 1}, or all zeros
     *         when {@code d} exceeds {@value #LPC_ORDER}
     */
    public static float[] nlsf2a(float[] nlsf, int d) {
        var a = new float[d + 1];
        if (d > LPC_ORDER) {
            return a;
        }
        var nlsf16 = new short[d];
        for (var i = 0; i < d; i++) {
            nlsf16[i] = (short) Math.round(nlsf[i] * SCALE1_NUM / SMPL_PI);
        }
        var a32QA1 = new int[LPC_ORDER + 1];
        SilkNlsf2a.nlsf2a32(a32QA1, nlsf16, d);

        a[0] = 1.0f;
        for (var i = 0; i < d; i++) {
            a[i + 1] = -a32QA1[i] * SCALE2;
        }
        return a;
    }
}
