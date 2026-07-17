package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.EncoderTables;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.MiscTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Searches the closed loop adaptive codebook gain (long term prediction gain) for one voiced analysis by
 * synthesis subframe of the MLow speech codec.
 *
 * <p>For a voiced subframe the open loop pitch estimator ({@link OpenLoopPitch}) has fixed the pitch lags and
 * the analysis by synthesis loop has synthesized the two tap symmetric long term prediction basis from the
 * adaptive codebook history. This class projects that basis through the perceptually weighted impulse response
 * auto correlation, walks the adaptive codebook gain codebook to pick the gain pair minimizing the weighted
 * reconstruction error scaled by the entropy coder bit cost, and produces the residual fixed codebook target
 * that the subsequent fixed codebook search drives.
 *
 * <p>{@link #search} returns three things: the chosen adaptive codebook gain index, the fixed codebook target
 * {@code dLtp} (the perceptually weighted target with the dequantized adaptive codebook contribution removed),
 * and an {@link AcbParams} holder carrying the weighted cross correlation system and the residual weighted error
 * floor. The fixed codebook search ({@link FcbSearch}) consumes {@code dLtp}; the gain quantizer
 * ({@link GainQuantizer#quantizeVoiced}) consumes the whole {@link AcbParams} to re decide the adaptive codebook
 * gain jointly with the fixed codebook gain. The adaptive codebook index this class returns is provisional: it
 * is used only to form {@code dLtp}; the committed bitstream index is the one
 * {@link GainQuantizer#quantizeVoiced} produces.
 *
 * <p>The weighted error of a candidate gain pair {@code g} is {@code werrIn + g' Phi g - 2 d' g}, where
 * {@code Phi} is the weighted basis auto correlation and {@code d} is the basis against target cross
 * correlation. The rate distortion cost multiplies that weighted error by the conditional inverse probability
 * penalty of the index under the previous subframe's committed adaptive codebook index (with {@code -1}
 * selecting the unconditional context). The cost tables come from {@link EncoderTables#gainCosts()} and the gain
 * codebooks from {@link MiscTables}.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono low band voiced path with two gain taps. All accumulations are single
 * precision to mirror the reference codec arithmetic. This type is stateless and thread safe.
 */
public final class AcbSearch {
    /**
     * The logger for {@link AcbSearch}.
     */
    private static final System.Logger LOGGER = Log.get(AcbSearch.class);

    /**
     * Number of adaptive codebook gain taps evaluated jointly, fixed at two for the low band voiced path.
     */
    private static final int ACBG_M = 2;

    /**
     * Number of entries in the adaptive codebook gain codebook.
     */
    private static final int ACBG_N = 16;

    /**
     * Maximum low band subframe length in samples; the flipped weighting column {@code phiFlip} is centered at
     * this index.
     */
    private static final int MAX_SF_LEN = 160;

    /**
     * The Q14 scale {@code 1 / 2^14} that dequantizes the adaptive codebook gain codebook entries.
     */
    private static final float Q14_SCALE = 1.0f / (1 << 14);

    /**
     * The gain inverse probability cost tables driving the rate distortion penalty of each candidate index.
     */
    private final EncoderTables.GainCosts gainCosts;

    /**
     * Holds the weighted adaptive codebook cross correlation system the gain search builds.
     *
     * <p>This holder threads the search state forward to {@link GainQuantizer#quantizeVoiced}, which augments it
     * with the fixed codebook excitation to re decide the gains jointly.
     *
     * @param werrIn      the residual weighted error floor (the zero input response and target energy)
     * @param phiAcb      the two by two weighted basis auto correlation, stored row major
     * @param dAcbLpc     the basis against target cross correlation, two entries
     * @param acbBasisPhi the perceptually weighted adaptive codebook basis, {@code 2 * fcbSubfrlen} entries
     */
    public record AcbParams(float werrIn, float[] phiAcb, float[] dAcbLpc, float[] acbBasisPhi) {
    }

    /**
     * Carries the closed loop adaptive codebook search result for one voiced subframe.
     *
     * @param acbIdx the provisional adaptive codebook gain index used to form {@link #dLtp()}; the committed
     *               bitstream index is produced by {@link GainQuantizer#quantizeVoiced}
     * @param dLtp   the fixed codebook target, the perceptually weighted target with the dequantized adaptive
     *               codebook contribution removed, {@code fcbSubfrlen} entries
     * @param params the weighted cross correlation system the gain quantizer consumes
     */
    public record Result(int acbIdx, float[] dLtp, AcbParams params) {
    }

    /**
     * Constructs an adaptive codebook gain search bound to the shared gain cost tables.
     */
    public AcbSearch() {
        this.gainCosts = EncoderTables.gainCosts();
    }

    /**
     * Runs the closed loop adaptive codebook gain search for one voiced subframe.
     *
     * <p>Projects the long term prediction basis through the perceptually weighted impulse response auto
     * correlation, picks the adaptive codebook gain index minimizing the rate distortion scaled weighted error,
     * and forms the fixed codebook target with the chosen contribution removed.
     *
     * @param phiFlip     the flipped perceptually weighted impulse response auto correlation column the analysis
     *                    by synthesis loop builds; centered at the maximum subframe length, at least twice that
     *                    length in entries
     * @param lResp       the perceptual response length
     * @param acbBasis    the long term prediction basis synthesized from the adaptive codebook history,
     *                    {@code 2 * fcbSubfrlen} entries
     * @param dLpc        the perceptually weighted target cross correlation, {@code fcbSubfrlen} entries
     * @param werrIn      the residual weighted error floor
     * @param fcbSubfrlen the subframe length in samples
     * @param lowRate     {@code true} selects the low rate gain codebook and cost rows
     * @param prevAcbIdx  the previous subframe's committed adaptive codebook index, or {@code -1} for the
     *                    unconditional context
     * @return the provisional adaptive codebook index, fixed codebook target, and weighted cross correlation
     *         system
     */
    public Result search(float[] phiFlip, int lResp, float[] acbBasis, float[] dLpc, float werrIn,
                         int fcbSubfrlen, boolean lowRate, int prevAcbIdx) {
        var acbBasisPhi = new float[ACBG_M * fcbSubfrlen];
        var phiAcb = new float[ACBG_M * ACBG_M];
        var dAcbLpc = new float[ACBG_M];
        // Offset of the first valid sample of the symmetric weighting column.
        var symBase = MAX_SF_LEN - lResp + 1;
        for (var m = 0; m < ACBG_M; m++) {
            multSymToepl2(phiFlip, symBase, lResp, acbBasis, m * fcbSubfrlen, acbBasisPhi, m * fcbSubfrlen, fcbSubfrlen);
            for (var i = 0; i < ACBG_M; i++) {
                phiAcb[m * ACBG_M + i] = dotProd(acbBasis, i * fcbSubfrlen, acbBasisPhi, m * fcbSubfrlen, fcbSubfrlen);
            }
            dAcbLpc[m] = dotProd(acbBasis, m * fcbSubfrlen, dLpc, 0, fcbSubfrlen);
        }

        var bestRd = 1e30f;
        var bestAcbgIdx = 0;
        var transitionIdx = prevAcbIdx == -1 ? 0 : (prevAcbIdx + 1);
        var acbgInvProb = lowRate ? gainCosts.acbgInvProbLr()[transitionIdx] : gainCosts.acbgInvProbHr()[transitionIdx];
        var cbAcbgains = lowRate ? MiscTables.ACB_GAINS_LR_Q14 : MiscTables.ACB_GAINS_HR_Q14;
        var acbGains = new float[ACBG_M];
        for (var n = 0; n < ACBG_N; n++) {
            for (var m = 0; m < ACBG_M; m++) {
                acbGains[m] = cbAcbgains[n * ACBG_M + m] * Q14_SCALE;
            }
            var werrOut = werrIn + wnrg2(phiAcb, acbGains)
                          - 2.0f * (dAcbLpc[0] * acbGains[0] + dAcbLpc[1] * acbGains[1]);
            var rd = werrOut * acbgInvProb[n];
            if (rd < bestRd) {
                bestRd = rd;
                bestAcbgIdx = n;
            }
        }

        // Fixed codebook target: dLtp = dLpc - g0*acbBasisPhi[0] - g1*acbBasisPhi[1].
        var dLtp = new float[fcbSubfrlen];
        var g = -cbAcbgains[bestAcbgIdx * ACBG_M] * Q14_SCALE;
        for (var i = 0; i < fcbSubfrlen; i++) {
            dLtp[i] = dLpc[i] + g * acbBasisPhi[i];
        }
        g = -cbAcbgains[bestAcbgIdx * ACBG_M + 1] * Q14_SCALE;
        for (var i = 0; i < fcbSubfrlen; i++) {
            dLtp[i] += g * acbBasisPhi[fcbSubfrlen + i];
        }

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "acb search: subframeLen={0} lowRate={1} prevIdx={2} chosenIdx={3}",
                    fcbSubfrlen, lowRate, prevAcbIdx, bestAcbgIdx);
        }
        return new Result(bestAcbgIdx, dLtp, new AcbParams(werrIn, phiAcb, dAcbLpc, acbBasisPhi));
    }

    /**
     * Multiplies a symmetric Toeplitz weighting against a vector.
     *
     * <p>The weighting is the symmetric column {@code c} centered at {@code cBase + lResp - 1}; output element
     * {@code n} is the dot product of the column window against the matching window of {@code x}. The run splits
     * into a ramp up, a steady, and a ramp down region so the column window slides exactly as the symmetric
     * Toeplitz band dictates.
     *
     * @param c     the symmetric weighting column array
     * @param cBase the offset of the column's first valid sample
     * @param lResp the half bandwidth
     * @param x     the input vector array
     * @param xOff  the offset of the input vector
     * @param y     the output array
     * @param yOff  the offset of the output vector
     * @param n     the output length
     */
    private static void multSymToepl2(float[] c, int cBase, int lResp, float[] x, int xOff,
                                      float[] y, int yOff, int n) {
        var idx = 0;
        var len = lResp;
        for (; idx < lResp - 1; idx++) {
            y[yOff + idx] = dotProd(c, cBase + lResp - 1 - idx, x, xOff, len++);
        }
        len = 2 * lResp;
        for (; idx < n - lResp; idx++) {
            y[yOff + idx] = dotProd(c, cBase, x, xOff + idx - lResp + 1, len);
        }
        for (; idx < n; idx++) {
            y[yOff + idx] = dotProd(c, cBase, x, xOff + idx - lResp + 1, --len);
        }
    }

    /**
     * Evaluates the two variable weighted energy quadratic form.
     *
     * <p>Computes {@code x' C x} for the symmetric two by two matrix {@code C} held row major and the two vector
     * {@code x}, in a fixed accumulation order.
     *
     * @param c the row major two by two matrix
     * @param x the two vector
     * @return the quadratic form {@code x' C x}
     */
    private static float wnrg2(float[] c, float[] x) {
        return x[0] * (c[0] * x[0] + c[1] * x[1])
                + x[1] * (c[2] * x[0] + c[3] * x[1]);
    }

    /**
     * Computes a single precision dot product over a window of two arrays.
     *
     * <p>The accumulation is split into four independent lane sums, each gathering the products at indices
     * congruent to its lane modulo four, combined as {@code (s0 + s2) + (s1 + s3)}, with the trailing
     * {@code len mod 4} products added sequentially. This reproduces the four lane packed single reduction the
     * reference codec is compiled into: the lane structure changes the rounding relative to a strict left to
     * right sum, and the searched targets are near zero after adaptive codebook cancellation, so the lane order
     * is load bearing.
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
}
