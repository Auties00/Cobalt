package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Encoder-only line-spectral-frequency search tables, gain bit-cost tables, and rate-control constants for
 * the MLow speech codec, the port of the encode-relevant products of {@code smpl_load_lsf_CBks}
 * ({@code smpl_lsf_tables.c}), the gain inverse-probability builders in {@code smpl_create_celp_tables}
 * ({@code smpl_celp.c}), and the encoder-side literals in {@code smpl_tables.c}.
 *
 * <p>The decode pipeline never reads these products, so {@link LsfCodebooks} and {@link MiscTables}
 * deliberately leave them out; the analysis-by-synthesis encoder needs them to quantize and rate-control a
 * frame. This class derives every table from the raw data already transcribed in {@link LsfTables},
 * {@link MiscTables}, and {@link CmfBuilder}; no codebook bytes are duplicated. Three families are built:
 * <ul>
 * <li>The LSF stage-1 search products the vector quantizer {@code smpl_lsf_quant} reads: the symmetric
 * inverse-covariance matrix {@code cInv}, the projected codebook {@code cbCinv}, the inverse weighting
 * matrix {@code wie}, and the per-centroid stage-1 bit costs {@code bits} and {@code bits_cond}; plus the
 * stage-2 per-level bit costs {@code numBits} that the same routine accumulates as it walks the scalar
 * quantizer levels.</li>
 * <li>The adaptive-codebook and fixed-codebook gain inverse-probability cost tables the gain quantizer
 * reads: {@code acbg_inv_prob_lr} / {@code acbg_inv_prob_hr} and {@code fcbg_v_inv_prob} /
 * {@code fcbg_v_delta_inv_prob}, each a {@code cmf_to_bits} self-information scaled by
 * {@code pow(2, bits * SMPL_G_ACB_RD_MU)}.</li>
 * <li>The rate-control, perceptual-emphasis, and voiced/unvoiced-weight scalars from {@code smpl_tables.c}
 * the bitrate controller and pitch analysis read, plus the pulse-count-to-bitrate sigmoid
 * {@code smpl_get_normalized_bitrate}.</li>
 * </ul>
 *
 * <p>The LSF and gain cost tables are floating point computed in single precision to match the reference
 * {@code log2f} and {@code powf}, so they are near-exact (within IEEE-754 rounding) rather than
 * bit-exact-critical; they feed rate-distortion comparisons, never entropy-coder symbol resolution. The
 * scalar literals are transcribed verbatim. Scope is the 16 kHz / 60 ms / mono SMPL encode path; high-band
 * ({@code >16} kHz) tables are out of scope.
 *
 * @implNote This implementation ports the encode-relevant half of {@code smpl_load_lsf_CBks} (the
 * {@code cInv}, {@code cbCinv}, {@code wie}, {@code bits}, {@code bits_cond}, and stage-2 {@code numBits}
 * build), the gain inverse-probability loops of {@code smpl_create_celp_tables}, and the encoder literals
 * of {@code smpl_tables.c}. The gain cost transform uses the libm {@code powf} the native build links for
 * {@code powf(2.0f, ...)} (it is not one of the {@code smpl_powf_fast} call sites), reproduced with
 * {@code (float) Math.pow(2.0, ...)}. The LSF symmetric matrix multiply mirrors
 * {@code smpl_matrix_mult_transp_16}: with the row-major flattened symmetric {@code cInv} the projected
 * codebook entry {@code i} is {@code sum_j cInv[i][j] * lsf[j]}. The codebook is built once and cached
 * behind a double-checked lock, like {@link LsfCodebooks}.
 */
public final class EncoderTables {
    /**
     * The logger for {@link EncoderTables}.
     */
    private static final System.Logger LOGGER = Log.get(EncoderTables.class);

    /**
     * Smallest LSF spacing used by the Laroia weighting, the {@code 1e-3f} floor in
     * {@code smpl_lsf_weights_laroia}.
     *
     * <p>Each inverse delta is computed over a spacing clamped up to this value so a near-coincident pair of
     * line spectral frequencies cannot produce an unbounded weight; identical to the decode-side constant in
     * {@link LsfCodebooks}.
     */
    private static final float LAROIA_MIN_DIST = 1e-3f;

    /**
     * Upper band edge for the Laroia weighting, the {@code SMPL_PI} constant.
     *
     * <p>This is the {@code float}-rounded value of pi used by the reference; the last inverse delta is taken
     * against the distance from the highest line spectral frequency to this edge.
     */
    private static final float SMPL_PI = 3.1415926535897f;

    /**
     * Number of adaptive-codebook gain centroids, {@code SMPL_ACBG_N}.
     *
     * <p>The conditional gain CMFs hold {@value #ACBG_N}{@code  + 1} rows (one per previous-index context,
     * including the no-previous-index context) of {@value #ACBG_N} symbols each.
     */
    private static final int ACBG_N = 16;

    /**
     * Number of voiced fixed-codebook absolute gain levels, {@code SMPL_FCBG_V_N}.
     *
     * <p>Matches the decode-side voiced fixed-codebook-gain CMF length minus one in {@link MiscTables}.
     */
    private static final int FCBG_V_N = 34;

    /**
     * Number of voiced fixed-codebook delta gain levels, {@code SMPL_FCBG_V_DELTA_N}.
     */
    private static final int FCBG_V_DELTA_N = 67;

    /**
     * Rate-distortion mu weighting the gain bit cost, {@code SMPL_G_ACB_RD_MU}.
     *
     * <p>Each gain symbol's self-information in bits is scaled by this factor and exponentiated as
     * {@code pow(2, bits * mu)} so the stored cost is a multiplicative penalty the gain quantizer applies to
     * the squared error rather than an additive bit count.
     */
    private static final float SMPL_G_ACB_RD_MU = 0.014999999664723873f;

    /**
     * Per-centroid stage-1 cost weighting, fixed point {@code SMPL_E} base for the rate-control sigmoid,
     * {@code SMPL_E}.
     *
     * <p>The {@code float}-rounded value of Euler's number used by {@code bitrate2pulses}; carried here for
     * documentation parity with the native source, which the rate-control model does not invoke in this
     * table-build path.
     */
    private static final float SMPL_E = 2.7182818284590f;

    /**
     * Coefficients of the pulse-count-to-normalized-bitrate sigmoid, {@code smpl_pulses2normalized_bitrate}.
     *
     * <p>The two scalars feed {@code smpl_get_normalized_bitrate}: a gain on the log pulse density and a
     * bias subtracted before the sigmoid.
     */
    private static final float[] PULSES2NORMALIZED_BITRATE = {1.4f, 6.5f};

    /**
     * The lazily built, cached encoder LSF search tables shared by all encode sessions.
     */
    private static volatile LsfSearch cachedLsf;

    /**
     * The lazily built, cached gain inverse-probability cost tables shared by all encode sessions.
     */
    private static volatile GainCosts cachedGains;

    /**
     * Pitch-emphasis perceptual weighting coefficient, {@code smpl_perc_emph_pitch}.
     *
     * <p>Applied by the harmonic perceptual weighting to the pitch-correlation autoregressive coefficients.
     */
    public static final float PERC_EMPH_PITCH = -0.82f;

    /**
     * Voiced perceptual-emphasis coefficients per rate class, {@code smpl_perc_emph_v}.
     *
     * <p>Index {@code 0} is high rate, index {@code 1} is low rate; the coefficient shapes the analysis
     * weighting filter for voiced frames.
     */
    public static final float[] PERC_EMPH_V = {-0.72f, -0.77f};

    /**
     * Unvoiced perceptual-emphasis coefficients per rate class, {@code smpl_perc_emph_uv}.
     *
     * <p>Index {@code 0} is high rate, index {@code 1} is low rate.
     */
    public static final float[] PERC_EMPH_UV = {-0.55f, -0.6f};

    /**
     * Low-rate mode bitrate thresholds per payload length, {@code smpl_low_rate_thr}.
     *
     * <p>Indexed {@code [hysteresis][framelenIndex]} where the outer index selects the rising
     * ({@code 0}) versus falling ({@code 1}) hysteresis edge and the inner index selects the payload length
     * bucket; the encoder switches into low-rate coding when the target bitrate drops below the matching
     * threshold.
     */
    public static final float[][] LOW_RATE_THR = {
        {12000.0f, 10200.0f, 8700.0f, 8700.0f},
        {12500.0f, 10700.0f, 9200.0f, 9200.0f}
    };

    /**
     * Voiced/unvoiced decision feature weights, {@code smpl_vuv_weights}.
     *
     * <p>Five active weights on the voicing features, in order: normalized correlations, voice-activity
     * detector output, spectral tilt, harmonicity, and short-lag preference. The sixth slot is a trailing
     * zero carried verbatim from the C literal.
     */
    public static final float[] VUV_WEIGHTS = {1.0f, 0.5f, 0.5f, 0.7f, 0.3f, 0.0f};

    /**
     * Default fixed-codebook total survivor caps per rate class, {@code smpl_fcb_tot_surv_20ms_max}.
     *
     * <p>Index {@code 0} is high rate, index {@code 1} is low rate; the pulse-search survivor budget per
     * 20 ms used when the complexity setting selects the data-driven default rather than a fixed override.
     */
    public static final int[] FCB_TOT_SURV_20MS_MAX = {100, 100};

    /**
     * Rate-control pulse-target thresholds per payload length and rate class,
     * {@code smpl_rate_control_thrs_comp5}.
     *
     * <p>Indexed {@code [framelenIndex][lowRate]}; when the de-banded target bitrate is below the matching
     * threshold the controller forces a single-pulse target, otherwise it evaluates the rate-control model
     * polynomial {@link #RATE_CONTROL_MODEL_COMP5}.
     */
    public static final int[][] RATE_CONTROL_THRS_COMP5 = {
        {7500, 10000},
        {4500, 5750},
        {4000, 5000},
        {4000, 4750}
    };

    /**
     * Rate-control pulse-target model coefficients, {@code smpl_rate_control_model_comp5}.
     *
     * <p>Indexed {@code [framelenIndex][lowRate]} over four payload-length buckets and two rate classes;
     * each row is the eight-coefficient {@code bitrate2pulses} model the controller evaluates as
     * {@code c0 + c1*r + c2*r^2 + c3*r^3 + c4*r^4 + c5*exp((r - c6)*c7)} on the kbit/s target {@code r}.
     * Stored as {@code double} to carry the native {@code float} literals at their full written precision.
     */
    public static final double[][][] RATE_CONTROL_MODEL_COMP5 = {
        {
            {5.166876656946171, -8.981699804753452, 0.07280811614105594, 0.1301196310618402, -0.01597680442864421, 1.7601470147884113, -3.8161195433141755, 0.3038629198331684},
            {-71.71229978402292, 14.197572549553076, -0.9863630205846172, 0.032124893286072924, -0.0003538411576874928, 1.803705259861388e-11, 10.0, 1.2454667523627154}
        },
        {
            {32.5371190670542, -41.270234279452104, 10.490270829170875, -1.102121269442237, 0.03848319274046071, 3.405326741403831, -5.102658181889428, 0.2141935195026695},
            {-177.10486363500775, 43.952329593498376, -3.7049735533247454, 0.14239771116996938, -0.001919963993993193, 7.953695588409639e-6, 5.220317075476664, 0.6435364076926223}
        },
        {
            {-79.2663194911617, 45.00981883522089, -10.063311543498518, 1.2311531056576501, -0.06023559069137118, 0.059204788212259364, 3.033961466462233, 1.0111383197827808},
            {-122.04861900525415, 31.62096398905459, -2.613237037423586, 0.10050433143234094, -0.0013233009240188039, 2.14859438836692e-7, 1.9077791307787761, 0.7059420500333776}
        },
        {
            {-182.64255084224325, 122.90780796179816, -31.308790671748525, 3.7850563849431462, -0.1750480676903051, 0.05399618467364628, 3.009451055091342, 1.1243365512229038},
            {-132.4565456943888, 34.361297004632966, -2.7956546289118887, 0.10428149547078584, -0.001322667891395693, 2.678747426340249e-6, 6.9940208056381925, 0.7551244069345737}
        }
    };

    /**
     * The encoder LSF stage-1 search products, stage-2 bit costs, and the scalar-quantizer geometry the
     * survivor search needs, the encode fields of {@code smpl_LSF_CBs} plus the stage-2 step and clamp
     * tables {@code smpl_LSF_qstep}, {@code smpl_LSF_St2_min_qi}, and {@code smpl_LSF_St2_max_qi}.
     *
     * <p>Indexing follows the C convention: {@code voiced} is {@code 0} for unvoiced and {@code 1} for
     * voiced; {@code lowRate} is {@code 0} for high rate and {@code 1} for low rate; {@code centroid} runs
     * over {@code [0, LSF_CB_CENTROIDS]} where the final index is the conditional slot. The stage-1 matrices
     * are per voicing class; the stage-2 costs and clamps are per {@code (voiced, lowRate, centroid)} tuple.
     *
     * @param stage1        the two stage-1 search products, indexed by {@code voiced}
     * @param stage2NumBits the stage-2 per-level bit costs, indexed {@code [voiced][lowRate][centroid][coefficient]}
     *                      with the innermost array holding one cost per quantizer level
     * @param qstep         the stage-2 quantization step, indexed {@code [voiced][lowRate]};
     *                      {@code smpl_LSF_qstep}
     * @param condMult      the conditional-centroid step multiplier, {@code LSF_QSTEP_COND_MULT}
     * @param minQi         the per-coefficient lower clamp on the stage-2 level offset, indexed
     *                      {@code [voiced][lowRate][centroid][coefficient]}; {@code smpl_LSF_St2_min_qi}
     * @param maxQi         the per-coefficient upper clamp on the stage-2 level offset, indexed
     *                      {@code [voiced][lowRate][centroid][coefficient]}; {@code smpl_LSF_St2_max_qi}
     */
    public record LsfSearch(LsfStage1[] stage1, float[][][][][] stage2NumBits, float[][] qstep, float condMult,
                            int[][][][] minQi, int[][][][] maxQi) {
        /**
         * Returns the stage-1 search products for a voicing class.
         *
         * @param voiced {@code 0} for unvoiced, {@code 1} for voiced
         * @return the stage-1 search products
         */
        public LsfStage1 stage1(int voiced) {
            return stage1[voiced];
        }

        /**
         * Returns the stage-2 per-level bit costs for a voicing class, rate class, and centroid.
         *
         * @param voiced   {@code 0} for unvoiced, {@code 1} for voiced
         * @param lowRate  {@code 0} for high rate, {@code 1} for low rate
         * @param centroid the stage-1 centroid index in {@code [0, LSF_CB_CENTROIDS]}
         * @return the per-coefficient per-level bit cost arrays
         */
        public float[][] stage2NumBits(int voiced, int lowRate, int centroid) {
            return stage2NumBits[voiced][lowRate][centroid];
        }

        /**
         * Returns the stage-2 quantization step for a voicing and rate class.
         *
         * <p>The conditional centroid additionally scales this by {@link #condMult()}.
         *
         * @param voiced  {@code 0} for unvoiced, {@code 1} for voiced
         * @param lowRate {@code 0} for high rate, {@code 1} for low rate
         * @return the stage-2 quantization step
         */
        public float qstep(int voiced, int lowRate) {
            return qstep[voiced][lowRate];
        }

        /**
         * Returns the per-coefficient lower clamp on the stage-2 level offset for a tuple.
         *
         * @param voiced   {@code 0} for unvoiced, {@code 1} for voiced
         * @param lowRate  {@code 0} for high rate, {@code 1} for low rate
         * @param centroid the stage-1 centroid index in {@code [0, LSF_CB_CENTROIDS]}
         * @return the per-coefficient lower clamps
         */
        public int[] minQi(int voiced, int lowRate, int centroid) {
            return minQi[voiced][lowRate][centroid];
        }

        /**
         * Returns the per-coefficient upper clamp on the stage-2 level offset for a tuple.
         *
         * @param voiced   {@code 0} for unvoiced, {@code 1} for voiced
         * @param lowRate  {@code 0} for high rate, {@code 1} for low rate
         * @param centroid the stage-1 centroid index in {@code [0, LSF_CB_CENTROIDS]}
         * @return the per-coefficient upper clamps
         */
        public int[] maxQi(int voiced, int lowRate, int centroid) {
            return maxQi[voiced][lowRate][centroid];
        }
    }

    /**
     * The encoder LSF stage-1 search products for one voicing class, the encode fields of
     * {@code smpl_LSF_CB_st1}.
     *
     * @param cInv     the symmetric inverse-covariance matrix, {@value LsfTables#SMPL_LPC_ORDER} square;
     *                 reconstructed from the packed lower triangle and mirrored
     * @param cbCinv   the projected codebook, {@value LsfTables#LSF_CB_CENTROIDS} rows of
     *                 {@value LsfTables#SMPL_LPC_ORDER}; row {@code c} is {@code cInv} applied to the
     *                 absolute centroid LSF, used by the stage-1 nearest-neighbour error
     * @param wie      the inverse weighting rotation per centroid,
     *                 {@value LsfTables#LSF_CB_CENTROIDS} matrices of {@value LsfTables#SMPL_LPC_ORDER}
     *                 square; the {@code wrot2} output of {@code smpl_rot_apply_wght}, applied to the
     *                 stage-1 quantization error before stage-2 scalar quantization
     * @param bits     the per-centroid stage-1 bit cost for non-conditional coding,
     *                 {@value LsfTables#LSF_CB_CENTROIDS} entries from {@code cmf_to_bits} of the stage-1
     *                 centroid CMF
     * @param bitsCond the per-centroid stage-1 bit cost for conditional coding,
     *                 {@value LsfTables#LSF_CB_CENTROIDS}{@code  + 1} entries from {@code cmf_to_bits} of the
     *                 conditional stage-1 centroid CMF (the extra symbol selects the conditional centroid)
     */
    public record LsfStage1(float[][] cInv, float[][] cbCinv, float[][][] wie, float[] bits, float[] bitsCond) {
    }

    /**
     * The gain inverse-probability cost tables the gain quantizer reads, the gain-cost fields of
     * {@code CelpTables}.
     *
     * <p>Every entry is {@code pow(2, selfInformationBits * SMPL_G_ACB_RD_MU)}, a multiplicative
     * rate-distortion penalty rather than an additive bit count.
     *
     * @param acbgInvProbLr the low-rate adaptive-codebook gain cost, {@value #ACBG_N}{@code  + 1} rows
     *                      (one per previous-index context) of {@value #ACBG_N} entries
     * @param acbgInvProbHr the high-rate adaptive-codebook gain cost, same shape as {@code acbgInvProbLr}
     * @param fcbgVInvProb  the voiced fixed-codebook absolute gain cost, {@value #FCBG_V_N} entries
     * @param fcbgVDeltaInvProb the voiced fixed-codebook delta gain cost, {@value #FCBG_V_DELTA_N} entries
     */
    public record GainCosts(float[][] acbgInvProbLr, float[][] acbgInvProbHr, float[] fcbgVInvProb, float[] fcbgVDeltaInvProb) {
    }

    /**
     * Prevents instantiation of this stateless loader.
     */
    private EncoderTables() {
        throw new AssertionError("no instances");
    }

    /**
     * Returns the encoder LSF stage-1 search products and stage-2 bit costs, building and caching them on
     * first call.
     *
     * <p>The tables are immutable after construction, so the single cached instance is shared across all
     * encode sessions; the first caller performs the full build and subsequent callers reuse it.
     *
     * @return the shared encoder LSF search tables
     */
    public static LsfSearch lsfSearch() {
        var local = cachedLsf;
        if (local == null) {
            synchronized (EncoderTables.class) {
                local = cachedLsf;
                if (local == null) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "building encoder lsf search tables");
                    }
                    local = buildLsfSearch();
                    cachedLsf = local;
                }
            }
        }
        return local;
    }

    /**
     * Returns the gain inverse-probability cost tables, building and caching them on first call.
     *
     * <p>The tables are immutable after construction and shared across all encode sessions.
     *
     * @return the shared gain inverse-probability cost tables
     */
    public static GainCosts gainCosts() {
        var local = cachedGains;
        if (local == null) {
            synchronized (EncoderTables.class) {
                local = cachedGains;
                if (local == null) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "building encoder gain cost tables");
                    }
                    local = buildGainCosts();
                    cachedGains = local;
                }
            }
        }
        return local;
    }

    /**
     * Computes the normalized bitrate for a pulse count and frame length, {@code smpl_get_normalized_bitrate}.
     *
     * <p>The pulse density per 20 ms is {@code numPulses * frameLength16 / (20 * 16)}; the result is the
     * logistic of {@code coeff0 * log2(density + 1) - coeff1} using {@link #PULSES2NORMALIZED_BITRATE} and
     * the same {@code +-80} clamped sigmoid the reference uses to avoid overflow. The computation is single
     * precision to match the native {@code log2f} and {@code expf}.
     *
     * @param numPulses     the number of allocated pulses
     * @param frameLength16 the frame length in 16 kHz samples
     * @return the normalized bitrate in {@code [0, 1]}
     */
    public static float getNormalizedBitrate(int numPulses, int frameLength16) {
        var pulsesPer20ms = (numPulses * frameLength16) / (20.0f * 16.0f);
        var x = PULSES2NORMALIZED_BITRATE[0] * log2f(pulsesPer20ms + 1.0f) - PULSES2NORMALIZED_BITRATE[1];
        return sigmoid(x);
    }

    /**
     * Builds the encoder LSF stage-1 search products and stage-2 bit costs, the encode-relevant body of
     * {@code smpl_load_lsf_CBks}.
     *
     * <p>Stage one is built per voicing class; stage two walks the shared packed delta-CMF stream
     * {@link LsfTables#ST2_ALL_QLVL_DCMFS} in flat declaration order, slicing one variable-length run per
     * {@code (voiced, lowRate, centroid, coefficient)} tuple and converting each run's CMF to per-level bit
     * costs. The walk offset advances exactly as the C pointer arithmetic does so the runs line up with the
     * decode-side stage-2 build in {@link LsfCodebooks}.
     *
     * @return the freshly built encoder LSF search tables
     */
    private static LsfSearch buildLsfSearch() {
        var stage1 = new LsfStage1[2];
        for (var voiced = 0; voiced <= 1; voiced++) {
            stage1[voiced] = buildLsfStage1(voiced);
        }

        var order = LsfTables.SMPL_LPC_ORDER;
        var centroids = LsfTables.LSF_CB_CENTROIDS;
        var stage2NumBits = new float[2][2][centroids + 1][order][];
        var dcmf = packDcmf();
        var dcmfOffset = 0;
        for (var voiced = 0; voiced <= 1; voiced++) {
            for (var lowRate = 0; lowRate <= 1; lowRate++) {
                for (var c = 0; c <= centroids; c++) {
                    for (var i = 0; i < order; i++) {
                        var minQi = LsfTables.MIN_QI[voiced][lowRate][c][i];
                        var maxQi = LsfTables.MAX_QI[voiced][lowRate][c][i];
                        var count = maxQi - minQi + 1;
                        var cmf = CmfBuilder.dcmfToCmf(dcmf, dcmfOffset, count);
                        stage2NumBits[voiced][lowRate][c][i] = CmfBuilder.cmfToBits(cmf);
                        dcmfOffset += count;
                    }
                }
            }
        }
        return new LsfSearch(stage1, stage2NumBits, LsfTables.QSTEP, LsfTables.LSF_QSTEP_COND_MULT,
                LsfTables.MIN_QI, LsfTables.MAX_QI);
    }

    /**
     * Builds the encoder LSF stage-1 search products for one voicing class.
     *
     * <p>The symmetric inverse-covariance matrix is reconstructed from the packed lower triangle and
     * mirrored. Each centroid's absolute LSF (the affine-reconstructed codebook value offset by the class
     * mean) is projected through that matrix into {@code cbCinv} and run through the inverse weighting of
     * its unpacked rotation matrix into {@code wie}. The per-centroid stage-1 bit costs are
     * {@code cmf_to_bits} of the stage-1 and conditional stage-1 centroid CMFs.
     *
     * @param voiced {@code 0} for unvoiced, {@code 1} for voiced
     * @return the stage-1 search products for the class
     */
    private static LsfStage1 buildLsfStage1(int voiced) {
        var order = LsfTables.SMPL_LPC_ORDER;
        var centroids = LsfTables.LSF_CB_CENTROIDS;

        var cinvPacked = voiced == 0 ? LsfTables.CINV_UV : LsfTables.CINV_V;
        var cinvMin = voiced == 0 ? LsfTables.LSF_CINV_UV_MIN : LsfTables.LSF_CINV_V_MIN;
        var cinvScale = voiced == 0 ? LsfTables.LSF_CINV_UV_SCALE : LsfTables.LSF_CINV_V_SCALE;
        var cInv = new float[order][order];
        var p = 0;
        for (var i = 0; i < order; i++) {
            for (var j = 0; j <= i; j++) {
                var value = cinvMin + cinvScale * cinvPacked[p++];
                cInv[i][j] = value;
                cInv[j][i] = value;
            }
        }

        var cb16 = voiced == 0 ? LsfTables.CB_UV : LsfTables.CB_V;
        var cbMin = voiced == 0 ? LsfTables.LSF_CB_UV_MIN : LsfTables.LSF_CB_V_MIN;
        var cbScale = voiced == 0 ? LsfTables.LSF_CB_UV_SCALE : LsfTables.LSF_CB_V_SCALE;
        var mean = voiced == 0 ? LsfTables.MEAN_UV : LsfTables.MEAN_V;
        var rot8 = voiced == 0 ? LsfTables.ROT_UV : LsfTables.ROT_V;
        var rotMin = voiced == 0 ? LsfTables.LSF_ROT_UV_MIN : LsfTables.LSF_ROT_V_MIN;
        var rotScale = voiced == 0 ? LsfTables.LSF_ROT_UV_SCALE : LsfTables.LSF_ROT_V_SCALE;

        var cbCinv = new float[centroids][order];
        var wie = new float[centroids][order][order];
        for (var c = 0; c < centroids; c++) {
            var lsfCb = new float[order];
            for (var i = 0; i < order; i++) {
                lsfCb[i] = cbMin + cb16[c][i] * cbScale + mean[i];
            }
            cbCinv[c] = symMatrixMult(cInv, lsfCb);
            var rot = new float[order][order];
            for (var i = 0; i < order; i++) {
                for (var j = 0; j < order; j++) {
                    rot[i][j] = rotMin + rot8[c][i][j] * rotScale;
                }
            }
            inverseRotWeight(rot, lsfCb, wie[c]);
        }

        var stage1Cmf = voiced == 0 ? LsfTables.CMF_UV : LsfTables.CMF_V;
        var stage1CmfCond = voiced == 0 ? LsfTables.CMF_COND_UV : LsfTables.CMF_COND_V;
        var bits = CmfBuilder.cmfToBits(stage1Cmf);
        var bitsCond = CmfBuilder.cmfToBits(stage1CmfCond);
        return new LsfStage1(cInv, cbCinv, wie, bits, bitsCond);
    }

    /**
     * Builds the gain inverse-probability cost tables, the gain-cost loops of {@code smpl_create_celp_tables}.
     *
     * <p>The adaptive-codebook gain CMFs come from the decode-side row builders in {@link MiscTables}; each
     * row's {@code cmf_to_bits} self-information is exponentiated by {@code pow(2, bits * mu)}. The voiced
     * fixed-codebook absolute and delta CMFs come from {@link MiscTables} and are transformed the same way.
     *
     * @return the freshly built gain inverse-probability cost tables
     */
    private static GainCosts buildGainCosts() {
        var acbgLr = new float[ACBG_N + 1][];
        var acbgHr = new float[ACBG_N + 1][];
        for (var context = 0; context <= ACBG_N; context++) {
            acbgLr[context] = invProb(CmfBuilder.cmfToBits(MiscTables.acbGainsCmfLr(context)));
            acbgHr[context] = invProb(CmfBuilder.cmfToBits(MiscTables.acbGainsCmfHr(context)));
        }
        var fcbgV = invProb(CmfBuilder.cmfToBits(MiscTables.fcbgVCmf()));
        var fcbgVDelta = invProb(CmfBuilder.cmfToBits(MiscTables.fcbgVDeltaCmf()));
        return new GainCosts(acbgLr, acbgHr, fcbgV, fcbgVDelta);
    }

    /**
     * Converts a per-symbol bit-cost vector into the multiplicative gain penalty, the
     * {@code powf(2.0f, bits * SMPL_G_ACB_RD_MU)} loop of {@code smpl_create_celp_tables}.
     *
     * <p>The transform is applied in place semantics over a freshly allocated copy so the caller's CMF-bit
     * array is left untouched; each entry becomes {@code pow(2, bits * mu)} in single precision.
     *
     * @param bits the per-symbol self-information in bits
     * @return a freshly allocated array of the exponentiated multiplicative penalties
     */
    private static float[] invProb(float[] bits) {
        var out = new float[bits.length];
        for (var i = 0; i < bits.length; i++) {
            out[i] = (float) Math.pow(2.0, bits[i] * (double) SMPL_G_ACB_RD_MU);
        }
        return out;
    }

    /**
     * Applies a symmetric matrix to a vector, the {@code smpl_matrix_mult_transp_16} call shape used for
     * {@code cbCinv}.
     *
     * <p>With a symmetric matrix the transposed multiply {@code y[i] = sum_j C[j][i] * x[j]} equals
     * {@code y[i] = sum_j C[i][j] * x[j]}, so the result is computed directly from the row-major matrix.
     *
     * @param matrix the symmetric matrix, {@value LsfTables#SMPL_LPC_ORDER} square
     * @param vector the input vector, {@value LsfTables#SMPL_LPC_ORDER} entries
     * @return a freshly allocated {@value LsfTables#SMPL_LPC_ORDER}-entry product vector
     */
    private static float[] symMatrixMult(float[][] matrix, float[] vector) {
        var order = LsfTables.SMPL_LPC_ORDER;
        var out = new float[order];
        for (var i = 0; i < order; i++) {
            var sum = 0.0f;
            for (var j = 0; j < order; j++) {
                sum += matrix[i][j] * vector[j];
            }
            out[i] = sum;
        }
        return out;
    }

    /**
     * Computes the inverse weighting rotation, the {@code wrot2} output of {@code smpl_rot_apply_wght}.
     *
     * <p>The Laroia weights of {@code lsf} are square-rooted, and the inverse weighting is
     * {@code wie[j][i] = rot[i][j] * sqrt(weight[j])}, the transpose-and-scale companion of the forward
     * weighting {@link LsfCodebooks} builds. Only this inverse matrix is produced here; the forward matrix
     * lives in the decode-side codebook. The square root is the {@code -Ofast} {@link FastSqrt#sqrt(float)}
     * approximation rather than a correctly rounded one, matching the reference codebook tables exactly.
     *
     * @param rot the unpacked rotation matrix, {@value LsfTables#SMPL_LPC_ORDER} square
     * @param lsf the absolute codebook line spectral frequencies, {@value LsfTables#SMPL_LPC_ORDER} entries
     * @param wie the destination inverse weighting matrix, {@value LsfTables#SMPL_LPC_ORDER} square
     */
    private static void inverseRotWeight(float[][] rot, float[] lsf, float[][] wie) {
        var order = LsfTables.SMPL_LPC_ORDER;
        var weight = laroiaWeights(lsf);
        var sqrtWeight = new float[order];
        for (var i = 0; i < order; i++) {
            sqrtWeight[i] = FastSqrt.sqrt(weight[i]);
        }
        for (var i = 0; i < order; i++) {
            for (var j = 0; j < order; j++) {
                wie[j][i] = rot[i][j] * sqrtWeight[j];
            }
        }
    }

    /**
     * Computes the Laroia perceptual weights of a line-spectral-frequency vector,
     * {@code smpl_lsf_weights_laroia}.
     *
     * <p>Identical to the decode-side weighting in {@link LsfCodebooks}: the inverse spacing between adjacent
     * line spectral frequencies (and against the {@code 0} and {@code SMPL_PI} band edges, each spacing
     * clamped up to {@link #LAROIA_MIN_DIST}) is accumulated so each coefficient's weight is the sum of the
     * inverse spacing on either side of it.
     *
     * @param lsf the line spectral frequencies, {@value LsfTables#SMPL_LPC_ORDER} ascending entries
     * @return a freshly allocated weight vector of {@value LsfTables#SMPL_LPC_ORDER} entries
     */
    private static float[] laroiaWeights(float[] lsf) {
        var order = LsfTables.SMPL_LPC_ORDER;
        var invDelta = new float[order + 1];
        invDelta[0] = 1.0f / Math.max(lsf[0], LAROIA_MIN_DIST);
        for (var i = 1; i < order; i++) {
            invDelta[i] = 1.0f / Math.max(lsf[i] - lsf[i - 1], LAROIA_MIN_DIST);
        }
        invDelta[order] = 1.0f / Math.max(SMPL_PI - lsf[order - 1], LAROIA_MIN_DIST);
        var weight = new float[order];
        for (var i = 0; i < order; i++) {
            weight[i] = invDelta[i] + invDelta[i + 1];
        }
        return weight;
    }

    /**
     * Computes the base-two logarithm in single precision, {@code log2f}.
     *
     * <p>Used only by {@link #getNormalizedBitrate(int, int)}; the {@code float} cast applied to the
     * {@code double} {@link Math#log} ratio reproduces the single-precision rounding of the native call.
     *
     * @param x the operand, strictly positive
     * @return the single-precision base-two logarithm of {@code x}
     */
    private static float log2f(float x) {
        return (float) (Math.log(x) / Math.log(2.0));
    }

    /**
     * Computes the clamped logistic, {@code smpl_sigmoid}.
     *
     * <p>Returns {@code 1} for inputs above {@code 80}, {@code 0} for inputs below {@code -80}, and
     * {@code 1 / (1 + exp(-x))} otherwise, matching the native overflow guard that keeps the exponent in a
     * finite single-precision range.
     *
     * @param x the logistic input
     * @return the logistic of {@code x} in {@code [0, 1]}
     */
    private static float sigmoid(float x) {
        if (x > 80.0f) {
            return 1.0f;
        }
        if (x < -80.0f) {
            return 0.0f;
        }
        return 1.0f / (1.0f + (float) Math.exp(-x));
    }

    /**
     * Narrows the unsigned delta-CMF stream {@link LsfTables#ST2_ALL_QLVL_DCMFS} into a signed
     * {@code byte[]} view {@link CmfBuilder#dcmfToCmf(byte[], int, int)} consumes.
     *
     * <p>Mirrors the decode-side packing in {@link LsfCodebooks}; each unsigned byte carried as {@code int}
     * is narrowed to a signed {@code byte} once so the per-run slices need no further copy.
     *
     * @return a freshly allocated byte array of {@value LsfTables#ST2_ALL_QLVLS_LEN} entries
     */
    private static byte[] packDcmf() {
        var out = new byte[LsfTables.ST2_ALL_QLVL_DCMFS.length];
        for (var i = 0; i < out.length; i++) {
            out[i] = (byte) LsfTables.ST2_ALL_QLVL_DCMFS[i];
        }
        return out;
    }
}
