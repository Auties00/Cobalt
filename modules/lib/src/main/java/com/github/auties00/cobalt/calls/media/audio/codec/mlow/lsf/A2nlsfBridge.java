package com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.silk.SilkA2nlsf;

/**
 * Adapts MLow float linear prediction filter coefficients to normalized line spectral frequencies (NLSFs).
 *
 * <p>This is the encoder counterpart of {@link NlsfBridge}: the MLow encoder computes a float prediction
 * filter and must express it as line spectral frequencies for quantization. The conversion runs in SILK's
 * integer fixed point core, because the rest of MLow is built with fast math float arithmetic that would
 * perturb the bit exact integer pipeline. This class is the bridge: it quantizes the float filter to Q16
 * {@code int32}, runs the integer core {@link SilkA2nlsf#a2nlsf(short[], int[], int)}, and converts the
 * resulting Q15 NLSFs back to {@code float} radians.
 *
 * <p>The conversion drops the leading {@code a[0] == 1} and negates the remaining taps:
 * {@code a_Q16[i] = round(-A[i + 1] * 2^16)} for {@code i} in {@code [0, LPC_ORDER)}. The integer core writes
 * Q15 line spectral frequencies, which the bridge maps back to radians by
 * {@code nlsf[i] = lsf_Q15[i] / 32768 * SMPL_PI}. The Q16 sign convention is the exact inverse of
 * {@link NlsfBridge}, which reads {@code a[i + 1] = -a32_QA1[i] / 2^17} from the Q17 coefficients; the encoder
 * direction here uses Q16 because {@code silk_A2NLSF} consumes Q16 coefficients.
 *
 * <p>The integer intermediates are bit exact against the reference encoder. The float result matches the
 * reference single precision NLSFs to within IEEE-754 rounding of the one division and multiply per
 * coefficient.
 *
 * <p>Scope is the SMPL 16 kHz, 60 ms, mono path with order {@value #LPC_ORDER}; the reference is hard coded to
 * that order, and this bridge exposes exactly that order. This type is stateless and thread safe.
 *
 * @implNote This implementation quantizes each float tap to Q16 with {@code roundf(-A[i + 1] * 65536)}
 * reproduced by {@link #roundf(float)} (ties away from zero); this is deliberately not
 * {@link Math#round(float)}, whose {@code floor(x + 0.5f)} rounds negative ties toward positive infinity and
 * produces an off by one Q16 tap that flips the low NLSF roots. The {@code silk_A2NLSF} core may bandwidth
 * expand its coefficient array in place, so a fresh Q16 array is allocated per call and the caller's filter is
 * never mutated. The pi constant is the {@code float} rounded {@link #SMPL_PI} the reference uses, identical to
 * {@link NlsfBridge#nlsf2a(float[])}.
 */
public final class A2nlsfBridge {
    /**
     * Linear prediction order of the MLow short term filter, which is also the length of the NLSF vector.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Upper band edge of the NLSF range, the {@code float} rounded value of pi.
     *
     * <p>This is the exact single precision value the reference uses to scale the Q15 cosine table domain back
     * to radians; the constant is load bearing because it sets the radians scale of the result.
     */
    private static final float SMPL_PI = 3.1415926535897f;

    /**
     * Prevents instantiation of this static holder.
     *
     * <p>The conversion is a pure function with no state.
     */
    private A2nlsfBridge() {
    }

    /**
     * Converts a float linear prediction filter to an order {@value #LPC_ORDER} float NLSF vector.
     *
     * <p>Drops the leading {@code a[0]}, quantizes the remaining {@value #LPC_ORDER} taps to Q16 by
     * {@code round(-A[i + 1] * 65536)}, runs the integer core {@link SilkA2nlsf#a2nlsf(short[], int[], int)} to
     * obtain the Q15 NLSFs, and maps them back to radians by {@code nlsf[i] = lsf_Q15[i] / 32768 * SMPL_PI}.
     * The Q16 coefficient buffer is allocated fresh inside this method, so the integer core's in place
     * bandwidth expansion never affects the caller's array {@code a}.
     *
     * @param a the monic prediction filter, at least {@value #LPC_ORDER}{@code  + 1} entries, with
     *          {@code a[0] == 1} and the {@value #LPC_ORDER} taps following
     * @return a freshly allocated NLSF vector of {@value #LPC_ORDER} entries in radians within
     *         {@code (0, SMPL_PI)}
     */
    public static float[] a2nlsf(float[] a) {
        var aQ16 = new int[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            aQ16[i] = roundf(-a[i + 1] * 65536.0f);
        }

        var lsfQ15 = new short[LPC_ORDER];
        SilkA2nlsf.a2nlsf(lsfQ15, aQ16, LPC_ORDER);

        var nlsf = new float[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            nlsf[i] = lsfQ15[i] / 32768.0f * SMPL_PI;
        }
        return nlsf;
    }

    /**
     * Rounds a {@code float} to the nearest integer with halfway cases rounded away from zero, matching the C
     * {@code roundf} contract.
     *
     * <p>This is the rounding mode the reference's {@code roundf(-A[i + 1] * 65536)} applies, and it is not the
     * same as {@link Math#round(float)}: {@code Math.round} computes {@code floor(x + 0.5f)}, which rounds
     * halfway cases toward positive infinity, so a negative tie like {@code -0.5f} rounds to {@code 0} instead
     * of the {@code -1} that {@code roundf} produces. A single such off by one in the Q16 coefficient fed to
     * {@link SilkA2nlsf} flips the low NLSF roots and the spectral weighting that depends on them, so the away
     * from zero direction is load bearing for bit exact line spectral frequencies.
     *
     * <p>The result is formed in the {@code float} domain to mirror {@code roundf}, taking
     * {@code floor(v + 0.5f)} for nonnegative inputs and {@code ceil(v - 0.5f)} for negative inputs, then
     * truncating to {@code int} exactly as the reference {@code (opus_int32)roundf(...)} cast does.
     *
     * @param v the value to round
     * @return {@code v} rounded to the nearest integer with ties away from zero
     */
    private static int roundf(float v) {
        return (int) (v >= 0.0f ? (float) StrictMath.floor(v + 0.5f) : (float) StrictMath.ceil(v - 0.5f));
    }
}
