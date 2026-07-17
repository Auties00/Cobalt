package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.A2nlsfBridge;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.LsfDequantizer;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.EncoderTables;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.FastSqrt;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.EncoderTables.LsfSearch;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.EncoderTables.LsfStage1;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Codebook;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Stage1;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Stage2;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Two stage line spectral frequency (LSF) vector quantizer for the MLow speech codec, the encode side inverse
 * of {@link LsfDequantizer}.
 *
 * <p>MLow codes the short term linear prediction spectrum of each frame as a stage 1 codebook index plus, per
 * filter coefficient, a stage 2 scalar refinement index. This class is the encode side search that selects
 * those indices by analysis by synthesis. Given the float prediction filter and the rate distortion weighting
 * it produces exactly the integer indices the decode side {@link LsfDequantizer} consumes, so the two
 * reconstruct the identical quantized LSF vector.
 *
 * <p>The search runs in three layers:
 * <ul>
 * <li>The filter is converted to line spectral frequencies with {@link A2nlsfBridge#a2nlsf(float[])} and the
 * spectral perceptual weight is computed from the filter and those frequencies.</li>
 * <li>Stage one scores every codebook centroid (and, when coding conditionally, a synthetic centroid built
 * from the previous frame) by the projected weighted distortion, and keeps the {@code surv} best survivors
 * with a tournament partial sort.</li>
 * <li>For each surviving centroid the stage 1 quantization error is rotated into the stage 2 domain, scalar
 * quantized per coefficient with the per coefficient minimum and maximum clamps, and refined by a bounded
 * alternate index loop that flips the most ambiguous coefficients one at a time; every candidate is scored by
 * the weighted reconstruction error plus the entropy bit cost, and the lowest rate distortion candidate wins.
 * </li>
 * </ul>
 *
 * <p>The non conditional path ({@link #quant(int, float[], float, int, int)}) searches the fixed codebook
 * centroids only; the conditional path ({@link #quantCond(int, float[], float[], float, int, int)}) first
 * builds the synthetic stage 1 centroid, its projection, and its forward and inverse weighting matrices from
 * the previous frame's reconstructed LSF vector, then searches those plus the fixed centroids. Both return the
 * same {@link QuantizedLsf} shape.
 *
 * <p>The selected integer indices ({@link QuantizedLsf#indices()}) are bit exact against the reference encoder.
 * The quantized LSF vector ({@link QuantizedLsf#lsf()}) and the rate distortion scalars match the reference
 * single precision results to within IEEE 754 rounding, because every accumulation order, the single precision
 * rounded value of pi, and the minimum distance relaxation are reproduced exactly.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono SMPL low band encode path. This type is stateless and thread safe; the
 * caller threads the previous frame LSF vector explicitly.
 *
 * @implNote This implementation replays every {@code float} accumulation in a fixed order because single
 * precision addition is not associative and a changed rounding can flip a survivor index; in particular the
 * tournament partial sort uses a strict greater tie break and a per leaf flag toggle, and the spectral weight
 * accumulates the complex spectrum power in a fixed coefficient order. The spectral weight takes single
 * precision {@code cos} and {@code sin} via {@link StrictMath} cast to {@code float}.
 */
public final class LsfQuantizer {
    /**
     * Linear prediction order of the MLow short term filter.
     *
     * <p>Every LSF vector and stage 2 index run has this many coefficients; the stage 1 index plus these
     * stage 2 indices make {@value #LPC_ORDER}{@code  + 1} integer indices per frame.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Number of stage 1 codebook centroids.
     *
     * <p>A selected stage 1 index in {@code [0, CB_CENTROIDS)} picks a fixed centroid; the value {@value}
     * selects the conditional centroid synthesized from the previous frame, available only on the conditional
     * coding path.
     */
    private static final int CB_CENTROIDS = 16;

    /**
     * Smallest LSF spacing used by the Laroia weighting, the {@code 1e-3f} floor applied to every adjacent
     * spacing.
     */
    private static final float LAROIA_MIN_DIST = 1e-3f;

    /**
     * Upper band edge for the LSF range, the value of pi rounded to single precision.
     *
     * <p>Identical to the decode side constant; both the Laroia weighting and the minimum distance relaxation
     * measure the top spacing against this edge, so the exact value is load bearing for bit near
     * reconstruction.
     */
    private static final float SMPL_PI = 3.1415926535897f;

    /**
     * Largest {@code float} value, used as a sentinel.
     *
     * <p>Initializes the running best rate distortion score and floors the removed entry in the tournament
     * partial sort.
     */
    private static final float FLT_MAX = Float.MAX_VALUE;

    /**
     * The logger for {@link LsfQuantizer}.
     */
    private static final System.Logger LOGGER = Log.get(LsfQuantizer.class);

    /**
     * The encoder stage 1 search products, stage 2 bit costs, and scalar quantizer geometry, loaded once from
     * {@link EncoderTables#lsfSearch()}.
     */
    private final LsfSearch search;

    /**
     * The decode ready codebook supplying the half centroids, forward weighting matrices, conditional
     * rotation matrices, stage 2 levels, and per voicing mean, regularization, and minimum distance tables,
     * loaded once from {@link LsfCodebooks#load()}.
     */
    private final Codebook codebook;

    /**
     * The result of one frame's LSF quantization: the selected indices, the quantized vector, and the
     * rate distortion scalars.
     *
     * <p>The indices are the bit exact gate against the reference encoder and feed the range encoder;
     * {@code lsf} is the quantized reconstruction the analysis by synthesis loop consumes and is threaded back
     * as the previous frame vector for the next conditionally coded frame; {@code bits} and {@code rdBest} are
     * the winning candidate's bit cost and rate distortion score the rate controller reads.
     *
     * @param indices the {@value #LPC_ORDER}{@code  + 1} selected indices: {@code indices[0]} is the stage 1
     *                centroid in {@code [0, CB_CENTROIDS]}, and {@code indices[i + 1]} is the stage 2 level
     *                index for coefficient {@code i}
     * @param lsf     the {@value #LPC_ORDER} quantized line spectral frequencies of the winning candidate,
     *                strictly increasing within {@code (0, SMPL_PI)}
     * @param bits    the entropy bit cost of the winning candidate
     * @param rdBest  the rate distortion score of the winning candidate
     * @param wlsf    the {@value #LPC_ORDER} spectral perceptual weights used by the rate distortion
     *                comparison, returned because the caller reuses them for the non flatness measure
     */
    public record QuantizedLsf(int[] indices, float[] lsf, float bits, float rdBest, float[] wlsf) {
    }

    /**
     * Constructs a quantizer over the shared encoder LSF tables and decode ready codebook.
     *
     * <p>Both table sets are the immutable cached instances returned by {@link EncoderTables#lsfSearch()} and
     * {@link LsfCodebooks#load()}; multiple quantizers share them without copying.
     */
    public LsfQuantizer() {
        this.search = EncoderTables.lsfSearch();
        this.codebook = LsfCodebooks.load();
    }

    /**
     * Quantizes one frame's LSF vector without conditioning on the previous frame.
     *
     * <p>Runs the two stage survivor search over the fixed codebook centroids only; the conditional centroid
     * is not available. The selected indices reconstruct, through {@link LsfDequantizer}, to the returned
     * quantized LSF vector.
     *
     * @param surv    the survivor count, the number of stage 1 candidates carried into stage two; at most
     *                {@value #LPC_ORDER}
     * @param a       the monic prediction filter, at least {@value #LPC_ORDER}{@code  + 1} entries with
     *                {@code a[0] == 1}
     * @param rdwAdj  the rate distortion weighting adjustment
     * @param voiced  {@code 0} for the unvoiced class, {@code 1} for the voiced class
     * @param lowRate {@code 0} for high rate, {@code 1} for low rate
     * @return the selected indices, quantized LSF vector, and rate distortion scalars
     */
    public QuantizedLsf quant(int surv, float[] a, float rdwAdj, int voiced, int lowRate) {
        return quantCore(surv, a, null, rdwAdj, voiced, lowRate, null);
    }

    /**
     * Quantizes one frame's LSF vector without conditioning on the previous frame, reusing a line spectral
     * frequency vector the caller already computed.
     *
     * <p>Behaves exactly like {@link #quant(int, float[], float, int, int)} but skips the internal
     * {@link A2nlsfBridge#a2nlsf(float[])} call and uses the supplied {@code lsf}. The caller must pass the
     * vector obtained by {@link A2nlsfBridge#a2nlsf(float[])} on the identical {@code a}; because that
     * conversion is a pure function that does not mutate its input, the result is bit identical to the other
     * overload while transforming the filter to line spectral frequencies only once per frame.
     *
     * @param surv    the survivor count, the number of stage 1 candidates carried into stage two; at most
     *                {@value #LPC_ORDER}
     * @param a       the monic prediction filter, at least {@value #LPC_ORDER}{@code  + 1} entries with
     *                {@code a[0] == 1}
     * @param lsf     the precomputed line spectral frequencies of {@code a}, {@value #LPC_ORDER} entries
     * @param rdwAdj  the rate distortion weighting adjustment
     * @param voiced  {@code 0} for the unvoiced class, {@code 1} for the voiced class
     * @param lowRate {@code 0} for high rate, {@code 1} for low rate
     * @return the selected indices, quantized LSF vector, and rate distortion scalars
     */
    public QuantizedLsf quant(int surv, float[] a, float[] lsf, float rdwAdj, int voiced, int lowRate) {
        return quantCore(surv, a, lsf, rdwAdj, voiced, lowRate, null);
    }

    /**
     * Quantizes one frame's LSF vector conditioned on the previous frame.
     *
     * <p>Builds the synthetic conditional stage 1 centroid by pulling the previous frame toward the class
     * mean, projects it through the inverse covariance matrix, and derives its forward and inverse weighting
     * matrices from the conditional rotation matrix, then runs the two stage survivor search over the fixed
     * centroids plus that synthetic centroid. When the synthetic centroid wins, the stage 1 index is
     * {@value #CB_CENTROIDS}.
     *
     * @param surv        the survivor count; at most {@value #LPC_ORDER}
     * @param a           the monic prediction filter, at least {@value #LPC_ORDER}{@code  + 1} entries
     * @param previousLsf the previous frame's reconstructed LSF vector, {@value #LPC_ORDER} entries
     * @param rdwAdj      the rate distortion weighting adjustment
     * @param voiced      {@code 0} for unvoiced, {@code 1} for voiced
     * @param lowRate     {@code 0} for high rate, {@code 1} for low rate
     * @return the selected indices, quantized LSF vector, and rate distortion scalars
     */
    public QuantizedLsf quantCond(int surv, float[] a, float[] previousLsf, float rdwAdj, int voiced, int lowRate) {
        return quantCond(surv, a, previousLsf, null, rdwAdj, voiced, lowRate);
    }

    /**
     * Quantizes one frame's LSF vector conditioned on the previous frame, reusing a line spectral frequency
     * vector the caller already computed.
     *
     * <p>Behaves exactly like {@link #quantCond(int, float[], float[], float, int, int)} but skips the internal
     * {@link A2nlsfBridge#a2nlsf(float[])} call and uses the supplied {@code lsf}. The caller must pass the
     * vector obtained by {@link A2nlsfBridge#a2nlsf(float[])} on the identical {@code a}; because that
     * conversion is a pure function that does not mutate its input, the result is bit identical to the other
     * overload while transforming the filter to line spectral frequencies only once per frame.
     *
     * @param surv        the survivor count; at most {@value #LPC_ORDER}
     * @param a           the monic prediction filter, at least {@value #LPC_ORDER}{@code  + 1} entries
     * @param previousLsf the previous frame's reconstructed LSF vector, {@value #LPC_ORDER} entries
     * @param lsf         the precomputed line spectral frequencies of {@code a}, {@value #LPC_ORDER} entries
     * @param rdwAdj      the rate distortion weighting adjustment
     * @param voiced      {@code 0} for unvoiced, {@code 1} for voiced
     * @param lowRate     {@code 0} for high rate, {@code 1} for low rate
     * @return the selected indices, quantized LSF vector, and rate distortion scalars
     */
    public QuantizedLsf quantCond(int surv, float[] a, float[] previousLsf, float[] lsf, float rdwAdj,
                                  int voiced, int lowRate) {
        var dec = codebook.stage1(voiced);
        var enc = search.stage1(voiced);
        var mean = dec.mean();
        var regCond = dec.regCond();

        var lsfqPrev = new float[LPC_ORDER];
        var st1CbHalf = new float[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            lsfqPrev[i] = previousLsf[i] + regCond * (mean[i] - previousLsf[i]);
            st1CbHalf[i] = 0.5f * lsfqPrev[i];
        }
        var st1CbCinv = matrixMultTransp(enc.cInv(), lsfqPrev);
        var st1We = new float[LPC_ORDER][LPC_ORDER];
        var st1Wie = new float[LPC_ORDER][LPC_ORDER];
        rotApplyWeight(dec.rotCond()[lowRate], lsfqPrev, st1We, st1Wie);

        var bitsCond = enc.bitsCond()[CB_CENTROIDS];
        var cond = new CondParams(st1CbHalf, st1CbCinv, st1We, st1Wie, bitsCond);
        return quantCore(surv, a, lsf, rdwAdj, voiced, lowRate, cond);
    }

    /**
     * The runtime conditional stage 1 centroid and its weighting products.
     *
     * <p>Built once per conditionally coded frame from the previous reconstructed LSF vector; the search
     * treats this synthetic centroid as the {@value #CB_CENTROIDS}th stage 1 candidate.
     *
     * @param cbHalf the half scaled synthetic centroid, {@value #LPC_ORDER} entries; doubled to recover the
     *               stage 1 estimate
     * @param cbCinv the synthetic centroid projected through the inverse covariance matrix,
     *               {@value #LPC_ORDER} entries; used by the stage 1 distortion score
     * @param we     the forward weighting matrix applied to the stage 2 residual, {@value #LPC_ORDER} square
     * @param wie    the inverse weighting matrix applied to the stage 1 error, {@value #LPC_ORDER} square
     * @param bits   the stage 1 bit cost of selecting the conditional centroid
     */
    private record CondParams(float[] cbHalf, float[] cbCinv, float[][] we, float[][] wie, float bits) {
    }

    /**
     * Runs the two stage LSF survivor search shared by the conditional and non conditional paths.
     *
     * <p>Converts the filter to line spectral frequencies, computes the spectral perceptual weight, scores and
     * ranks the stage 1 centroids, and for each survivor scalar quantizes and refines the stage 2 residual,
     * keeping the lowest rate distortion candidate. When {@code cond} is non-{@code null} the synthetic
     * conditional centroid is appended as the {@value #CB_CENTROIDS}th stage 1 candidate and the conditional
     * stage 1 bit costs are used.
     *
     * @param surv           the survivor count
     * @param a              the monic prediction filter
     * @param precomputedLsf the line spectral frequencies of {@code a} if the caller already computed them, or
     *                       {@code null} to convert {@code a} here; when non-{@code null} it must equal
     *                       {@link A2nlsfBridge#a2nlsf(float[])} on the identical {@code a}
     * @param rdwAdj         the rate distortion weighting adjustment
     * @param voiced         {@code 0} for unvoiced, {@code 1} for voiced
     * @param lowRate        {@code 0} for high rate, {@code 1} for low rate
     * @param cond           the conditional centroid parameters, or {@code null} for the non conditional path
     * @return the selected indices, quantized LSF vector, and rate distortion scalars
     */
    private QuantizedLsf quantCore(int surv, float[] a, float[] precomputedLsf, float rdwAdj, int voiced,
                                   int lowRate, CondParams cond) {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "lsf quant: surv={0} voiced={1} lowRate={2} cond={3}",
                    surv, voiced, lowRate, cond != null);
        }
        var dec = codebook.stage1(voiced);
        var enc = search.stage1(voiced);

        var lsf = precomputedLsf != null ? precomputedLsf : A2nlsfBridge.a2nlsf(a);
        var wlsf = spectralWeights(a, lsf);

        var qstep = search.qstep(voiced, lowRate);
        var qstepCond = qstep * search.condMult();
        var minDist = dec.minDist();

        var qim1 = vqTemp(lsf, dec.cbHalf(), enc.cbCinv(), cond, surv);

        var qi = new int[LPC_ORDER + 1];
        var qlsfOut = new float[LPC_ORDER];
        var bitsUsed = new float[]{0.0f};
        var rdBest = FLT_MAX;

        for (var s1 = 0; s1 < surv; s1++) {
            var qi1 = qim1[s1];
            var isCond = qi1 == CB_CENTROIDS;

            var lsfq1 = new float[LPC_ORDER];
            var half = isCond ? cond.cbHalf() : dec.cbHalf()[qi1];
            for (var i = 0; i < LPC_ORDER; i++) {
                lsfq1[i] = half[i] * 2.0f;
            }

            var qerrIn = new float[LPC_ORDER];
            for (var i = 0; i < LPC_ORDER; i++) {
                qerrIn[i] = lsf[i] - lsfq1[i];
            }
            var wiePtr = isCond ? cond.wie() : enc.wie()[qi1];
            var qerr = matrixMultTransp(wiePtr, qerrIn);

            var invQstep = 1.0f / (qi1 < CB_CENTROIDS ? qstep : qstepCond);
            for (var i = 0; i < LPC_ORDER; i++) {
                qerr[i] *= invQstep;
            }

            var bits = cond == null ? enc.bits()[qi1] : enc.bitsCond()[qi1];
            var minQiRow = search.minQi(voiced, lowRate, qi1);
            var maxQiRow = search.maxQi(voiced, lowRate, qi1);
            var st2 = codebook.stage2(voiced, lowRate, qi1);
            var qLvls = st2.qLvls();
            var numBits = search.stage2NumBits(voiced, lowRate, qi1);

            var alt = new int[LPC_ORDER];
            var qi2 = new int[LPC_ORDER];
            var absQerr = new float[LPC_ORDER];
            var qres = new float[LPC_ORDER];
            for (var i = 0; i < LPC_ORDER; i++) {
                var qi2i = roundf(qerr[i]);
                var minQi = minQiRow[i];
                var maxQi = maxQiRow[i];
                qi2i = Math.min(qi2i, maxQi);
                qi2i = Math.max(qi2i, minQi);
                qerr[i] -= qi2i;
                alt[i] = sign(qerr[i]);
                if ((qi2i == maxQi && alt[i] > 0) || (qi2i == minQi && alt[i] < 0)) {
                    absQerr[i] = -1.0f;
                } else {
                    absQerr[i] = Math.abs(qerr[i]);
                }
                qi2i -= minQi;
                bits += numBits[i][qi2i];
                qres[i] = qLvls[i][qi2i];
                qi2[i] = qi2i;
            }

            var iAlt = getMaxiK(absQerr, LPC_ORDER, surv);
            var wePtr = isCond ? cond.we() : dec.we()[qi1];
            var lsfq = matrixMultTransp(wePtr, qres);
            for (var i = 0; i < LPC_ORDER; i++) {
                lsfq[i] += lsfq1[i];
            }

            var surv2 = surv - s1;
            var indChgd = -1;
            var bits0 = bits;
            var lsfqBase = lsfq.clone();
            for (var s2 = 0; s2 < surv2; s2++) {
                enforceMinDistance(lsfq, minDist);
                var werr = werr(lsf, lsfq, wlsf);
                var rd = 0.5f * LPC_ORDER * log2f(werr) * rdwAdj + bits;
                if (rd < rdBest) {
                    rdBest = rd;
                    qi[0] = qi1;
                    for (var i = 0; i < LPC_ORDER; i++) {
                        qi[i + 1] = qi2[i];
                    }
                    bitsUsed[0] = bits;
                    System.arraycopy(lsfq, 0, qlsfOut, 0, LPC_ORDER);
                }
                if (s2 == surv2 - 1 || absQerr[iAlt[s2]] < 0.25f) {
                    break;
                }
                if (s2 > 0) {
                    qi2[indChgd] -= alt[indChgd];
                }
                indChgd = iAlt[s2];
                var qi2Old = qi2[indChgd];
                qi2[indChgd] += alt[indChgd];
                var qi2New = qi2[indChgd];
                var qlvlsDiff = qLvls[indChgd][qi2New] - qLvls[indChgd][qi2Old];
                var weCol = wePtr[indChgd];
                for (var i = 0; i < LPC_ORDER; i++) {
                    lsfq[i] = lsfqBase[i] + qlvlsDiff * weCol[i];
                }
                bits = bits0 + numBits[indChgd][qi2New] - numBits[indChgd][qi2Old];
            }
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "lsf quant result: stage1Idx={0} bits={1} rdBest={2}",
                    qi[0], bitsUsed[0], rdBest);
        }
        return new QuantizedLsf(qi, qlsfOut, bitsUsed[0], rdBest, wlsf);
    }

    /**
     * Scores every stage 1 centroid and returns the {@code surv} best survivor indices.
     *
     * <p>Each fixed centroid's score is the negated projected weighted distortion
     * {@code -dot(cbHalf[s] - lsf, cbCinv[s])}; when {@code cond} is non-{@code null} the synthetic centroid
     * is appended with its own score. The {@code surv} highest scores are selected by the tournament partial
     * sort {@link #getMaxiK(float[], int, int)}.
     *
     * @param lsf    the analysis line spectral frequencies, {@value #LPC_ORDER} entries
     * @param cbHalf the half scaled fixed centroid codebook, {@value #CB_CENTROIDS} rows of
     *               {@value #LPC_ORDER}
     * @param cbCinv the projected fixed centroid codebook, {@value #CB_CENTROIDS} rows of {@value #LPC_ORDER}
     * @param cond   the conditional centroid parameters, or {@code null}
     * @param surv   the survivor count
     * @return the {@code surv} selected stage 1 centroid indices, in selection order
     */
    private static int[] vqTemp(float[] lsf, float[][] cbHalf, float[][] cbCinv, CondParams cond, int surv) {
        var cbCentroids = CB_CENTROIDS;
        var err = new float[CB_CENTROIDS + 1];
        var tmp = new float[LPC_ORDER];
        for (var s = 0; s < CB_CENTROIDS; s++) {
            for (var i = 0; i < LPC_ORDER; i++) {
                tmp[i] = cbHalf[s][i] - lsf[i];
            }
            err[s] = -dotProd(tmp, cbCinv[s]);
        }
        if (cond != null) {
            for (var i = 0; i < LPC_ORDER; i++) {
                tmp[i] = cond.cbHalf()[i] - lsf[i];
            }
            err[CB_CENTROIDS] = -dotProd(tmp, cond.cbCinv());
            cbCentroids++;
        }
        return getMaxiK(err, cbCentroids, surv);
    }

    /**
     * Computes the spectral perceptual weight of a line spectral frequency vector.
     *
     * <p>For each line spectral frequency the prediction filter is evaluated on the unit circle at that
     * frequency: the running complex power {@code e^(j*k*lsf[i])} is accumulated against the filter taps so
     * the squared magnitude of {@code A(e^(j*lsf[i]))} is the raw weight. The raw weights are normalized by
     * their minimum and inverse square rooted, so a narrow spectral peak yields a large weight. The complex
     * recurrence advances the power by one multiply per tap, and the cosine and sine are taken in single
     * precision.
     *
     * @param a   the monic prediction filter, at least {@value #LPC_ORDER}{@code  + 1} entries
     * @param lsf the line spectral frequencies, {@value #LPC_ORDER} entries
     * @return a freshly allocated spectral weight vector of {@value #LPC_ORDER} entries
     */
    private static float[] spectralWeights(float[] a, float[] lsf) {
        var lsfw = new float[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            var eRe = (float) StrictMath.cos(lsf[i]);
            var eIm = (float) StrictMath.sin(lsf[i]);
            var accRe = 1.0f;
            var accIm = 0.0f;
            var pRe = eRe;
            var pIm = eIm;
            for (var j = 1; j < LPC_ORDER; j++) {
                accRe += pRe * a[j];
                accIm -= pIm * a[j];
                var nRe = eRe * pRe - eIm * pIm;
                var nIm = eRe * pIm + eIm * pRe;
                pRe = nRe;
                pIm = nIm;
            }
            accRe += pRe * a[LPC_ORDER];
            accIm -= pIm * a[LPC_ORDER];
            lsfw[i] = accRe * accRe + accIm * accIm;
        }
        var minLsfw = lsfw[0];
        for (var i = 1; i < LPC_ORDER; i++) {
            if (lsfw[i] < minLsfw) {
                minLsfw = lsfw[i];
            }
        }
        var scale = 1.0f / minLsfw;
        for (var i = 0; i < LPC_ORDER; i++) {
            lsfw[i] = 1.0f / (float) Math.sqrt(lsfw[i] * scale);
        }
        return lsfw;
    }

    /**
     * Derives the forward and inverse weighting matrices from a rotation matrix and a line spectral frequency
     * vector.
     *
     * <p>The Laroia weights of {@code lsf} are square rooted; the forward matrix is
     * {@code wrot1[i][j] = rot[i][j] / sqrt(weight[j])} and the inverse matrix is
     * {@code wrot2[j][i] = rot[i][j] * sqrt(weight[j])}, the transpose and scale companion of the forward
     * matrix. Both are filled in place into the supplied destinations. The square root uses the
     * {@link FastSqrt#sqrt(float)} approximation rather than a correctly rounded one, so the runtime
     * conditional weighting matches the precomputed unconditional codebook tables exactly.
     *
     * @param rot   the rotation matrix, {@value #LPC_ORDER} square
     * @param lsf   the line spectral frequencies, {@value #LPC_ORDER} entries
     * @param wrot1 the destination forward weighting matrix, {@value #LPC_ORDER} square
     * @param wrot2 the destination inverse weighting matrix, {@value #LPC_ORDER} square
     */
    private static void rotApplyWeight(float[][] rot, float[] lsf, float[][] wrot1, float[][] wrot2) {
        var lsfw = laroiaWeights(lsf);
        for (var i = 0; i < LPC_ORDER; i++) {
            lsfw[i] = FastSqrt.sqrt(lsfw[i]);
        }
        var lsfwInv = new float[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            lsfwInv[i] = 1.0f / lsfw[i];
        }
        for (var i = 0; i < LPC_ORDER; i++) {
            for (var j = 0; j < LPC_ORDER; j++) {
                wrot1[i][j] = rot[i][j] * lsfwInv[j];
                wrot2[j][i] = rot[i][j] * lsfw[j];
            }
        }
    }

    /**
     * Returns the indices of the {@code k} largest values via a tournament partial sort.
     *
     * <p>A binary max tree is folded over {@code x}: a first pass maxes the two halves, then while the level
     * length is even the level is halved again, recording the number of folds. Each of the {@code k} outputs
     * is found by scanning the top level for its maximum, descending the recorded branches to the leaf pair,
     * resolving the pair with a per leaf flag toggle and a strict greater or equal tie break, then removing
     * the winner by flooring it to {@code -FLT_MAX} and re propagating the maxima up the recorded path. The
     * strict greater scan and the flag toggle are reproduced exactly because the resolved index decides a
     * survivor and a flipped tie would flip the search.
     *
     * @param x     the scores to rank, of length {@code xLen}
     * @param xLen  the number of valid scores in {@code x}
     * @param k     the number of largest indices to return
     * @return a freshly allocated array of the {@code k} selected indices into {@code x}, in selection order
     */
    private static int[] getMaxiK(float[] x, int xLen, int k) {
        var idx = new int[k];
        var buf = new float[2 * xLen + 2];
        var flags = new byte[xLen / 2 + 1];
        var is = new int[16];
        var numHalves = 0;
        var len = (xLen + 1) >> 1;
        var bufPtr = 0;
        for (var n = 0; n < xLen - len; n++) {
            buf[n] = Math.max(x[n], x[n + len]);
        }
        buf[xLen - len] = x[xLen - len];
        while ((len & 1) == 0) {
            bufPtr += len;
            len >>= 1;
            for (var n = 0; n < len; n++) {
                buf[bufPtr + n] = Math.max(buf[bufPtr - 2 * len + n], buf[bufPtr - len + n]);
            }
            numHalves++;
        }
        for (var kk = 0; kk < k; kk++) {
            var i = 0;
            var maxtmp = buf[bufPtr];
            for (var n = 1; n < len; n++) {
                var xtmp = buf[bufPtr + n];
                if (xtmp > maxtmp) {
                    maxtmp = xtmp;
                    i = n;
                }
            }
            for (var n = 0; n < numHalves; n++) {
                is[n] = i;
                bufPtr -= 2 * len;
                if (buf[bufPtr + i] < buf[bufPtr + i + len]) {
                    i += len;
                }
                len <<= 1;
            }
            var xtmp = -FLT_MAX;
            var iFinal = i;
            if (i + len < xLen) {
                if (flags[i]++ == 0) {
                    if (x[i] < x[i + len]) {
                        xtmp = x[i];
                        iFinal += len;
                    } else {
                        xtmp = x[i + len];
                    }
                } else {
                    if (x[i] >= x[i + len]) {
                        iFinal += len;
                    }
                }
            }
            idx[kk] = iFinal;
            if (kk == k - 1) {
                return idx;
            }
            buf[bufPtr + i] = xtmp;
            for (var n = numHalves - 1; n >= 0; n--) {
                i = is[n];
                len >>= 1;
                buf[bufPtr + i + 2 * len] = Math.max(buf[bufPtr + i], buf[bufPtr + i + len]);
                bufPtr += 2 * len;
            }
        }
        return idx;
    }

    /**
     * Computes the transpose matrix product {@code y[i] = sum_j m[j][i] * x[j]}.
     *
     * @param m the {@value #LPC_ORDER} by {@value #LPC_ORDER} matrix, indexed {@code m[row][col]}
     * @param x the input vector of {@value #LPC_ORDER} entries
     * @return a freshly allocated output vector of {@value #LPC_ORDER} entries
     * @implNote This implementation fixes the single precision accumulation schedule because the reduction is
     *           bit sensitive: the body terms {@code j} in {@code [1, 12]} feed four lane partials selected by
     *           {@code l = (j - 1) & 3}, each lane accumulated in ascending {@code j}; the lanes are then
     *           folded as {@code (l0 + l2) + (l1 + l3)}; the {@code j == 0} term is added as the left operand
     *           of that fold ({@code m[0][i]*x[0] + hsum}); and the remainder terms {@code j} in {@code [13, 15]}
     *           are summed in scalar order afterward. Plain source order accumulation diverges by up to a few
     *           units in the last place, which flips downstream quantizer reconstruction.
     */
    private static float[] matrixMultTransp(float[][] m, float[] x) {
        var y = new float[LPC_ORDER];
        var x0 = x[0];

        // Four lane partials per output element, indexed l = (j - 1) & 3, accumulated over the body
        // term span j in [1, 12]; the trailing j in [13, 15] form a scalar remainder.
        var l0 = new float[LPC_ORDER];
        var l1 = new float[LPC_ORDER];
        var l2 = new float[LPC_ORDER];
        var l3 = new float[LPC_ORDER];
        var body = ((LPC_ORDER - 1) & ~3) + 1; // body terms are j = 1..12
        for (var j = 1; j < body; j += 4) {
            var xa = x[j];
            var xb = x[j + 1];
            var xc = x[j + 2];
            var xd = x[j + 3];
            var ma = m[j];
            var mb = m[j + 1];
            var mc = m[j + 2];
            var md = m[j + 3];
            for (var i = 0; i < LPC_ORDER; i++) {
                l0[i] += ma[i] * xa;
                l1[i] += mb[i] * xb;
                l2[i] += mc[i] * xc;
                l3[i] += md[i] * xd;
            }
        }
        for (var i = 0; i < LPC_ORDER; i++) {
            var hsum = (l0[i] + l2[i]) + (l1[i] + l3[i]);
            y[i] = m[0][i] * x0 + hsum;
        }
        for (var j = body; j < LPC_ORDER; j++) {
            var xj = x[j];
            var mj = m[j];
            for (var i = 0; i < LPC_ORDER; i++) {
                y[i] += mj[i] * xj;
            }
        }
        return y;
    }

    /**
     * Computes the weighted reconstruction error {@code sum_k w[k] * (x[k] - y[k])^2}.
     *
     * @param x the analysis line spectral frequencies, {@value #LPC_ORDER} entries
     * @param y the quantized line spectral frequencies, {@value #LPC_ORDER} entries
     * @param w the spectral perceptual weights, {@value #LPC_ORDER} entries
     * @return the single precision weighted error
     */
    private static float werr(float[] x, float[] y, float[] w) {
        var s = 0.0f;
        for (var k = 0; k < LPC_ORDER; k++) {
            var e = x[k] - y[k];
            s += w[k] * e * e;
        }
        return s;
    }

    /**
     * Computes the inner product {@code sum_i a[i] * b[i]} over {@value #LPC_ORDER} entries.
     *
     * @param a the first vector, {@value #LPC_ORDER} entries
     * @param b the second vector, {@value #LPC_ORDER} entries
     * @return the single precision inner product
     */
    private static float dotProd(float[] a, float[] b) {
        var ret = 0.0f;
        for (var i = 0; i < LPC_ORDER; i++) {
            ret += a[i] * b[i];
        }
        return ret;
    }

    /**
     * Computes the Laroia perceptual weights of a line spectral frequency vector.
     *
     * <p>The inverse spacing between adjacent line spectral frequencies, and against the {@code 0} and
     * {@link #SMPL_PI} band edges with each spacing clamped up to {@link #LAROIA_MIN_DIST}, is accumulated so
     * each coefficient's weight is the sum of the inverse spacing on either side of it.
     *
     * @param lsf the line spectral frequencies, {@value #LPC_ORDER} entries
     * @return a freshly allocated weight vector of {@value #LPC_ORDER} entries
     */
    private static float[] laroiaWeights(float[] lsf) {
        var invDelta = new float[LPC_ORDER + 1];
        invDelta[0] = 1.0f / Math.max(lsf[0], LAROIA_MIN_DIST);
        for (var i = 1; i < LPC_ORDER; i++) {
            invDelta[i] = 1.0f / Math.max(lsf[i] - lsf[i - 1], LAROIA_MIN_DIST);
        }
        invDelta[LPC_ORDER] = 1.0f / Math.max(SMPL_PI - lsf[LPC_ORDER - 1], LAROIA_MIN_DIST);
        var weight = new float[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            weight[i] = invDelta[i] + invDelta[i + 1];
        }
        return weight;
    }

    /**
     * Enforces the minimum distance ordering on a quantized LSF vector in place.
     *
     * <p>The vector is converted to spacings against the {@code 0} and {@link #SMPL_PI} band edges and each
     * adjacent pair, minus the per position minimum spacing. If the smallest spacing is already positive the
     * vector is left unchanged. Otherwise a fixed point relaxation runs for up to {@code 1000} iterations:
     * each iteration pushes the most violated spacing up by {@code k * 1.0e-6f - dm} and pulls the
     * neighbouring spacing or spacings down by the same amount (halved and split to both sides for an interior
     * position), recomputes the minimum, and stops once every spacing is non negative. The line spectral
     * frequencies are then rebuilt cumulatively from the corrected spacings.
     *
     * @param lsf     the LSF vector to correct in place, {@value #LPC_ORDER} entries
     * @param minDist the per position minimum spacings, {@value #LPC_ORDER}{@code  + 1} entries
     */
    private static void enforceMinDistance(float[] lsf, float[] minDist) {
        var dlsfs = new float[LPC_ORDER + 1];
        dlsfs[0] = (lsf[0] - 0.0f) - minDist[0];
        for (var i = 1; i < LPC_ORDER; i++) {
            dlsfs[i] = (lsf[i] - lsf[i - 1]) - minDist[i];
        }
        dlsfs[LPC_ORDER] = (SMPL_PI - lsf[LPC_ORDER - 1]) - minDist[LPC_ORDER];

        var minIx = 0;
        var dm = dlsfs[0];
        for (var i = 1; i < LPC_ORDER + 1; i++) {
            if (dlsfs[i] < dm) {
                dm = dlsfs[i];
                minIx = i;
            }
        }
        if (dm > 0.0f) {
            return;
        }
        for (var k = 0; k < 1000; k++) {
            var delta = k * 1.0e-6f - dm;
            dlsfs[minIx] += delta;
            if (minIx == 0) {
                dlsfs[1] -= delta;
            } else if (minIx == LPC_ORDER) {
                dlsfs[LPC_ORDER - 1] -= delta;
            } else {
                delta *= 0.5f;
                dlsfs[minIx - 1] -= delta;
                dlsfs[minIx + 1] -= delta;
            }
            minIx = 0;
            dm = dlsfs[0];
            for (var i = 1; i < LPC_ORDER + 1; i++) {
                if (dlsfs[i] < dm) {
                    dm = dlsfs[i];
                    minIx = i;
                }
            }
            if (dm >= 0.0f) {
                lsf[0] = dlsfs[0] + minDist[0];
                for (var i = 1; i < LPC_ORDER; i++) {
                    lsf[i] = lsf[i - 1] + (dlsfs[i] + minDist[i]);
                }
                return;
            }
        }
        if (Log.ERROR) {
            LOGGER.log(Level.ERROR, "lsf minimum-distance relaxation did not converge after 1000 iterations");
        }
        throw new AssertionError("LSF minimum-distance relaxation did not converge");
    }

    /**
     * Returns the sign of a {@code float}.
     *
     * <p>Returns {@code 1} for a strictly positive value, {@code -1} for a strictly negative value, and
     * {@code 0} for exactly zero.
     *
     * @param a the value to test
     * @return the integer sign of {@code a}
     */
    private static int sign(float a) {
        if (a > 0.0f) {
            return 1;
        }
        return a == 0.0f ? 0 : -1;
    }

    /**
     * Rounds a {@code float} to the nearest integer with halfway cases rounded away from zero, used by the
     * second stage residual quantizer.
     *
     * <p>This is not {@link Math#round(float)}: {@code Math.round} evaluates {@code floor(x + 0.5f)} and so
     * rounds halfway cases toward positive infinity, sending a negative tie like {@code -0.5f} to {@code 0}
     * rather than the {@code -1} that rounding away from zero produces. A single such off by one in the
     * second stage index selects a different quantizer level, flips the alternative coding sign, and changes
     * the chosen line spectral frequencies, so the away from zero direction is load bearing.
     *
     * @param v the value to round
     * @return {@code v} rounded to the nearest integer with ties away from zero
     */
    private static int roundf(float v) {
        return (int) (v >= 0.0f ? (float) StrictMath.floor(v + 0.5f) : (float) StrictMath.ceil(v - 0.5f));
    }

    /**
     * Computes the base two logarithm in single precision.
     *
     * <p>The {@code float} cast applied to the {@code double} {@link Math#log} ratio reproduces the single
     * precision rounding used in the rate distortion score.
     *
     * @param x the operand, strictly positive
     * @return the single precision base two logarithm of {@code x}
     */
    private static float log2f(float x) {
        return (float) (Math.log(x) / Math.log(2.0));
    }
}
