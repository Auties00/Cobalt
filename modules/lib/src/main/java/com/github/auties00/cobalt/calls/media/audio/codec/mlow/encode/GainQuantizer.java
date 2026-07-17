package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.EncoderTables;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.MiscTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Jointly quantizes the adaptive codebook and fixed codebook gains of one analysis by synthesis (AbS) subframe
 * of the MLow speech codec.
 *
 * <p>After the closed loop adaptive codebook (ACB) search and the fixed codebook (FCB) pulse search have run,
 * the encoder commits the final quantized gains. For a voiced subframe this is a joint rate distortion
 * decision: {@link #quantizeVoiced} walks the full adaptive codebook gain codebook crossed with a small window
 * of fixed codebook gain levels around the open loop estimate, scores each candidate by its perceptually
 * weighted reconstruction error scaled by the entropy coder bit cost of the pair, and keeps the cheapest. The
 * decoder later dequantizes the indices this routine selects. For an unvoiced subframe there is no adaptive
 * codebook and the fixed codebook gain is a single scalar quantization, {@link #quantizeUnvoiced}.
 *
 * <p>{@link #quantizeVoiced} re decides the adaptive codebook gain index jointly with the fixed codebook gain,
 * overriding the index {@link AcbSearch} chose while the fixed codebook excitation was excluded. It rebuilds
 * the {@code (ACBG_M + 1)}-dimensional cross correlation system that adds the just searched fixed codebook
 * excitation as an extra basis vector, then minimizes the joint weighted error. The adaptive codebook index
 * this routine returns is the one that goes into the bitstream; the one {@link AcbSearch} returned only formed
 * the fixed codebook target.
 *
 * <p>The cost tables are entropy coder self information exponentiated into multiplicative rate distortion
 * penalties ({@code pow(2, bits * mu)}), supplied by {@link EncoderTables#gainCosts()}; the adaptive codebook
 * gain codebooks and fixed codebook voiced gain table come from {@link MiscTables} and a locally built voiced
 * gain table. The conditional cost rows are selected by the previous subframe's committed indices, which the
 * AbS loop threads in through {@link #quantizeVoiced}; a previous index of {@code -1} selects the
 * unconditional first symbol context.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono low band path. The rate distortion weight is a positive build constant,
 * so the joint search branch is always taken and the scalar only branch is unreachable. All accumulations are
 * single precision. This type holds immutable gain tables and is thread safe; one instance may be shared, or
 * the static entry points called directly.
 *
 * @implNote This implementation forms the fixed codebook gain window in two steps: the first index is
 * {@code floor((gainDb - V_GAIN_MIN_DB) / V_GAIN_STEP_DB) - (N_GAIN_STEPS - 1) / 2} clamped to
 * {@code [0, maxGainIdx - 1]}, and the window is the {@code N_GAIN_STEPS} consecutive levels from there. The
 * joint error is the quadratic form {@code werrIn + x' Phi x - 2 d' x} over the vector
 * {@code x = (acbGain0, acbGain1, fcbGain)}, scaled by the product of the adaptive codebook context cost and
 * the fixed codebook context cost (absolute or delta). The delta cost index applies the offset
 * {@code floor((V_GAIN_MIN_DB - V_GAIN_MAX_DB) / V_GAIN_STEP_DB)} so a previous index minus candidate delta
 * maps to the correct cumulative mass function row.
 */
public final class GainQuantizer {
    /**
     * The logger for {@link GainQuantizer}.
     */
    private static final System.Logger LOGGER = Log.get(GainQuantizer.class);

    /**
     * Number of adaptive codebook gain taps.
     */
    private static final int ACBG_M = 2;

    /**
     * Number of adaptive codebook gain codebook entries.
     */
    private static final int ACBG_N = 16;

    /**
     * Number of voiced fixed codebook absolute gain levels.
     */
    private static final int FCBG_V_N = 34;

    /**
     * Index span of the unvoiced fixed codebook gain table; the table has {@code UV_GAIN_IDX_LEN + 1} entries.
     */
    private static final int UV_GAIN_IDX_LEN = 90;

    /**
     * Minimum voiced fixed codebook gain in decibels.
     */
    private static final float V_GAIN_MIN_DB = -100.0f;

    /**
     * Maximum voiced fixed codebook gain in decibels.
     */
    private static final float V_GAIN_MAX_DB = 0.0f;

    /**
     * Voiced fixed codebook gain quantization step in decibels.
     */
    private static final float V_GAIN_STEP_DB = 3.0f;

    /**
     * Minimum unvoiced fixed codebook gain in decibels.
     */
    private static final float UV_GAIN_MIN_DB = -90.0f;

    /**
     * Maximum unvoiced fixed codebook gain in decibels.
     */
    private static final float UV_GAIN_MAX_DB = 0.0f;

    /**
     * Unvoiced fixed codebook gain quantization step in decibels.
     */
    private static final float UV_GAIN_STEP_DB = 1.0f;

    /**
     * Number of fixed codebook gain levels evaluated around the open loop estimate.
     */
    private static final int N_GAIN_STEPS = 2;

    /**
     * The Q14 scale {@code 1 / 2^14} dequantizing the adaptive codebook gain codebook entries.
     */
    private static final float Q14_SCALE = 1.0f / (1 << 14);

    /**
     * The voiced fixed codebook gain table; entry {@code ix} is
     * {@code 10^(0.05 * (ix * V_GAIN_STEP_DB + V_GAIN_MIN_DB))}.
     */
    private final float[] fcbgainsV;

    /**
     * The unvoiced fixed codebook gain table; entry {@code ix} is
     * {@code 10^(0.05 * (ix * UV_GAIN_STEP_DB + UV_GAIN_MIN_DB))}.
     */
    private final float[] fcbgainsUv;

    /**
     * The gain inverse probability cost tables.
     */
    private final EncoderTables.GainCosts gainCosts;

    /**
     * The quantized voiced gain decision for one subframe.
     *
     * @param acbIdx  the chosen adaptive codebook gain index written to the bitstream; overrides the index
     *                {@link AcbSearch} returned
     * @param fcbIdx  the chosen voiced fixed codebook gain index written to the bitstream
     * @param fcbGain the dequantized fixed codebook gain {@code fcbgainsV[fcbIdx]} the caller scales the fixed
     *                codebook excitation by
     */
    public record VoicedGains(int acbIdx, int fcbIdx, float fcbGain) {
    }

    /**
     * Constructs a gain quantizer with freshly computed fixed codebook gain tables and the shared gain cost
     * tables.
     *
     * <p>The voiced and unvoiced gain tables are computed once; the inverse probability cost tables are the
     * process wide cached {@link EncoderTables#gainCosts()}.
     */
    public GainQuantizer() {
        this.fcbgainsV = buildVoicedGains();
        this.fcbgainsUv = buildUnvoicedGains();
        this.gainCosts = EncoderTables.gainCosts();
    }

    /**
     * Jointly quantizes the adaptive codebook and fixed codebook gains of a voiced subframe.
     *
     * <p>Forms the {@code (ACBG_M + 1)}-dimensional weighted cross correlation system that augments the
     * adaptive codebook basis with the searched fixed codebook excitation, derives a two level window of fixed
     * codebook gain indices around the open loop gain estimate, and over the full adaptive codebook gain
     * codebook crossed with that window keeps the index pair whose weighted error scaled by the entropy coder
     * cost is smallest. The returned adaptive codebook index replaces the one {@link AcbSearch} produced.
     *
     * @param acbg              the adaptive codebook search state holding the input weighted error, the
     *                          weighted adaptive codebook auto correlation, the target cross correlation, and
     *                          the weighted basis
     * @param fcbWnrg           the weighted energy of the fixed codebook excitation at the search optimum
     * @param gainFromSearch    the open loop fixed codebook gain estimate from the pulse search
     * @param excFcb            the fixed codebook excitation of this subframe (after pitch sharpening on the
     *                          low rate path), {@code fcbSubfrlen} entries
     * @param dLpc              the perceptually weighted target cross correlation, {@code fcbSubfrlen} entries
     * @param fcbSubfrlen       the subframe length in samples
     * @param lowRate           {@code true} selects the low rate adaptive codebook codebook and cost rows
     * @param prevAcbIdx        the previous subframe's committed adaptive codebook index, or {@code -1} for the
     *                          unconditional context
     * @param prevFcbIdx        the previous subframe's committed fixed codebook index, or {@code -1} for the
     *                          unconditional context
     * @return the chosen adaptive codebook index, fixed codebook index, and dequantized fixed codebook gain
     */
    public VoicedGains quantizeVoiced(AcbSearch.AcbParams acbg, float fcbWnrg, float gainFromSearch,
                                      float[] excFcb, float[] dLpc, int fcbSubfrlen, boolean lowRate,
                                      int prevAcbIdx, int prevFcbIdx) {
        var fcbgain = Math.max(gainFromSearch, 0.0f);
        var gainDb = 20.0f * log10f(fcbgain + 1.0e-16f);
        gainDb = Math.min(Math.max(gainDb, V_GAIN_MIN_DB), V_GAIN_MAX_DB);
        var maxGainIdx = Math.round((V_GAIN_MAX_DB - V_GAIN_MIN_DB) / V_GAIN_STEP_DB);

        var acbBasisPhi = acbg.acbBasisPhi();
        var acbFcb = new float[ACBG_M];
        for (var i = 0; i < ACBG_M; i++) {
            acbFcb[i] = dotProd(acbBasisPhi, i * fcbSubfrlen, excFcb, 0, fcbSubfrlen);
        }
        // phiAll is the symmetric (ACBG_M + 1) system: the top left block is phiAcb, the border is acbFcb, and
        // the bottom right corner is fcbWnrg, stored row major.
        var phiAll = new float[(ACBG_M + 1) * (ACBG_M + 1)];
        var phiAcb = acbg.phiAcb();
        for (var i = 0; i < ACBG_M; i++) {
            for (var j = 0; j < ACBG_M; j++) {
                phiAll[i * (ACBG_M + 1) + j] = phiAcb[i * ACBG_M + j];
            }
        }
        for (var i = 0; i < ACBG_M; i++) {
            phiAll[i * (ACBG_M + 1) + ACBG_M] = acbFcb[i];
            phiAll[ACBG_M * (ACBG_M + 1) + i] = acbFcb[i];
        }
        phiAll[ACBG_M * (ACBG_M + 1) + ACBG_M] = fcbWnrg;
        var dAll = new float[ACBG_M + 1];
        var dAcbLpc = acbg.dAcbLpc();
        dAll[0] = dAcbLpc[0];
        dAll[1] = dAcbLpc[1];
        dAll[ACBG_M] = dotProd(dLpc, 0, excFcb, 0, fcbSubfrlen);

        var gainIdxs = new int[N_GAIN_STEPS];
        var fcbgains = new float[N_GAIN_STEPS];
        var fcbgInvProb = new float[N_GAIN_STEPS];
        var firstGainIdx = Math.max((int) Math.floor((gainDb - V_GAIN_MIN_DB) / V_GAIN_STEP_DB) - (N_GAIN_STEPS - 1) / 2, 0);
        firstGainIdx = Math.min(firstGainIdx, maxGainIdx - 1);
        var offset = (int) Math.floor((V_GAIN_MIN_DB - V_GAIN_MAX_DB) / V_GAIN_STEP_DB);
        for (var i = 0; i < N_GAIN_STEPS; i++) {
            gainIdxs[i] = firstGainIdx + i;
            fcbgains[i] = fcbgainsV[gainIdxs[i]];
            if (prevFcbIdx == -1) {
                fcbgInvProb[i] = gainCosts.fcbgVInvProb()[gainIdxs[i]];
            } else {
                var delta = prevFcbIdx - gainIdxs[i];
                var cmfIdx = delta - offset;
                fcbgInvProb[i] = gainCosts.fcbgVDeltaInvProb()[cmfIdx];
            }
        }

        var bestRd = 1e30f;
        var bestAcbgIdx = 0;
        var bestFcbgIdx = 0;
        var transitionIdx = prevAcbIdx == -1 ? 0 : (prevAcbIdx + 1);
        var cbAcbgains = lowRate ? MiscTables.ACB_GAINS_LR_Q14 : MiscTables.ACB_GAINS_HR_Q14;
        var acbgInvProb = lowRate ? gainCosts.acbgInvProbLr()[transitionIdx] : gainCosts.acbgInvProbHr()[transitionIdx];
        var gains = new float[ACBG_M + 1];
        for (var n = 0; n < ACBG_N; n++) {
            for (var m = 0; m < ACBG_M; m++) {
                gains[m] = cbAcbgains[n * ACBG_M + m] * Q14_SCALE;
            }
            for (var i = 0; i < N_GAIN_STEPS; i++) {
                gains[ACBG_M] = fcbgains[i];
                var werrOut = acbg.werrIn() + wnrg3(phiAll, gains)
                              - 2.0f * (dAll[0] * gains[0] + dAll[1] * gains[1] + dAll[2] * gains[2]);
                var rd = werrOut * fcbgInvProb[i] * acbgInvProb[n];
                if (rd < bestRd) {
                    bestRd = rd;
                    bestAcbgIdx = n;
                    bestFcbgIdx = gainIdxs[i];
                }
            }
        }

        var fcbIdx = Math.min(Math.max(bestFcbgIdx, 0), maxGainIdx);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "voiced gain quantize: subfrlen={0} lowRate={1} acbIdx={2} fcbIdx={3}",
                    fcbSubfrlen, lowRate, bestAcbgIdx, fcbIdx);
        }
        return new VoicedGains(bestAcbgIdx, fcbIdx, fcbgainsV[fcbIdx]);
    }

    /**
     * Scalar quantizes the fixed codebook gain of an unvoiced subframe.
     *
     * <p>Converts the search gain to decibels, clamps it to the unvoiced gain range, and rounds to the nearest
     * quantizer level.
     *
     * @param gainFromSearch the open loop fixed codebook gain estimate from the pulse search
     * @return the unvoiced fixed codebook gain index in {@code [0, UV_GAIN_IDX_LEN]}
     */
    public static int quantizeUnvoiced(float gainFromSearch) {
        var gainDb = 20.0f * log10f(gainFromSearch + 1.0e-16f);
        gainDb = Math.min(Math.max(gainDb, UV_GAIN_MIN_DB), UV_GAIN_MAX_DB);
        var fcbIdx = Math.round((gainDb - UV_GAIN_MIN_DB) / UV_GAIN_STEP_DB);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "unvoiced gain quantize: fcbIdx={0}", fcbIdx);
        }
        return fcbIdx;
    }

    /**
     * Returns the dequantized unvoiced fixed codebook gain for an index.
     *
     * @param fcbIdx the unvoiced fixed codebook gain index
     * @return the gain {@code fcbgainsUv[fcbIdx]}
     */
    public float unvoicedGain(int fcbIdx) {
        return fcbgainsUv[fcbIdx];
    }

    /**
     * Evaluates the three variable weighted energy quadratic form.
     *
     * <p>Computes {@code x' C x} for the symmetric {@code 3x3} matrix {@code C} held row major and the three
     * vector {@code x}, in a fixed nested accumulation order.
     *
     * @param c the row major {@code 3x3} matrix
     * @param x the three vector
     * @return the quadratic form {@code x' C x}
     */
    private static float wnrg3(float[] c, float[] x) {
        return x[0] * (c[0] * x[0] + c[1] * x[1] + c[2] * x[2])
                + x[1] * (c[3] * x[0] + c[4] * x[1] + c[5] * x[2])
                + x[2] * (c[6] * x[0] + c[7] * x[1] + c[8] * x[2]);
    }

    /**
     * Computes a single precision dot product over a window of two arrays.
     *
     * <p>The reduction runs four lanes: four partial sums gather the products at indices congruent to their
     * lane modulo four, are combined as {@code (s0 + s2) + (s1 + s3)}, and the trailing {@code len mod 4}
     * products are added sequentially. This lane order changes the rounding relative to a strict left to right
     * sum and is reproduced exactly to match the reference accumulator.
     *
     * @param a    the first array
     * @param aOff the offset into the first array
     * @param b    the second array
     * @param bOff the offset into the second array
     * @param len  the number of elements
     * @return the accumulated single precision dot product
     */
    private static float dotProd(float[] a, int aOff, float[] b, int bOff, int len) {
        var s0 = 0.0f;
        var s1 = 0.0f;
        var s2 = 0.0f;
        var s3 = 0.0f;
        var m = len & ~3;
        var i = 0;
        for (; i < m; i += 4) {
            s0 += a[aOff + i] * b[bOff + i];
            s1 += a[aOff + i + 1] * b[bOff + i + 1];
            s2 += a[aOff + i + 2] * b[bOff + i + 2];
            s3 += a[aOff + i + 3] * b[bOff + i + 3];
        }
        var acc = (s0 + s2) + (s1 + s3);
        for (; i < len; i++) {
            acc += a[aOff + i] * b[bOff + i];
        }
        return acc;
    }

    /**
     * Computes the base ten logarithm in single precision.
     *
     * @param x the operand, strictly positive
     * @return the single precision base ten logarithm of {@code x}
     */
    private static float log10f(float x) {
        return (float) Math.log10(x);
    }

    /**
     * Computes the voiced fixed codebook gain table.
     *
     * @implNote This implementation forms the exponent {@code 0.05f * db} in single precision before the power.
     * Scaling by {@code 0.05} in {@code double} and then narrowing the power rounds the exponent the other way
     * at 18 of the 34 levels, shifting the dequantized gain by one unit in the last place; that gain scales the
     * reconstructed fixed codebook excitation, so the shift would propagate through the adaptive codebook ring
     * and the weighting filter memory into every later subframe. A single precision argument to
     * {@link Math#pow(double, double)} reproduces the reference table exactly over all levels.
     *
     * @return a freshly allocated {@code FCBG_V_N}-entry table
     */
    private static float[] buildVoicedGains() {
        var tab = new float[FCBG_V_N];
        for (var ix = 0; ix < FCBG_V_N; ix++) {
            var db = ix * V_GAIN_STEP_DB + V_GAIN_MIN_DB;
            tab[ix] = (float) Math.pow(10.0, 0.05f * db);
        }
        return tab;
    }

    /**
     * Computes the unvoiced fixed codebook gain table.
     *
     * @implNote This implementation forms the exponent {@code 0.05f * db} in single precision for the same
     * reason given on {@link #buildVoicedGains()}: a {@code double} scaled exponent rounds differently at most
     * levels, and the dequantized unvoiced gain scales the reconstructed excitation directly.
     *
     * @return a freshly allocated {@code UV_GAIN_IDX_LEN + 1}-entry table
     */
    private static float[] buildUnvoicedGains() {
        var tab = new float[UV_GAIN_IDX_LEN + 1];
        for (var ix = 0; ix <= UV_GAIN_IDX_LEN; ix++) {
            var db = ix * UV_GAIN_STEP_DB + UV_GAIN_MIN_DB;
            tab[ix] = (float) Math.pow(10.0, 0.05f * db);
        }
        return tab;
    }
}
