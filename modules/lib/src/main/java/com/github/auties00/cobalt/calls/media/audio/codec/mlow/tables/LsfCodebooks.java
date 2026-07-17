package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Builds the decode-ready two-stage line-spectral-frequency (LSF) codebook from the packed tables in
 * {@link LsfTables}, the port of the decode-relevant half of {@code smpl_load_lsf_CBks}
 * ({@code smpl_lsf_tables.c}).
 *
 * <p>MLow reconstructs the short-term linear-prediction spectrum of each frame from two quantizer indices.
 * Stage one selects one of {@value LsfTables#LSF_CB_CENTROIDS} codebook centroids (or a {@code 17}th
 * conditional centroid built from the previous frame); stage two adds a per-coefficient scalar refinement.
 * The dequantizer {@code smpl_lsf_dequant} reads, for the chosen stage-1 centroid, its half-scaled centroid
 * vector and its weighting rotation matrix, and for each coefficient the stage-2 quantization level selected
 * by the stage-2 index. This loader rebuilds exactly those runtime structures plus the entropy-coding
 * cumulative mass functions (CMFs) the range decoder uses to read the indices.
 *
 * <p>The build proceeds in two phases mirroring the C loader:
 * <ul>
 * <li>Stage one, per voicing class: each packed centroid in {@link LsfTables#CB_V} / {@link LsfTables#CB_UV}
 * is affine-reconstructed and offset by the class mean to form the absolute codebook LSF; its half value is
 * stored as {@code cbHalf}. The packed per-centroid rotation matrix ({@link LsfTables#ROT_V} /
 * {@link LsfTables#ROT_UV}) is unpacked and folded with the Laroia weights of that codebook LSF into the
 * forward weighting matrix {@code we} used by the dequantizer. The two conditional rotation matrices
 * ({@link LsfTables#ROT_COND_V} / {@link LsfTables#ROT_COND_UV}) are unpacked into {@code rotCond}.</li>
 * <li>Stage two, walking the two packed streams in flat declaration order: for every
 * {@code (voiced, lowRate, centroid, coefficient)} tuple the quantizer-level count is
 * {@code MAX_QI - MIN_QI + 1}; that many bytes of {@link LsfTables#ST2_ALL_QLVLS_8} are affine-reconstructed
 * and scaled by the stage-2 step into the {@code qLvls} run, and that many bytes of
 * {@link LsfTables#ST2_ALL_QLVL_DCMFS} are expanded by {@link CmfBuilder#dcmfToCmf(byte[], int, int)} into
 * the {@code (count + 1)}-entry decoder CMF.</li>
 * </ul>
 *
 * <p>The CMFs are the bit-exact-critical product: the range decoder resolves each stage-2 index by comparing
 * its decoded cumulative value against the exact integer CMF boundaries, so {@link CmfBuilder} reproduces the
 * C integer arithmetic exactly. The float codebook values ({@code cbHalf}, {@code we}, {@code rotCond},
 * {@code qLvls}) match the C single-precision results to within IEEE-754 rounding.
 *
 * <p>Scope is the 16 kHz / 60 ms / mono SMPL decode path. The encoder-only stage-1 products of the C loader
 * (the symmetric inverse-covariance matrix {@code cInv}, the projected codebook {@code cbCinv}, the inverse
 * weighting matrix {@code wie}, and the per-centroid bit costs {@code bits} / {@code bits_cond}) are not
 * built here because the dequantizer never reads them; the packed inputs for those
 * ({@link LsfTables#CINV_V}, {@link LsfTables#CINV_UV}, {@link LsfTables#CMF_V}, {@link LsfTables#CMF_UV},
 * {@link LsfTables#CMF_COND_V}, {@link LsfTables#CMF_COND_UV}) are transcribed in {@link LsfTables} and left
 * for an encoder-side loader. High-band ({@code >16} kHz) LSF tables are out of scope.
 *
 * @implNote This implementation ports the stage-1 and stage-2 build loops of {@code smpl_load_lsf_CBks},
 * {@code smpl_lsf_weights_laroia}, and {@code smpl_rot_apply_wght} from {@code smpl_lsf_tables.c} and
 * {@code smpl_lsf_quant.c}. The single shared stage-2 streams are walked with running offsets exactly as the
 * C pointer arithmetic does, so each {@code qLvls} / {@code cmf} run is sliced from the same positions. The
 * codebook is built once and cached; the holder records are immutable views over the freshly allocated
 * arrays.
 */
public final class LsfCodebooks {
    /**
     * The logger for {@link LsfCodebooks}.
     */
    private static final System.Logger LOGGER = Log.get(LsfCodebooks.class);

    /**
     * Smallest LSF spacing used by the Laroia weighting, the {@code 1e-3f} floor in
     * {@code smpl_lsf_weights_laroia}.
     *
     * <p>Each inverse delta is computed over a spacing clamped up to this value so a near-coincident pair of
     * line spectral frequencies cannot produce an unbounded weight.
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
     * The lazily built, cached codebook shared by all decode sessions.
     */
    private static volatile Codebook cached;

    /**
     * The decode-ready two-stage LSF codebook, the decode-relevant fields of {@code smpl_LSF_CBs}.
     *
     * <p>Indexing follows the C convention throughout: {@code voiced} is {@code 0} for the unvoiced class and
     * {@code 1} for the voiced class; {@code lowRate} is {@code 0} for high rate and {@code 1} for low rate;
     * {@code centroid} runs over {@code [0, LSF_CB_CENTROIDS]} where the final index
     * {@value LsfTables#LSF_CB_CENTROIDS} is the conditional slot.
     *
     * @param stage1 the two stage-1 sub-codebooks, indexed by {@code voiced}
     * @param stage2 the stage-2 sub-codebooks, indexed {@code [voiced][lowRate][centroid]}
     */
    public record Codebook(Stage1[] stage1, Stage2[][][] stage2) {
        /**
         * Returns the stage-1 sub-codebook for a voicing class.
         *
         * @param voiced {@code 0} for unvoiced, {@code 1} for voiced
         * @return the stage-1 sub-codebook
         */
        public Stage1 stage1(int voiced) {
            return stage1[voiced];
        }

        /**
         * Returns the stage-2 sub-codebook for a voicing class, rate class, and stage-1 centroid.
         *
         * @param voiced   {@code 0} for unvoiced, {@code 1} for voiced
         * @param lowRate  {@code 0} for high rate, {@code 1} for low rate
         * @param centroid the stage-1 centroid index in {@code [0, LSF_CB_CENTROIDS]}
         * @return the stage-2 sub-codebook
         */
        public Stage2 stage2(int voiced, int lowRate, int centroid) {
            return stage2[voiced][lowRate][centroid];
        }
    }

    /**
     * The decode-relevant stage-1 products for one voicing class, the decode fields of
     * {@code smpl_LSF_CB_st1} plus the per-voicing scalar tables the dequantizer reads.
     *
     * <p>The {@code mean}, {@code regCond}, and {@code minDist} fields are not part of the C
     * {@code smpl_LSF_CB_st1} struct; they are the per-voicing tables {@code smpl_LSF_mean_*},
     * {@code smpl_LSF_reg_cond}, and {@code smpl_LSF_min_dist_*} that {@code smpl_lsf_dequant} indexes by
     * voicing class. They are carried here so the conditional-centroid reconstruction and the
     * minimum-distance ordering can be done entirely from this decode-ready facade.
     *
     * @param cbHalf  the half-scaled centroid codebook, {@value LsfTables#LSF_CB_CENTROIDS} rows of
     *                {@value LsfTables#SMPL_LPC_ORDER} values; the dequantizer doubles the selected row to
     *                recover the stage-1 LSF estimate
     * @param we      the forward weighting rotation per centroid,
     *                {@value LsfTables#LSF_CB_CENTROIDS} matrices of
     *                {@value LsfTables#SMPL_LPC_ORDER} by {@value LsfTables#SMPL_LPC_ORDER}; applied to the
     *                stage-2 residual by the dequantizer for a non-conditional centroid
     * @param rotCond the two conditional rotation matrices (index {@code 0} high rate, index {@code 1} low
     *                rate), each {@value LsfTables#SMPL_LPC_ORDER} by {@value LsfTables#SMPL_LPC_ORDER};
     *                applied when the conditional centroid is selected
     * @param cmf     the stage-1 centroid cumulative mass function for non-conditional coding,
     *                {@code smpl_LSF_CMF_*}; {@value LsfTables#LSF_CB_CENTROIDS}{@code  + 1} entries
     * @param cmfCond the stage-1 centroid cumulative mass function for conditional coding,
     *                {@code smpl_LSF_CMF_cond_*}; {@value LsfTables#LSF_CB_CENTROIDS}{@code  + 2} entries,
     *                the extra symbol selecting the conditional centroid
     * @param mean    the per-coefficient LSF mean, {@code smpl_LSF_mean_*}; {@value LsfTables#SMPL_LPC_ORDER}
     *                entries, used to pull the previous frame toward the class mean when the conditional
     *                centroid is selected
     * @param regCond the conditional-centroid regularization weight, {@code smpl_LSF_reg_cond[voiced]}
     * @param minDist the minimum LSF spacing constraints, {@code smpl_LSF_min_dist_*};
     *                {@value LsfTables#SMPL_LPC_ORDER}{@code  + 1} entries enforced against the band edges and
     *                each adjacent pair
     */
    public record Stage1(float[][] cbHalf, float[][][] we, float[][][] rotCond,
                         int[] cmf, int[] cmfCond, float[] mean, float regCond, float[] minDist) {
    }

    /**
     * The stage-2 scalar-quantizer products for one {@code (voiced, lowRate, centroid)} tuple, the decode
     * fields of {@code smpl_LSF_CB_st2}.
     *
     * @param numQlvls the quantizer-level count per coefficient, {@value LsfTables#SMPL_LPC_ORDER} entries;
     *                 entry {@code i} equals {@code qLvls[i].length} and {@code cmf[i].length - 1}
     * @param qLvls    the dequantized levels per coefficient; ragged, with {@code qLvls[i]} holding
     *                 {@code numQlvls[i]} reconstructed values
     * @param cmf      the decoder cumulative mass function per coefficient; ragged, with {@code cmf[i]}
     *                 holding {@code numQlvls[i] + 1} strictly increasing entries (a leading zero followed by
     *                 the running cumulative total)
     */
    public record Stage2(int[] numQlvls, float[][] qLvls, int[][] cmf) {
    }

    /**
     * Prevents instantiation of this stateless loader.
     */
    private LsfCodebooks() {
        throw new AssertionError("no instances");
    }

    /**
     * Returns the decode-ready LSF codebook, building and caching it on first call.
     *
     * <p>The codebook is immutable and stateless after construction, so the single cached instance is shared
     * across all decode sessions. The first caller performs the full build; subsequent callers reuse the
     * cached instance.
     *
     * @return the shared two-stage LSF codebook
     */
    public static Codebook load() {
        var local = cached;
        if (local == null) {
            synchronized (LsfCodebooks.class) {
                local = cached;
                if (local == null) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "building decode-ready lsf codebook");
                    }
                    local = build();
                    cached = local;
                }
            }
        }
        return local;
    }

    /**
     * Builds the decode-ready codebook from the packed tables, the decode-relevant body of
     * {@code smpl_load_lsf_CBks}.
     *
     * <p>Stage one is built per voicing class; stage two walks the two shared packed streams
     * ({@link LsfTables#ST2_ALL_QLVLS_8} and {@link LsfTables#ST2_ALL_QLVL_DCMFS}) in flat declaration order,
     * slicing one variable-length run per {@code (voiced, lowRate, centroid, coefficient)} tuple. The walk
     * offsets advance exactly as the C pointer arithmetic does, so the slices line up with the C loader.
     *
     * @return the freshly built codebook
     */
    private static Codebook build() {
        var stage1 = new Stage1[2];
        for (var voiced = 0; voiced <= 1; voiced++) {
            stage1[voiced] = buildStage1(voiced);
        }

        var stage2 = new Stage2[2][2][LsfTables.LSF_CB_CENTROIDS + 1];
        var qlvlsOffset = 0;
        var dcmfOffset = 0;
        for (var voiced = 0; voiced <= 1; voiced++) {
            for (var lowRate = 0; lowRate <= 1; lowRate++) {
                var qstep = LsfTables.QSTEP[voiced][lowRate];
                for (var c = 0; c <= LsfTables.LSF_CB_CENTROIDS; c++) {
                    var qs = qstep;
                    if (c == LsfTables.LSF_CB_CENTROIDS) {
                        qs *= LsfTables.LSF_QSTEP_COND_MULT;
                    }
                    var numQlvls = new int[LsfTables.SMPL_LPC_ORDER];
                    var qLvls = new float[LsfTables.SMPL_LPC_ORDER][];
                    var cmf = new int[LsfTables.SMPL_LPC_ORDER][];
                    for (var i = 0; i < LsfTables.SMPL_LPC_ORDER; i++) {
                        var minQi = LsfTables.MIN_QI[voiced][lowRate][c][i];
                        var maxQi = LsfTables.MAX_QI[voiced][lowRate][c][i];
                        var count = maxQi - minQi + 1;
                        numQlvls[i] = count;
                        var levels = new float[count];
                        for (var lvl = 0; lvl < count; lvl++) {
                            var packed = LsfTables.ST2_ALL_QLVLS_8[qlvlsOffset++];
                            levels[lvl] = (LsfTables.ST2_ALL_QLVLS_MIN
                                    + LsfTables.ST2_ALL_QLVLS_SCALE * packed
                                    + lvl + minQi) * qs;
                        }
                        qLvls[i] = levels;
                        cmf[i] = CmfBuilder.dcmfToCmf(DCMF_BYTES, dcmfOffset, count);
                        dcmfOffset += count;
                    }
                    stage2[voiced][lowRate][c] = new Stage2(numQlvls, qLvls, cmf);
                }
            }
        }
        return new Codebook(stage1, stage2);
    }

    /**
     * Builds the stage-1 decode products for one voicing class.
     *
     * <p>Each centroid is affine-reconstructed from its packed indices, offset by the class mean, and halved
     * into {@code cbHalf}; its packed rotation matrix is unpacked and folded with the Laroia weights of the
     * absolute codebook LSF into the forward weighting matrix {@code we}. The conditional rotation matrices
     * are unpacked into {@code rotCond}.
     *
     * @param voiced {@code 0} for unvoiced, {@code 1} for voiced
     * @return the stage-1 products for the class
     */
    private static Stage1 buildStage1(int voiced) {
        var cb16 = voiced == 0 ? LsfTables.CB_UV : LsfTables.CB_V;
        var cbMin = voiced == 0 ? LsfTables.LSF_CB_UV_MIN : LsfTables.LSF_CB_V_MIN;
        var cbScale = voiced == 0 ? LsfTables.LSF_CB_UV_SCALE : LsfTables.LSF_CB_V_SCALE;
        var mean = voiced == 0 ? LsfTables.MEAN_UV : LsfTables.MEAN_V;
        var rot8 = voiced == 0 ? LsfTables.ROT_UV : LsfTables.ROT_V;
        var rotMin = voiced == 0 ? LsfTables.LSF_ROT_UV_MIN : LsfTables.LSF_ROT_V_MIN;
        var rotScale = voiced == 0 ? LsfTables.LSF_ROT_UV_SCALE : LsfTables.LSF_ROT_V_SCALE;
        var rotCond8 = voiced == 0 ? LsfTables.ROT_COND_UV : LsfTables.ROT_COND_V;
        var rotCondMin = voiced == 0 ? LsfTables.LSF_ROT_COND_UV_MIN : LsfTables.LSF_ROT_COND_V_MIN;
        var rotCondScale = voiced == 0 ? LsfTables.LSF_ROT_COND_UV_SCALE : LsfTables.LSF_ROT_COND_V_SCALE;

        var order = LsfTables.SMPL_LPC_ORDER;
        var cbHalf = new float[LsfTables.LSF_CB_CENTROIDS][order];
        var we = new float[LsfTables.LSF_CB_CENTROIDS][order][order];
        for (var c = 0; c < LsfTables.LSF_CB_CENTROIDS; c++) {
            var lsfCb = new float[order];
            for (var i = 0; i < order; i++) {
                lsfCb[i] = cbMin + cb16[c][i] * cbScale + mean[i];
                cbHalf[c][i] = lsfCb[i] * 0.5f;
            }
            var rot = new float[order][order];
            for (var i = 0; i < order; i++) {
                for (var j = 0; j < order; j++) {
                    rot[i][j] = rotMin + rot8[c][i][j] * rotScale;
                }
            }
            rotApplyWeight(rot, lsfCb, we[c]);
        }

        var rotCond = new float[2][order][order];
        for (var hr = 0; hr < 2; hr++) {
            for (var i = 0; i < order; i++) {
                for (var j = 0; j < order; j++) {
                    rotCond[hr][i][j] = rotCondMin + rotCond8[hr][i][j] * rotCondScale;
                }
            }
        }
        var stage1Cmf = voiced == 0 ? LsfTables.CMF_UV : LsfTables.CMF_V;
        var stage1CmfCond = voiced == 0 ? LsfTables.CMF_COND_UV : LsfTables.CMF_COND_V;
        var regCond = LsfTables.REG_COND[voiced];
        var minDist = voiced == 0 ? LsfTables.MIN_DIST_UV : LsfTables.MIN_DIST_V;
        return new Stage1(cbHalf, we, rotCond, stage1Cmf, stage1CmfCond, mean, regCond, minDist);
    }

    /**
     * Computes the forward weighting rotation, the {@code wrot1} output of {@code smpl_rot_apply_wght}.
     *
     * <p>The Laroia weights of {@code lsf} are square-rooted, and the forward weighting is
     * {@code we[i][j] = rot[i][j] / sqrt(weight[j])}. Only the forward matrix is built; the inverse
     * weighting {@code wrot2} is an encoder-side product the dequantizer does not read. The square root is the
     * {@code -Ofast} {@link FastSqrt#sqrt(float)} approximation rather than a correctly rounded one, matching
     * the reference codebook tables exactly.
     *
     * @param rot the unpacked rotation matrix, {@value LsfTables#SMPL_LPC_ORDER} square
     * @param lsf the absolute codebook line spectral frequencies, {@value LsfTables#SMPL_LPC_ORDER} entries
     * @param we  the destination forward weighting matrix, {@value LsfTables#SMPL_LPC_ORDER} square
     */
    private static void rotApplyWeight(float[][] rot, float[] lsf, float[][] we) {
        var order = LsfTables.SMPL_LPC_ORDER;
        var weight = laroiaWeights(lsf);
        var sqrtInv = new float[order];
        for (var i = 0; i < order; i++) {
            sqrtInv[i] = 1.0f / FastSqrt.sqrt(weight[i]);
        }
        for (var i = 0; i < order; i++) {
            for (var j = 0; j < order; j++) {
                we[i][j] = rot[i][j] * sqrtInv[j];
            }
        }
    }

    /**
     * Computes the Laroia perceptual weights of a line-spectral-frequency vector,
     * {@code smpl_lsf_weights_laroia}.
     *
     * <p>The inverse spacing between adjacent line spectral frequencies (and against the {@code 0} and
     * {@code SMPL_PI} band edges, each spacing clamped up to {@link #LAROIA_MIN_DIST}) is accumulated so each
     * coefficient's weight is the sum of the inverse spacing on either side of it.
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
     * The stage-2 delta-CMF stream as signed bytes, the {@code byte[]} view
     * {@link CmfBuilder#dcmfToCmf(byte[], int, int)} consumes.
     *
     * <p>{@link LsfTables#ST2_ALL_QLVL_DCMFS} carries the {@value LsfTables#ST2_ALL_QLVLS_LEN} unsigned
     * delta-CMF bytes as {@code int} for unsigned cleanliness; this view narrows each to a signed
     * {@code byte} once at class init so the per-run slices passed to {@link CmfBuilder} need no copy.
     */
    private static final byte[] DCMF_BYTES = packDcmf();

    /**
     * Narrows the unsigned delta-CMF table {@link LsfTables#ST2_ALL_QLVL_DCMFS} into a signed
     * {@code byte[]} view.
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
