package com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeDecoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Codebook;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Stage1;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Stage2;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Two stage line spectral frequency (LSF) inverse vector quantizer for the MLow speech codec.
 *
 * <p>MLow codes the short term linear prediction spectrum of each frame as two range coded indices: a
 * stage 1 codebook centroid and, per filter coefficient, a stage 2 scalar refinement. This class is the
 * decode side inverse of that quantizer. It performs two steps, exposed as the two halves of one
 * {@link #decode(MlowRangeDecoder, int, int, boolean, float[])} transaction:
 * <ul>
 * <li>{@link #decodeIndices(MlowRangeDecoder, int, int, boolean)} reads the stage 1 and stage 2 indices
 * from the range coder using the LSF cumulative mass functions.</li>
 * <li>{@link #dequantize(int[], int, int, float[])} reconstructs the per frame LSF vector from those
 * indices.</li>
 * </ul>
 *
 * <p>Stage one selects one of {@value #CB_CENTROIDS} codebook centroids, or a {@value #CB_CENTROIDS}th
 * conditional centroid built from the previous frame's reconstructed LSFs. The conditional path is only
 * available when the frame is coded conditionally: it pulls the previous frame toward the class mean,
 * applies the conditional rotation matrix, and divides out the square rooted Laroia weights. The
 * unconditional path doubles the chosen half centroid and applies that centroid's forward weighting
 * matrix. Either path adds the stage 2 residual and then enforces the minimum distance ordering so the
 * reconstructed line spectral frequencies stay strictly increasing and bounded inside the band.
 *
 * <p>The decoded integer indices are bit exact against the native decoder. The reconstructed
 * {@code float} LSF vector matches the native single precision result to within IEEE 754 rounding,
 * because every accumulation order, the {@code float} rounded pi constant, and the minimum distance
 * relaxation are reproduced exactly.
 *
 * <p>Scope is the 16 kHz / 60 ms / mono SMPL low band decode path. The encoder side quantization and
 * survivor search are not ported. This type is stateless and thread safe; the caller threads the
 * previous frame LSF vector explicitly.
 *
 * @implNote This implementation accumulates the transpose matrix multiply with the {@code j == 0} term
 * first and then ascending {@code j}, because {@code float} addition is not associative and this order
 * reproduces the native single precision rounding exactly. The minimum distance relaxation replays the
 * fixed point loop of up to {@code 1000} iterations, including the {@code k * 1.0e-6f - dm} step schedule,
 * for the same reason.
 */
public final class LsfDequantizer {
    /**
     * The logger for {@link LsfDequantizer}.
     */
    private static final System.Logger LOGGER = Log.get(LsfDequantizer.class);

    /**
     * Linear prediction order of the MLow short term filter.
     *
     * <p>Every LSF vector and stage 2 index run has this many coefficients; the stage 1 index plus these
     * stage 2 indices make {@code LPC_ORDER + 1} integer indices per frame.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Number of stage 1 codebook centroids.
     *
     * <p>A decoded stage 1 index in {@code [0, CB_CENTROIDS)} selects a fixed centroid; the value
     * {@value} selects the conditional centroid reconstructed from the previous frame.
     */
    private static final int CB_CENTROIDS = 16;

    /**
     * Smallest LSF spacing used by the Laroia weighting.
     *
     * <p>Each inverse spacing is computed over a gap clamped up to this value so a nearly coincident pair
     * of line spectral frequencies cannot produce an unbounded weight.
     */
    private static final float LAROIA_MIN_DIST = 1e-3f;

    /**
     * Upper band edge for the LSF range.
     *
     * <p>This is the {@code float} rounded value of pi that the codec uses; both the Laroia weighting and
     * the minimum distance relaxation measure the top spacing against this edge, so the exact constant is
     * load bearing for reproducing the reconstruction bit for bit.
     */
    private static final float SMPL_PI = 3.1415926535897f;

    /**
     * The shared decode ready two stage codebook, loaded once from {@link LsfCodebooks#load()}.
     */
    private final Codebook codebook;

    /**
     * Constructs a dequantizer over the shared LSF codebook.
     *
     * <p>The codebook is the immutable, cached instance returned by {@link LsfCodebooks#load()}; multiple
     * dequantizers share it without copying.
     */
    public LsfDequantizer() {
        this.codebook = LsfCodebooks.load();
    }

    /**
     * The result of one frame's LSF decode: the integer indices and the reconstructed LSF vector.
     *
     * <p>The indices are the bit exact gate against the native decoder; {@code lsf} is the single
     * precision reconstruction the linear prediction interpolation consumes and is also threaded back as
     * the previous frame vector for the next conditionally coded frame.
     *
     * @param indices the {@code LPC_ORDER + 1} decoded indices: {@code indices[0]} is the stage 1 centroid
     *                in {@code [0, CB_CENTROIDS]}, and {@code indices[i + 1]} is the stage 2 level index
     *                for coefficient {@code i}
     * @param lsf     the {@code LPC_ORDER} reconstructed line spectral frequencies, strictly increasing
     *                within {@code (0, SMPL_PI)}
     */
    public record DecodedLsf(int[] indices, float[] lsf) {
    }

    /**
     * Decodes and dequantizes one frame's LSF vector in a single transaction.
     *
     * <p>Reads the stage 1 and stage 2 indices from {@code decoder} with
     * {@link #decodeIndices(MlowRangeDecoder, int, int, boolean)}, then reconstructs the LSF vector with
     * {@link #dequantize(int[], int, int, float[])}. The two halves are exposed separately for callers
     * that only need one; this method performs both.
     *
     * @param decoder     the range decoder positioned at the frame's stage 1 LSF symbol; advanced past all
     *                    {@code LPC_ORDER + 1} LSF symbols on return
     * @param voiced      {@code 0} for the unvoiced class, {@code 1} for the voiced class
     * @param lowRate     {@code 0} for high rate, {@code 1} for low rate
     * @param conditional {@code true} when the frame is coded conditionally on the previous frame, which
     *                    enables the conditional stage 1 centroid
     * @param previousLsf the previous frame's reconstructed LSF vector, read only when the conditional
     *                    centroid is selected; {@code LPC_ORDER} entries
     * @return the decoded indices and reconstructed LSF vector
     */
    public DecodedLsf decode(MlowRangeDecoder decoder, int voiced, int lowRate, boolean conditional,
                             float[] previousLsf) {
        var indices = decodeIndices(decoder, voiced, lowRate, conditional);
        var lsf = dequantize(indices, voiced, lowRate, previousLsf);
        return new DecodedLsf(indices, lsf);
    }

    /**
     * Reads the stage 1 and stage 2 LSF indices from the range coder.
     *
     * <p>The stage 1 index is decoded against the centroid cumulative mass function for the class,
     * choosing the conditional coding table when {@code conditional} is set. Each of the
     * {@value #LPC_ORDER} stage 2 indices is then decoded against the per coefficient cumulative mass
     * function of the selected stage 1 centroid, drawn from the {@code (voiced, lowRate, centroid)} stage 2
     * sub codebook. The decoder is advanced past every symbol.
     *
     * @param decoder     the range decoder positioned at the stage 1 LSF symbol
     * @param voiced      {@code 0} for unvoiced, {@code 1} for voiced
     * @param lowRate     {@code 0} for high rate, {@code 1} for low rate
     * @param conditional {@code true} to decode the stage 1 index against the conditional coding table
     * @return a freshly allocated array of {@code LPC_ORDER + 1} indices: stage 1 at {@code [0]}, stage 2
     *         levels at {@code [1, LPC_ORDER]}
     */
    public int[] decodeIndices(MlowRangeDecoder decoder, int voiced, int lowRate, boolean conditional) {
        var st1 = codebook.stage1(voiced);
        var stage1Cmf = conditional ? st1.cmfCond() : st1.cmf();
        var indices = new int[LPC_ORDER + 1];
        indices[0] = MlowEntropyWrapper.decodeUpdate(decoder, stage1Cmf);
        var st2 = codebook.stage2(voiced, lowRate, indices[0]);
        for (var i = 0; i < LPC_ORDER; i++) {
            indices[i + 1] = MlowEntropyWrapper.decodeUpdate(decoder, st2.cmf()[i]);
        }
        return indices;
    }

    /**
     * Reconstructs the per frame LSF vector from the decoded indices.
     *
     * <p>The stage 2 residual is the per coefficient quantization level selected by each stage 2 index.
     * When the stage 1 index is the conditional centroid ({@value #CB_CENTROIDS}) the stage 1 estimate is
     * the previous frame pulled toward the class mean, the residual is rotated by the conditional rotation
     * matrix and divided by the square rooted Laroia weights of that estimate; otherwise the stage 1
     * estimate is the doubled half centroid and the residual is rotated by that centroid's forward
     * weighting matrix. The two are summed and then the minimum distance ordering is enforced.
     *
     * @param indices     the {@code LPC_ORDER + 1} indices from
     *                    {@link #decodeIndices(MlowRangeDecoder, int, int, boolean)}
     * @param voiced      {@code 0} for unvoiced, {@code 1} for voiced
     * @param lowRate     {@code 0} for high rate, {@code 1} for low rate
     * @param previousLsf the previous frame's reconstructed LSF vector, read only when the conditional
     *                    centroid is selected; {@code LPC_ORDER} entries
     * @return a freshly allocated reconstructed LSF vector of {@value #LPC_ORDER} entries
     */
    public float[] dequantize(int[] indices, int voiced, int lowRate, float[] previousLsf) {
        var st1 = codebook.stage1(voiced);
        var centroid = indices[0];
        var st2 = codebook.stage2(voiced, lowRate, centroid);

        var qres = new float[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            qres[i] = st2.qLvls()[i][indices[i + 1]];
        }

        var lsfq1 = new float[LPC_ORDER];
        float[] lsfq2;
        if (centroid == CB_CENTROIDS) {
            var mean = st1.mean();
            var regCond = st1.regCond();
            for (var i = 0; i < LPC_ORDER; i++) {
                lsfq1[i] = previousLsf[i] + regCond * (mean[i] - previousLsf[i]);
            }
            var lsfw = laroiaWeights(lsfq1);
            for (var i = 0; i < LPC_ORDER; i++) {
                lsfw[i] = (float) Math.sqrt(lsfw[i]);
            }
            lsfq2 = matrixMultTransp(st1.rotCond()[lowRate], qres);
            for (var i = 0; i < LPC_ORDER; i++) {
                lsfq2[i] /= lsfw[i];
            }
        } else {
            var cbHalf = st1.cbHalf();
            for (var i = 0; i < LPC_ORDER; i++) {
                lsfq1[i] = cbHalf[centroid][i] * 2.0f;
            }
            lsfq2 = matrixMultTransp(st1.we()[centroid], qres);
        }

        var lsfq = new float[LPC_ORDER];
        for (var i = 0; i < LPC_ORDER; i++) {
            lsfq[i] = lsfq1[i] + lsfq2[i];
        }
        enforceMinDistance(lsfq, st1.minDist());
        return lsfq;
    }

    /**
     * Computes the transpose matrix product {@code y[i] = sum_j m[j][i] * x[j]}.
     *
     * <p>The matrix is indexed {@code m[row][col]}, so reading element {@code (j, i)} as {@code m[j][i]}
     * transposes the supplied matrix. Each output is initialized with the {@code j == 0} term and then
     * accumulates ascending {@code j} terms in place; this order is preserved because {@code float}
     * addition is not associative and the native reference rounds in exactly this sequence.
     *
     * @param m the {@value #LPC_ORDER} by {@value #LPC_ORDER} matrix, indexed {@code m[row][col]}
     * @param x the input vector of {@value #LPC_ORDER} entries
     * @return a freshly allocated output vector of {@value #LPC_ORDER} entries
     */
    private static float[] matrixMultTransp(float[][] m, float[] x) {
        var y = new float[LPC_ORDER];
        var x0 = x[0];
        for (var i = 0; i < LPC_ORDER; i++) {
            y[i] = m[0][i] * x0;
        }
        for (var j = 1; j < LPC_ORDER; j++) {
            var xj = x[j];
            for (var i = 0; i < LPC_ORDER; i++) {
                y[i] += m[j][i] * xj;
            }
        }
        return y;
    }

    /**
     * Computes the Laroia perceptual weights of a line spectral frequency vector.
     *
     * <p>The inverse spacing between adjacent line spectral frequencies, and against the {@code 0} and
     * {@link #SMPL_PI} band edges with each spacing clamped up to {@link #LAROIA_MIN_DIST}, is accumulated
     * so each coefficient's weight is the sum of the inverse spacing on either side of it.
     *
     * @param lsf the line spectral frequencies, {@value #LPC_ORDER} ascending entries
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
     * Enforces the minimum distance ordering on a reconstructed LSF vector in place.
     *
     * <p>The vector is converted to spacings against the {@code 0} and {@link #SMPL_PI} band edges and each
     * adjacent pair, minus the per position minimum spacing. If the smallest spacing is already positive
     * the vector is left unchanged. Otherwise a fixed point relaxation runs for up to {@code 1000}
     * iterations: each iteration pushes the most violated spacing up by {@code k * 1.0e-6f - dm} and pulls
     * the neighbouring spacing(s) down by the same amount (halved and split to both sides for an interior
     * position), recomputes the minimum, and stops once every spacing is non negative. The reconstructed
     * line spectral frequencies are then rebuilt cumulatively from the corrected spacings.
     *
     * @param lsf     the LSF vector to correct in place, {@value #LPC_ORDER} entries
     * @param minDist the per position minimum spacings, {@value #LPC_ORDER}{@code  + 1} entries
     * @throws AssertionError if the relaxation does not reach a non negative spacing within {@code 1000}
     *                        iterations
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
            LOGGER.log(Level.ERROR, "mlow lsf dequantize: minimum-distance relaxation did not converge");
        }
        throw new AssertionError("LSF minimum-distance relaxation did not converge");
    }
}
