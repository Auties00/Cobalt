package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

/**
 * Raw two-stage line-spectral-frequency (LSF) vector-quantization tables for the MLow speech codec, the
 * byte-for-byte port of the static literals in {@code smpl_lsf_tables_st1.c} and
 * {@code smpl_lsf_tables_st2.c}.
 *
 * <p>MLow codes each frame's short-term linear-prediction spectrum as two-stage quantized LSFs. Stage one
 * picks one of {@value #LSF_CB_CENTROIDS} codebook centroids (or a conditional centroid derived from the
 * previous frame); stage two refines the residual with a per-coefficient scalar quantizer whose level count
 * is data driven. Every table is split by the binary voicing class (unvoiced {@code uv} versus voiced
 * {@code v}) and, for stage two, by the rate class (high rate {@code HR} versus low rate {@code LR},
 * here named {@code lowRate}). The decode path in {@link LsfCodebooks} reads these tables to rebuild the
 * runtime codebook the dequantizer consumes.
 *
 * <p>The tables are stored in numeric forms matching the C storage: {@code int[]} holds unsigned 16-bit
 * codebook indices and pre-built CMF entries ({@code uint16_t} in C) and unsigned 8-bit rotation and
 * stage-2 stream values ({@code uint8_t} in C), both widened to {@code int} so the unsigned ranges carry
 * without sign extension; {@code int[]} also holds the signed 8-bit quantizer-index bounds
 * ({@code int8_t} in C); and {@code float[]} holds the single-precision constants transcribed verbatim
 * from the C literals.
 *
 * <p>This class holds DATA only. The transform to the decoder-ready runtime codebook (affine unpacking, the
 * delta-CMF to CMF expansion via {@link CmfBuilder#dcmfToCmf(byte[], int, int)}, and the stage-2
 * quantizer-level reconstruction) lives in {@link LsfCodebooks}. Each table is initialized from a private
 * factory method rather than an inline field literal so the combined data stays under the per-method
 * bytecode limit; the two large stage-2 streams are additionally split into concatenated chunk methods.
 * The scope is the 16 kHz / 60 ms / mono SMPL decode path only; high-band ({@code >16} kHz) LSF tables
 * ({@code smpl_hb_lpc_tables.c}) are out of scope and not ported here.
 *
 * @implNote This implementation transcribes the literals byte-for-byte from {@code smpl_lsf_tables_st1.c}
 * and {@code smpl_lsf_tables_st2.c}. Multidimensional C arrays keep their original row-major shape so each
 * Java row corresponds one-to-one with a C brace group. The two stage-2 streams ({@link #ST2_ALL_QLVLS_8}
 * and {@link #ST2_ALL_QLVL_DCMFS}) are flat because the loader walks them in flat declaration order,
 * slicing out one variable-length run per (voicing, rate, centroid, coefficient) tuple.
 */
final class LsfTables {

    /**
     * Linear-prediction order of the MLow short-term filter, {@code SMPL_LPC_ORDER}.
     *
     * <p>Every LSF vector and rotation matrix has this many coefficients; the inverse-covariance matrices are this order square, stored in packed lower-triangular form.
     */
    static final int SMPL_LPC_ORDER = 16;

    /**
     * Number of stage-1 codebook centroids, {@code LSF_CB_CENTROIDS}.
     *
     * <p>Stage-1 selects one of these {@value} centroids; a conditional slot (index {@value #LSF_CB_CENTROIDS}) is reconstructed at runtime from the previous frame, so stage-2 tables carry {@code LSF_CB_CENTROIDS + 1} rows.
     */
    static final int LSF_CB_CENTROIDS = 16;

    /**
     * Affine origin for the voiced stage-1 centroid codebook, {@code LSF_CB_V_MIN}.
     *
     * <p>Each packed index {@code u} in {@link #CB_V} reconstructs to {@code LSF_CB_V_MIN + u * LSF_CB_V_SCALE} before the per-coefficient mean {@link #MEAN_V} is added.
     */
    static final float LSF_CB_V_MIN = -0.24721986f;

    /**
     * Affine step for the voiced stage-1 centroid codebook, {@code LSF_CB_V_SCALE}.
     */
    static final float LSF_CB_V_SCALE = 7.226229e-6f;

    /**
     * Affine origin for the unvoiced stage-1 centroid codebook, {@code LSF_CB_UV_MIN}.
     */
    static final float LSF_CB_UV_MIN = -0.5873778f;

    /**
     * Affine step for the unvoiced stage-1 centroid codebook, {@code LSF_CB_UV_SCALE}.
     */
    static final float LSF_CB_UV_SCALE = 1.3145164e-5f;

    /**
     * Affine origin for the voiced packed inverse-covariance matrix, {@code LSF_CINV_V_MIN}.
     */
    static final float LSF_CINV_V_MIN = -2.778548e-5f;

    /**
     * Affine step for the voiced packed inverse-covariance matrix, {@code LSF_CINV_V_SCALE}.
     */
    static final float LSF_CINV_V_SCALE = 1.2180106e-9f;

    /**
     * Affine origin for the unvoiced packed inverse-covariance matrix, {@code LSF_CINV_UV_MIN}.
     */
    static final float LSF_CINV_UV_MIN = -3.5960955e-5f;

    /**
     * Affine step for the unvoiced packed inverse-covariance matrix, {@code LSF_CINV_UV_SCALE}.
     */
    static final float LSF_CINV_UV_SCALE = 1.8589316e-9f;

    /**
     * Affine origin for the voiced conditional rotation matrices, {@code LSF_ROT_COND_V_MIN}.
     */
    static final float LSF_ROT_COND_V_MIN = -0.8248211f;

    /**
     * Affine step for the voiced conditional rotation matrices, {@code LSF_ROT_COND_V_SCALE}.
     */
    static final float LSF_ROT_COND_V_SCALE = 0.0064186584f;

    /**
     * Affine origin for the unvoiced conditional rotation matrices, {@code LSF_ROT_COND_UV_MIN}.
     */
    static final float LSF_ROT_COND_UV_MIN = -0.67291605f;

    /**
     * Affine step for the unvoiced conditional rotation matrices, {@code LSF_ROT_COND_UV_SCALE}.
     */
    static final float LSF_ROT_COND_UV_SCALE = 0.0052386564f;

    /**
     * Affine origin for the voiced per-centroid rotation matrices, {@code LSF_ROT_V_MIN}.
     */
    static final float LSF_ROT_V_MIN = -0.8455929f;

    /**
     * Affine step for the voiced per-centroid rotation matrices, {@code LSF_ROT_V_SCALE}.
     */
    static final float LSF_ROT_V_SCALE = 0.0069253775f;

    /**
     * Affine origin for the unvoiced per-centroid rotation matrices, {@code LSF_ROT_UV_MIN}.
     */
    static final float LSF_ROT_UV_MIN = -0.9124832f;

    /**
     * Affine step for the unvoiced per-centroid rotation matrices, {@code LSF_ROT_UV_SCALE}.
     */
    static final float LSF_ROT_UV_SCALE = 0.006554049f;

    /**
     * Stage-2 step multiplier applied to the conditional centroid, {@code LSF_QSTEP_COND_MULT}.
     *
     * <p>When stage-1 selects the conditional slot (centroid index {@value #LSF_CB_CENTROIDS}) the stage-2 quantization step from {@link #QSTEP} is scaled by this factor before the levels are reconstructed.
     */
    static final float LSF_QSTEP_COND_MULT = 0.9f;

    /**
     * Total length of each packed stage-2 stream, {@code LSF_ST2_ALL_QLVLS_LEN}.
     *
     * <p>Both {@link #ST2_ALL_QLVLS_8} and {@link #ST2_ALL_QLVL_DCMFS} hold exactly this many entries; the loader walk over each stream consumes all {@value} bytes.
     */
    static final int ST2_ALL_QLVLS_LEN = 9593;

    /**
     * Total length of the built stage-2 cumulative-mass-function table, {@code LSF_ST2_ALL_QLVL_CMFS_LEN}.
     *
     * <p>Expanding the {@value #ST2_ALL_QLVLS_LEN}-entry delta-CMF stream {@link #ST2_ALL_QLVL_DCMFS} into decoder-ready CMFs adds one leading-zero entry per quantizer run, yielding this many {@code uint16_t} entries in total.
     */
    static final int ST2_ALL_QLVL_CMFS_LEN = 10681;

    /**
     * Affine origin for the packed stage-2 quantization levels, {@code LSF_ST2_ALL_QLVLS_MIN}.
     */
    static final float ST2_ALL_QLVLS_MIN = -0.45f;

    /**
     * Affine step for the packed stage-2 quantization levels, {@code LSF_ST2_ALL_QLVLS_SCALE}.
     */
    static final float ST2_ALL_QLVLS_SCALE = 0.0034478905f;

    /**
     * Voiced stage-1 centroid codebook, {@code smpl_LSF_cb_v_16}.
     *
     * <p>{@value #LSF_CB_CENTROIDS} rows of {@value #SMPL_LPC_ORDER} unsigned 16-bit indices; each index reconstructs through the affine pair {@link #LSF_CB_V_MIN} / {@link #LSF_CB_V_SCALE} and the mean {@link #MEAN_V}.
     */
    static final int[][] CB_V = makeCB_V();

    /**
     * Unvoiced stage-1 centroid codebook, {@code smpl_LSF_cb_uv_16}.
     *
     * <p>{@value #LSF_CB_CENTROIDS} rows of {@value #SMPL_LPC_ORDER} unsigned 16-bit indices reconstructed through {@link #LSF_CB_UV_MIN} / {@link #LSF_CB_UV_SCALE} and {@link #MEAN_UV}.
     */
    static final int[][] CB_UV = makeCB_UV();

    /**
     * Voiced inverse-covariance matrix in packed lower-triangular form, {@code smpl_LSF_cinv_v_16}.
     *
     * <p>{@code SMPL_LPC_ORDER * (SMPL_LPC_ORDER + 1) / 2 = 136} unsigned 16-bit entries laid out row by row over the lower triangle ({@code j <= i}); the loader mirrors each entry into both {@code [i][j]} and {@code [j][i]} of the symmetric matrix.
     */
    static final int[] CINV_V = makeCINV_V();

    /**
     * Unvoiced inverse-covariance matrix in packed lower-triangular form, {@code smpl_LSF_cinv_uv_16}.
     *
     * <p>{@code 136} unsigned 16-bit entries with the same lower-triangular layout as {@link #CINV_V}.
     */
    static final int[] CINV_UV = makeCINV_UV();

    /**
     * Voiced conditional rotation matrices, {@code smpl_LSF_rot_cond_v_8}.
     *
     * <p>Two {@value #SMPL_LPC_ORDER}-by-{@value #SMPL_LPC_ORDER} matrices of unsigned 8-bit values, one per rate class (index {@code 0} high rate, index {@code 1} low rate); reconstructed through {@link #LSF_ROT_COND_V_MIN} / {@link #LSF_ROT_COND_V_SCALE}.
     */
    static final int[][][] ROT_COND_V = makeROT_COND_V();

    /**
     * Unvoiced conditional rotation matrices, {@code smpl_LSF_rot_cond_uv_8}.
     *
     * <p>Two {@value #SMPL_LPC_ORDER}-by-{@value #SMPL_LPC_ORDER} matrices of unsigned 8-bit values (high rate, low rate) reconstructed through {@link #LSF_ROT_COND_UV_MIN} / {@link #LSF_ROT_COND_UV_SCALE}.
     */
    static final int[][][] ROT_COND_UV = makeROT_COND_UV();

    /**
     * Voiced per-centroid rotation matrices, {@code smpl_LSF_Rot_v_8}.
     *
     * <p>{@value #LSF_CB_CENTROIDS} matrices of {@value #SMPL_LPC_ORDER} by {@value #SMPL_LPC_ORDER} unsigned 8-bit values, one per stage-1 centroid; reconstructed through {@link #LSF_ROT_V_MIN} / {@link #LSF_ROT_V_SCALE} and folded into the stage-2 weighting at load time.
     */
    static final int[][][] ROT_V = makeROT_V();

    /**
     * Unvoiced per-centroid rotation matrices, {@code smpl_LSF_Rot_uv_8}.
     *
     * <p>{@value #LSF_CB_CENTROIDS} matrices of {@value #SMPL_LPC_ORDER} by {@value #SMPL_LPC_ORDER} unsigned 8-bit values reconstructed through {@link #LSF_ROT_UV_MIN} / {@link #LSF_ROT_UV_SCALE}.
     */
    static final int[][][] ROT_UV = makeROT_UV();

    /**
     * Pre-built voiced stage-1 cumulative mass function, {@code smpl_LSF_CMF_v}.
     *
     * <p>{@code LSF_CB_CENTROIDS + 1 = 17} strictly increasing {@code uint16_t} entries (a leading zero followed by the running total) used directly as the decoder CMF for the voiced stage-1 centroid index. Unlike the stage-2 streams this table is stored already-cumulative, not as a delta-CMF.
     */
    static final int[] CMF_V = makeCMF_V();

    /**
     * Pre-built unvoiced stage-1 cumulative mass function, {@code smpl_LSF_CMF_uv}.
     *
     * <p>{@code 17} strictly increasing {@code uint16_t} entries used directly as the decoder CMF for the unvoiced stage-1 centroid index.
     */
    static final int[] CMF_UV = makeCMF_UV();

    /**
     * Pre-built voiced conditional-centroid stage-1 cumulative mass function, {@code smpl_LSF_CMF_cond_v}.
     *
     * <p>{@code LSF_CB_CENTROIDS + 2 = 18} strictly increasing {@code uint16_t} entries; the extra symbol over {@link #CMF_V} codes selection of the conditional centroid.
     */
    static final int[] CMF_COND_V = makeCMF_COND_V();

    /**
     * Pre-built unvoiced conditional-centroid stage-1 cumulative mass function, {@code smpl_LSF_CMF_cond_uv}.
     *
     * <p>{@code 18} strictly increasing {@code uint16_t} entries; the extra symbol over {@link #CMF_UV} codes selection of the conditional centroid.
     */
    static final int[] CMF_COND_UV = makeCMF_COND_UV();

    /**
     * Stage-2 minimum quantizer index per coefficient, {@code smpl_LSF_St2_min_qi}.
     *
     * <p>Indexed {@code [voiced][lowRate][centroid][coefficient]} over {@code 2} voicing classes, {@code 2} rate classes, {@code LSF_CB_CENTROIDS + 1 = 17} centroids (the last being the conditional slot), and {@value #SMPL_LPC_ORDER} coefficients. Each value is a signed 8-bit lower bound; the number of stage-2 levels for a coefficient is {@code MAX_QI - MIN_QI + 1}.
     */
    static final int[][][][] MIN_QI = makeMIN_QI();

    /**
     * Stage-2 maximum quantizer index per coefficient, {@code smpl_LSF_St2_max_qi}.
     *
     * <p>Same {@code [voiced][lowRate][centroid][coefficient]} shape as {@link #MIN_QI}; each value is a signed 8-bit upper bound and {@code MAX_QI - MIN_QI + 1} is the quantizer-level count for that coefficient.
     */
    static final int[][][][] MAX_QI = makeMAX_QI();

    /**
     * Packed stage-2 quantization-level stream, {@code smpl_LSF_St2_all_qlvls_8}.
     *
     * <p>{@value #ST2_ALL_QLVLS_LEN} unsigned 8-bit values concatenated across every {@code (voiced, lowRate, centroid, coefficient)} run in loader order. Each value reconstructs through {@link #ST2_ALL_QLVLS_MIN} / {@link #ST2_ALL_QLVLS_SCALE} plus a per-level integer offset and the stage-2 step from {@link #QSTEP}; see {@link LsfCodebooks}.
     */
    static final int[] ST2_ALL_QLVLS_8 = makeST2_ALL_QLVLS_8();

    /**
     * Packed stage-2 delta cumulative-mass-function stream, {@code smpl_LSF_St2_all_qlvl_dcmfs}.
     *
     * <p>{@value #ST2_ALL_QLVLS_LEN} unsigned 8-bit delta-CMF values concatenated across the same runs as {@link #ST2_ALL_QLVLS_8}. Each {@code numQlvls}-byte run is expanded into a {@code numQlvls + 1}-entry decoder CMF by {@link CmfBuilder#dcmfToCmf(byte[], int, int)} at load time.
     */
    static final int[] ST2_ALL_QLVL_DCMFS = makeST2_ALL_QLVL_DCMFS();

    /**
     * Conditional-centroid regularization weights, {@code smpl_LSF_reg_cond}.
     *
     * <p>One weight per voicing class ({@code [0]} unvoiced, {@code [1]} voiced); the conditional centroid is the previous frame's LSFs pulled toward the class mean by this fraction.
     */
    static final float[] REG_COND = makeREG_COND();

    /**
     * Voiced LSF mean vector, {@code smpl_LSF_mean_v}.
     *
     * <p>{@value #SMPL_LPC_ORDER} per-coefficient means added to the affine-reconstructed {@link #CB_V} centroids to form the absolute voiced stage-1 LSF estimate.
     */
    static final float[] MEAN_V = makeMEAN_V();

    /**
     * Unvoiced LSF mean vector, {@code smpl_LSF_mean_uv}.
     *
     * <p>{@value #SMPL_LPC_ORDER} per-coefficient means added to the affine-reconstructed {@link #CB_UV} centroids.
     */
    static final float[] MEAN_UV = makeMEAN_UV();

    /**
     * Voiced minimum-distance constraints, {@code smpl_LSF_min_dist_v}.
     *
     * <p>{@code SMPL_LPC_ORDER + 1 = 17} minimum spacings enforced between successive dequantized LSFs (and against the {@code 0} and {@code pi} band edges) to keep the reconstructed spectrum stable.
     */
    static final float[] MIN_DIST_V = makeMIN_DIST_V();

    /**
     * Unvoiced minimum-distance constraints, {@code smpl_LSF_min_dist_uv}.
     *
     * <p>{@code 17} minimum LSF spacings with the same role as {@link #MIN_DIST_V} for the unvoiced class.
     */
    static final float[] MIN_DIST_UV = makeMIN_DIST_UV();

    /**
     * Stage-2 quantization steps, {@code smpl_LSF_qstep}.
     *
     * <p>Indexed {@code [voiced][lowRate]}; the scalar step applied to the reconstructed stage-2 levels for each voicing and rate class. The conditional centroid additionally scales this by {@link #LSF_QSTEP_COND_MULT}.
     */
    static final float[][] QSTEP = makeQSTEP();

    /**
     * Concatenates the chunked sub-arrays of a split table into one flat array.
     *
     * <p>The two {@value #ST2_ALL_QLVLS_LEN}-entry stage-2 streams are too large for a single Java array
     * literal to fit the per-method bytecode limit, so each is produced by several chunk methods that this
     * helper joins in order into the full flat array.
     *
     * @param chunks the chunk sub-arrays in declaration order
     * @return a freshly allocated array holding every chunk concatenated
     */
    private static int[] concat(int[]... chunks) {
        var total = 0;
        for (var chunk : chunks) {
            total += chunk.length;
        }
        var out = new int[total];
        var pos = 0;
        for (var chunk : chunks) {
            System.arraycopy(chunk, 0, out, pos, chunk.length);
            pos += chunk.length;
        }
        return out;
    }

    /**
     * Returns the CB_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CB_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][] makeCB_V() {
        return new int[][] {
                {38837, 37446, 38894, 43362, 35806, 37112, 29887, 30471, 31489, 25042, 20522, 11668, 7001, 34288, 35211, 32709},
                {37569, 39713, 35665, 23815, 12518, 26430, 42263, 33070, 31947, 24403, 27838, 26740, 28202, 29879, 33752, 33633},
                {36279, 38074, 34524, 34701, 33448, 23253, 33295, 25912, 32077, 28681, 25592, 21381, 10649, 22463, 39534, 37090},
                {34320, 34645, 32953, 38786, 31082, 37077, 28320, 34584, 27821, 19574, 13079, 19074, 44640, 34383, 24987, 13243},
                {36064, 36150, 30731, 38910, 37305, 27377, 34185, 25929, 29942, 27284, 24403, 16574, 42276, 40865, 36882, 33473},
                {37416, 37544, 32995, 41191, 49849, 36550, 41056, 29956, 28751, 32642, 22913, 14652, 7322, 7169, 23650, 35231},
                {36815, 36507, 35601, 33017, 22254, 10705, 0, 25831, 27701, 28250, 31202, 29157, 39252, 41040, 39298, 36245},
                {31265, 29912, 28757, 33592, 33798, 35058, 33776, 33548, 35748, 37232, 43318, 46651, 44313, 38964, 36253, 34817},
                {29720, 26762, 23559, 16755, 38641, 34377, 30975, 28915, 36137, 40268, 37017, 37112, 39380, 36874, 35342, 35091},
                {35352, 35428, 32562, 13960, 11334, 49603, 38634, 38493, 26511, 30837, 32134, 36863, 37262, 35092, 29946, 29953},
                {33171, 33780, 33289, 37572, 35110, 37189, 35884, 40084, 38264, 38197, 39239, 36056, 28949, 19715, 14217, 2027},
                {33053, 31242, 32025, 48438, 38860, 37976, 27429, 35319, 24425, 21470, 24845, 39588, 37615, 34615, 34590, 36895},
                {37322, 37606, 43862, 34884, 29915, 27593, 33432, 35052, 32062, 37131, 38299, 40644, 40153, 37120, 34482, 33439},
                {29622, 28658, 33820, 31489, 36073, 38117, 41745, 43330, 44178, 47789, 48851, 48057, 44507, 41070, 38372, 36963},
                {33220, 31369, 24009, 33524, 65535, 50553, 43825, 31283, 33674, 31332, 27205, 21649, 26518, 38352, 37715, 35278},
                {29869, 26825, 40946, 46454, 47010, 51967, 51728, 54003, 53190, 54983, 53974, 51342, 46177, 41078, 37233, 35112}
            };
    }

    /**
     * Returns the CB_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CB_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][] makeCB_UV() {
        return new int[][] {
                {44999, 39100, 32188, 41087, 38539, 38669, 35300, 34858, 34535, 33540, 32972, 35553, 40090, 41925, 42807, 43922},
                {45572, 44231, 39542, 41407, 36799, 35332, 32371, 28683, 25232, 19327, 11735, 4866, 0, 32337, 41107, 43662},
                {41630, 40922, 41430, 42095, 39388, 39178, 40549, 40473, 40042, 40438, 41026, 41427, 41989, 43134, 43393, 44299},
                {46062, 41982, 41276, 42021, 40128, 42620, 40482, 40847, 39072, 37664, 37908, 38210, 38857, 35765, 34132, 28015},
                {41353, 39598, 38115, 38559, 36633, 36199, 37816, 38507, 38948, 40661, 41764, 42809, 43666, 44608, 44887, 45733},
                {46963, 48080, 46046, 43147, 37593, 37539, 39670, 37602, 37082, 36465, 37213, 37154, 37491, 40542, 42789, 44030},
                {39575, 33075, 36662, 37734, 40350, 40698, 42146, 42995, 44488, 45449, 45941, 46449, 46595, 46732, 46466, 46202},
                {44763, 45353, 50096, 47370, 48499, 49178, 48078, 47293, 47315, 46966, 46425, 46262, 45893, 45157, 44892, 45163},
                {44493, 40996, 39841, 42280, 41104, 42019, 41087, 41508, 42277, 43031, 42804, 43625, 43996, 44224, 44274, 44813},
                {45323, 46142, 51228, 48959, 50703, 51024, 51835, 49146, 48842, 46369, 45735, 41833, 41623, 35928, 34641, 25829},
                {43720, 44512, 43078, 41988, 42325, 42324, 42216, 42844, 43766, 44535, 44950, 45497, 45447, 45824, 45660, 46303},
                {44029, 41932, 55188, 53515, 57405, 57720, 56757, 55505, 54297, 52638, 50812, 49450, 48127, 46718, 45759, 45472},
                {48147, 65535, 65355, 63039, 61914, 57686, 53458, 49347, 48267, 47742, 47786, 47005, 46211, 44679, 43937, 43709},
                {46923, 55828, 55613, 52275, 54138, 53936, 53642, 53202, 53515, 53247, 52458, 51537, 49905, 48626, 47334, 46461},
                {46067, 55799, 55844, 55439, 64547, 65072, 62168, 59365, 56928, 53736, 51281, 49596, 48343, 47272, 46736, 46334},
                {45491, 49040, 52142, 50875, 51316, 54819, 54445, 57520, 57202, 57217, 56054, 54514, 51688, 49948, 48240, 46960}
            };
    }

    /**
     * Returns the CINV_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CINV_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[] makeCINV_V() {
        return new int[] {65535, 0, 53282, 20726, 14262, 42686, 21270, 24350, 18000, 36700, 22500, 23560, 24417, 14678, 36367, 21875, 24775, 20959, 23055, 17449, 37020, 22695, 21736, 23711, 22772, 21421, 18181, 37328, 22640, 24655, 20935, 24213, 23110, 21607, 17513, 37662, 22426, 22724, 24267, 21365, 23426, 21569, 21662, 18478, 37730, 21930, 23894, 21783, 23352, 22290, 23723, 21975, 20933, 16573, 37822, 24474, 21678, 23809, 22236, 23418, 21925, 23380, 22733, 21914, 16905, 36336, 22650, 23799, 22058, 23643, 22282, 23770, 22252, 23199, 22259, 23142, 16546, 36717, 23619, 22146, 23411, 22910, 23046, 22590, 22370, 22870, 23067, 22190, 23515, 15946, 38058, 22278, 23901, 22311, 23369, 22613, 23249, 22281, 23096, 22793, 23166, 22124, 24274, 15358, 39799, 23289, 22732, 22948, 22745, 22746, 22775, 22492, 23127, 22784, 22379, 22913, 22441, 23484, 14993, 40326, 22946, 24176, 22578, 22927, 22990, 22978, 23489, 22525, 22466, 23023, 23301, 23176, 22106, 24514, 14392, 44344};
    }

    /**
     * Returns the CINV_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CINV_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[] makeCINV_UV() {
        return new int[] {37945, 10608, 38305, 20524, 12017, 40876, 18176, 19826, 10254, 39827, 20207, 19558, 18665, 9584, 41648, 18789, 20180, 19933, 17964, 9709, 45342, 20461, 18158, 18668, 20093, 16030, 9348, 47325, 18708, 20276, 18694, 20045, 20635, 15093, 8831, 49438, 19115, 19085, 20208, 18966, 18674, 20295, 15472, 7364, 51061, 18764, 20332, 19221, 19925, 19005, 19602, 19751, 16061, 5238, 53254, 19717, 18785, 20081, 18350, 20147, 18766, 19878, 19242, 17016, 4919, 54967, 18993, 20165, 20078, 19227, 19286, 19435, 19432, 19399, 18586, 18312, 2783, 56745, 19992, 19201, 18481, 19837, 19862, 18930, 19087, 19416, 19515, 18489, 18634, 2957, 59416, 19851, 19233, 19020, 19826, 19686, 18960, 19752, 18568, 20173, 19360, 18156, 18753, 0, 62281, 20395, 18827, 20002, 19646, 18721, 19706, 18785, 19827, 18627, 19672, 19162, 18026, 17498, 1860, 63566, 18393, 20017, 19473, 19421, 19542, 20015, 19606, 19130, 18569, 18736, 19727, 18746, 20358, 19022, 871, 65535};
    }

    /**
     * Returns the ROT_COND_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #ROT_COND_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][][] makeROT_COND_V() {
        return new int[][][] {
                {
                    {0, 216, 129, 135, 125, 136, 123, 131, 127, 130, 125, 128, 127, 130, 127, 129},
                    {77, 58, 246, 95, 145, 111, 140, 110, 136, 116, 130, 118, 132, 118, 136, 116},
                    {82, 49, 98, 212, 50, 157, 142, 123, 107, 134, 122, 127, 118, 130, 130, 118},
                    {130, 125, 155, 131, 119, 138, 132, 123, 118, 147, 118, 138, 114, 172, 46, 246},
                    {144, 158, 132, 133, 114, 114, 224, 24, 131, 176, 115, 129, 136, 122, 139, 123},
                    {110, 111, 101, 140, 169, 30, 184, 180, 57, 141, 139, 121, 127, 128, 117, 132},
                    {117, 119, 97, 154, 134, 84, 158, 113, 218, 36, 156, 118, 146, 110, 113, 161},
                    {119, 115, 115, 127, 149, 99, 128, 121, 172, 126, 76, 183, 58, 210, 106, 76},
                    {114, 106, 111, 139, 152, 100, 94, 133, 166, 179, 36, 158, 159, 60, 146, 163},
                    {107, 97, 86, 115, 169, 129, 90, 88, 144, 172, 143, 47, 168, 152, 66, 95},
                    {161, 171, 190, 194, 82, 80, 117, 169, 167, 159, 116, 90, 164, 158, 116, 102},
                    {131, 133, 127, 94, 104, 160, 173, 157, 112, 90, 77, 160, 197, 111, 50, 82},
                    {139, 141, 152, 191, 159, 110, 79, 80, 99, 122, 175, 201, 133, 87, 77, 101},
                    {129, 126, 134, 174, 185, 150, 124, 106, 94, 88, 94, 136, 203, 201, 180, 146},
                    {138, 136, 148, 189, 206, 195, 177, 162, 146, 132, 110, 91, 80, 94, 108, 119},
                    {144, 148, 135, 131, 109, 100, 93, 86, 75, 64, 57, 64, 81, 104, 117, 126}
                },
                {
                    {255, 39, 130, 121, 131, 120, 134, 126, 129, 126, 133, 128, 131, 126, 130, 127},
                    {76, 65, 245, 92, 148, 109, 141, 110, 139, 112, 132, 116, 135, 114, 140, 111},
                    {136, 136, 162, 111, 140, 126, 130, 125, 126, 140, 120, 139, 113, 176, 44, 240},
                    {173, 204, 140, 49, 211, 95, 108, 146, 152, 113, 140, 126, 142, 117, 143, 117},
                    {115, 98, 133, 127, 126, 170, 26, 218, 136, 81, 142, 129, 119, 139, 121, 129},
                    {152, 154, 152, 113, 93, 216, 97, 54, 208, 119, 114, 139, 129, 129, 139, 128},
                    {140, 136, 159, 98, 132, 164, 99, 136, 61, 219, 82, 158, 94, 162, 140, 87},
                    {140, 144, 149, 122, 110, 170, 115, 135, 73, 163, 159, 79, 196, 52, 148, 181},
                    {113, 104, 112, 143, 149, 94, 98, 140, 174, 163, 33, 171, 151, 65, 145, 166},
                    {147, 156, 162, 129, 96, 138, 169, 159, 101, 79, 132, 209, 81, 102, 192, 169},
                    {90, 78, 58, 66, 182, 174, 132, 82, 94, 108, 143, 150, 99, 103, 133, 150},
                    {126, 124, 131, 164, 152, 95, 83, 101, 149, 171, 176, 91, 63, 147, 205, 175},
                    {139, 141, 152, 191, 160, 111, 77, 78, 99, 124, 177, 198, 133, 87, 78, 102},
                    {130, 127, 135, 174, 184, 150, 125, 106, 93, 87, 95, 137, 202, 202, 181, 146},
                    {139, 136, 149, 189, 206, 195, 178, 162, 146, 132, 109, 91, 80, 94, 108, 119},
                    {144, 149, 134, 131, 109, 100, 93, 86, 74, 63, 57, 64, 82, 104, 117, 126}
                }
            };
    }

    /**
     * Returns the ROT_COND_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #ROT_COND_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][][] makeROT_COND_UV() {
        return new int[][][] {
                {
                    {126, 130, 129, 128, 130, 129, 130, 126, 131, 129, 119, 152, 78, 216, 7, 232},
                    {132, 127, 129, 127, 129, 129, 127, 133, 126, 115, 180, 40, 236, 59, 85, 214},
                    {118, 134, 126, 134, 122, 134, 132, 126, 104, 199, 14, 210, 162, 47, 127, 184},
                    {138, 121, 141, 117, 126, 146, 111, 92, 245, 17, 129, 193, 116, 79, 143, 164},
                    {121, 136, 122, 121, 162, 98, 80, 255, 61, 80, 171, 174, 87, 91, 152, 166},
                    {102, 145, 126, 114, 152, 141, 30, 194, 177, 113, 59, 114, 199, 170, 79, 69},
                    {246, 68, 168, 73, 200, 55, 164, 134, 126, 122, 104, 143, 157, 137, 110, 111},
                    {164, 104, 183, 57, 146, 206, 39, 113, 136, 188, 133, 89, 83, 131, 179, 175},
                    {69, 175, 85, 113, 232, 41, 125, 102, 182, 164, 116, 87, 95, 123, 163, 161},
                    {197, 95, 97, 231, 81, 96, 111, 175, 173, 134, 82, 80, 116, 159, 188, 176},
                    {192, 120, 43, 191, 175, 146, 55, 82, 125, 166, 183, 166, 119, 90, 78, 95},
                    {93, 90, 225, 161, 83, 54, 98, 144, 177, 187, 166, 131, 92, 77, 78, 98},
                    {78, 75, 189, 199, 176, 111, 82, 67, 78, 102, 139, 165, 186, 184, 173, 154},
                    {78, 12, 111, 156, 190, 189, 170, 151, 126, 105, 87, 80, 81, 89, 99, 111},
                    {169, 221, 204, 188, 163, 140, 119, 100, 85, 77, 75, 76, 82, 90, 101, 112},
                    {141, 171, 178, 183, 198, 198, 194, 191, 188, 181, 173, 165, 157, 148, 142, 134}
                },
                {
                    {125, 131, 128, 129, 129, 128, 131, 125, 131, 131, 110, 168, 60, 230, 14, 209},
                    {129, 128, 128, 127, 131, 128, 127, 133, 131, 104, 189, 37, 224, 90, 60, 220},
                    {117, 136, 125, 135, 123, 131, 131, 137, 90, 205, 22, 194, 175, 48, 116, 195},
                    {129, 125, 139, 119, 122, 151, 113, 86, 242, 26, 119, 201, 122, 71, 138, 168},
                    {134, 123, 129, 142, 93, 152, 189, 0, 187, 182, 86, 86, 167, 162, 110, 94},
                    {143, 119, 123, 145, 114, 99, 233, 72, 76, 136, 201, 148, 60, 82, 169, 191},
                    {194, 95, 141, 96, 207, 16, 193, 138, 136, 97, 100, 154, 173, 134, 94, 93},
                    {199, 80, 204, 31, 176, 167, 79, 99, 131, 178, 132, 103, 98, 128, 162, 169},
                    {199, 71, 177, 136, 39, 197, 152, 148, 71, 90, 143, 176, 166, 129, 86, 89},
                    {42, 174, 149, 39, 170, 161, 142, 85, 87, 127, 173, 173, 141, 98, 68, 74},
                    {56, 139, 217, 73, 76, 111, 198, 176, 133, 91, 77, 93, 136, 166, 176, 161},
                    {167, 169, 37, 86, 170, 203, 164, 118, 81, 69, 89, 126, 162, 179, 178, 159},
                    {75, 75, 180, 200, 179, 117, 84, 66, 75, 100, 137, 166, 185, 185, 174, 153},
                    {182, 243, 152, 105, 68, 68, 86, 104, 128, 150, 170, 177, 177, 169, 158, 146},
                    {169, 218, 204, 189, 164, 141, 121, 100, 85, 77, 75, 75, 81, 90, 100, 112},
                    {116, 87, 79, 74, 59, 59, 62, 66, 69, 76, 83, 91, 100, 108, 115, 123}
                }
            };
    }

    /**
     * Returns the ROT_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #ROT_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][][] makeROT_V() {
        return new int[][][] {
                {
                    {224, 26, 122, 119, 115, 100, 139, 133, 124, 110, 128, 120, 123, 118, 127, 118},
                    {103, 129, 148, 61, 184, 84, 153, 128, 174, 54, 145, 108, 136, 98, 144, 105},
                    {151, 119, 106, 65, 174, 206, 52, 106, 105, 135, 133, 107, 138, 109, 137, 113},
                    {135, 126, 116, 75, 169, 101, 124, 133, 133, 141, 60, 171, 99, 159, 63, 182},
                    {119, 128, 112, 98, 154, 71, 155, 134, 64, 202, 92, 106, 153, 89, 161, 95},
                    {134, 142, 109, 116, 134, 104, 136, 141, 80, 125, 205, 59, 130, 130, 76, 187},
                    {152, 165, 36, 148, 125, 110, 98, 198, 136, 87, 108, 140, 149, 98, 135, 122},
                    {147, 143, 62, 126, 118, 127, 159, 45, 187, 145, 100, 72, 151, 131, 111, 125},
                    {102, 95, 171, 136, 105, 119, 82, 142, 154, 131, 85, 93, 204, 77, 104, 173},
                    {117, 117, 102, 118, 112, 146, 164, 74, 87, 92, 127, 179, 128, 51, 148, 188},
                    {103, 103, 105, 116, 124, 135, 146, 109, 77, 87, 121, 149, 198, 142, 47, 66},
                    {100, 93, 92, 156, 160, 53, 50, 70, 130, 133, 158, 149, 111, 96, 103, 113},
                    {171, 172, 165, 151, 141, 96, 102, 84, 96, 98, 121, 143, 173, 182, 169, 141},
                    {182, 189, 168, 109, 97, 113, 117, 111, 131, 138, 133, 142, 107, 57, 67, 87},
                    {132, 123, 140, 198, 196, 146, 141, 119, 96, 91, 75, 78, 88, 94, 100, 115},
                    {120, 127, 107, 77, 72, 83, 82, 95, 74, 65, 68, 69, 94, 118, 122, 122}
                },
                {
                    {53, 228, 56, 130, 123, 133, 125, 129, 124, 123, 118, 123, 118, 129, 121, 128},
                    {145, 114, 100, 218, 29, 127, 113, 143, 95, 142, 121, 129, 119, 130, 118, 127},
                    {218, 131, 32, 112, 155, 116, 99, 150, 130, 96, 130, 122, 126, 117, 129, 119},
                    {144, 119, 95, 101, 145, 139, 162, 76, 37, 204, 110, 131, 110, 135, 113, 130},
                    {126, 119, 97, 145, 106, 118, 194, 50, 141, 94, 137, 96, 153, 75, 162, 75},
                    {140, 119, 113, 131, 114, 127, 166, 69, 169, 84, 111, 149, 84, 176, 65, 181},
                    {118, 110, 111, 121, 119, 140, 102, 107, 153, 134, 50, 194, 79, 133, 155, 52},
                    {134, 132, 123, 112, 107, 147, 94, 96, 154, 152, 65, 142, 189, 59, 120, 187},
                    {69, 57, 81, 164, 175, 101, 155, 160, 98, 95, 84, 142, 135, 98, 111, 145},
                    {96, 85, 84, 138, 137, 113, 71, 86, 162, 166, 122, 65, 148, 152, 66, 85},
                    {107, 94, 110, 120, 126, 255, 110, 123, 111, 92, 149, 114, 119, 120, 122, 121},
                    {143, 146, 141, 102, 105, 139, 165, 161, 103, 98, 71, 117, 184, 146, 57, 69},
                    {138, 131, 140, 156, 155, 144, 151, 144, 152, 147, 80, 68, 137, 186, 193, 146},
                    {101, 87, 82, 48, 52, 110, 128, 134, 113, 106, 80, 68, 94, 127, 145, 142},
                    {135, 127, 126, 126, 127, 141, 170, 171, 174, 175, 122, 94, 61, 59, 84, 113},
                    {138, 150, 149, 156, 147, 122, 84, 87, 79, 77, 64, 65, 73, 88, 104, 120}
                },
                {
                    {15, 201, 83, 148, 107, 131, 118, 127, 118, 139, 112, 126, 112, 126, 118, 126},
                    {61, 102, 197, 137, 162, 56, 155, 83, 145, 108, 137, 94, 133, 110, 134, 108},
                    {80, 78, 201, 112, 82, 196, 109, 121, 98, 149, 100, 129, 124, 137, 97, 150},
                    {118, 117, 135, 118, 160, 78, 102, 157, 145, 106, 103, 179, 58, 155, 75, 184},
                    {138, 124, 102, 121, 106, 127, 207, 35, 94, 143, 127, 149, 74, 132, 109, 145},
                    {128, 79, 114, 217, 101, 74, 105, 138, 67, 174, 106, 140, 120, 122, 130, 106},
                    {110, 117, 139, 94, 127, 135, 119, 126, 128, 119, 92, 189, 69, 116, 167, 27},
                    {125, 117, 133, 192, 65, 147, 131, 128, 158, 39, 174, 137, 98, 131, 125, 117},
                    {132, 117, 112, 150, 104, 124, 141, 100, 194, 114, 18, 139, 167, 115, 127, 136},
                    {110, 122, 125, 117, 137, 127, 138, 139, 70, 80, 118, 170, 146, 53, 186, 182},
                    {113, 124, 116, 117, 138, 118, 161, 140, 63, 63, 101, 145, 179, 162, 59, 80},
                    {72, 33, 55, 87, 98, 110, 142, 145, 127, 99, 109, 84, 96, 121, 130, 126},
                    {140, 153, 151, 123, 110, 115, 173, 175, 106, 122, 93, 79, 102, 194, 188, 138},
                    {108, 97, 96, 139, 173, 149, 71, 64, 113, 103, 127, 133, 141, 200, 172, 136},
                    {120, 136, 132, 69, 34, 52, 83, 92, 111, 111, 129, 148, 149, 149, 138, 131},
                    {136, 143, 134, 131, 120, 119, 75, 84, 76, 67, 63, 61, 68, 93, 108, 118}
                },
                {
                    {9, 203, 113, 138, 115, 141, 105, 135, 115, 130, 111, 122, 121, 128, 127, 111},
                    {166, 175, 40, 65, 185, 145, 103, 135, 96, 142, 120, 126, 118, 125, 120, 131},
                    {92, 116, 195, 49, 196, 131, 106, 113, 156, 85, 135, 125, 118, 104, 131, 137},
                    {118, 109, 166, 111, 142, 128, 126, 95, 97, 205, 71, 140, 84, 187, 84, 123},
                    {136, 111, 107, 142, 102, 197, 38, 89, 177, 98, 125, 130, 113, 156, 94, 129},
                    {113, 143, 99, 123, 128, 76, 166, 132, 166, 82, 123, 127, 124, 161, 33, 174},
                    {118, 109, 153, 130, 117, 158, 98, 158, 42, 128, 173, 110, 157, 103, 61, 167},
                    {131, 122, 134, 128, 129, 121, 123, 177, 88, 69, 165, 135, 60, 197, 142, 81},
                    {84, 95, 83, 98, 117, 137, 163, 28, 95, 118, 190, 111, 107, 137, 124, 120},
                    {68, 33, 68, 88, 114, 115, 99, 168, 117, 119, 83, 116, 89, 108, 120, 141},
                    {135, 151, 132, 137, 99, 119, 120, 113, 120, 129, 136, 153, 30, 80, 147, 204},
                    {118, 122, 118, 140, 144, 35, 47, 112, 133, 158, 168, 70, 115, 136, 127, 127},
                    {110, 109, 110, 137, 139, 81, 89, 103, 102, 113, 131, 245, 149, 111, 115, 107},
                    {132, 137, 128, 133, 129, 99, 96, 73, 63, 54, 60, 89, 92, 92, 88, 96},
                    {113, 99, 110, 199, 197, 139, 137, 112, 107, 103, 97, 111, 145, 152, 169, 177},
                    {117, 107, 112, 176, 181, 145, 144, 137, 151, 147, 148, 121, 76, 65, 73, 68}
                },
                {
                    {217, 27, 162, 101, 133, 110, 128, 119, 130, 106, 127, 121, 128, 116, 126, 119},
                    {65, 120, 185, 127, 177, 46, 140, 107, 149, 100, 141, 96, 153, 90, 134, 125},
                    {117, 115, 139, 126, 134, 102, 126, 119, 122, 126, 121, 126, 110, 192, 26, 197},
                    {180, 157, 35, 147, 134, 77, 135, 124, 151, 88, 144, 93, 157, 103, 116, 151},
                    {128, 119, 111, 138, 159, 68, 47, 202, 132, 126, 93, 150, 98, 148, 129, 90},
                    {126, 154, 140, 54, 138, 162, 105, 150, 171, 75, 91, 164, 131, 83, 126, 178},
                    {120, 125, 101, 120, 133, 101, 181, 73, 164, 80, 76, 176, 82, 164, 128, 76},
                    {113, 130, 137, 111, 101, 156, 104, 146, 162, 52, 189, 79, 116, 165, 99, 71},
                    {110, 102, 145, 213, 64, 121, 129, 148, 118, 68, 131, 166, 109, 102, 141, 161},
                    {129, 136, 114, 92, 160, 109, 140, 127, 69, 105, 187, 130, 39, 130, 168, 157},
                    {102, 106, 97, 98, 138, 118, 148, 150, 45, 80, 133, 169, 168, 96, 69, 84},
                    {101, 87, 89, 134, 144, 133, 43, 46, 141, 118, 160, 158, 104, 86, 97, 122},
                    {185, 205, 185, 147, 126, 115, 112, 99, 110, 137, 144, 159, 116, 102, 89, 90},
                    {131, 134, 132, 142, 161, 150, 92, 90, 89, 94, 119, 139, 193, 194, 178, 139},
                    {127, 117, 123, 181, 206, 189, 141, 135, 117, 106, 88, 82, 92, 96, 100, 116},
                    {116, 108, 112, 139, 155, 151, 166, 160, 176, 183, 186, 178, 150, 135, 129, 120}
                },
                {
                    {228, 47, 139, 100, 154, 84, 130, 116, 131, 100, 128, 123, 124, 123, 122, 123},
                    {167, 98, 103, 123, 58, 188, 68, 182, 118, 151, 80, 134, 129, 123, 124, 124},
                    {102, 127, 118, 136, 187, 51, 57, 189, 125, 157, 92, 124, 123, 120, 116, 127},
                    {127, 122, 125, 110, 131, 114, 172, 77, 109, 190, 27, 161, 103, 133, 110, 130},
                    {179, 188, 12, 136, 133, 108, 133, 111, 138, 129, 131, 114, 128, 120, 127, 123},
                    {121, 112, 132, 112, 123, 123, 123, 98, 130, 177, 112, 32, 197, 88, 143, 84},
                    {130, 154, 150, 73, 129, 141, 96, 108, 213, 131, 116, 84, 102, 166, 96, 172},
                    {135, 113, 119, 129, 129, 131, 101, 100, 42, 179, 176, 89, 103, 147, 86, 182},
                    {109, 73, 105, 225, 99, 105, 98, 75, 165, 124, 114, 113, 118, 165, 115, 118},
                    {110, 108, 112, 142, 101, 104, 192, 178, 120, 92, 94, 87, 166, 127, 95, 197},
                    {121, 123, 126, 133, 139, 138, 80, 74, 134, 108, 117, 170, 175, 46, 120, 191},
                    {142, 161, 146, 136, 162, 150, 100, 107, 76, 74, 85, 121, 181, 198, 133, 112},
                    {160, 167, 182, 175, 110, 113, 141, 139, 128, 149, 133, 120, 100, 113, 210, 154},
                    {157, 164, 171, 164, 117, 125, 140, 140, 138, 144, 146, 147, 146, 105, 28, 83},
                    {130, 121, 123, 160, 178, 177, 126, 124, 113, 88, 83, 65, 58, 70, 103, 117},
                    {137, 157, 140, 110, 50, 55, 83, 93, 89, 81, 84, 89, 99, 98, 101, 120}
                },
                {
                    {29, 223, 120, 160, 99, 117, 124, 127, 123, 123, 116, 125, 119, 129, 118, 129},
                    {85, 143, 148, 32, 220, 116, 97, 129, 119, 120, 119, 124, 121, 124, 121, 126},
                    {154, 157, 72, 136, 160, 38, 160, 123, 116, 119, 133, 110, 138, 83, 171, 85},
                    {167, 151, 68, 128, 146, 93, 122, 134, 110, 133, 119, 131, 101, 181, 52, 187},
                    {89, 83, 183, 144, 130, 55, 165, 99, 145, 96, 151, 86, 149, 125, 84, 162},
                    {144, 152, 112, 101, 109, 159, 122, 125, 117, 131, 132, 94, 185, 42, 120, 200},
                    {115, 103, 154, 156, 140, 93, 104, 102, 77, 216, 56, 141, 130, 101, 125, 137},
                    {117, 117, 120, 146, 130, 126, 66, 150, 63, 148, 186, 44, 142, 141, 109, 91},
                    {134, 117, 133, 180, 150, 101, 29, 153, 158, 70, 114, 157, 137, 100, 130, 139},
                    {158, 151, 194, 126, 114, 116, 142, 154, 62, 106, 164, 155, 71, 119, 161, 155},
                    {61, 63, 59, 136, 141, 129, 127, 130, 105, 120, 144, 120, 69, 104, 167, 178},
                    {145, 149, 129, 126, 127, 123, 82, 19, 163, 149, 164, 107, 92, 138, 158, 142},
                    {114, 120, 110, 141, 147, 141, 128, 71, 84, 106, 164, 177, 109, 62, 50, 87},
                    {117, 117, 113, 147, 154, 155, 142, 87, 78, 93, 125, 157, 204, 184, 162, 134},
                    {135, 130, 139, 177, 188, 179, 170, 160, 180, 167, 144, 117, 116, 122, 121, 115},
                    {139, 135, 132, 152, 154, 153, 132, 92, 92, 62, 53, 50, 82, 110, 118, 122}
                },
                {
                    {4, 204, 120, 123, 125, 126, 122, 122, 125, 121, 120, 125, 117, 125, 123, 124},
                    {136, 134, 93, 126, 118, 133, 117, 122, 118, 129, 116, 145, 67, 208, 51, 181},
                    {153, 161, 29, 139, 101, 155, 100, 134, 107, 139, 101, 154, 92, 112, 168, 75},
                    {137, 150, 61, 136, 110, 131, 119, 127, 113, 124, 134, 83, 199, 76, 84, 189},
                    {111, 105, 150, 144, 100, 121, 146, 111, 86, 172, 74, 187, 103, 69, 141, 186},
                    {147, 154, 138, 41, 200, 143, 64, 136, 133, 132, 109, 140, 117, 99, 134, 153},
                    {116, 119, 121, 116, 152, 76, 124, 184, 37, 179, 109, 108, 144, 141, 95, 85},
                    {139, 147, 85, 94, 159, 53, 205, 99, 117, 112, 155, 115, 85, 112, 153, 143},
                    {116, 113, 108, 81, 132, 162, 156, 29, 120, 182, 93, 103, 144, 133, 95, 87},
                    {120, 120, 143, 116, 105, 181, 114, 116, 59, 144, 187, 66, 91, 136, 172, 153},
                    {119, 118, 124, 85, 118, 175, 167, 140, 77, 66, 160, 179, 123, 88, 71, 95},
                    {185, 216, 187, 131, 92, 129, 155, 123, 115, 117, 96, 117, 143, 142, 124, 112},
                    {140, 151, 141, 162, 128, 88, 72, 75, 120, 156, 191, 142, 94, 79, 75, 96},
                    {123, 124, 126, 84, 75, 127, 142, 175, 169, 160, 111, 71, 62, 73, 85, 112},
                    {126, 122, 128, 192, 199, 160, 140, 120, 109, 92, 78, 75, 81, 92, 102, 116},
                    {125, 121, 125, 159, 165, 164, 168, 166, 180, 184, 171, 155, 149, 140, 133, 123}
                },
                {
                    {0, 195, 144, 115, 129, 119, 127, 119, 125, 125, 122, 121, 121, 124, 123, 121},
                    {101, 47, 226, 76, 147, 104, 135, 114, 125, 107, 140, 112, 126, 121, 125, 113},
                    {119, 114, 133, 125, 131, 121, 122, 121, 120, 136, 104, 149, 82, 198, 45, 200},
                    {127, 135, 109, 120, 114, 125, 143, 104, 132, 82, 191, 53, 178, 106, 87, 184},
                    {112, 104, 144, 141, 99, 138, 136, 124, 95, 179, 68, 137, 169, 55, 132, 189},
                    {70, 52, 82, 182, 101, 95, 157, 155, 82, 130, 128, 91, 127, 143, 102, 91},
                    {124, 127, 122, 90, 159, 145, 39, 170, 76, 172, 131, 78, 155, 138, 100, 108},
                    {115, 116, 163, 149, 38, 205, 91, 152, 119, 93, 144, 120, 101, 141, 141, 127},
                    {115, 118, 113, 138, 161, 84, 95, 193, 97, 74, 150, 149, 88, 92, 168, 179},
                    {162, 174, 193, 201, 124, 73, 126, 147, 121, 126, 122, 121, 157, 141, 109, 102},
                    {147, 154, 136, 98, 109, 121, 179, 105, 32, 164, 171, 117, 79, 129, 146, 125},
                    {114, 104, 125, 184, 169, 139, 96, 61, 144, 160, 137, 81, 99, 149, 185, 148},
                    {114, 114, 125, 164, 167, 166, 103, 75, 77, 95, 156, 181, 129, 82, 71, 96},
                    {123, 125, 114, 112, 152, 153, 151, 117, 77, 68, 80, 137, 188, 189, 169, 131},
                    {134, 134, 133, 135, 182, 188, 185, 173, 143, 117, 93, 78, 85, 96, 102, 112},
                    {128, 132, 133, 125, 107, 97, 88, 81, 72, 67, 53, 59, 80, 98, 106, 122}
                },
                {
                    {25, 225, 103, 121, 131, 137, 113, 125, 122, 121, 119, 124, 123, 126, 118, 126},
                    {173, 149, 7, 183, 122, 121, 127, 133, 107, 131, 117, 127, 121, 125, 120, 125},
                    {110, 128, 141, 124, 125, 49, 191, 188, 49, 135, 116, 129, 115, 130, 113, 130},
                    {114, 128, 107, 122, 143, 75, 156, 99, 146, 97, 141, 87, 172, 50, 181, 74},
                    {124, 135, 119, 128, 124, 70, 185, 44, 177, 113, 121, 128, 94, 164, 82, 144},
                    {105, 114, 129, 142, 99, 130, 139, 92, 117, 176, 61, 182, 77, 112, 174, 66},
                    {193, 187, 119, 23, 142, 109, 113, 121, 119, 133, 114, 141, 111, 129, 140, 109},
                    {136, 137, 136, 118, 98, 142, 145, 88, 106, 183, 79, 110, 181, 65, 97, 187},
                    {117, 117, 119, 127, 132, 83, 95, 163, 176, 93, 96, 179, 94, 70, 144, 194},
                    {141, 149, 139, 130, 62, 168, 166, 102, 88, 69, 178, 144, 89, 96, 158, 151},
                    {140, 154, 141, 135, 14, 91, 105, 161, 169, 121, 103, 93, 140, 140, 121, 97},
                    {109, 113, 108, 113, 116, 102, 105, 114, 121, 179, 146, 46, 68, 138, 188, 171},
                    {80, 74, 52, 45, 72, 126, 140, 127, 122, 118, 122, 120, 101, 91, 84, 111},
                    {116, 107, 107, 102, 131, 166, 173, 144, 137, 77, 51, 100, 151, 175, 174, 147},
                    {143, 141, 142, 136, 152, 165, 162, 161, 156, 125, 100, 68, 55, 73, 83, 98},
                    {119, 117, 118, 116, 129, 153, 169, 168, 183, 187, 189, 163, 153, 139, 135, 123}
                },
                {
                    {16, 211, 105, 131, 123, 137, 110, 136, 120, 123, 107, 133, 111, 142, 121, 116},
                    {156, 117, 102, 128, 116, 129, 129, 118, 125, 128, 101, 180, 31, 197, 127, 84},
                    {125, 120, 115, 136, 116, 127, 117, 128, 126, 103, 174, 43, 121, 205, 60, 138},
                    {74, 91, 198, 95, 134, 94, 126, 110, 173, 61, 157, 125, 81, 116, 137, 102},
                    {167, 160, 79, 78, 173, 134, 60, 164, 162, 78, 142, 121, 133, 113, 139, 94},
                    {131, 136, 113, 118, 149, 66, 160, 167, 121, 87, 115, 176, 116, 114, 60, 192},
                    {113, 124, 134, 73, 189, 87, 135, 139, 41, 171, 161, 104, 98, 129, 142, 107},
                    {135, 156, 67, 152, 97, 92, 189, 104, 125, 90, 179, 104, 105, 105, 185, 125},
                    {117, 114, 114, 104, 119, 207, 100, 101, 95, 111, 167, 137, 67, 87, 118, 191},
                    {109, 87, 126, 165, 76, 128, 98, 206, 63, 85, 152, 144, 129, 111, 125, 83},
                    {134, 129, 151, 147, 109, 96, 86, 181, 145, 150, 92, 78, 79, 128, 181, 186},
                    {158, 178, 181, 186, 135, 103, 79, 87, 117, 142, 176, 163, 136, 124, 116, 123},
                    {164, 181, 191, 87, 101, 172, 178, 146, 96, 90, 98, 110, 136, 137, 132, 117},
                    {134, 143, 127, 135, 114, 133, 149, 156, 161, 171, 136, 94, 70, 54, 62, 76},
                    {121, 108, 125, 200, 212, 154, 141, 115, 105, 81, 81, 93, 107, 109, 126, 113},
                    {106, 96, 127, 137, 156, 172, 169, 169, 176, 169, 165, 159, 162, 154, 150, 134}
                },
                {
                    {14, 210, 130, 122, 135, 140, 102, 140, 111, 120, 118, 127, 119, 126, 120, 123},
                    {93, 120, 119, 212, 37, 74, 170, 126, 126, 116, 120, 125, 124, 119, 122, 123},
                    {128, 145, 125, 80, 165, 44, 204, 166, 82, 119, 131, 114, 127, 114, 122, 118},
                    {77, 96, 187, 96, 141, 91, 150, 50, 193, 111, 121, 111, 119, 115, 134, 100},
                    {126, 135, 106, 115, 129, 109, 143, 87, 136, 142, 108, 148, 71, 188, 59, 200},
                    {90, 56, 195, 119, 117, 130, 105, 174, 93, 89, 145, 110, 134, 126, 88, 179},
                    {98, 78, 151, 116, 115, 132, 130, 126, 66, 184, 85, 164, 61, 143, 144, 72},
                    {116, 126, 133, 123, 122, 135, 138, 76, 85, 195, 103, 119, 174, 52, 123, 180},
                    {116, 120, 123, 125, 116, 131, 128, 98, 93, 156, 140, 54, 172, 181, 58, 67},
                    {69, 73, 42, 78, 100, 167, 172, 108, 112, 95, 149, 113, 113, 108, 127, 128},
                    {89, 77, 72, 125, 151, 74, 85, 147, 150, 129, 58, 150, 160, 104, 74, 106},
                    {98, 98, 89, 139, 144, 68, 73, 118, 118, 161, 157, 58, 92, 140, 180, 154},
                    {123, 124, 129, 87, 88, 144, 147, 169, 164, 144, 51, 83, 153, 169, 171, 149},
                    {129, 135, 128, 95, 86, 124, 116, 161, 164, 164, 129, 74, 59, 58, 68, 109},
                    {126, 119, 122, 170, 169, 147, 142, 105, 95, 74, 49, 55, 81, 101, 115, 124},
                    {116, 106, 120, 183, 189, 169, 170, 164, 172, 168, 159, 135, 128, 127, 123, 120}
                },
                {
                    {107, 139, 117, 134, 106, 131, 126, 120, 114, 135, 108, 145, 77, 203, 42, 183},
                    {153, 81, 168, 72, 160, 100, 138, 82, 150, 104, 155, 79, 160, 120, 78, 169},
                    {71, 176, 93, 152, 117, 112, 126, 157, 95, 117, 138, 79, 187, 82, 100, 178},
                    {195, 77, 78, 192, 62, 137, 130, 125, 133, 96, 150, 97, 136, 129, 111, 127},
                    {142, 92, 171, 124, 74, 144, 143, 112, 79, 187, 69, 149, 145, 74, 122, 160},
                    {157, 96, 133, 105, 152, 101, 118, 204, 38, 142, 140, 99, 125, 154, 106, 93},
                    {73, 118, 208, 126, 56, 156, 97, 135, 123, 87, 152, 102, 136, 145, 112, 86},
                    {111, 131, 107, 139, 122, 110, 129, 79, 138, 191, 89, 71, 167, 156, 87, 52},
                    {95, 123, 138, 145, 104, 62, 227, 97, 94, 118, 157, 119, 88, 124, 140, 111},
                    {116, 98, 153, 172, 130, 33, 96, 158, 162, 106, 87, 166, 136, 105, 89, 112},
                    {124, 124, 149, 155, 119, 83, 61, 109, 129, 175, 145, 71, 83, 140, 183, 167},
                    {98, 96, 109, 150, 144, 131, 78, 72, 81, 146, 191, 158, 103, 75, 73, 100},
                    {48, 23, 97, 141, 153, 148, 133, 130, 119, 111, 89, 99, 120, 137, 142, 139},
                    {112, 110, 108, 88, 93, 124, 134, 171, 171, 154, 125, 76, 53, 62, 79, 111},
                    {143, 147, 150, 159, 154, 135, 114, 91, 81, 66, 60, 67, 75, 91, 100, 111},
                    {109, 104, 77, 59, 57, 62, 82, 88, 79, 89, 96, 112, 122, 123, 121, 122}
                },
                {
                    {120, 123, 124, 123, 122, 124, 123, 117, 130, 111, 134, 127, 88, 198, 26, 187},
                    {121, 122, 125, 121, 123, 122, 119, 126, 115, 129, 135, 64, 217, 69, 86, 186},
                    {112, 131, 119, 125, 123, 123, 128, 113, 127, 143, 46, 212, 116, 69, 115, 179},
                    {118, 122, 126, 117, 125, 123, 124, 124, 118, 70, 203, 133, 67, 75, 165, 187},
                    {122, 128, 114, 128, 125, 119, 127, 138, 56, 212, 110, 79, 83, 143, 164, 168},
                    {104, 129, 128, 122, 111, 148, 106, 94, 201, 100, 75, 79, 127, 163, 186, 167},
                    {116, 126, 119, 129, 98, 148, 149, 14, 150, 176, 154, 110, 99, 90, 99, 105},
                    {213, 37, 174, 94, 134, 104, 142, 100, 125, 125, 109, 124, 123, 128, 133, 136},
                    {86, 142, 125, 117, 151, 48, 224, 96, 112, 100, 116, 120, 141, 143, 139, 127},
                    {71, 125, 182, 57, 186, 92, 82, 130, 155, 154, 125, 108, 98, 102, 106, 111},
                    {160, 121, 70, 162, 129, 57, 126, 157, 187, 136, 109, 85, 80, 85, 101, 118},
                    {162, 150, 67, 125, 203, 102, 70, 72, 116, 128, 144, 150, 152, 148, 141, 132},
                    {154, 150, 77, 47, 158, 185, 167, 131, 113, 97, 91, 91, 97, 102, 106, 115},
                    {76, 49, 108, 181, 182, 147, 115, 99, 90, 88, 90, 94, 101, 105, 110, 115},
                    {164, 199, 192, 163, 128, 112, 103, 95, 89, 86, 89, 92, 99, 104, 110, 115},
                    {107, 105, 83, 66, 62, 63, 74, 80, 81, 88, 93, 102, 108, 114, 116, 122}
                },
                {
                    {4, 204, 125, 129, 126, 131, 118, 121, 121, 125, 122, 121, 121, 126, 121, 121},
                    {112, 109, 141, 109, 198, 58, 55, 193, 110, 141, 117, 125, 118, 124, 118, 126},
                    {146, 149, 75, 132, 77, 191, 37, 178, 130, 116, 125, 124, 122, 127, 122, 126},
                    {89, 67, 219, 111, 99, 167, 87, 121, 127, 107, 136, 107, 137, 107, 151, 83},
                    {111, 101, 167, 115, 105, 142, 117, 119, 123, 127, 107, 139, 106, 168, 46, 216},
                    {156, 175, 150, 89, 163, 124, 108, 100, 202, 54, 147, 112, 132, 112, 126, 146},
                    {130, 135, 128, 131, 116, 121, 128, 131, 75, 141, 172, 58, 181, 67, 135, 190},
                    {98, 91, 95, 132, 111, 102, 141, 154, 117, 60, 208, 86, 101, 165, 87, 101},
                    {116, 126, 138, 117, 64, 86, 172, 207, 166, 101, 76, 123, 156, 98, 117, 124},
                    {126, 125, 112, 92, 119, 118, 108, 101, 163, 182, 112, 54, 169, 145, 57, 75},
                    {69, 50, 60, 136, 131, 115, 94, 91, 172, 102, 102, 123, 140, 74, 122, 155},
                    {139, 135, 148, 240, 139, 115, 109, 111, 121, 101, 116, 140, 162, 113, 76, 98},
                    {120, 124, 109, 64, 144, 151, 130, 125, 65, 78, 111, 160, 152, 69, 55, 93},
                    {118, 110, 107, 124, 162, 154, 137, 131, 94, 77, 65, 87, 179, 195, 160, 132},
                    {125, 117, 127, 148, 172, 178, 158, 155, 134, 128, 93, 60, 50, 80, 103, 120},
                    {130, 139, 130, 119, 77, 70, 78, 85, 82, 72, 68, 64, 79, 109, 115, 120}
                },
                {
                    {242, 45, 134, 104, 130, 116, 124, 123, 119, 124, 119, 121, 122, 123, 119, 124},
                    {122, 119, 124, 121, 122, 121, 119, 135, 108, 120, 157, 65, 192, 41, 184, 93},
                    {120, 120, 124, 121, 122, 123, 120, 129, 129, 80, 185, 61, 159, 146, 48, 184},
                    {122, 129, 119, 125, 122, 115, 139, 108, 102, 188, 53, 133, 181, 82, 82, 185},
                    {121, 113, 129, 121, 116, 145, 94, 106, 207, 51, 99, 172, 140, 71, 126, 151},
                    {122, 130, 117, 111, 148, 100, 103, 208, 59, 92, 141, 176, 99, 85, 124, 164},
                    {118, 118, 120, 138, 120, 52, 225, 79, 123, 96, 151, 145, 106, 96, 129, 134},
                    {115, 127, 128, 102, 172, 48, 124, 165, 152, 98, 78, 118, 172, 152, 95, 66},
                    {135, 114, 109, 198, 37, 135, 140, 159, 97, 92, 105, 143, 155, 134, 97, 85},
                    {114, 121, 136, 77, 136, 163, 128, 84, 87, 122, 160, 175, 146, 94, 56, 59},
                    {49, 19, 184, 115, 102, 113, 120, 138, 119, 134, 117, 118, 112, 119, 120, 126},
                    {130, 156, 186, 59, 94, 157, 170, 130, 95, 83, 91, 119, 148, 161, 161, 142},
                    {94, 68, 34, 116, 164, 160, 144, 107, 92, 89, 103, 129, 153, 157, 151, 134},
                    {118, 118, 71, 65, 90, 145, 168, 170, 152, 124, 96, 80, 74, 79, 88, 106},
                    {126, 127, 165, 184, 192, 165, 146, 118, 99, 87, 80, 81, 87, 94, 100, 109},
                    {125, 122, 136, 148, 159, 171, 177, 181, 179, 174, 165, 156, 147, 139, 134, 127}
                }
            };
    }

    /**
     * Returns the ROT_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #ROT_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][][] makeROT_UV() {
        return new int[][][] {
                {
                    {228, 33, 195, 127, 142, 128, 150, 128, 146, 130, 147, 131, 148, 132, 148, 133},
                    {187, 134, 106, 141, 138, 164, 120, 140, 135, 156, 118, 153, 107, 212, 63, 215},
                    {79, 135, 208, 131, 144, 79, 200, 137, 144, 107, 177, 137, 117, 166, 97, 183},
                    {132, 127, 152, 158, 119, 106, 188, 128, 91, 221, 79, 184, 97, 161, 154, 101},
                    {186, 156, 83, 137, 167, 69, 179, 208, 80, 136, 160, 117, 169, 125, 137, 150},
                    {157, 154, 138, 103, 188, 120, 114, 176, 163, 90, 144, 177, 76, 196, 154, 71},
                    {158, 135, 73, 187, 97, 111, 200, 98, 209, 89, 123, 152, 143, 147, 135, 120},
                    {141, 151, 148, 99, 193, 104, 135, 141, 188, 141, 60, 194, 168, 89, 140, 186},
                    {147, 136, 150, 172, 87, 160, 138, 198, 125, 100, 146, 195, 87, 104, 195, 198},
                    {179, 184, 128, 85, 164, 162, 186, 58, 118, 145, 182, 149, 105, 129, 186, 170},
                    {164, 172, 168, 129, 117, 181, 165, 139, 98, 103, 146, 209, 182, 113, 66, 95},
                    {174, 196, 186, 103, 93, 166, 184, 187, 178, 149, 100, 87, 160, 176, 163, 139},
                    {193, 207, 190, 208, 136, 83, 87, 115, 159, 165, 168, 147, 139, 133, 136, 140},
                    {132, 142, 159, 189, 181, 154, 141, 114, 91, 88, 103, 148, 199, 208, 192, 158},
                    {151, 151, 156, 197, 206, 181, 175, 146, 134, 120, 100, 85, 79, 91, 110, 129},
                    {144, 150, 141, 102, 87, 95, 95, 99, 85, 79, 80, 94, 110, 121, 128, 137}
                },
                {
                    {145, 144, 140, 142, 132, 140, 145, 134, 131, 159, 74, 207, 135, 153, 65, 229},
                    {150, 131, 144, 138, 133, 145, 134, 159, 61, 255, 122, 92, 148, 151, 158, 144},
                    {155, 138, 141, 149, 141, 137, 146, 137, 136, 122, 227, 73, 127, 140, 123, 240},
                    {150, 132, 165, 105, 172, 114, 165, 77, 209, 175, 97, 119, 147, 109, 204, 179},
                    {168, 38, 222, 99, 158, 113, 151, 145, 115, 129, 162, 165, 128, 147, 120, 124},
                    {131, 130, 145, 155, 130, 132, 124, 194, 90, 99, 126, 180, 158, 81, 225, 188},
                    {158, 132, 127, 125, 190, 105, 54, 222, 193, 147, 119, 118, 131, 162, 131, 148},
                    {109, 170, 147, 65, 204, 207, 83, 102, 103, 134, 162, 162, 134, 139, 145, 148},
                    {174, 89, 161, 177, 83, 230, 76, 116, 173, 139, 123, 127, 146, 135, 148, 146},
                    {23, 137, 199, 177, 121, 105, 98, 127, 156, 165, 155, 139, 144, 130, 127, 144},
                    {145, 173, 187, 168, 140, 143, 148, 138, 133, 120, 128, 150, 89, 250, 197, 151},
                    {162, 104, 87, 213, 192, 99, 99, 74, 114, 152, 159, 173, 115, 138, 151, 134},
                    {209, 218, 213, 167, 148, 113, 110, 125, 128, 138, 150, 145, 188, 110, 117, 122},
                    {115, 113, 147, 187, 215, 174, 174, 147, 130, 110, 109, 106, 226, 167, 128, 143},
                    {139, 153, 169, 187, 193, 191, 180, 179, 159, 165, 142, 146, 59, 81, 127, 128},
                    {145, 139, 128, 143, 140, 161, 161, 168, 187, 198, 214, 215, 193, 166, 161, 144}
                },
                {
                    {119, 154, 127, 146, 135, 146, 138, 144, 136, 142, 126, 164, 101, 214, 45, 215},
                    {122, 149, 128, 149, 128, 148, 131, 152, 118, 172, 93, 210, 62, 176, 183, 74},
                    {69, 190, 101, 164, 125, 158, 117, 176, 105, 180, 109, 151, 177, 74, 149, 181},
                    {208, 95, 176, 120, 163, 99, 162, 163, 85, 181, 88, 178, 149, 108, 131, 179},
                    {134, 149, 127, 139, 165, 110, 132, 195, 70, 182, 166, 69, 159, 196, 124, 93},
                    {141, 132, 154, 120, 159, 145, 93, 212, 112, 82, 204, 155, 82, 119, 170, 178},
                    {146, 155, 89, 172, 133, 102, 225, 100, 106, 154, 192, 131, 94, 131, 177, 177},
                    {198, 99, 129, 203, 48, 195, 115, 158, 125, 154, 141, 112, 139, 152, 151, 155},
                    {162, 143, 85, 196, 135, 81, 157, 193, 157, 69, 125, 181, 177, 140, 116, 109},
                    {119, 126, 158, 119, 106, 189, 185, 127, 72, 115, 183, 184, 158, 111, 80, 91},
                    {71, 113, 228, 172, 85, 81, 170, 168, 159, 143, 139, 125, 129, 140, 144, 143},
                    {143, 174, 145, 83, 114, 177, 208, 179, 129, 91, 90, 117, 161, 182, 187, 162},
                    {81, 28, 95, 149, 179, 160, 150, 131, 121, 112, 104, 116, 134, 144, 151, 148},
                    {130, 151, 183, 192, 170, 143, 112, 91, 93, 114, 153, 179, 200, 196, 187, 163},
                    {155, 174, 185, 214, 204, 188, 171, 150, 136, 119, 107, 100, 99, 108, 115, 124},
                    {141, 163, 138, 126, 100, 92, 93, 85, 76, 79, 88, 96, 107, 118, 125, 133}
                },
                {
                    {211, 56, 174, 115, 151, 125, 146, 127, 157, 121, 160, 109, 192, 86, 139, 173},
                    {82, 193, 122, 149, 126, 160, 128, 139, 142, 137, 140, 127, 206, 39, 170, 158},
                    {181, 119, 142, 142, 135, 134, 152, 147, 88, 208, 82, 195, 122, 95, 201, 108},
                    {137, 145, 129, 151, 142, 121, 166, 172, 48, 197, 145, 103, 179, 155, 73, 181},
                    {148, 140, 145, 109, 178, 146, 62, 247, 109, 104, 159, 151, 126, 127, 148, 136},
                    {155, 123, 159, 148, 99, 241, 66, 124, 151, 176, 103, 134, 160, 159, 107, 147},
                    {81, 136, 190, 69, 197, 109, 126, 122, 166, 169, 80, 163, 140, 133, 104, 164},
                    {109, 133, 176, 94, 157, 159, 130, 111, 107, 178, 187, 59, 130, 156, 199, 93},
                    {95, 109, 197, 141, 88, 165, 159, 134, 101, 122, 184, 179, 78, 114, 140, 200},
                    {178, 171, 80, 123, 190, 158, 117, 101, 145, 169, 159, 131, 87, 131, 162, 225},
                    {160, 158, 120, 88, 170, 185, 158, 92, 96, 112, 181, 197, 158, 119, 87, 87},
                    {175, 190, 154, 65, 102, 179, 207, 178, 145, 111, 93, 108, 133, 149, 153, 160},
                    {85, 54, 59, 128, 160, 184, 170, 154, 130, 117, 101, 116, 122, 130, 140, 134},
                    {132, 140, 156, 158, 171, 148, 131, 105, 84, 79, 106, 163, 194, 200, 200, 178},
                    {155, 161, 192, 211, 207, 172, 161, 134, 127, 111, 105, 96, 96, 100, 112, 121},
                    {130, 134, 158, 164, 190, 192, 196, 188, 191, 186, 184, 180, 174, 167, 162, 150}
                },
                {
                    {133, 141, 140, 137, 143, 136, 139, 141, 135, 135, 145, 132, 138, 201, 27, 221},
                    {137, 140, 139, 138, 140, 140, 137, 140, 138, 140, 141, 113, 225, 40, 138, 212},
                    {129, 147, 139, 136, 145, 137, 140, 145, 125, 149, 116, 216, 55, 107, 177, 222},
                    {135, 146, 136, 145, 133, 139, 142, 134, 152, 150, 54, 228, 181, 117, 81, 94},
                    {144, 129, 145, 133, 139, 138, 140, 135, 154, 72, 233, 185, 112, 83, 89, 108},
                    {144, 139, 135, 140, 148, 125, 132, 182, 45, 229, 171, 137, 125, 109, 104, 110},
                    {127, 156, 122, 146, 134, 140, 178, 32, 181, 207, 150, 116, 105, 111, 116, 133},
                    {203, 57, 201, 104, 175, 94, 192, 113, 126, 142, 128, 147, 146, 144, 143, 145},
                    {175, 68, 172, 167, 77, 202, 82, 152, 168, 161, 121, 118, 107, 118, 121, 141},
                    {145, 133, 141, 160, 94, 210, 168, 81, 64, 126, 166, 180, 179, 171, 162, 151},
                    {174, 121, 71, 206, 89, 105, 218, 174, 137, 118, 124, 123, 123, 128, 133, 143},
                    {190, 120, 89, 207, 188, 102, 64, 102, 144, 150, 161, 168, 160, 157, 153, 150},
                    {186, 136, 70, 93, 206, 216, 146, 136, 121, 115, 107, 115, 117, 121, 124, 133},
                    {40, 70, 142, 193, 190, 146, 144, 126, 111, 112, 117, 118, 126, 127, 132, 129},
                    {181, 212, 211, 203, 163, 140, 136, 121, 105, 102, 108, 107, 113, 118, 124, 132},
                    {133, 131, 111, 89, 81, 79, 87, 88, 84, 95, 104, 113, 120, 126, 131, 133}
                },
                {
                    {141, 162, 101, 211, 75, 154, 130, 175, 99, 177, 112, 176, 102, 175, 105, 180},
                    {130, 137, 164, 80, 189, 135, 143, 124, 152, 129, 133, 162, 102, 195, 63, 212},
                    {134, 161, 103, 175, 123, 141, 132, 145, 167, 88, 203, 66, 187, 136, 103, 198},
                    {149, 114, 160, 174, 78, 161, 170, 50, 213, 114, 134, 156, 116, 167, 137, 124},
                    {173, 83, 164, 141, 124, 114, 221, 112, 81, 192, 143, 100, 172, 132, 125, 172},
                    {168, 63, 203, 134, 104, 180, 75, 182, 147, 121, 133, 148, 148, 104, 143, 182},
                    {183, 90, 131, 154, 147, 98, 158, 189, 126, 91, 199, 135, 75, 188, 151, 104},
                    {93, 172, 211, 121, 100, 167, 112, 133, 107, 175, 192, 85, 113, 178, 132, 104},
                    {175, 97, 104, 143, 161, 156, 92, 141, 162, 181, 109, 101, 178, 188, 79, 77},
                    {102, 148, 196, 164, 113, 47, 149, 178, 162, 120, 107, 154, 173, 130, 85, 109},
                    {148, 132, 145, 178, 167, 61, 68, 94, 162, 199, 167, 136, 115, 132, 170, 174},
                    {114, 134, 145, 140, 140, 131, 156, 175, 176, 140, 65, 68, 126, 193, 208, 173},
                    {36, 67, 127, 185, 176, 155, 133, 118, 101, 117, 145, 158, 158, 159, 146, 137},
                    {185, 174, 172, 151, 144, 130, 113, 112, 91, 97, 132, 170, 211, 213, 177, 148},
                    {165, 159, 166, 187, 178, 148, 124, 104, 91, 89, 85, 86, 87, 99, 110, 128},
                    {156, 161, 194, 210, 210, 188, 181, 176, 181, 174, 165, 157, 151, 143, 140, 139}
                },
                {
                    {0, 180, 129, 151, 131, 151, 134, 148, 136, 145, 131, 140, 130, 146, 122, 173},
                    {172, 126, 143, 134, 146, 125, 146, 129, 144, 143, 136, 154, 108, 192, 56, 242},
                    {144, 136, 139, 138, 137, 142, 136, 138, 141, 133, 163, 90, 240, 60, 113, 195},
                    {145, 139, 143, 133, 148, 130, 141, 145, 120, 163, 56, 240, 154, 80, 158, 168},
                    {131, 140, 134, 144, 130, 145, 155, 91, 249, 57, 113, 164, 152, 137, 130, 129},
                    {136, 144, 141, 140, 139, 145, 143, 134, 121, 163, 80, 136, 217, 198, 68, 75},
                    {131, 144, 135, 138, 141, 129, 222, 32, 133, 199, 136, 117, 134, 134, 161, 145},
                    {133, 140, 137, 142, 135, 151, 135, 109, 124, 146, 214, 202, 117, 86, 59, 91},
                    {127, 141, 134, 136, 161, 76, 233, 176, 90, 67, 157, 155, 154, 146, 138, 133},
                    {126, 138, 149, 126, 195, 44, 131, 169, 202, 190, 131, 114, 119, 110, 114, 116},
                    {119, 132, 165, 100, 224, 121, 80, 83, 117, 117, 172, 167, 176, 176, 171, 149},
                    {126, 125, 202, 26, 144, 189, 169, 155, 140, 130, 119, 116, 115, 119, 122, 132},
                    {154, 160, 44, 132, 218, 180, 136, 132, 119, 115, 104, 106, 108, 113, 119, 133},
                    {110, 30, 171, 192, 147, 133, 122, 115, 107, 110, 102, 108, 112, 116, 124, 131},
                    {163, 228, 227, 195, 169, 146, 135, 126, 121, 115, 118, 117, 118, 120, 126, 137},
                    {145, 172, 117, 90, 68, 69, 88, 96, 100, 105, 111, 114, 119, 124, 129, 134}
                },
                {
                    {139, 138, 139, 140, 135, 139, 139, 143, 137, 131, 164, 91, 222, 45, 200, 111},
                    {144, 136, 138, 145, 135, 137, 141, 142, 117, 188, 78, 202, 95, 119, 217, 74},
                    {131, 147, 125, 154, 139, 124, 158, 145, 90, 212, 77, 153, 189, 105, 95, 208},
                    {130, 146, 91, 187, 136, 75, 228, 95, 125, 158, 165, 100, 128, 168, 148, 112},
                    {140, 138, 139, 145, 122, 149, 177, 58, 227, 103, 101, 188, 151, 102, 134, 163},
                    {145, 140, 120, 177, 65, 231, 115, 102, 144, 183, 129, 96, 152, 173, 137, 120},
                    {108, 174, 63, 217, 111, 138, 109, 189, 139, 88, 147, 177, 127, 117, 143, 156},
                    {136, 149, 108, 146, 185, 102, 96, 157, 199, 146, 74, 113, 189, 178, 114, 83},
                    {94, 186, 120, 126, 187, 140, 98, 107, 172, 183, 148, 99, 94, 133, 195, 199},
                    {241, 63, 104, 170, 156, 126, 118, 144, 152, 141, 133, 123, 125, 141, 172, 186},
                    {112, 134, 199, 153, 82, 110, 165, 189, 160, 113, 86, 99, 142, 176, 192, 181},
                    {181, 199, 84, 78, 154, 181, 181, 145, 108, 95, 108, 138, 165, 176, 176, 156},
                    {68, 63, 133, 159, 188, 169, 133, 105, 97, 107, 131, 159, 178, 179, 171, 150},
                    {95, 75, 78, 82, 126, 163, 174, 179, 161, 141, 116, 101, 94, 101, 113, 130},
                    {153, 160, 180, 203, 207, 193, 173, 149, 126, 110, 99, 94, 99, 105, 115, 126},
                    {141, 142, 134, 119, 105, 94, 83, 75, 78, 85, 92, 100, 108, 116, 122, 132}
                },
                {
                    {150, 125, 145, 135, 142, 134, 141, 143, 136, 131, 158, 103, 201, 52, 221, 84},
                    {119, 171, 117, 153, 132, 148, 130, 144, 131, 172, 80, 211, 81, 140, 200, 76},
                    {74, 224, 90, 166, 131, 151, 124, 153, 124, 162, 122, 131, 188, 97, 122, 190},
                    {183, 88, 161, 127, 141, 140, 149, 120, 129, 201, 61, 177, 170, 91, 122, 191},
                    {147, 133, 146, 125, 127, 217, 56, 135, 209, 85, 132, 169, 138, 114, 143, 159},
                    {170, 124, 125, 135, 145, 182, 65, 197, 89, 191, 127, 91, 160, 181, 126, 106},
                    {130, 138, 163, 103, 198, 96, 111, 223, 97, 109, 159, 183, 113, 107, 148, 176},
                    {123, 160, 137, 99, 213, 95, 125, 129, 195, 134, 94, 143, 189, 158, 95, 82},
                    {84, 161, 213, 70, 162, 174, 133, 96, 134, 194, 155, 107, 113, 142, 166, 154},
                    {89, 130, 218, 148, 77, 157, 155, 170, 111, 117, 131, 175, 177, 134, 89, 90},
                    {104, 117, 180, 179, 112, 87, 119, 183, 194, 142, 88, 92, 136, 171, 187, 174},
                    {112, 104, 143, 197, 147, 100, 82, 114, 160, 193, 191, 146, 103, 89, 94, 115},
                    {192, 194, 146, 95, 86, 124, 159, 190, 188, 168, 140, 110, 95, 95, 104, 123},
                    {71, 63, 74, 97, 138, 168, 176, 169, 150, 133, 118, 110, 107, 114, 120, 130},
                    {153, 158, 177, 208, 208, 188, 171, 148, 131, 111, 98, 93, 97, 105, 116, 128},
                    {138, 132, 144, 166, 176, 186, 190, 196, 200, 195, 191, 181, 172, 162, 155, 145}
                },
                {
                    {140, 137, 140, 141, 138, 135, 141, 142, 141, 134, 147, 124, 210, 68, 75, 233},
                    {149, 133, 140, 137, 146, 135, 138, 134, 164, 102, 224, 47, 115, 198, 113, 159},
                    {151, 135, 142, 138, 143, 131, 161, 98, 175, 140, 100, 201, 84, 199, 68, 190},
                    {133, 140, 140, 138, 125, 151, 120, 124, 239, 42, 136, 167, 155, 108, 162, 119},
                    {150, 139, 122, 163, 117, 143, 207, 31, 163, 179, 142, 106, 143, 112, 178, 140},
                    {136, 136, 124, 153, 167, 70, 185, 156, 113, 107, 199, 184, 78, 89, 166, 163},
                    {157, 138, 152, 146, 71, 235, 111, 138, 114, 141, 180, 164, 85, 114, 154, 176},
                    {136, 146, 100, 228, 66, 133, 186, 170, 115, 105, 137, 147, 168, 168, 106, 112},
                    {251, 69, 191, 123, 127, 125, 165, 147, 125, 120, 134, 149, 166, 148, 153, 132},
                    {90, 146, 175, 68, 135, 167, 194, 104, 95, 107, 178, 170, 171, 146, 102, 98},
                    {96, 157, 204, 115, 91, 123, 191, 182, 149, 126, 88, 101, 119, 151, 183, 188},
                    {190, 207, 66, 97, 166, 173, 167, 145, 112, 98, 114, 136, 154, 163, 173, 172},
                    {190, 217, 145, 98, 89, 105, 145, 163, 176, 177, 156, 134, 118, 104, 95, 96},
                    {134, 80, 96, 117, 156, 179, 179, 168, 144, 125, 94, 92, 85, 88, 95, 106},
                    {162, 199, 211, 201, 190, 157, 134, 112, 105, 99, 109, 114, 114, 113, 115, 120},
                    {135, 127, 116, 108, 84, 80, 76, 82, 81, 95, 104, 114, 119, 129, 132, 139}
                },
                {
                    {137, 143, 139, 137, 137, 131, 154, 152, 128, 156, 96, 199, 59, 227, 86, 144},
                    {140, 135, 149, 130, 155, 122, 127, 183, 99, 187, 61, 198, 142, 81, 198, 127},
                    {140, 139, 139, 135, 142, 127, 161, 160, 85, 191, 114, 103, 213, 142, 57, 188},
                    {140, 141, 138, 140, 147, 167, 85, 112, 230, 117, 69, 171, 179, 126, 101, 169},
                    {145, 129, 159, 125, 148, 193, 46, 149, 128, 198, 135, 85, 134, 186, 150, 94},
                    {137, 138, 142, 157, 166, 111, 66, 230, 119, 78, 181, 162, 138, 131, 108, 158},
                    {127, 149, 124, 169, 163, 42, 149, 171, 195, 148, 104, 83, 153, 179, 163, 110},
                    {150, 122, 152, 141, 83, 186, 177, 190, 124, 70, 93, 132, 185, 172, 147, 85},
                    {141, 140, 153, 183, 49, 152, 134, 183, 166, 165, 132, 105, 96, 124, 163, 215},
                    {145, 112, 195, 166, 76, 94, 127, 127, 161, 176, 172, 178, 154, 112, 90, 71},
                    {166, 96, 219, 44, 161, 125, 164, 165, 173, 129, 137, 111, 114, 127, 133, 159},
                    {167, 56, 183, 195, 159, 120, 126, 94, 108, 121, 133, 152, 163, 183, 178, 183},
                    {82, 209, 194, 95, 102, 111, 117, 121, 125, 130, 151, 165, 181, 185, 178, 167},
                    {50, 147, 202, 186, 171, 156, 147, 122, 117, 112, 105, 101, 101, 104, 113, 124},
                    {240, 223, 179, 162, 144, 124, 128, 113, 114, 115, 114, 117, 122, 126, 130, 132},
                    {150, 173, 187, 187, 195, 191, 186, 184, 183, 179, 177, 172, 167, 160, 153, 143}
                },
                {
                    {140, 139, 136, 140, 137, 141, 131, 154, 127, 132, 184, 66, 226, 62, 180, 125},
                    {131, 146, 135, 139, 139, 140, 145, 136, 110, 210, 54, 190, 149, 84, 196, 109},
                    {124, 144, 140, 138, 138, 143, 137, 165, 65, 218, 126, 94, 169, 174, 72, 182},
                    {129, 146, 141, 135, 138, 140, 129, 186, 71, 152, 195, 125, 72, 169, 190, 71},
                    {168, 121, 142, 134, 141, 141, 185, 50, 170, 192, 137, 75, 135, 183, 160, 96},
                    {143, 130, 136, 143, 154, 76, 233, 111, 93, 138, 183, 164, 114, 96, 141, 187},
                    {194, 103, 148, 100, 222, 65, 133, 183, 143, 129, 112, 138, 165, 160, 129, 107},
                    {74, 190, 104, 186, 119, 81, 182, 154, 151, 113, 115, 133, 174, 169, 118, 83},
                    {72, 202, 131, 97, 207, 112, 106, 123, 174, 163, 144, 109, 108, 134, 170, 181},
                    {176, 115, 73, 226, 134, 108, 112, 165, 157, 154, 122, 106, 109, 147, 181, 188},
                    {159, 153, 47, 151, 186, 160, 106, 97, 125, 157, 180, 176, 144, 114, 87, 99},
                    {176, 188, 88, 103, 149, 178, 168, 130, 92, 89, 113, 142, 174, 192, 191, 172},
                    {57, 43, 129, 159, 183, 162, 134, 114, 113, 116, 135, 151, 164, 168, 165, 147},
                    {120, 103, 78, 85, 132, 176, 190, 183, 159, 125, 102, 92, 91, 99, 111, 128},
                    {151, 173, 187, 213, 220, 199, 175, 151, 131, 119, 116, 115, 119, 120, 124, 129},
                    {142, 140, 151, 138, 121, 103, 87, 77, 76, 80, 87, 94, 104, 110, 118, 128}
                },
                {
                    {139, 134, 144, 136, 145, 133, 154, 120, 147, 161, 88, 208, 48, 213, 109, 138},
                    {140, 138, 134, 142, 141, 125, 148, 154, 89, 210, 72, 182, 150, 76, 203, 113},
                    {139, 140, 139, 141, 128, 142, 156, 124, 129, 183, 84, 130, 207, 141, 50, 209},
                    {138, 143, 136, 139, 136, 157, 124, 111, 235, 78, 83, 194, 166, 95, 152, 142},
                    {137, 142, 126, 143, 164, 127, 68, 245, 106, 96, 124, 180, 147, 146, 112, 157},
                    {142, 141, 126, 162, 123, 104, 210, 128, 98, 94, 190, 203, 122, 90, 128, 192},
                    {142, 150, 104, 137, 223, 31, 169, 142, 174, 140, 127, 115, 154, 150, 140, 129},
                    {142, 144, 179, 52, 216, 158, 104, 107, 133, 161, 171, 159, 122, 107, 143, 188},
                    {127, 166, 35, 194, 160, 169, 90, 109, 147, 173, 158, 135, 108, 121, 140, 172},
                    {151, 101, 178, 170, 111, 104, 121, 166, 176, 156, 125, 102, 101, 137, 193, 232},
                    {121, 217, 104, 85, 128, 164, 179, 155, 118, 100, 96, 107, 132, 170, 188, 182},
                    {206, 95, 138, 175, 185, 158, 128, 87, 81, 89, 109, 132, 165, 176, 173, 152},
                    {204, 68, 76, 84, 134, 168, 176, 179, 159, 141, 130, 118, 110, 110, 119, 133},
                    {70, 115, 165, 168, 181, 162, 158, 138, 116, 102, 95, 85, 86, 86, 106, 127},
                    {235, 224, 178, 170, 146, 142, 139, 150, 145, 141, 128, 118, 104, 102, 108, 126},
                    {144, 137, 119, 92, 75, 70, 75, 86, 105, 112, 117, 116, 115, 115, 119, 127}
                },
                {
                    {142, 136, 140, 140, 134, 149, 132, 143, 141, 125, 176, 83, 208, 61, 215, 97},
                    {140, 141, 141, 136, 143, 136, 139, 142, 127, 181, 67, 208, 99, 119, 215, 78},
                    {139, 141, 141, 133, 144, 145, 124, 159, 93, 211, 78, 140, 207, 92, 104, 199},
                    {137, 141, 140, 145, 134, 136, 147, 169, 49, 205, 178, 80, 111, 191, 148, 103},
                    {141, 147, 121, 136, 202, 79, 89, 239, 122, 94, 144, 159, 144, 137, 134, 131},
                    {145, 146, 97, 181, 161, 56, 226, 107, 113, 142, 162, 155, 133, 105, 151, 170},
                    {136, 138, 178, 136, 78, 176, 150, 179, 82, 102, 181, 190, 101, 92, 152, 186},
                    {137, 141, 83, 243, 65, 149, 130, 175, 149, 127, 109, 135, 153, 149, 124, 117},
                    {139, 122, 202, 110, 101, 103, 209, 162, 128, 109, 110, 150, 195, 162, 105, 83},
                    {136, 203, 78, 110, 153, 185, 148, 116, 106, 132, 167, 182, 159, 116, 81, 79},
                    {83, 255, 143, 114, 114, 125, 161, 164, 162, 136, 114, 108, 132, 152, 170, 167},
                    {113, 155, 188, 160, 117, 88, 105, 138, 182, 195, 177, 143, 105, 80, 85, 103},
                    {42, 138, 178, 194, 171, 144, 113, 98, 106, 126, 155, 177, 181, 170, 155, 139},
                    {44, 75, 98, 110, 151, 160, 175, 165, 146, 131, 116, 106, 102, 103, 118, 130},
                    {166, 170, 195, 201, 207, 188, 173, 147, 130, 113, 103, 100, 105, 108, 115, 126},
                    {137, 135, 142, 156, 170, 185, 194, 203, 201, 197, 188, 181, 169, 161, 154, 147}
                },
                {
                    {139, 140, 138, 144, 133, 151, 123, 153, 144, 100, 219, 55, 201, 94, 174, 119},
                    {138, 136, 148, 139, 129, 163, 105, 185, 66, 220, 90, 140, 177, 96, 175, 118},
                    {138, 138, 137, 140, 137, 145, 118, 175, 98, 159, 163, 98, 159, 176, 44, 219},
                    {136, 149, 128, 136, 153, 102, 210, 80, 156, 162, 98, 128, 218, 90, 120, 177},
                    {142, 134, 148, 140, 133, 181, 71, 157, 192, 90, 109, 192, 165, 81, 131, 197},
                    {132, 156, 113, 131, 193, 58, 156, 203, 95, 105, 167, 184, 126, 100, 155, 163},
                    {160, 128, 131, 127, 211, 91, 85, 166, 192, 145, 89, 100, 174, 191, 146, 115},
                    {125, 179, 12, 194, 110, 154, 133, 151, 146, 134, 123, 144, 153, 154, 134, 122},
                    {142, 153, 98, 125, 194, 154, 100, 87, 154, 204, 175, 118, 84, 108, 171, 190},
                    {135, 97, 155, 214, 96, 100, 152, 161, 155, 142, 113, 100, 111, 149, 202, 206},
                    {182, 54, 130, 208, 153, 115, 120, 119, 139, 162, 172, 168, 141, 105, 90, 99},
                    {229, 86, 97, 104, 154, 186, 173, 139, 106, 109, 132, 146, 157, 169, 178, 169},
                    {48, 96, 140, 160, 179, 157, 124, 100, 105, 125, 158, 176, 186, 189, 172, 154},
                    {81, 81, 117, 125, 181, 186, 184, 179, 153, 124, 102, 94, 96, 99, 111, 123},
                    {108, 79, 99, 55, 70, 97, 127, 152, 171, 172, 164, 156, 149, 142, 144, 142},
                    {135, 127, 124, 118, 116, 101, 86, 75, 73, 79, 90, 99, 111, 118, 126, 132}
                },
                {
                    {137, 141, 139, 137, 140, 141, 134, 141, 146, 145, 97, 221, 50, 202, 93, 164},
                    {139, 137, 141, 136, 140, 140, 146, 132, 155, 100, 202, 88, 153, 176, 51, 214},
                    {144, 141, 137, 126, 139, 172, 116, 156, 96, 205, 76, 150, 196, 101, 98, 207},
                    {133, 142, 116, 227, 120, 47, 200, 155, 133, 135, 114, 154, 157, 120, 119, 153},
                    {136, 148, 155, 83, 194, 89, 119, 227, 116, 83, 152, 175, 147, 110, 139, 150},
                    {141, 140, 134, 138, 146, 123, 152, 184, 45, 191, 157, 90, 124, 209, 134, 104},
                    {127, 144, 121, 159, 111, 181, 168, 138, 73, 130, 203, 167, 86, 84, 174, 190},
                    {135, 156, 84, 212, 128, 169, 51, 203, 159, 118, 135, 120, 133, 154, 155, 146},
                    {141, 153, 179, 131, 37, 181, 164, 184, 126, 91, 125, 161, 174, 152, 112, 100},
                    {89, 186, 178, 106, 85, 99, 137, 166, 179, 174, 124, 93, 103, 140, 181, 189},
                    {186, 46, 224, 177, 136, 134, 122, 154, 135, 132, 131, 131, 131, 154, 177, 176},
                    {193, 143, 78, 106, 143, 157, 197, 149, 143, 99, 93, 121, 153, 184, 198, 185},
                    {17, 102, 145, 152, 170, 166, 156, 128, 120, 103, 108, 137, 163, 169, 158, 148},
                    {122, 42, 81, 101, 111, 141, 159, 182, 173, 162, 139, 118, 108, 103, 110, 120},
                    {151, 170, 176, 168, 191, 187, 185, 166, 147, 121, 93, 81, 85, 94, 108, 121},
                    {145, 129, 124, 118, 105, 99, 86, 82, 80, 80, 88, 97, 106, 114, 124, 133}
                }
            };
    }

    /**
     * Returns the CMF_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CMF_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[] makeCMF_V() {
        return new int[] {0, 1713, 3997, 6053, 7053, 8520, 10041, 11474, 15913, 18784, 20226, 20994, 23598, 26703, 28912, 30415, 32760};
    }

    /**
     * Returns the CMF_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CMF_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[] makeCMF_UV() {
        return new int[] {0, 1148, 1204, 4742, 5155, 8714, 10707, 14477, 17237, 21524, 22034, 25105, 26891, 27714, 29801, 30978, 32762};
    }

    /**
     * Returns the CMF_COND_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CMF_COND_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[] makeCMF_COND_V() {
        return new int[] {0, 127, 289, 514, 599, 815, 994, 1101, 1880, 2347, 2428, 2507, 2784, 3053, 3986, 4100, 5462, 32759};
    }

    /**
     * Returns the CMF_COND_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #CMF_COND_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[] makeCMF_COND_UV() {
        return new int[] {0, 623, 645, 1444, 1588, 2310, 2832, 5736, 6721, 8146, 8234, 9190, 9531, 9708, 10051, 10359, 10794, 32759};
    }

    /**
     * Returns the MIN_QI table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #MIN_QI} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][][][] makeMIN_QI() {
        return new int[][][][] {
                {
                    {
                        {-3, -4, -3, -4, -4, -4, -4, -4, -5, -6, -6, -7, -7, -8, -13, -18},
                        {-2, -2, -2, -3, -2, -3, -3, -3, -4, -4, -5, -5, -4, -6, -10, -9},
                        {-2, -2, -3, -3, -3, -3, -3, -4, -4, -4, -4, -4, -6, -7, -8, -10},
                        {-3, -3, -3, -3, -3, -4, -4, -5, -5, -5, -5, -7, -9, -9, -11, -12},
                        {-2, -2, -2, -2, -2, -2, -3, -2, -3, -4, -3, -3, -5, -6, -6, -8},
                        {-3, -3, -3, -3, -4, -3, -3, -5, -5, -5, -7, -8, -8, -10, -13, -14},
                        {-2, -1, -2, -1, -2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -5, -6},
                        {-2, -3, -3, -3, -3, -3, -3, -4, -4, -4, -6, -5, -7, -8, -9, -14},
                        {-3, -3, -3, -3, -4, -3, -4, -4, -4, -4, -6, -6, -7, -8, -10, -11},
                        {-2, -2, -2, -3, -3, -3, -3, -3, -3, -4, -4, -5, -7, -7, -9, -14},
                        {-2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -3, -5, -4, -8},
                        {-2, -2, -2, -3, -3, -3, -3, -3, -4, -3, -4, -6, -6, -7, -9, -18},
                        {-2, -2, -3, -3, -2, -3, -3, -3, -3, -6, -5, -5, -5, -9, -5, -18},
                        {-2, -2, -3, -3, -3, -3, -3, -3, -3, -3, -3, -5, -7, -8, -12, -12},
                        {-2, -2, -2, -3, -3, -3, -4, -3, -4, -4, -4, -4, -6, -8, -8, -17},
                        {-2, -2, -2, -2, -3, -3, -3, -3, -3, -4, -3, -4, -5, -7, -8, -12},
                        {-2, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -4, -5, -5, -6, -10}
                    },
                    {
                        {-2, -2, -2, -2, -2, -2, -2, -2, -3, -4, -3, -4, -4, -5, -7, -10},
                        {-1, -1, -1, -1, -1, -2, -1, -2, -3, -2, -3, -3, -2, -4, -5, -5},
                        {-1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -4, -4, -6},
                        {-2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -3, -4, -5, -5, -7, -7},
                        {-1, -1, -2, -1, -1, -1, -2, -1, -2, -2, -2, -2, -3, -4, -4, -4},
                        {-2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -4, -4, -4, -5, -7, -8},
                        {-2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, -2, -2, -3, -3},
                        {-1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -3, -4, -5, -5, -8},
                        {-1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -4, -4, -5, -6, -7},
                        {-1, -1, -1, -2, -2, -2, -2, -2, -2, -2, -3, -3, -4, -4, -5, -8},
                        {-1, -1, -1, -1, -1, -1, -1, -2, -2, -2, -1, -2, -2, -3, -2, -5},
                        {-1, -1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -4, -4, -5, -10},
                        {-1, -1, -1, -1, -1, -2, -2, -2, -2, -3, -3, -3, -3, -5, -3, -10},
                        {-1, -1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -3, -5, -6, -7},
                        {-1, -1, -1, -2, -2, -2, -2, -2, -2, -3, -2, -2, -4, -5, -5, -10},
                        {-1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -2, -2, -3, -4, -5, -7},
                        {-1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -7}
                    }
                },
                {
                    {
                        {-4, -3, -3, -3, -3, -4, -5, -4, -4, -6, -7, -7, -8, -6, -10, -14},
                        {-2, -4, -3, -3, -4, -4, -4, -5, -5, -5, -5, -7, -8, -9, -10, -14},
                        {-2, -3, -3, -3, -5, -4, -4, -4, -4, -5, -7, -8, -7, -9, -9, -13},
                        {-2, -3, -2, -3, -4, -3, -3, -3, -5, -6, -5, -8, -6, -12, -9, -12},
                        {-3, -3, -3, -3, -3, -3, -4, -4, -5, -4, -6, -6, -6, -7, -10, -15},
                        {-2, -2, -3, -4, -5, -3, -4, -4, -5, -5, -5, -6, -6, -7, -9, -14},
                        {-2, -2, -3, -3, -3, -5, -4, -5, -5, -5, -7, -5, -7, -10, -9, -14},
                        {-2, -3, -4, -3, -4, -3, -4, -4, -5, -5, -5, -4, -6, -9, -10, -11},
                        {-2, -2, -3, -4, -4, -5, -3, -3, -5, -4, -4, -6, -7, -9, -10, -16},
                        {-1, -2, -3, -3, -3, -4, -4, -4, -5, -5, -7, -6, -8, -10, -10, -11},
                        {-2, -3, -3, -3, -3, -3, -4, -4, -5, -5, -5, -5, -6, -8, -8, -9},
                        {-1, -4, -2, -2, -3, -3, -4, -4, -5, -5, -5, -6, -7, -9, -10, -13},
                        {-3, -3, -3, -5, -4, -4, -4, -6, -5, -5, -6, -8, -9, -10, -13, -17},
                        {-2, -2, -2, -2, -2, -3, -3, -4, -3, -3, -4, -4, -4, -8, -6, -17},
                        {-1, -3, -2, -2, -3, -4, -4, -4, -5, -5, -7, -6, -6, -6, -11, -16},
                        {-3, -2, -2, -3, -4, -4, -3, -3, -4, -4, -6, -5, -6, -7, -10, -9},
                        {-2, -3, -4, -3, -4, -4, -4, -4, -4, -4, -4, -5, -5, -5, -7, -10}
                    },
                    {
                        {-3, -2, -2, -2, -2, -2, -3, -3, -2, -3, -4, -4, -4, -4, -6, -8},
                        {-1, -2, -2, -2, -2, -2, -2, -3, -3, -3, -3, -4, -5, -5, -6, -8},
                        {-1, -2, -2, -2, -3, -2, -2, -2, -2, -3, -4, -5, -4, -5, -5, -8},
                        {-1, -2, -1, -2, -2, -2, -2, -2, -3, -4, -3, -5, -3, -7, -5, -7},
                        {-2, -2, -2, -2, -2, -2, -2, -2, -3, -2, -3, -4, -3, -4, -6, -9},
                        {-2, -1, -2, -2, -3, -2, -2, -2, -3, -3, -3, -3, -3, -4, -5, -8},
                        {-1, -1, -2, -2, -2, -3, -2, -3, -3, -3, -4, -3, -4, -5, -5, -8},
                        {-1, -2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -2, -3, -5, -6, -6},
                        {-1, -1, -2, -2, -2, -3, -2, -2, -3, -2, -3, -3, -4, -5, -6, -9},
                        {-1, -1, -2, -2, -2, -2, -2, -2, -3, -3, -4, -4, -5, -6, -6, -6},
                        {-1, -2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -3, -3, -5, -5, -5},
                        {-1, -2, -1, -1, -2, -2, -2, -2, -3, -3, -3, -4, -4, -5, -6, -7},
                        {-2, -2, -2, -3, -2, -2, -2, -3, -3, -3, -3, -4, -5, -6, -7, -10},
                        {-1, -1, -1, -1, -1, -2, -2, -3, -2, -2, -2, -2, -2, -5, -4, -10},
                        {-1, -2, -1, -1, -2, -2, -2, -2, -3, -3, -4, -3, -4, -4, -6, -9},
                        {-2, -1, -1, -2, -2, -2, -2, -2, -2, -3, -3, -3, -3, -4, -6, -5},
                        {-2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -3, -3, -4, -6}
                    }
                }
            };
    }

    /**
     * Returns the MAX_QI table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #MAX_QI} is initialized from this factory to split the table out of the class initializer.
     */
    private static int[][][][] makeMAX_QI() {
        return new int[][][][] {
                {
                    {
                        {2, 4, 3, 4, 5, 5, 4, 5, 5, 5, 7, 10, 7, 10, 13, 16},
                        {2, 2, 2, 3, 3, 6, 3, 5, 3, 3, 3, 5, 8, 9, 10, 23},
                        {2, 2, 2, 3, 3, 4, 3, 3, 4, 4, 4, 5, 4, 6, 8, 9},
                        {2, 3, 3, 4, 4, 3, 4, 3, 4, 5, 6, 7, 7, 9, 11, 15},
                        {2, 2, 2, 2, 3, 3, 2, 3, 3, 3, 3, 4, 6, 3, 7, 9},
                        {3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 7, 7, 5, 10, 11, 12},
                        {1, 2, 1, 2, 2, 2, 2, 2, 2, 2, 3, 2, 3, 2, 4, 9},
                        {2, 3, 3, 3, 3, 3, 3, 4, 4, 3, 5, 6, 7, 7, 9, 12},
                        {3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 7, 8, 6, 9, 17},
                        {2, 2, 2, 2, 2, 3, 4, 3, 3, 3, 4, 6, 7, 7, 12, 14},
                        {2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 3, 3, 4, 10, 10},
                        {2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 9, 13, 12},
                        {2, 2, 2, 2, 3, 3, 2, 3, 4, 4, 4, 5, 5, 5, 11, 12},
                        {2, 3, 2, 4, 4, 3, 3, 3, 4, 3, 4, 6, 4, 5, 9, 13},
                        {2, 2, 2, 2, 2, 3, 4, 3, 4, 4, 5, 5, 5, 7, 6, 12},
                        {2, 2, 2, 3, 2, 3, 4, 3, 5, 3, 4, 4, 3, 6, 8, 9},
                        {3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 5, 5, 7, 11}
                    },
                    {
                        {1, 2, 2, 2, 2, 3, 2, 3, 3, 3, 4, 6, 4, 5, 7, 9},
                        {2, 2, 1, 2, 2, 4, 2, 2, 2, 2, 2, 3, 5, 5, 6, 13},
                        {1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 4, 5, 5},
                        {1, 1, 2, 2, 3, 2, 2, 2, 2, 3, 3, 4, 4, 5, 6, 8},
                        {1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 4, 2, 4, 5},
                        {2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 4, 3, 6, 7, 7},
                        {1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 2, 1, 2, 2, 3, 5},
                        {1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 4, 4, 5, 7},
                        {2, 2, 2, 2, 2, 2, 2, 3, 2, 2, 3, 4, 4, 4, 5, 10},
                        {1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 4, 4, 7, 8},
                        {1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 2, 2, 2, 6, 5},
                        {1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 5, 7, 7},
                        {1, 1, 2, 2, 2, 2, 1, 2, 2, 2, 2, 3, 3, 3, 6, 7},
                        {1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 5, 8},
                        {1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 3, 8},
                        {1, 1, 1, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 3, 5, 5},
                        {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 4, 6}
                    }
                },
                {
                    {
                        {1, 3, 3, 4, 3, 3, 4, 4, 5, 5, 5, 6, 8, 10, 13, 14},
                        {2, 2, 3, 4, 3, 4, 4, 4, 6, 5, 5, 6, 7, 8, 10, 15},
                        {3, 3, 4, 3, 3, 4, 3, 3, 4, 5, 5, 4, 7, 7, 9, 11},
                        {2, 3, 3, 3, 3, 3, 4, 4, 4, 5, 5, 7, 8, 10, 11, 12},
                        {2, 3, 3, 2, 4, 4, 3, 4, 3, 5, 5, 7, 10, 7, 9, 13},
                        {2, 3, 3, 3, 2, 4, 4, 4, 4, 5, 5, 6, 7, 8, 10, 11},
                        {4, 3, 3, 3, 4, 4, 5, 4, 5, 6, 5, 8, 8, 9, 14, 11},
                        {3, 3, 3, 3, 4, 5, 4, 5, 4, 5, 7, 8, 8, 8, 8, 16},
                        {2, 3, 3, 4, 4, 3, 4, 4, 5, 4, 6, 5, 6, 7, 11, 13},
                        {3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 4, 7, 6, 8, 11, 15},
                        {3, 3, 3, 4, 4, 4, 4, 3, 4, 4, 5, 7, 7, 8, 10, 17},
                        {3, 2, 3, 3, 3, 4, 4, 4, 5, 4, 5, 6, 6, 11, 10, 16},
                        {3, 3, 4, 3, 4, 3, 4, 5, 5, 5, 6, 8, 5, 8, 10, 10},
                        {2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 6, 6, 12, 10},
                        {3, 3, 3, 4, 4, 3, 4, 4, 6, 5, 4, 5, 7, 8, 10, 20},
                        {2, 2, 3, 3, 3, 3, 4, 4, 3, 5, 3, 5, 5, 8, 11, 22},
                        {3, 4, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 6, 8, 9}
                    },
                    {
                        {1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 6, 7, 8},
                        {1, 1, 1, 3, 2, 2, 2, 3, 3, 3, 3, 3, 4, 5, 6, 9},
                        {2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 6},
                        {1, 2, 2, 2, 1, 2, 2, 2, 3, 3, 3, 4, 5, 6, 6, 7},
                        {1, 2, 2, 1, 2, 2, 2, 2, 2, 3, 3, 4, 5, 4, 5, 8},
                        {1, 2, 2, 2, 1, 2, 2, 3, 2, 3, 3, 3, 4, 5, 6, 6},
                        {2, 2, 2, 2, 2, 2, 3, 2, 3, 3, 3, 5, 5, 5, 8, 6},
                        {2, 2, 1, 2, 2, 3, 2, 3, 2, 3, 4, 4, 4, 4, 5, 9},
                        {1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 6, 7},
                        {2, 1, 2, 2, 2, 2, 2, 2, 3, 3, 2, 4, 3, 4, 6, 9},
                        {2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 3, 4, 4, 4, 5, 10},
                        {2, 1, 2, 2, 2, 2, 2, 2, 3, 2, 3, 4, 3, 6, 6, 9},
                        {2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 3, 5, 6, 6},
                        {1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 4, 3, 7, 6},
                        {2, 1, 2, 2, 2, 2, 2, 3, 3, 3, 2, 3, 4, 4, 6, 11},
                        {1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 2, 3, 3, 5, 7, 13},
                        {1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 5}
                    }
                }
            };
    }

    /**
     * Returns the ST2_ALL_QLVLS_8 table by concatenating its literal chunks.
     *
     * <p>Initializes {@link #ST2_ALL_QLVLS_8}; see {@link #concat(int[]...)}.
     */
    private static int[] makeST2_ALL_QLVLS_8() {
        return concat(makeST2_ALL_QLVLS_8_0(), makeST2_ALL_QLVLS_8_1(), makeST2_ALL_QLVLS_8_2(), makeST2_ALL_QLVLS_8_3(), makeST2_ALL_QLVLS_8_4());
    }

    /**
     * Returns chunk 0 of the ST2_ALL_QLVLS_8 table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVLS_8()} concatenates the chunks into {@link #ST2_ALL_QLVLS_8}.
     */
    private static int[] makeST2_ALL_QLVLS_8_0() {
        return new int[] {
                179, 178, 171, 142, 75, 43, 213, 178, 176, 163, 129, 101, 86, 81, 65, 212, 185, 158, 129, 105,
                86, 65, 188, 183, 171, 155, 128, 105, 87, 69, 73, 191, 167, 168, 157, 129, 103, 89, 87, 110,
                79, 183, 191, 170, 158, 130, 109, 100, 86, 87, 68, 184, 175, 167, 147, 128, 113, 95, 73, 82,
                182, 173, 163, 155, 131, 116, 106, 85, 77, 73, 178, 184, 167, 161, 142, 130, 117, 103, 103, 96,
                60, 111, 146, 161, 167, 152, 143, 129, 121, 105, 103, 90, 89, 192, 175, 157, 159, 149, 138, 131,
                123, 108, 106, 106, 100, 116, 104, 187, 149, 165, 163, 151, 147, 138, 128, 121, 116, 115, 115, 107,
                115, 119, 100, 130, 90, 185, 150, 183, 148, 146, 152, 144, 135, 128, 120, 116, 110, 104, 116, 109,
                166, 150, 141, 149, 148, 142, 139, 136, 127, 123, 120, 120, 111, 121, 96, 105, 62, 68, 36, 154,
                158, 147, 173, 155, 162, 146, 144, 141, 140, 136, 132, 128, 132, 126, 125, 120, 121, 119, 126, 118,
                118, 125, 130, 98, 94, 124, 82, 139, 114, 145, 147, 137, 143, 127, 146, 141, 138, 135, 136, 128,
                131, 131, 130, 127, 131, 131, 126, 127, 124, 125, 120, 125, 125, 131, 121, 111, 104, 89, 111, 127,
                53, 221, 209, 135, 58, 109, 196, 194, 125, 70, 108, 184, 178, 142, 67, 51, 227, 180, 173, 125,
                83, 60, 80, 229, 182, 137, 95, 92, 142, 214, 174, 172, 121, 87, 87, 78, 104, 98, 129, 201,
                197, 162, 124, 96, 71, 70, 216, 168, 165, 138, 99, 74, 105, 75, 100, 210, 159, 175, 160, 133,
                97, 83, 36, 191, 183, 165, 149, 145, 114, 89, 64, 197, 144, 148, 157, 135, 113, 113, 94, 76,
                207, 198, 156, 139, 147, 126, 109, 124, 107, 121, 107, 214, 180, 172, 141, 134, 117, 115, 93, 122,
                136, 98, 63, 121, 182, 184, 159, 178, 140, 131, 119, 119, 136, 112, 121, 147, 121, 30, 140, 47,
                228, 150, 191, 147, 116, 141, 144, 140, 128, 131, 129, 139, 120, 129, 123, 112, 105, 101, 64, 84,
                145, 93, 148, 142, 140, 140, 133, 122, 142, 114, 115, 104, 118, 116, 165, 137, 147, 120, 170, 149,
                132, 80, 93, 92, 145, 108, 139, 120, 166, 164, 135, 70, 128, 146, 212, 187, 131, 69, 62, 190,
                180, 130, 80, 62, 179, 184, 170, 128, 83, 58, 213, 191, 174, 131, 97, 81, 61, 199, 180, 166,
                131, 96, 71, 59, 169, 188, 166, 129, 100, 80, 74, 70, 184, 173, 160, 131, 99, 78, 66, 195,
                206, 177, 158, 129, 103, 86, 73, 164, 178, 174, 158, 134, 106, 87, 77, 56, 175, 170, 162, 149,
                125, 108, 89, 93, 94, 207, 168, 160, 145, 126, 104, 86, 97, 71, 165, 170, 162, 151, 128, 115,
                105, 98, 103, 74, 161, 145, 164, 155, 148, 139, 126, 113, 101, 88, 68, 158, 157, 148, 158, 154,
                149, 141, 130, 120, 111, 103, 108, 97, 78, 195, 158, 156, 157, 145, 142, 140, 136, 133, 128, 124,
                122, 118, 112, 115, 109, 85, 135, 141, 150, 140, 142, 144, 140, 139, 139, 137, 134, 135, 135, 131,
                122, 121, 112, 107, 116, 97, 186, 182, 171, 133, 84, 70, 178, 174, 168, 132, 89, 80, 61, 200,
                184, 162, 126, 96, 82, 76, 206, 174, 163, 133, 102, 88, 70, 63, 201, 173, 159, 130, 104, 87,
                90, 101, 142, 170, 166, 156, 133, 103, 88, 68, 230, 186, 171, 154, 138, 109, 94, 83, 128, 208,
                176, 166, 154, 152, 128, 105, 76, 83, 186, 141, 156, 160, 147, 131, 113, 105, 100, 132, 184, 195,
                164, 162, 154, 136, 122, 109, 98, 86, 55, 172, 160, 156, 153, 141, 125, 121, 117, 100, 95, 97,
                59, 165, 152, 159, 167, 144, 143, 138, 129, 122, 121, 106, 104, 114, 123, 118, 214, 173, 91, 164,
                150, 146, 144, 131, 137, 138, 130, 120, 108, 114, 101, 119, 53, 137, 156, 171, 145, 148, 151, 145,
                139, 133, 126, 122, 124, 127, 117, 103, 105, 114, 119, 109, 166, 100, 119, 147, 143, 148, 142, 146,
                141, 136, 133, 129, 125, 127, 126, 122, 120, 110, 125, 123, 91, 187, 107, 155, 171, 148, 141, 146,
                142, 133, 134, 132, 132, 135, 134, 125, 124, 126, 127, 116, 112, 123, 131, 118, 142, 106, 115, 109,
                177, 112, 63, 197, 208, 131, 51, 37, 188, 201, 133, 52, 46, 177, 192, 133, 59, 44, 212, 197,
                127, 73, 81, 207, 191, 129, 75, 82, 107, 200, 183, 127, 82, 78, 80, 187, 188, 176, 129, 80,
                54, 208, 179, 135, 89, 58, 56, 194, 183, 167, 135, 91, 75, 84, 183, 173, 173, 163, 130, 94,
                82, 72, 191, 171, 163, 132, 99, 77, 82, 183, 178, 157, 135, 104, 95, 90, 91, 221, 177, 173,
                166, 150, 137, 110, 102, 93, 103, 126, 114, 144, 166, 160, 151, 145, 135, 138, 109, 91, 71, 178,
                157, 123, 154, 148, 142, 138, 121, 121, 119, 116, 108, 100, 67, 177, 148, 146, 145, 138, 142, 134,
                130, 128, 125, 123, 116, 116, 94, 135, 150, 113, 57, 187, 185, 169, 132, 90, 66, 64, 202, 189,
                168, 129, 93, 83, 75, 190, 176, 163, 131, 100, 79, 54, 184, 178, 161, 129, 98, 86, 90, 59,
                205, 180, 171, 157, 129, 103, 85, 67, 60, 184, 175, 158, 132, 107, 91, 81, 86, 182, 173, 153,
                131, 108, 92, 81, 77, 139, 187, 173, 164, 148, 128, 110, 95, 83, 69, 172, 184, 167, 161, 150,
                137, 117, 100, 89, 81, 152, 164, 163, 156, 145, 131, 113, 104, 97, 85, 88, 180, 124, 162, 166,
                154, 154, 145, 131, 120, 106, 106, 102, 90, 100, 62, 191, 166, 180, 150, 152, 155, 149, 140, 127,
                120, 113, 107, 97, 102, 103, 96, 169, 151, 168, 156, 156, 144, 142, 139, 133, 120, 107, 96, 87,
                82, 201, 180, 184, 128, 161, 157, 152, 148, 145, 136, 129, 124, 123, 116, 105, 113, 107, 92, 115,
                135, 106, 130, 143, 171, 145, 139, 151, 150, 142, 138, 144, 137, 136, 138, 133, 129, 123, 124, 120,
                119, 118, 106, 117, 116, 115, 129, 117, 180, 134, 148, 136, 131, 136, 145, 138, 141, 140, 136, 134,
                133, 129, 130, 123, 123, 122, 121, 125, 114, 120, 105, 105, 134, 150, 179, 189, 133, 17, 229, 129,
                41, 59, 255, 222, 132, 35, 223, 129, 45, 69, 213, 214, 132, 44, 41, 230, 215, 131, 47, 58,
                211, 207, 131, 46, 31, 206, 209, 130, 54, 63, 224, 207, 128, 57, 73, 204, 200, 132, 58, 57,
                198, 195, 129, 64, 71, 99, 187, 192, 189, 125, 68, 53, 175, 180, 182, 137, 78, 77, 67, 180,
                178, 164, 122, 85, 63, 242, 52, 185, 190, 153, 119, 117, 124, 104, 82, 157, 160, 162, 176, 178,
                133, 115, 117, 118, 120, 112, 132, 111, 140, 127, 51, 199, 180, 131, 80, 59, 201, 188, 173, 130,
                86, 61, 56, 191, 187, 170, 130, 93, 76, 79, 207, 189, 173, 136, 88, 63, 53, 210, 193, 172,
                130, 86, 69, 57, 198, 180, 160, 128, 98, 84, 73, 197, 183, 163, 132, 104, 83, 56, 158, 173,
                172, 157, 133, 107, 92, 81, 75, 167, 180, 170, 150, 128, 111, 101, 86, 87, 186, 177, 157, 149,
                137, 117, 92, 58, 195, 179, 173, 156, 153, 143, 127, 111, 99, 99, 107, 83, 156, 170, 161, 153,
                146, 135, 126, 117, 104, 100, 89, 80, 121, 170, 159, 152, 151, 143, 134, 127, 123, 116, 111, 109,
                96, 103, 74, 167, 154, 161, 146, 151, 146, 141, 135, 127, 124, 121, 112, 114, 111, 100, 92, 149,
                150, 159, 148, 149, 149, 148, 141, 139, 138, 137, 134, 129, 124, 125, 122, 111, 107, 144, 166, 146,
                151, 148, 141, 149, 144, 145, 139, 142, 137, 142, 141, 139, 139, 135, 135, 131, 129, 127, 119, 113,
                118, 110, 118, 99, 179, 215, 193, 176, 131, 84, 65, 72, 184, 183, 170, 131, 90, 75, 77, 193,
                195, 167, 126, 97, 77, 74, 188, 175, 161, 132, 96, 75, 77, 180, 175, 165, 159, 132, 101, 81,
                60, 185, 175, 158, 131, 105, 91, 83, 71, 181, 174, 169, 158, 134, 107, 91, 74, 75, 169, 177,
                170, 153, 131, 110, 96, 89, 89, 188, 177, 166, 147, 130, 112, 96, 81, 80, 187, 171, 162, 146,
                129, 111, 103, 90, 84, 178, 156, 165, 159, 155, 142, 127, 115, 104, 103, 95, 87, 166, 162, 165,
                157, 149, 140, 130, 120, 109, 107, 103, 93, 88, 61, 179, 172, 157, 159, 151, 144, 138, 135, 127,
                120, 109, 108, 111, 109, 102, 57, 101, 171, 153, 156, 143, 142, 137, 131, 128, 121, 114, 110, 101,
                91, 108, 150, 160, 156, 142, 142, 146, 143, 137, 134, 132, 129, 126, 125, 120, 120, 114, 113, 105,
                104, 106, 155, 146, 138, 140, 140, 137, 134, 138, 133, 132, 132, 127, 129, 131, 127, 126, 129, 129,
                134, 122, 127, 121, 116, 103, 113, 113, 122, 139, 112, 221, 217, 134, 59, 74, 216, 195, 137, 59,
                27, 208, 206, 132, 73, 68, 200, 186, 183, 133, 64, 51, 166, 191, 173, 128, 73, 37, 202, 198,
                179, 133, 84, 54, 90, 215, 195, 171, 128, 84, 64, 89, 70, 179, 200, 170, 131, 92, 66, 78,
                198, 182, 164, 124, 96, 77, 69, 140, 198, 178, 157, 124, 96, 83, 80, 138, 161, 164, 147, 129,
                109, 95, 87, 75, 160, 181, 166, 160, 152, 138, 120, 108, 88, 103, 60, 108, 106, 188, 147, 155,
                165, 152, 143, 134, 120, 110, 99, 104, 108, 86, 150, 222, 139, 152, 157, 154, 141, 135, 129, 124,
                119, 112, 107, 97, 84, 66, 176, 153, 175, 150, 144, 147, 145, 139, 132, 126, 130, 121, 120, 125,
                125, 118, 115, 113, 118, 80, 93, 59, 221, 151, 139, 136, 135, 138, 132, 138, 129, 139, 133, 132,
                133, 125, 131, 132, 129, 136, 127, 134, 125, 117, 126, 106, 118, 128, 110, 133, 99, 227, 209, 131,
                50, 58, 228, 209, 129, 54, 63, 220, 207, 130, 57, 62, 205, 204, 131, 58, 60, 217, 200, 131,
                59, 43, 214, 204, 131, 63, 50, 209, 194, 130, 63, 76, 192, 192, 130, 70, 60, 207, 187, 130,
                76, 70, 203, 184, 125, 81, 81, 81, 194, 183, 127, 86, 55, 197, 193, 181, 126, 89, 78, 87,
                183, 172, 168, 117, 96, 89, 69, 174, 150, 167, 168, 150, 118, 111, 102, 83, 62, 218, 185, 158,
                137, 139, 110, 112, 114, 122, 146, 106, 109, 101, 106, 73, 179, 148, 138, 128, 146, 152, 141, 127,
                128, 147, 103, 118, 121, 131, 110, 120, 136, 109, 126, 209, 189, 130, 68, 52, 200, 186, 128, 80,
                51, 208, 180, 129, 86, 65, 93, 220, 181, 176, 131, 85, 72, 60, 184, 182, 169, 131, 93, 80,
                59, 199, 180, 164, 130, 97, 75, 60, 187, 177, 161, 128, 97, 81, 85, 200, 180, 163, 134, 107,
                83, 85, 61, 202, 194, 164, 151, 130, 106, 81, 69, 66, 190, 177, 158, 136, 110, 98, 98, 82,
                128, 179, 162, 149, 133, 114, 104, 96, 106, 106, 178, 167, 169, 158, 153, 142, 133, 123, 115, 111,
                88, 91, 165, 155, 161, 143, 139, 138, 134, 122, 114, 92, 97, 63, 194, 171, 163, 148, 149, 140,
                134, 132, 131, 125, 118, 118, 114, 106, 115, 112, 131, 116, 166, 142, 157, 136, 133, 132, 127, 122,
                122, 123, 120, 120, 113, 117, 117, 110, 103, 101, 99, 153, 81, 117, 154, 129, 148, 150, 137, 149,
                134, 126, 133, 139, 135, 139, 136, 134, 136, 131, 133, 133, 130, 130, 126, 126, 124, 132, 126, 124,
                115, 119, 107, 106, 93, 221, 196, 130, 67, 46, 205, 193, 128, 72, 45, 221, 197, 183, 133, 75
        };
    }

    /**
     * Returns chunk 1 of the ST2_ALL_QLVLS_8 table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVLS_8()} concatenates the chunks into {@link #ST2_ALL_QLVLS_8}.
     */
    private static int[] makeST2_ALL_QLVLS_8_1() {
        return new int[] {
                57, 154, 198, 182, 129, 78, 71, 216, 184, 129, 87, 72, 36, 192, 194, 174, 132, 87, 62, 88,
                236, 198, 170, 134, 90, 44, 166, 197, 170, 125, 97, 78, 45, 195, 178, 154, 131, 107, 96, 86,
                55, 224, 180, 98, 168, 172, 149, 133, 113, 98, 75, 39, 157, 158, 159, 153, 143, 128, 112, 99,
                110, 120, 166, 164, 162, 159, 148, 138, 127, 109, 99, 95, 78, 188, 185, 163, 151, 141, 129, 127,
                122, 110, 98, 93, 165, 144, 178, 140, 134, 137, 146, 147, 143, 135, 125, 118, 104, 85, 83, 192,
                171, 144, 143, 134, 126, 124, 120, 119, 125, 109, 119, 107, 64, 91, 126, 105, 180, 141, 138, 120,
                146, 157, 153, 111, 154, 140, 144, 131, 137, 134, 134, 132, 133, 130, 127, 131, 128, 124, 127, 118,
                124, 120, 107, 120, 107, 80, 73, 200, 195, 128, 70, 42, 199, 187, 129, 74, 63, 138, 219, 197,
                180, 131, 79, 64, 177, 186, 178, 129, 87, 76, 86, 38, 176, 179, 173, 133, 89, 78, 90, 87,
                126, 198, 171, 130, 95, 80, 68, 167, 182, 166, 129, 92, 78, 86, 213, 183, 168, 137, 102, 79,
                36, 202, 170, 159, 127, 103, 90, 86, 74, 172, 164, 149, 125, 107, 96, 94, 177, 156, 138, 126,
                107, 92, 100, 52, 203, 172, 165, 153, 140, 124, 116, 112, 113, 109, 112, 83, 158, 179, 159, 157,
                148, 138, 136, 130, 120, 108, 101, 92, 154, 141, 149, 161, 151, 149, 141, 139, 135, 124, 117, 119,
                94, 78, 205, 231, 153, 150, 99, 156, 152, 138, 149, 134, 136, 129, 123, 122, 122, 124, 125, 122,
                111, 116, 111, 101, 162, 139, 144, 143, 139, 137, 135, 131, 132, 129, 130, 130, 136, 133, 135, 133,
                135, 131, 127, 130, 119, 119, 116, 123, 105, 84, 221, 191, 132, 67, 40, 199, 189, 130, 74, 58,
                214, 183, 130, 75, 62, 225, 191, 176, 131, 81, 53, 180, 195, 174, 129, 83, 67, 166, 181, 166,
                134, 95, 66, 37, 154, 194, 179, 163, 131, 98, 74, 102, 86, 172, 179, 159, 134, 102, 80, 64,
                157, 179, 172, 153, 125, 104, 87, 84, 101, 162, 171, 166, 149, 136, 114, 90, 86, 71, 170, 175,
                169, 148, 133, 116, 103, 93, 89, 126, 180, 165, 157, 147, 135, 122, 111, 91, 82, 65, 157, 156,
                157, 150, 141, 136, 128, 120, 108, 99, 79, 69, 156, 148, 131, 137, 135, 140, 136, 139, 130, 118,
                113, 105, 114, 119, 100, 116, 127, 167, 152, 152, 141, 141, 133, 131, 127, 126, 121, 118, 108, 97,
                91, 181, 147, 171, 158, 166, 141, 151, 146, 140, 137, 137, 140, 137, 138, 138, 133, 135, 134, 131,
                127, 129, 126, 119, 120, 122, 115, 119, 127, 111, 115, 215, 192, 132, 66, 33, 206, 188, 128, 76,
                55, 191, 179, 132, 80, 54, 193, 179, 131, 88, 72, 124, 212, 196, 173, 130, 86, 69, 181, 194,
                176, 130, 89, 66, 61, 178, 185, 167, 131, 93, 77, 78, 96, 197, 187, 164, 131, 96, 87, 80,
                210, 187, 158, 123, 101, 84, 86, 119, 59, 161, 180, 169, 155, 126, 101, 88, 88, 216, 182, 143,
                117, 111, 98, 86, 100, 201, 177, 171, 152, 131, 115, 102, 88, 95, 182, 168, 157, 149, 136, 131,
                112, 80, 50, 157, 152, 145, 158, 146, 142, 134, 128, 124, 116, 114, 109, 99, 89, 144, 178, 162,
                160, 150, 145, 136, 133, 133, 130, 129, 127, 131, 122, 122, 118, 101, 165, 137, 165, 152, 141, 142,
                140, 137, 137, 134, 132, 131, 128, 128, 127, 127, 123, 127, 120, 117, 109, 79, 208, 190, 131, 74,
                57, 59, 210, 202, 185, 130, 76, 59, 58, 202, 199, 182, 131, 79, 64, 64, 194, 193, 177, 132,
                81, 61, 54, 186, 190, 176, 131, 84, 65, 69, 201, 194, 175, 130, 85, 73, 72, 200, 191, 175,
                128, 87, 79, 71, 190, 190, 172, 131, 87, 68, 62, 190, 186, 170, 132, 89, 69, 69, 186, 187,
                170, 130, 90, 74, 62, 190, 186, 169, 128, 94, 83, 80, 76, 185, 179, 178, 165, 130, 96, 84,
                82, 77, 142, 172, 177, 170, 161, 131, 99, 87, 85, 84, 88, 168, 167, 165, 165, 156, 129, 103,
                96, 93, 94, 90, 173, 164, 162, 159, 158, 149, 130, 115, 107, 105, 103, 100, 109, 98, 142, 144,
                150, 152, 146, 146, 145, 149, 149, 139, 122, 116, 121, 120, 118, 118, 117, 118, 109, 113, 114, 114,
                233, 199, 142, 35, 201, 202, 129, 64, 42, 239, 200, 131, 73, 26, 222, 190, 128, 74, 54, 208,
                189, 130, 73, 55, 216, 190, 131, 80, 59, 42, 207, 179, 124, 80, 39, 203, 178, 128, 93, 61,
                66, 217, 194, 172, 131, 92, 71, 39, 229, 174, 187, 166, 133, 96, 71, 51, 182, 178, 157, 132,
                106, 91, 68, 87, 201, 178, 179, 156, 129, 110, 101, 100, 96, 137, 59, 170, 197, 140, 155, 139,
                113, 97, 77, 74, 191, 176, 163, 152, 142, 127, 116, 101, 100, 96, 155, 174, 153, 158, 149, 146,
                147, 136, 135, 121, 119, 112, 110, 106, 93, 86, 91, 193, 157, 151, 147, 138, 139, 137, 135, 131,
                130, 127, 124, 118, 120, 120, 114, 87, 115, 74, 230, 130, 59, 44, 216, 128, 61, 164, 197, 146,
                29, 204, 123, 54, 67, 223, 129, 64, 62, 201, 209, 118, 60, 69, 113, 39, 213, 127, 66, 36,
                206, 187, 134, 65, 118, 183, 202, 187, 136, 68, 61, 197, 168, 145, 91, 35, 209, 183, 163, 102,
                61, 70, 225, 195, 154, 125, 104, 94, 83, 206, 179, 141, 115, 108, 89, 43, 46, 219, 181, 160,
                142, 124, 129, 105, 116, 49, 60, 64, 171, 150, 124, 131, 130, 116, 111, 130, 87, 45, 80, 197,
                179, 142, 133, 135, 120, 95, 135, 137, 115, 128, 100, 117, 119, 163, 145, 163, 78, 120, 223, 131,
                32, 244, 214, 131, 41, 215, 202, 129, 40, 221, 211, 130, 68, 35, 203, 200, 133, 56, 22, 202,
                203, 131, 65, 60, 207, 189, 131, 63, 57, 210, 192, 128, 73, 33, 210, 191, 134, 72, 58, 193,
                178, 127, 79, 66, 195, 169, 122, 71, 59, 197, 174, 127, 94, 76, 74, 176, 173, 156, 122, 89,
                63, 173, 165, 170, 157, 124, 106, 88, 73, 97, 186, 167, 161, 152, 136, 123, 109, 101, 85, 82,
                201, 173, 140, 129, 136, 126, 125, 131, 116, 100, 80, 76, 240, 204, 136, 41, 232, 201, 130, 49,
                248, 200, 126, 59, 48, 234, 206, 132, 68, 82, 208, 188, 131, 72, 56, 34, 191, 182, 134, 69,
                35, 206, 188, 138, 77, 68, 221, 199, 175, 127, 63, 43, 225, 187, 175, 131, 78, 56, 200, 213,
                180, 140, 99, 83, 55, 181, 189, 162, 121, 109, 80, 85, 203, 179, 173, 148, 130, 106, 94, 98,
                57, 197, 157, 164, 158, 138, 134, 114, 89, 64, 64, 182, 171, 158, 162, 145, 129, 115, 121, 97,
                122, 68, 173, 164, 132, 162, 150, 146, 137, 129, 129, 123, 103, 107, 74, 129, 207, 177, 153, 149,
                145, 129, 134, 119, 133, 118, 127, 125, 136, 124, 92, 92, 235, 130, 24, 223, 132, 26, 193, 214,
                131, 32, 223, 128, 51, 84, 219, 129, 56, 80, 215, 127, 53, 67, 203, 204, 130, 52, 27, 217,
                137, 51, 14, 215, 199, 136, 57, 67, 201, 188, 131, 66, 60, 220, 192, 136, 62, 50, 218, 191,
                135, 82, 62, 229, 200, 181, 142, 89, 83, 107, 85, 184, 177, 167, 146, 133, 79, 50, 219, 175,
                134, 167, 143, 116, 108, 93, 77, 177, 174, 166, 145, 130, 123, 104, 98, 159, 48, 206, 204, 133,
                48, 24, 211, 209, 129, 60, 36, 213, 201, 131, 60, 44, 221, 196, 128, 63, 53, 213, 189, 129,
                68, 35, 227, 191, 134, 74, 54, 217, 187, 133, 74, 57, 187, 201, 174, 127, 78, 52, 203, 193,
                177, 138, 88, 59, 201, 184, 167, 133, 89, 70, 52, 192, 181, 170, 166, 131, 97, 87, 82, 68,
                158, 174, 167, 161, 127, 101, 84, 83, 87, 182, 174, 164, 146, 132, 99, 67, 111, 139, 156, 166,
                162, 152, 130, 119, 101, 99, 84, 126, 119, 178, 166, 138, 148, 153, 145, 143, 136, 120, 118, 106,
                97, 85, 119, 51, 138, 156, 154, 143, 150, 153, 146, 140, 132, 120, 118, 112, 106, 83, 91, 72,
                247, 210, 131, 0, 255, 131, 21, 247, 131, 12, 248, 130, 19, 240, 130, 19, 237, 131, 24, 230,
                131, 21, 232, 129, 37, 235, 127, 39, 32, 224, 131, 32, 224, 128, 41, 31, 224, 213, 124, 40,
                220, 207, 139, 52, 60, 190, 192, 119, 52, 50, 232, 181, 205, 117, 111, 81, 5, 148, 185, 184,
                107, 113, 115, 125, 146, 54, 221, 132, 37, 213, 217, 130, 45, 20, 226, 208, 130, 55, 59, 217,
                199, 135, 60, 61, 222, 200, 130, 58, 53, 207, 196, 128, 67, 44, 226, 199, 134, 71, 40, 194,
                191, 135, 73, 58, 205, 182, 127, 84, 56, 196, 174, 144, 85, 33, 188, 188, 160, 123, 86, 76,
                73, 196, 176, 164, 135, 108, 82, 75, 78, 149, 178, 165, 143, 122, 105, 95, 72, 70, 192, 177,
                173, 164, 142, 127, 115, 96, 93, 94, 171, 159, 156, 151, 147, 141, 135, 122, 109, 94, 74, 132,
                146, 137, 139, 131, 129, 127, 128, 128, 126, 119, 116, 103, 91, 112, 105, 217, 131, 43, 26, 227,
                207, 130, 50, 21, 241, 210, 122, 59, 27, 217, 193, 134, 59, 41, 197, 189, 133, 64, 36, 213,
                192, 133, 73, 51, 201, 187, 132, 71, 47, 200, 185, 133, 78, 58, 22, 208, 179, 126, 81, 59,
                194, 172, 126, 87, 66, 180, 186, 164, 125, 91, 82, 72, 195, 182, 178, 155, 128, 99, 83, 71,
                68, 182, 184, 169, 150, 136, 118, 96, 87, 95, 151, 183, 173, 158, 139, 130, 113, 90, 69, 60,
                188, 160, 154, 150, 139, 133, 127, 120, 115, 106, 93, 84, 163, 149, 153, 140, 136, 129, 123, 118,
                113, 112, 118, 121, 122, 109, 114, 109, 107, 113, 236, 135, 34, 225, 136, 21, 236, 135, 44, 206,
                217, 133, 23, 201, 211, 124, 30, 232, 218, 132, 45, 37, 213, 215, 129, 48, 69, 226, 211, 131,
                50, 57, 227, 200, 121, 62, 45, 207, 192, 121, 55, 40, 232, 200, 178, 126, 79, 53, 183, 195,
                178, 142, 96, 84, 50, 145, 180, 171, 160, 136, 101, 84, 95, 98, 206, 162, 171, 149, 129, 111,
                94, 82, 70, 175, 173, 163, 155, 141, 124, 119, 120, 121, 108, 102, 70, 35, 227, 162, 148, 139,
                138, 142, 134, 131, 133, 130, 131, 129, 118, 109, 109, 112, 71, 240, 131, 23, 239, 130, 30, 230,
                130, 31, 229, 132, 32, 230, 132, 33, 234, 132, 34, 222, 129, 37, 246, 217, 130, 43, 206, 219,
                129, 49, 18, 222, 219, 123, 63, 64, 215, 128, 48, 215, 209, 124, 60, 55, 208, 196, 117, 69,
                40, 169, 184, 179, 116, 85, 68, 217, 177, 142, 94, 104, 135, 91, 105, 86, 210, 191, 152, 180,
                149, 131, 113, 116, 127, 103, 70, 231, 131, 32, 225, 127, 39, 226, 218, 129, 50, 37, 214, 205,
                132, 51, 44, 234, 203, 132, 54, 32, 233, 201, 132, 60, 32, 205, 196, 131, 62, 53, 224, 199,
                135, 69, 53, 229, 187, 127, 72, 36, 220, 190, 135, 82, 69, 215, 181, 131, 89, 79, 71, 197,
                184, 166, 135, 110, 86, 88, 140, 169, 152, 138, 132, 102, 70, 32, 196, 177, 154, 144, 128, 121,
                105, 94, 91, 107, 177, 166, 170, 145, 136, 131, 125, 112, 105, 106, 93, 112, 136, 132, 177, 169,
                140, 123, 139, 148, 140, 141, 133, 132, 127, 130, 131, 122, 111, 107, 67, 238, 131, 26, 234, 128
        };
    }

    /**
     * Returns chunk 2 of the ST2_ALL_QLVLS_8 table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVLS_8()} concatenates the chunks into {@link #ST2_ALL_QLVLS_8}.
     */
    private static int[] makeST2_ALL_QLVLS_8_2() {
        return new int[] {
                34, 220, 134, 39, 21, 227, 129, 40, 66, 231, 129, 49, 19, 221, 212, 132, 45, 47, 235, 213,
                132, 39, 224, 213, 124, 56, 39, 219, 191, 133, 76, 55, 188, 174, 188, 134, 81, 41, 190, 181,
                162, 124, 80, 75, 184, 182, 169, 143, 104, 70, 70, 216, 191, 159, 128, 115, 86, 68, 202, 196,
                149, 158, 156, 134, 106, 72, 67, 196, 183, 147, 126, 115, 114, 105, 83, 119, 107, 138, 156, 138,
                136, 150, 145, 147, 142, 138, 136, 128, 129, 122, 110, 105, 110, 72, 123, 236, 130, 32, 228, 129,
                35, 217, 219, 131, 39, 177, 220, 130, 50, 45, 174, 207, 132, 53, 71, 229, 212, 132, 57, 48,
                213, 199, 129, 58, 41, 238, 205, 136, 64, 28, 225, 192, 128, 72, 53, 210, 182, 127, 80, 63,
                213, 171, 126, 77, 56, 232, 185, 160, 124, 95, 87, 72, 179, 166, 143, 127, 100, 71, 73, 212,
                146, 177, 158, 147, 131, 110, 80, 54, 142, 135, 155, 155, 149, 137, 126, 124, 118, 123, 106, 79,
                148, 152, 152, 144, 137, 129, 128, 131, 133, 131, 119, 115, 107, 99, 87, 53, 231, 133, 25, 229,
                128, 34, 230, 128, 38, 212, 216, 131, 39, 220, 211, 131, 41, 211, 203, 134, 50, 194, 201, 131,
                57, 78, 209, 198, 140, 63, 35, 213, 189, 123, 68, 64, 198, 191, 183, 139, 80, 51, 220, 184,
                136, 88, 65, 75, 200, 171, 136, 99, 65, 33, 175, 191, 171, 148, 125, 99, 75, 60, 217, 185,
                162, 158, 153, 133, 105, 97, 94, 116, 171, 171, 165, 156, 138, 126, 119, 98, 88, 166, 143, 169,
                143, 127, 131, 141, 136, 135, 130, 126, 125, 123, 125, 130, 114, 137, 117, 35, 231, 133, 23, 232,
                128, 37, 220, 132, 38, 216, 132, 53, 74, 215, 129, 51, 34, 208, 214, 131, 48, 47, 220, 207,
                132, 56, 55, 227, 200, 134, 62, 51, 213, 203, 122, 63, 94, 61, 190, 189, 126, 64, 65, 234,
                190, 110, 87, 60, 199, 184, 132, 89, 63, 229, 188, 158, 130, 74, 28, 226, 190, 167, 147, 127,
                108, 92, 66, 245, 190, 176, 156, 133, 130, 124, 120, 106, 95, 71, 129, 152, 158, 153, 143, 134,
                136, 129, 132, 130, 129, 116, 45, 228, 133, 34, 23, 232, 224, 130, 39, 42, 222, 218, 131, 41,
                28, 222, 214, 132, 42, 31, 225, 214, 130, 47, 43, 217, 210, 132, 49, 38, 221, 211, 129, 52,
                37, 232, 211, 130, 51, 27, 227, 210, 128, 57, 43, 224, 205, 131, 56, 44, 214, 198, 132, 57,
                45, 208, 196, 132, 65, 54, 196, 197, 189, 131, 69, 60, 57, 202, 189, 184, 134, 80, 71, 75,
                186, 179, 173, 131, 96, 86, 87, 77, 171, 174, 161, 156, 153, 149, 150, 142, 105, 102, 105, 99,
                96, 106, 144, 158, 166, 168, 136, 50, 214, 187, 168, 128, 94, 72, 47, 212, 189, 163, 130, 89,
                81, 72, 210, 181, 157, 127, 102, 86, 78, 89, 214, 192, 159, 130, 112, 95, 78, 207, 182, 168,
                155, 133, 108, 85, 70, 168, 182, 165, 156, 153, 136, 112, 93, 64, 94, 173, 162, 161, 143, 129,
                118, 101, 84, 65, 182, 191, 168, 151, 136, 124, 110, 94, 98, 85, 146, 176, 178, 166, 156, 146,
                135, 123, 113, 100, 100, 94, 179, 186, 161, 153, 151, 148, 143, 137, 131, 119, 110, 92, 105, 173,
                166, 152, 146, 145, 143, 138, 133, 124, 115, 112, 102, 102, 35, 168, 170, 160, 151, 148, 142, 139,
                134, 128, 126, 122, 118, 113, 117, 110, 92, 70, 177, 169, 154, 151, 141, 140, 132, 128, 125, 123,
                121, 127, 123, 133, 111, 121, 88, 156, 151, 134, 146, 141, 136, 136, 133, 136, 133, 133, 128, 126,
                125, 121, 118, 118, 111, 109, 101, 119, 124, 92, 112, 157, 123, 142, 114, 137, 137, 132, 137, 132,
                127, 128, 127, 127, 126, 125, 127, 125, 120, 117, 118, 110, 118, 102, 112, 116, 112, 99, 97, 108,
                203, 173, 143, 74, 42, 196, 163, 167, 167, 143, 69, 27, 209, 192, 166, 131, 89, 63, 39, 213,
                194, 165, 122, 106, 97, 95, 102, 185, 180, 165, 150, 133, 102, 70, 33, 200, 177, 165, 150, 130,
                105, 86, 67, 46, 178, 178, 164, 148, 130, 112, 99, 88, 70, 200, 172, 165, 152, 145, 133, 120,
                105, 91, 75, 164, 170, 162, 151, 140, 129, 117, 109, 100, 94, 106, 88, 190, 172, 161, 150, 140,
                131, 123, 115, 105, 94, 63, 194, 165, 157, 144, 137, 132, 125, 117, 100, 83, 78, 201, 139, 159,
                155, 150, 143, 137, 132, 122, 115, 111, 104, 98, 84, 178, 167, 158, 150, 142, 145, 141, 134, 128,
                126, 121, 114, 107, 103, 91, 86, 161, 134, 149, 145, 145, 141, 136, 134, 133, 131, 128, 124, 121,
                115, 104, 99, 105, 90, 164, 149, 146, 146, 142, 140, 139, 139, 137, 132, 130, 128, 127, 123, 121,
                116, 112, 115, 118, 94, 92, 99, 132, 151, 158, 144, 146, 139, 139, 140, 135, 134, 133, 133, 131,
                130, 129, 126, 124, 124, 122, 122, 119, 114, 119, 119, 122, 118, 114, 119, 115, 219, 180, 140, 69,
                71, 116, 206, 184, 165, 129, 93, 73, 79, 193, 207, 165, 124, 99, 93, 90, 81, 198, 184, 160,
                133, 103, 73, 47, 210, 171, 168, 160, 153, 139, 100, 59, 54, 201, 188, 171, 155, 130, 103, 88,
                73, 53, 185, 179, 165, 149, 129, 107, 87, 64, 214, 185, 160, 143, 131, 114, 96, 68, 187, 174,
                153, 143, 134, 116, 94, 73, 51, 179, 169, 157, 147, 137, 126, 118, 111, 105, 98, 95, 153, 166,
                157, 146, 151, 149, 141, 137, 128, 117, 105, 97, 88, 170, 154, 147, 145, 144, 146, 142, 137, 127,
                125, 114, 96, 77, 138, 148, 154, 153, 145, 143, 136, 129, 124, 120, 115, 107, 100, 78, 112, 185,
                171, 143, 150, 151, 147, 146, 141, 135, 134, 128, 124, 115, 114, 108, 99, 102, 137, 160, 146, 147,
                144, 141, 143, 137, 136, 133, 130, 126, 123, 119, 115, 111, 108, 96, 88, 140, 163, 150, 146, 138,
                143, 136, 133, 134, 135, 132, 132, 131, 130, 130, 126, 127, 125, 127, 121, 118, 117, 114, 95, 112,
                238, 202, 129, 74, 52, 200, 188, 176, 130, 87, 81, 69, 215, 172, 119, 95, 94, 81, 224, 187,
                162, 133, 103, 86, 56, 205, 169, 162, 159, 138, 98, 63, 22, 217, 182, 159, 131, 108, 89, 64,
                181, 177, 156, 129, 107, 94, 81, 58, 187, 172, 148, 127, 108, 99, 99, 78, 175, 163, 160, 155,
                141, 131, 120, 107, 92, 82, 220, 172, 157, 156, 153, 142, 132, 117, 101, 84, 70, 42, 213, 186,
                168, 159, 151, 139, 127, 113, 94, 72, 46, 147, 192, 143, 157, 158, 157, 147, 142, 132, 123, 114,
                111, 118, 101, 82, 48, 197, 175, 168, 151, 144, 134, 127, 124, 121, 118, 116, 110, 116, 76, 124,
                150, 175, 139, 158, 137, 146, 141, 135, 141, 139, 138, 133, 131, 127, 122, 124, 119, 117, 118, 115,
                106, 102, 117, 181, 173, 161, 148, 149, 141, 140, 133, 131, 132, 126, 127, 123, 119, 122, 120, 123,
                133, 125, 83, 103, 167, 125, 140, 154, 152, 141, 139, 134, 136, 132, 130, 130, 127, 128, 124, 125,
                119, 124, 125, 123, 114, 113, 109, 86, 68, 171, 201, 183, 126, 76, 37, 237, 186, 164, 131, 95,
                68, 64, 187, 185, 162, 135, 100, 85, 73, 180, 162, 153, 129, 92, 61, 221, 192, 166, 130, 109,
                97, 97, 90, 175, 176, 158, 135, 113, 95, 85, 122, 181, 176, 161, 150, 134, 110, 90, 67, 180,
                173, 160, 150, 130, 117, 96, 84, 69, 157, 176, 163, 153, 146, 140, 119, 90, 65, 173, 167, 159,
                144, 131, 116, 106, 95, 85, 112, 152, 177, 162, 152, 149, 139, 129, 122, 115, 107, 104, 84, 151,
                164, 164, 154, 144, 136, 130, 121, 117, 114, 106, 101, 104, 96, 211, 179, 162, 154, 143, 133, 127,
                123, 119, 113, 117, 111, 108, 115, 121, 134, 68, 157, 148, 148, 146, 141, 141, 135, 131, 125, 119,
                116, 109, 102, 103, 82, 139, 159, 147, 140, 144, 135, 138, 136, 134, 133, 127, 128, 124, 123, 118,
                113, 119, 112, 107, 129, 168, 143, 151, 132, 142, 125, 130, 132, 134, 134, 137, 134, 130, 132, 130,
                128, 128, 123, 121, 119, 116, 121, 119, 111, 124, 110, 117, 96, 144, 200, 193, 130, 67, 45, 221,
                182, 118, 89, 90, 91, 208, 179, 173, 145, 87, 78, 67, 179, 179, 163, 159, 139, 87, 54, 33,
                158, 161, 158, 158, 155, 141, 85, 44, 208, 173, 154, 127, 105, 92, 75, 44, 160, 181, 170, 156,
                135, 113, 99, 86, 52, 195, 179, 164, 142, 126, 114, 98, 93, 90, 162, 189, 162, 159, 142, 128,
                119, 101, 87, 65, 161, 169, 165, 155, 141, 127, 115, 105, 92, 79, 77, 169, 170, 157, 150, 142,
                132, 122, 107, 98, 83, 109, 150, 169, 168, 159, 150, 142, 132, 123, 116, 108, 93, 95, 91, 200,
                170, 163, 157, 142, 135, 129, 124, 117, 113, 110, 106, 95, 87, 177, 158, 182, 150, 145, 138, 135,
                132, 129, 123, 116, 107, 109, 105, 92, 81, 154, 154, 136, 145, 140, 139, 134, 133, 132, 129, 127,
                126, 124, 122, 118, 117, 117, 104, 103, 87, 151, 148, 158, 139, 155, 133, 143, 149, 138, 142, 139,
                136, 135, 134, 132, 129, 127, 128, 127, 125, 131, 120, 118, 115, 100, 84, 228, 211, 126, 81, 72,
                95, 107, 230, 191, 117, 94, 89, 80, 181, 183, 161, 131, 95, 72, 88, 201, 182, 161, 129, 96,
                69, 65, 202, 180, 163, 132, 105, 90, 84, 57, 175, 186, 176, 159, 151, 132, 106, 91, 82, 88,
                188, 166, 161, 146, 133, 114, 99, 86, 88, 62, 155, 168, 165, 155, 142, 125, 111, 93, 92, 64,
                181, 170, 155, 149, 138, 129, 115, 100, 94, 89, 54, 183, 170, 162, 153, 140, 129, 119, 113, 110,
                97, 89, 20, 172, 135, 167, 155, 150, 146, 140, 132, 123, 115, 99, 90, 114, 200, 169, 157, 147,
                135, 128, 123, 121, 120, 118, 109, 102, 110, 125, 164, 161, 157, 154, 144, 140, 137, 134, 128, 122,
                121, 112, 109, 105, 98, 78, 204, 169, 140, 159, 149, 146, 145, 141, 139, 134, 131, 125, 119, 116,
                117, 116, 111, 115, 107, 135, 200, 174, 155, 154, 149, 142, 138, 133, 133, 128, 128, 124, 125, 124,
                127, 121, 118, 109, 126, 137, 126, 154, 91, 78, 165, 149, 147, 145, 151, 137, 138, 132, 140, 140,
                137, 136, 135, 131, 131, 127, 127, 127, 123, 120, 119, 111, 120, 123, 133, 111, 220, 205, 121, 80,
                80, 107, 215, 188, 165, 132, 97, 75, 66, 186, 175, 169, 157, 134, 98, 64, 35, 177, 167, 157,
                132, 100, 82, 66, 173, 182, 169, 152, 131, 110, 98, 87, 74, 207, 179, 150, 125, 114, 104, 100,
                101, 77, 192, 181, 165, 145, 130, 115, 100, 89, 82, 186, 185, 161, 142, 128, 117, 107, 95, 73,
                66, 198, 169, 166, 156, 144, 130, 114, 102, 83, 75, 172, 163, 160, 152, 144, 133, 118, 106, 97,
                86, 84, 173, 167, 161, 150, 140, 129, 119, 114, 112, 103, 95, 94, 99, 199, 170, 153, 142, 133,
                125, 118, 115, 114, 123, 102, 106, 109, 174, 166, 160, 152, 145, 138, 132, 124, 119, 116, 114, 107,
                93, 82, 118, 174, 145, 136, 154, 147, 148, 146, 140, 138, 133, 128, 122, 118, 113, 112, 111, 108,
                80, 155, 148, 162, 149, 146, 143, 139, 137, 135, 133, 128, 126, 123, 120, 117, 113, 113, 114, 90,
                168, 155, 154, 151, 146, 140, 134, 137, 135, 133, 132, 130, 125, 126, 125, 124, 123, 125, 121, 124
        };
    }

    /**
     * Returns chunk 3 of the ST2_ALL_QLVLS_8 table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVLS_8()} concatenates the chunks into {@link #ST2_ALL_QLVLS_8}.
     */
    private static int[] makeST2_ALL_QLVLS_8_3() {
        return new int[] {
                124, 122, 120, 123, 113, 110, 115, 101, 232, 228, 123, 66, 73, 218, 171, 124, 83, 81, 45, 217,
                182, 167, 135, 93, 82, 65, 200, 178, 174, 160, 132, 103, 86, 79, 50, 194, 170, 164, 151, 131,
                108, 94, 72, 51, 207, 170, 168, 162, 149, 134, 114, 89, 70, 193, 165, 146, 124, 111, 97, 81,
                67, 206, 176, 148, 134, 121, 99, 77, 58, 172, 167, 166, 160, 145, 131, 115, 104, 90, 83, 86,
                171, 180, 164, 147, 132, 121, 114, 102, 90, 183, 165, 154, 144, 132, 121, 113, 106, 94, 86, 133,
                178, 162, 156, 153, 148, 138, 131, 121, 110, 97, 90, 71, 137, 154, 160, 159, 157, 150, 140, 130,
                123, 115, 106, 107, 98, 86, 109, 132, 162, 154, 153, 148, 144, 140, 135, 130, 124, 118, 114, 110,
                107, 103, 99, 168, 143, 137, 153, 151, 144, 141, 139, 137, 133, 130, 128, 123, 124, 121, 120, 120,
                110, 110, 103, 128, 79, 122, 147, 152, 152, 149, 138, 144, 149, 143, 139, 140, 141, 137, 137, 135,
                133, 131, 129, 125, 121, 120, 117, 117, 109, 118, 115, 120, 112, 136, 46, 206, 124, 84, 74, 72,
                201, 184, 127, 85, 65, 34, 185, 173, 166, 134, 96, 89, 67, 195, 180, 159, 134, 109, 90, 74,
                86, 208, 195, 159, 132, 110, 101, 79, 62, 203, 179, 163, 150, 130, 111, 95, 64, 86, 193, 169,
                156, 147, 137, 118, 89, 73, 81, 180, 168, 161, 147, 131, 115, 99, 93, 65, 176, 186, 167, 154,
                143, 134, 122, 109, 103, 81, 70, 179, 176, 159, 148, 135, 128, 118, 110, 102, 93, 75, 165, 163,
                163, 148, 144, 148, 142, 132, 119, 106, 86, 70, 179, 168, 156, 147, 142, 137, 132, 125, 120, 112,
                107, 93, 73, 75, 166, 143, 144, 149, 142, 139, 139, 134, 132, 126, 120, 115, 102, 92, 83, 162,
                140, 140, 145, 146, 142, 139, 139, 137, 135, 131, 127, 124, 117, 110, 109, 105, 81, 68, 147, 173,
                147, 145, 145, 139, 138, 139, 136, 132, 132, 129, 128, 124, 122, 118, 120, 117, 116, 106, 88, 88,
                127, 144, 149, 140, 149, 143, 142, 139, 135, 134, 133, 129, 127, 128, 125, 125, 120, 121, 124, 120,
                124, 119, 119, 123, 105, 139, 99, 230, 194, 124, 77, 67, 99, 212, 193, 167, 125, 94, 77, 60,
                194, 176, 164, 133, 99, 82, 65, 199, 183, 157, 126, 101, 85, 75, 59, 181, 174, 155, 127, 105,
                88, 69, 92, 214, 179, 159, 127, 110, 99, 95, 78, 205, 194, 165, 148, 127, 106, 90, 81, 93,
                201, 178, 162, 147, 129, 111, 85, 64, 140, 149, 163, 164, 151, 132, 112, 101, 87, 54, 203, 183,
                168, 152, 143, 135, 113, 84, 63, 74, 159, 161, 168, 156, 146, 132, 120, 111, 107, 94, 69, 171,
                164, 156, 144, 139, 130, 123, 112, 109, 106, 88, 105, 119, 146, 170, 156, 156, 147, 139, 130, 127,
                122, 108, 109, 114, 96, 56, 166, 146, 145, 159, 148, 146, 139, 136, 133, 129, 128, 122, 114, 107,
                94, 119, 99, 169, 156, 150, 146, 143, 144, 137, 128, 127, 125, 122, 124, 120, 112, 109, 122, 114,
                118, 64, 161, 163, 151, 148, 138, 140, 133, 128, 132, 129, 129, 124, 127, 124, 123, 119, 124, 123,
                121, 124, 118, 152, 135, 140, 83, 118, 95, 215, 127, 73, 73, 68, 203, 164, 164, 166, 148, 74,
                36, 210, 180, 123, 97, 88, 96, 216, 175, 128, 107, 90, 77, 199, 175, 156, 132, 108, 90, 68,
                194, 174, 151, 127, 107, 91, 70, 76, 210, 187, 168, 148, 130, 112, 100, 85, 56, 193, 178, 162,
                148, 133, 114, 100, 84, 74, 173, 174, 166, 157, 147, 134, 119, 107, 94, 85, 68, 170, 158, 154,
                149, 140, 131, 117, 105, 94, 84, 168, 166, 160, 149, 139, 128, 115, 106, 97, 95, 87, 179, 164,
                157, 155, 146, 139, 132, 122, 114, 107, 102, 103, 95, 161, 171, 177, 151, 148, 139, 134, 131, 127,
                118, 110, 103, 98, 89, 160, 152, 157, 146, 144, 149, 141, 138, 134, 130, 124, 121, 120, 118, 112,
                115, 119, 127, 111, 124, 96, 160, 142, 139, 142, 144, 139, 140, 139, 134, 132, 129, 128, 125, 119,
                123, 120, 117, 118, 102, 96, 79, 142, 166, 156, 146, 144, 143, 141, 137, 137, 134, 136, 133, 134,
                132, 129, 129, 127, 128, 129, 128, 122, 121, 126, 113, 130, 123, 125, 136, 106, 111, 185, 183, 165,
                133, 97, 80, 65, 199, 176, 154, 127, 102, 83, 66, 193, 178, 157, 132, 109, 91, 83, 72, 158,
                172, 174, 162, 148, 128, 103, 71, 50, 179, 177, 165, 149, 128, 107, 91, 75, 93, 190, 165, 158,
                147, 133, 112, 87, 61, 181, 170, 155, 147, 133, 117, 98, 80, 72, 173, 130, 177, 166, 156, 146,
                129, 114, 106, 96, 93, 85, 181, 174, 165, 151, 141, 131, 118, 106, 96, 92, 75, 179, 166, 159,
                149, 141, 131, 117, 108, 102, 94, 82, 181, 157, 164, 156, 148, 140, 133, 121, 113, 106, 99, 94,
                95, 153, 155, 151, 163, 148, 149, 145, 138, 130, 123, 118, 114, 109, 107, 114, 104, 49, 163, 166,
                148, 153, 150, 149, 145, 138, 130, 130, 125, 121, 112, 101, 77, 112, 133, 146, 158, 157, 152, 143,
                143, 140, 137, 132, 126, 123, 118, 114, 106, 100, 100, 118, 157, 146, 145, 147, 142, 138, 137, 139,
                139, 136, 136, 135, 133, 131, 128, 127, 124, 122, 123, 122, 115, 113, 107, 102, 150, 143, 143, 123,
                146, 146, 141, 141, 142, 135, 139, 138, 139, 137, 135, 132, 131, 129, 128, 128, 126, 121, 123, 116,
                121, 118, 116, 131, 206, 199, 134, 59, 51, 195, 196, 130, 66, 72, 187, 192, 130, 73, 75, 185,
                187, 129, 77, 71, 90, 194, 179, 130, 81, 88, 95, 198, 176, 174, 131, 89, 85, 68, 206, 183,
                171, 129, 89, 74, 54, 189, 176, 171, 173, 129, 82, 61, 49, 199, 179, 164, 129, 97, 89, 67,
                186, 180, 162, 134, 103, 86, 77, 192, 170, 170, 157, 130, 105, 96, 93, 89, 177, 170, 162, 154,
                132, 111, 100, 85, 67, 199, 160, 158, 150, 128, 117, 109, 109, 104, 105, 106, 138, 179, 151, 156,
                151, 148, 144, 136, 128, 116, 111, 104, 103, 96, 69, 192, 166, 139, 138, 139, 137, 132, 127, 128,
                124, 122, 119, 123, 122, 124, 112, 117, 90, 112, 94, 122, 143, 130, 119, 153, 140, 142, 131, 139,
                145, 147, 136, 134, 135, 135, 132, 128, 126, 123, 123, 123, 122, 112, 109, 112, 116, 95, 211, 135,
                61, 84, 121, 211, 186, 179, 133, 81, 69, 35, 201, 181, 131, 96, 82, 56, 202, 172, 128, 102,
                99, 96, 55, 191, 187, 158, 133, 107, 88, 75, 58, 204, 181, 166, 154, 135, 106, 81, 65, 192,
                188, 168, 149, 132, 113, 101, 86, 69, 208, 179, 163, 144, 129, 113, 101, 81, 81, 150, 175, 163,
                162, 150, 134, 117, 106, 100, 93, 109, 146, 186, 169, 158, 154, 141, 129, 117, 105, 94, 85, 75,
                177, 160, 156, 162, 154, 147, 142, 133, 119, 102, 85, 60, 160, 170, 170, 162, 150, 139, 133, 127,
                118, 111, 102, 78, 172, 164, 159, 149, 146, 137, 129, 123, 118, 113, 102, 97, 91, 81, 171, 157,
                149, 143, 138, 133, 125, 122, 116, 116, 113, 104, 115, 81, 79, 164, 131, 146, 144, 141, 142, 135,
                140, 136, 134, 134, 127, 128, 125, 120, 123, 113, 118, 114, 94, 117, 108, 147, 119, 166, 163, 132,
                143, 147, 153, 137, 142, 137, 139, 133, 135, 135, 135, 128, 129, 128, 127, 122, 117, 125, 123, 120,
                113, 116, 129, 129, 122, 132, 115, 136, 133, 130, 118, 67, 183, 180, 186, 129, 53, 48, 199, 182,
                130, 79, 60, 205, 177, 131, 85, 67, 66, 189, 183, 173, 132, 86, 71, 58, 172, 163, 179, 166,
                132, 92, 75, 66, 142, 174, 171, 162, 135, 96, 77, 60, 209, 182, 162, 129, 97, 90, 84, 94,
                179, 173, 160, 132, 105, 85, 85, 69, 162, 169, 163, 151, 130, 106, 81, 48, 170, 169, 165, 151,
                131, 112, 97, 95, 105, 78, 132, 143, 163, 155, 151, 144, 133, 108, 85, 65, 166, 161, 160, 151,
                144, 134, 124, 108, 98, 91, 89, 192, 153, 166, 152, 146, 138, 127, 117, 114, 105, 93, 70, 171,
                166, 156, 154, 149, 143, 136, 128, 124, 122, 116, 110, 113, 101, 97, 84, 181, 138, 154, 157, 151,
                142, 141, 139, 136, 132, 128, 126, 125, 121, 123, 121, 114, 109, 110, 117, 117, 109, 155, 153, 145,
                141, 137, 137, 132, 132, 129, 129, 127, 123, 126, 125, 127, 129, 127, 126, 122, 122, 123, 122, 127,
                117, 119, 121, 110, 104, 107, 97, 72, 90, 220, 220, 128, 58, 70, 85, 192, 192, 187, 128, 85,
                83, 81, 89, 186, 185, 182, 171, 133, 89, 71, 60, 190, 187, 171, 129, 92, 81, 79, 80, 175,
                178, 175, 165, 133, 91, 79, 71, 65, 191, 185, 179, 167, 131, 96, 81, 76, 76, 186, 176, 175,
                163, 132, 96, 80, 73, 66, 198, 190, 178, 162, 130, 100, 86, 79, 68, 181, 178, 172, 159, 132,
                102, 87, 76, 78, 179, 178, 172, 157, 132, 105, 90, 82, 77, 182, 175, 167, 154, 130, 105, 91,
                80, 75, 194, 177, 174, 166, 153, 130, 107, 96, 88, 82, 68, 183, 177, 171, 163, 151, 131, 110,
                98, 91, 86, 85, 170, 167, 164, 159, 148, 131, 114, 104, 99, 97, 88, 94, 156, 161, 160, 156,
                155, 150, 141, 129, 119, 112, 108, 108, 106, 106, 103, 101, 144, 141, 148, 149, 147, 147, 148, 149,
                146, 139, 130, 122, 115, 112, 110, 112, 111, 110, 105, 93, 177, 177, 193, 133, 10, 241, 203, 129,
                65, 42, 211, 187, 130, 60, 22, 243, 196, 125, 69, 51, 239, 201, 128, 81, 54, 207, 186, 134,
                71, 31, 223, 194, 173, 137, 76, 40, 227, 195, 170, 130, 92, 51, 204, 179, 140, 100, 69, 69,
                167, 186, 165, 135, 97, 71, 69, 189, 183, 165, 155, 137, 112, 76, 65, 195, 171, 164, 152, 134,
                110, 95, 86, 177, 173, 158, 143, 133, 120, 109, 97, 87, 149, 191, 164, 143, 128, 116, 109, 110,
                119, 102, 62, 181, 173, 154, 151, 142, 136, 130, 121, 118, 108, 99, 79, 107, 91, 187, 165, 152,
                146, 148, 139, 135, 134, 131, 129, 122, 113, 110, 119, 123, 111, 67, 212, 145, 28, 190, 186, 143,
                22, 210, 203, 131, 49, 245, 210, 119, 80, 67, 71, 201, 176, 131, 57, 16, 211, 182, 131, 70,
                35, 210, 177, 129, 84, 56, 243, 188, 165, 135, 95, 58, 48, 205, 187, 159, 128, 94, 82, 74,
                207, 188, 158, 132, 106, 81, 63, 223, 182, 149, 133, 109, 71, 39, 217, 181, 167, 151, 131, 109,
                91, 63, 153, 191, 166, 157, 147, 127, 116, 102, 83, 65, 162, 163, 154, 145, 138, 132, 121, 106,
                83, 83, 50, 193, 167, 150, 150, 144, 140, 130, 124, 118, 104, 96, 86, 71, 150, 170, 163, 152,
                146, 143, 138, 135, 130, 125, 122, 119, 119, 113, 111, 97, 107, 68, 221, 140, 35, 44, 236, 200,
                128, 55, 74, 232, 213, 124, 82, 75, 212, 189, 135, 64, 25, 215, 188, 168, 143, 51, 22, 204,
                182, 128, 74, 44, 199, 177, 128, 70, 42, 218, 174, 132, 82, 37, 205, 163, 135, 85, 44, 204,
                183, 158, 125, 102, 80, 75, 195, 169, 167, 156, 139, 112, 81, 49, 176, 160, 161, 155, 148, 128,
                108, 67, 48, 180, 176, 163, 147, 127, 114, 100, 84, 71, 182, 168, 157, 154, 143, 133, 118, 100,
                93, 69, 140, 160, 156, 152, 144, 132, 124, 113, 98, 92, 53, 153, 153, 163, 147, 143, 140, 136
        };
    }

    /**
     * Returns chunk 4 of the ST2_ALL_QLVLS_8 table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVLS_8()} concatenates the chunks into {@link #ST2_ALL_QLVLS_8}.
     */
    private static int[] makeST2_ALL_QLVLS_8_4() {
        return new int[] {
                137, 130, 128, 124, 119, 112, 98, 81, 237, 128, 31, 235, 208, 129, 58, 31, 223, 116, 73, 51,
                236, 201, 135, 67, 34, 197, 179, 139, 51, 227, 199, 131, 77, 33, 233, 193, 127, 76, 61, 221,
                187, 125, 82, 60, 218, 189, 165, 133, 96, 60, 30, 169, 168, 165, 159, 130, 94, 70, 41, 233,
                195, 173, 145, 111, 77, 40, 190, 177, 171, 166, 156, 133, 105, 97, 82, 53, 201, 179, 148, 126,
                115, 101, 88, 64, 36, 193, 156, 149, 152, 146, 148, 140, 128, 120, 113, 107, 111, 70, 75, 206,
                182, 157, 145, 139, 130, 122, 117, 110, 110, 109, 75, 140, 149, 171, 153, 144, 138, 131, 130, 128,
                120, 116, 111, 107, 94, 60, 208, 223, 126, 30, 254, 203, 131, 51, 22, 223, 202, 135, 65, 16,
                203, 181, 126, 45, 241, 206, 130, 80, 70, 215, 191, 137, 83, 54, 204, 178, 138, 77, 37, 202,
                173, 129, 84, 52, 201, 186, 168, 144, 81, 38, 202, 174, 134, 97, 75, 68, 179, 179, 158, 131,
                104, 87, 79, 182, 178, 174, 153, 131, 112, 99, 94, 93, 205, 178, 151, 131, 117, 105, 105, 102,
                91, 154, 167, 155, 143, 133, 112, 99, 87, 84, 142, 167, 149, 152, 147, 135, 130, 123, 115, 100,
                93, 94, 152, 164, 149, 134, 140, 138, 149, 142, 141, 134, 126, 119, 113, 113, 107, 93, 107, 130,
                141, 227, 130, 27, 229, 117, 62, 58, 221, 199, 146, 52, 28, 211, 182, 139, 41, 9, 190, 177,
                174, 145, 37, 222, 191, 126, 73, 36, 208, 187, 137, 83, 52, 220, 174, 123, 85, 64, 42, 197,
                192, 170, 127, 94, 57, 199, 188, 165, 126, 92, 63, 58, 206, 182, 157, 133, 98, 71, 59, 192,
                184, 160, 134, 111, 92, 68, 197, 174, 148, 129, 111, 99, 85, 78, 181, 198, 164, 143, 134, 120,
                104, 91, 75, 68, 181, 159, 151, 141, 134, 128, 123, 114, 114, 105, 91, 79, 171, 198, 142, 154,
                153, 146, 140, 136, 131, 125, 124, 117, 111, 104, 65, 247, 130, 45, 96, 236, 117, 70, 46, 228,
                199, 132, 57, 28, 221, 198, 129, 55, 19, 228, 200, 134, 72, 56, 202, 201, 177, 131, 76, 56,
                199, 174, 134, 87, 63, 40, 181, 190, 165, 123, 79, 54, 212, 188, 157, 127, 88, 71, 27, 206,
                186, 163, 129, 104, 84, 65, 177, 184, 171, 155, 132, 106, 75, 61, 216, 186, 150, 125, 114, 109,
                92, 82, 43, 192, 177, 161, 146, 135, 117, 105, 92, 74, 79, 167, 170, 165, 154, 142, 130, 117,
                111, 103, 99, 94, 208, 171, 156, 145, 136, 127, 122, 122, 115, 113, 115, 124, 100, 111, 180, 153,
                155, 145, 138, 143, 142, 138, 131, 128, 121, 115, 110, 93, 102, 235, 127, 49, 79, 241, 204, 133,
                59, 21, 207, 185, 136, 53, 211, 189, 133, 65, 34, 200, 182, 131, 83, 57, 236, 191, 122, 92,
                80, 63, 215, 177, 131, 89, 60, 217, 174, 126, 93, 63, 27, 210, 191, 167, 130, 88, 60, 206,
                184, 164, 133, 95, 68, 65, 202, 185, 156, 127, 102, 88, 79, 71, 200, 161, 134, 112, 105, 106,
                88, 192, 172, 153, 134, 113, 97, 92, 85, 183, 161, 163, 158, 145, 133, 118, 104, 87, 84, 168,
                173, 161, 152, 149, 140, 132, 123, 114, 102, 99, 59, 164, 162, 155, 143, 140, 134, 128, 124, 122,
                120, 120, 120, 113, 112, 101, 91, 249, 128, 39, 216, 120, 56, 24, 226, 204, 135, 58, 29, 211,
                191, 132, 69, 47, 200, 183, 131, 77, 44, 201, 186, 174, 136, 78, 38, 218, 179, 123, 83, 50,
                236, 188, 138, 90, 48, 200, 192, 173, 131, 92, 68, 44, 206, 175, 135, 107, 81, 50, 235, 198,
                166, 130, 105, 83, 62, 189, 167, 157, 130, 100, 74, 61, 194, 179, 175, 159, 129, 110, 85, 76,
                154, 189, 165, 158, 144, 129, 110, 96, 94, 69, 152, 173, 160, 157, 147, 139, 129, 118, 117, 111,
                101, 109, 89, 127, 162, 137, 140, 145, 150, 150, 142, 138, 133, 124, 114, 107, 103, 111, 115, 131,
                249, 127, 49, 45, 223, 127, 49, 219, 195, 134, 68, 49, 228, 199, 135, 75, 46, 243, 205, 134,
                83, 54, 209, 177, 131, 80, 37, 196, 170, 139, 78, 41, 198, 170, 130, 86, 57, 194, 196, 165,
                136, 102, 79, 58, 212, 184, 154, 124, 97, 80, 50, 207, 175, 157, 158, 133, 91, 50, 173, 192,
                168, 151, 135, 115, 95, 68, 65, 202, 164, 159, 148, 144, 131, 114, 94, 67, 174, 172, 172, 155,
                149, 141, 131, 117, 105, 92, 58, 183, 175, 152, 152, 145, 135, 131, 125, 116, 112, 104, 99, 95,
                149, 164, 162, 150, 140, 136, 129, 125, 119, 115, 117, 121, 113, 109, 91, 82, 232, 127, 41, 43,
                241, 208, 123, 59, 40, 223, 199, 134, 66, 36, 224, 199, 125, 66, 61, 215, 189, 126, 73, 49,
                240, 198, 130, 86, 72, 37, 224, 180, 130, 74, 60, 210, 177, 129, 74, 32, 181, 195, 181, 132,
                88, 58, 223, 194, 160, 131, 73, 40, 208, 192, 170, 133, 97, 82, 67, 192, 184, 154, 128, 111,
                99, 70, 103, 210, 177, 151, 130, 117, 98, 94, 65, 230, 172, 158, 156, 145, 133, 123, 108, 86,
                59, 204, 171, 160, 156, 137, 127, 118, 112, 102, 109, 98, 175, 174, 149, 137, 134, 130, 121, 117,
                113, 122, 107, 112, 134, 121, 104, 54, 250, 129, 40, 44, 203, 189, 148, 33, 222, 123, 67, 66,
                224, 130, 74, 43, 225, 195, 133, 71, 36, 224, 187, 126, 75, 47, 221, 178, 129, 84, 47, 207,
                177, 133, 86, 58, 236, 193, 170, 137, 95, 67, 52, 186, 175, 159, 131, 94, 63, 199, 184, 156,
                125, 93, 71, 79, 194, 184, 173, 154, 133, 108, 91, 77, 91, 185, 186, 165, 144, 133, 114, 94,
                79, 207, 170, 159, 152, 142, 131, 121, 112, 105, 108, 104, 94, 158, 158, 152, 148, 146, 134, 129,
                121, 113, 110, 104, 86, 96, 176, 162, 149, 140, 141, 136, 134, 130, 128, 123, 121, 116, 113, 107,
                108, 113, 114, 229, 204, 134, 57, 29, 223, 192, 127, 67, 37, 228, 194, 132, 75, 54, 211, 201,
                179, 128, 62, 18, 206, 179, 128, 76, 39, 198, 169, 135, 73, 31, 204, 172, 136, 87, 53, 177,
                190, 169, 130, 93, 75, 62, 213, 198, 162, 130, 94, 73, 44, 220, 183, 161, 132, 98, 79, 53,
                187, 173, 157, 133, 106, 88, 68, 171, 176, 161, 150, 129, 111, 99, 94, 84, 190, 187, 170, 160,
                140, 128, 115, 89, 56, 169, 179, 162, 166, 155, 146, 135, 121, 107, 94, 95, 93, 155, 157, 153,
                143, 145, 143, 139, 132, 125, 121, 114, 109, 89, 82, 171, 137, 153, 156, 140, 147, 147, 148, 141,
                136, 129, 124, 119, 112, 107, 97, 67, 227, 134, 26, 225, 129, 39, 220, 130, 44, 214, 130, 51,
                75, 210, 130, 56, 74, 213, 203, 131, 61, 74, 220, 196, 128, 64, 66, 181, 201, 197, 128, 52,
                29, 225, 196, 128, 68, 41, 203, 195, 136, 66, 33, 196, 185, 130, 80, 79, 192, 180, 134, 87,
                55, 192, 171, 129, 96, 87, 85, 48, 154, 173, 172, 163, 151, 126, 95, 88, 74, 232, 196, 146,
                144, 132, 125, 113, 113, 110, 105, 110, 81, 193, 145, 159, 155, 139, 146, 151, 145, 139, 139, 126,
                122, 122, 114, 101, 94, 52, 244, 132, 33, 63, 236, 210, 134, 47, 221, 131, 60, 20, 219, 128,
                77, 55, 235, 199, 134, 69, 54, 209, 182, 136, 66, 37, 217, 182, 131, 83, 50, 213, 175, 128,
                85, 54, 79, 204, 197, 176, 135, 91, 86, 79, 209, 182, 165, 127, 94, 67, 65, 201, 179, 172,
                158, 134, 92, 49, 189, 187, 156, 132, 114, 92, 52, 189, 195, 172, 152, 128, 111, 96, 83, 51,
                182, 176, 160, 143, 124, 109, 97, 86, 74, 145, 167, 159, 149, 147, 137, 128, 121, 112, 102, 100,
                75, 87, 173, 134, 148, 155, 160, 151, 143, 135, 129, 128, 127, 124, 117, 119, 109, 120, 127, 126,
                145, 125, 87, 207, 212, 127, 25, 220, 130, 40, 216, 130, 46, 41, 201, 207, 132, 51, 34, 202,
                199, 132, 54, 42, 200, 195, 135, 61, 43, 216, 195, 128, 65, 55, 216, 194, 133, 72, 47, 200,
                179, 130, 68, 26, 171, 191, 178, 131, 85, 75, 45, 180, 172, 160, 134, 68, 30, 194, 182, 165,
                138, 101, 73, 71, 176, 172, 155, 124, 103, 74, 42, 189, 174, 165, 149, 127, 118, 101, 93, 89,
                61, 173, 158, 161, 155, 147, 139, 128, 124, 118, 110, 101, 92, 95, 53, 163, 152, 139, 137, 130,
                128, 123, 125, 125, 124, 121, 119, 116, 110, 116, 95, 104, 113, 156, 210, 226, 133, 15, 221, 220,
                128, 58, 51, 227, 208, 129, 60, 50, 225, 207, 128, 60, 44, 215, 202, 129, 66, 55, 219, 199,
                128, 65, 50, 219, 198, 130, 67, 50, 215, 194, 130, 66, 43, 208, 190, 131, 71, 51, 217, 206,
                185, 130, 74, 53, 202, 205, 186, 132, 80, 60, 42, 212, 198, 180, 131, 81, 61, 51, 203, 196,
                176, 130, 85, 66, 54, 194, 188, 170, 131, 92, 77, 67, 185, 179, 172, 158, 129, 103, 92, 88,
                82, 161, 155, 161, 162, 161, 152, 129, 108, 98, 96, 95, 93
        };
    }

    /**
     * Returns the ST2_ALL_QLVL_DCMFS table by concatenating its literal chunks.
     *
     * <p>Initializes {@link #ST2_ALL_QLVL_DCMFS}; see {@link #concat(int[]...)}.
     */
    private static int[] makeST2_ALL_QLVL_DCMFS() {
        return concat(makeST2_ALL_QLVL_DCMFS_0(), makeST2_ALL_QLVL_DCMFS_1(), makeST2_ALL_QLVL_DCMFS_2(), makeST2_ALL_QLVL_DCMFS_3(), makeST2_ALL_QLVL_DCMFS_4());
    }

    /**
     * Returns chunk 0 of the ST2_ALL_QLVL_DCMFS table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVL_DCMFS()} concatenates the chunks into {@link #ST2_ALL_QLVL_DCMFS}.
     */
    private static int[] makeST2_ALL_QLVL_DCMFS_0() {
        return new int[] {
                10, 45, 124, 255, 153, 25, 7, 17, 62, 176, 255, 176, 74, 19, 5, 15, 75, 185, 255, 192,
                80, 21, 5, 28, 91, 194, 255, 187, 85, 24, 6, 8, 31, 83, 188, 255, 188, 87, 30, 10,
                3, 6, 28, 85, 191, 255, 200, 102, 43, 15, 5, 9, 43, 124, 226, 255, 202, 110, 38, 9,
                14, 45, 113, 208, 255, 206, 126, 64, 22, 10, 11, 26, 66, 138, 219, 255, 216, 136, 70, 28,
                7, 9, 19, 39, 80, 153, 227, 255, 234, 153, 87, 34, 14, 12, 25, 52, 102, 167, 226, 255,
                234, 173, 105, 62, 33, 14, 11, 7, 14, 31, 62, 122, 188, 244, 255, 226, 175, 123, 82, 50,
                37, 22, 16, 13, 10, 9, 18, 45, 88, 107, 157, 211, 255, 248, 203, 152, 98, 53, 25, 12,
                20, 34, 58, 84, 129, 170, 212, 255, 251, 230, 192, 151, 103, 69, 45, 29, 12, 6, 6, 7,
                10, 15, 24, 38, 57, 81, 115, 140, 171, 210, 236, 241, 255, 248, 220, 204, 174, 144, 107, 83,
                67, 48, 33, 21, 13, 8, 14, 14, 16, 22, 36, 47, 58, 65, 89, 106, 129, 159, 184, 209,
                215, 229, 233, 243, 250, 255, 252, 235, 216, 186, 159, 145, 128, 108, 90, 73, 49, 28, 20, 16,
                14, 13, 92, 255, 86, 25, 25, 121, 255, 117, 25, 38, 97, 255, 138, 27, 19, 54, 165, 255,
                142, 42, 13, 42, 174, 255, 162, 65, 22, 19, 56, 181, 255, 141, 47, 32, 13, 16, 16, 14,
                65, 190, 255, 181, 78, 33, 28, 83, 174, 255, 194, 82, 30, 21, 15, 15, 43, 90, 181, 255,
                189, 86, 24, 32, 53, 141, 206, 255, 245, 164, 54, 34, 48, 109, 194, 255, 222, 131, 53, 20,
                28, 42, 121, 175, 240, 255, 207, 142, 112, 76, 34, 28, 65, 116, 191, 255, 237, 202, 132, 107,
                72, 49, 19, 19, 28, 54, 82, 131, 219, 243, 255, 206, 200, 175, 126, 82, 63, 32, 22, 22,
                34, 28, 49, 72, 108, 141, 198, 200, 255, 247, 253, 238, 238, 179, 144, 110, 119, 80, 44, 19,
                28, 28, 67, 110, 193, 232, 249, 255, 213, 194, 186, 121, 73, 61, 102, 98, 110, 119, 84, 91,
                140, 102, 106, 70, 70, 67, 40, 79, 61, 73, 108, 64, 45, 28, 18, 125, 255, 118, 18, 27,
                137, 255, 132, 23, 8, 37, 153, 255, 139, 22, 5, 36, 149, 255, 162, 50, 10, 10, 46, 156,
                255, 161, 48, 6, 8, 47, 162, 255, 165, 58, 14, 3, 20, 69, 174, 255, 174, 57, 13, 4,
                17, 75, 186, 255, 177, 70, 15, 5, 21, 69, 182, 255, 189, 79, 20, 6, 8, 34, 99, 204,
                255, 183, 86, 31, 11, 7, 49, 126, 219, 255, 181, 76, 28, 10, 14, 48, 108, 205, 255, 201,
                117, 54, 24, 11, 8, 17, 39, 90, 160, 230, 255, 207, 124, 54, 15, 8, 17, 26, 48, 88,
                154, 221, 255, 224, 147, 81, 41, 20, 10, 10, 25, 41, 71, 113, 155, 201, 243, 255, 241, 212,
                168, 128, 87, 55, 30, 17, 28, 44, 75, 101, 128, 149, 175, 207, 228, 244, 253, 249, 255, 243,
                207, 158, 104, 55, 25, 11, 13, 48, 152, 255, 167, 34, 18, 49, 155, 255, 161, 44, 8, 11,
                66, 183, 255, 164, 55, 17, 16, 61, 179, 255, 189, 84, 26, 6, 22, 77, 180, 255, 182, 85,
                28, 9, 10, 38, 90, 180, 255, 190, 80, 25, 7, 25, 88, 184, 255, 214, 106, 34, 13, 13,
                23, 57, 111, 202, 255, 186, 73, 26, 11, 25, 60, 128, 210, 255, 216, 111, 43, 11, 7, 13,
                46, 103, 194, 255, 239, 164, 82, 33, 7, 13, 30, 83, 165, 246, 255, 206, 154, 100, 47, 25,
                10, 10, 17, 30, 70, 124, 190, 234, 255, 228, 171, 120, 76, 54, 29, 13, 10, 14, 18, 52,
                76, 126, 179, 217, 239, 254, 255, 220, 144, 88, 41, 13, 11, 20, 32, 43, 64, 83, 119, 171,
                212, 250, 255, 224, 188, 156, 135, 97, 63, 38, 27, 13, 20, 21, 33, 38, 68, 91, 121, 163,
                192, 225, 238, 255, 236, 221, 207, 176, 155, 103, 74, 48, 30, 16, 11, 29, 40, 67, 88, 112,
                142, 181, 213, 232, 237, 230, 255, 244, 230, 219, 220, 199, 167, 149, 138, 121, 119, 84, 82, 58,
                33, 34, 22, 4, 92, 255, 85, 6, 8, 93, 255, 89, 3, 17, 105, 255, 97, 7, 8, 106,
                255, 104, 18, 15, 114, 255, 117, 22, 8, 21, 135, 255, 127, 27, 7, 7, 36, 143, 255, 136,
                25, 24, 134, 255, 157, 34, 5, 11, 50, 152, 255, 157, 43, 6, 4, 19, 64, 167, 255, 157,
                52, 13, 15, 64, 171, 255, 170, 57, 14, 17, 68, 175, 255, 181, 84, 34, 10, 5, 13, 39,
                100, 189, 255, 235, 122, 54, 25, 15, 9, 12, 25, 57, 106, 169, 214, 255, 215, 110, 30, 26,
                55, 82, 96, 156, 212, 255, 236, 180, 134, 93, 57, 29, 12, 18, 29, 58, 97, 144, 202, 249,
                255, 251, 235, 211, 165, 117, 84, 61, 76, 90, 31, 10, 49, 153, 255, 166, 44, 5, 10, 48,
                164, 255, 156, 51, 14, 15, 60, 168, 255, 178, 63, 14, 17, 66, 182, 255, 168, 67, 20, 5,
                5, 27, 79, 185, 255, 182, 75, 21, 4, 18, 75, 177, 255, 194, 91, 30, 8, 26, 83, 189,
                255, 195, 90, 32, 11, 6, 16, 50, 126, 219, 255, 201, 101, 36, 10, 7, 19, 45, 102, 187,
                255, 230, 125, 44, 10, 10, 27, 63, 127, 207, 255, 213, 128, 64, 27, 10, 4, 11, 22, 46,
                77, 137, 212, 255, 219, 142, 78, 41, 18, 8, 5, 6, 8, 13, 29, 50, 88, 150, 229, 255,
                205, 139, 82, 44, 22, 10, 6, 6, 13, 27, 48, 86, 133, 174, 216, 255, 244, 178, 90, 35,
                9, 6, 6, 8, 11, 23, 40, 76, 126, 183, 236, 255, 228, 186, 140, 89, 51, 33, 19, 11,
                7, 5, 6, 12, 15, 18, 32, 39, 64, 82, 114, 142, 177, 205, 235, 255, 245, 215, 179, 146,
                117, 82, 57, 37, 22, 16, 11, 8, 10, 18, 27, 34, 47, 49, 67, 87, 118, 154, 186, 217,
                238, 255, 252, 228, 195, 167, 126, 102, 81, 54, 39, 23, 16, 9, 14, 65, 255, 52, 77, 255,
                82, 4, 2, 81, 255, 80, 82, 255, 84, 3, 4, 90, 255, 87, 3, 5, 89, 255, 90, 6,
                10, 96, 255, 92, 4, 8, 97, 255, 97, 12, 7, 105, 255, 100, 16, 14, 106, 255, 108, 11,
                16, 117, 255, 112, 19, 4, 6, 30, 130, 255, 108, 20, 8, 35, 121, 255, 147, 36, 6, 19,
                69, 182, 255, 134, 35, 11, 11, 16, 95, 241, 255, 195, 144, 107, 35, 8, 10, 22, 49, 147,
                255, 208, 153, 115, 87, 65, 44, 35, 31, 38, 16, 26, 139, 255, 135, 25, 6, 35, 147, 255,
                149, 33, 4, 7, 38, 150, 255, 152, 42, 8, 9, 50, 152, 255, 178, 52, 8, 10, 54, 168,
                255, 159, 48, 11, 16, 64, 175, 255, 167, 57, 14, 12, 56, 168, 255, 188, 68, 13, 6, 21,
                68, 175, 255, 186, 77, 24, 5, 7, 33, 105, 213, 255, 192, 96, 35, 9, 13, 38, 104, 187,
                255, 241, 129, 31, 5, 9, 25, 62, 132, 219, 255, 200, 109, 48, 20, 9, 16, 37, 76, 143,
                217, 255, 236, 172, 101, 46, 19, 9, 13, 19, 41, 81, 147, 218, 255, 249, 218, 170, 117, 70,
                35, 14, 6, 8, 16, 30, 60, 99, 153, 213, 249, 255, 234, 188, 134, 86, 48, 28, 13, 17,
                32, 45, 75, 107, 145, 192, 232, 248, 255, 254, 241, 218, 178, 132, 88, 56, 26, 11, 25, 49,
                65, 84, 94, 111, 127, 141, 158, 173, 186, 192, 208, 219, 240, 248, 255, 255, 236, 215, 185, 150,
                105, 69, 38, 28, 14, 5, 31, 144, 255, 143, 33, 4, 8, 45, 155, 255, 151, 40, 6, 4,
                46, 176, 255, 154, 47, 9, 15, 59, 165, 255, 170, 56, 12, 8, 26, 72, 170, 255, 180, 66,
                14, 21, 68, 175, 255, 184, 81, 27, 7, 9, 27, 77, 174, 255, 197, 82, 23, 6, 7, 28,
                89, 192, 255, 199, 95, 34, 10, 10, 42, 118, 211, 255, 201, 104, 35, 11, 18, 54, 125, 217,
                255, 201, 109, 43, 14, 5, 15, 29, 68, 138, 221, 255, 206, 123, 61, 28, 14, 10, 22, 44,
                91, 161, 228, 255, 223, 153, 85, 43, 21, 8, 4, 8, 15, 33, 66, 119, 173, 225, 255, 253,
                209, 143, 84, 46, 24, 12, 6, 7, 18, 36, 66, 108, 161, 205, 239, 255, 242, 197, 133, 80,
                35, 12, 14, 26, 38, 55, 79, 112, 151, 188, 218, 241, 255, 247, 227, 203, 170, 139, 100, 70,
                39, 21, 32, 44, 67, 93, 126, 160, 195, 229, 245, 254, 255, 252, 235, 210, 197, 179, 160, 146,
                133, 116, 107, 85, 64, 51, 37, 28, 23, 15, 12, 7, 76, 255, 93, 9, 19, 108, 255, 118,
                9, 12, 108, 255, 132, 25, 7, 30, 132, 255, 131, 14, 10, 48, 168, 255, 135, 22, 7, 36,
                151, 255, 160, 43, 10, 9, 48, 163, 255, 153, 40, 11, 4, 11, 48, 169, 255, 174, 56, 11,
                21, 75, 196, 255, 169, 70, 16, 9, 27, 97, 206, 255, 164, 60, 15, 19, 59, 131, 224, 255,
                197, 104, 44, 13, 10, 18, 53, 106, 188, 255, 247, 169, 88, 47, 18, 7, 9, 14, 28, 52,
                90, 154, 219, 255, 237, 171, 101, 55, 33, 21, 10, 14, 26, 53, 79, 141, 207, 247, 255, 241,
                198, 145, 88, 52, 26, 14, 17, 28, 31, 69, 108, 142, 193, 228, 255, 246, 224, 201, 165, 146,
                138, 117, 97, 72, 51, 39, 22, 12, 24, 54, 86, 106, 135, 155, 161, 173, 177, 196, 206, 220,
                229, 227, 237, 241, 243, 235, 239, 255, 227, 196, 164, 135, 105, 86, 62, 41, 23, 6, 92, 255,
                91, 6, 4, 92, 255, 94, 8, 6, 93, 255, 94, 8, 8, 95, 255, 96, 8, 6, 100, 255,
                104, 7, 6, 100, 255, 106, 10, 11, 108, 255, 103, 15, 21, 114, 255, 117, 15, 20, 128, 255,
                123, 23, 21, 139, 255, 125, 33, 9, 27, 136, 255, 138, 33, 4, 33, 147, 255, 142, 42, 10,
                19, 67, 184, 255, 145, 54, 14, 9, 18, 45, 115, 219, 255, 169, 92, 36, 8, 14, 63, 165,
                239, 255, 249, 151, 89, 58, 62, 59, 33, 18, 11, 8, 21, 42, 82, 88, 103, 171, 234, 248,
                221, 255, 241, 142, 112, 93, 85, 52, 29, 11, 7, 15, 124, 255, 125, 17, 21, 131, 255, 126,
                22, 22, 143, 255, 139, 34, 4, 7, 40, 144, 255, 148, 36, 6, 8, 42, 155, 255, 158, 42,
                8, 12, 54, 169, 255, 167, 57, 13, 13, 61, 173, 255, 168, 60, 12, 13, 59, 172, 255, 198,
                79, 22, 8, 5, 28, 92, 193, 255, 193, 78, 19, 5, 16, 75, 178, 255, 199, 99, 40, 14,
                8, 39, 93, 193, 255, 222, 136, 60, 27, 9, 7, 19, 36, 73, 141, 218, 255, 241, 185, 117,
                51, 15, 23, 59, 116, 171, 207, 232, 255, 232, 169, 96, 41, 7, 11, 31, 67, 121, 166, 218,
                248, 255, 248, 218, 172, 122, 77, 44, 25, 20, 11, 13, 18, 35, 66, 119, 169, 210, 243, 255,
                251, 253, 238, 214, 174, 139, 97, 66, 43, 26, 23, 22, 14, 11, 12, 9, 17, 35, 55, 72,
                86, 94, 93, 103, 115, 145, 165, 192, 210, 228, 240, 248, 255, 255, 244, 237, 239, 218, 201, 181,
                156, 111, 79, 45, 24, 14, 116, 255, 123, 14, 12, 129, 255, 129, 20, 4, 28, 128, 255, 136
        };
    }

    /**
     * Returns chunk 1 of the ST2_ALL_QLVL_DCMFS table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVL_DCMFS()} concatenates the chunks into {@link #ST2_ALL_QLVL_DCMFS}.
     */
    private static int[] makeST2_ALL_QLVL_DCMFS_1() {
        return new int[] {
                27, 4, 23, 144, 255, 140, 29, 20, 146, 255, 150, 42, 6, 5, 39, 156, 255, 160, 40, 8,
                7, 45, 163, 255, 173, 30, 5, 48, 180, 255, 165, 54, 9, 25, 95, 202, 255, 203, 100, 35,
                9, 5, 16, 20, 32, 98, 196, 255, 220, 118, 49, 8, 21, 42, 90, 158, 229, 255, 197, 102,
                36, 9, 11, 33, 66, 113, 186, 247, 255, 196, 106, 38, 14, 18, 47, 104, 176, 235, 255, 240,
                208, 139, 67, 18, 11, 18, 23, 50, 66, 84, 118, 167, 232, 255, 241, 188, 116, 51, 17, 20,
                73, 147, 211, 255, 253, 223, 185, 141, 112, 83, 63, 35, 21, 21, 13, 12, 16, 13, 22, 25,
                21, 31, 32, 44, 57, 80, 114, 125, 164, 190, 204, 225, 240, 251, 253, 247, 255, 245, 227, 206,
                170, 132, 96, 74, 57, 35, 15, 12, 118, 255, 123, 14, 21, 132, 255, 127, 19, 3, 5, 30,
                138, 255, 138, 25, 4, 28, 142, 255, 149, 37, 8, 3, 8, 34, 141, 255, 154, 40, 13, 4,
                4, 31, 148, 255, 155, 46, 12, 12, 47, 159, 255, 153, 43, 11, 8, 44, 152, 255, 185, 58,
                6, 13, 62, 183, 255, 166, 66, 21, 6, 26, 87, 197, 255, 192, 90, 34, 46, 130, 226, 255,
                192, 90, 35, 10, 6, 23, 70, 146, 237, 255, 203, 120, 70, 39, 17, 8, 6, 15, 35, 78,
                135, 198, 238, 255, 236, 163, 84, 31, 13, 15, 28, 51, 81, 122, 173, 226, 255, 237, 176, 108,
                49, 13, 8, 8, 10, 17, 21, 27, 53, 90, 150, 207, 237, 255, 244, 217, 188, 170, 165, 149,
                127, 93, 61, 30, 30, 49, 74, 113, 150, 186, 230, 248, 255, 242, 223, 205, 188, 203, 201, 226,
                223, 225, 207, 182, 158, 133, 95, 65, 45, 33, 12, 114, 255, 120, 11, 15, 127, 255, 120, 18,
                18, 137, 255, 136, 24, 4, 33, 142, 255, 142, 23, 8, 39, 153, 255, 144, 28, 15, 54, 166,
                255, 171, 47, 5, 6, 16, 62, 171, 255, 172, 55, 11, 6, 18, 63, 169, 255, 196, 75, 15,
                7, 24, 89, 204, 255, 166, 72, 21, 6, 15, 37, 94, 186, 255, 240, 122, 35, 10, 9, 38,
                100, 192, 255, 235, 136, 62, 21, 10, 9, 59, 132, 209, 255, 243, 171, 74, 27, 11, 14, 31,
                69, 125, 192, 240, 255, 216, 155, 80, 30, 16, 13, 26, 42, 60, 88, 123, 164, 219, 255, 233,
                176, 111, 73, 50, 24, 14, 12, 35, 53, 82, 137, 183, 227, 244, 255, 233, 218, 177, 121, 62,
                16, 11, 13, 17, 22, 26, 40, 44, 57, 64, 76, 105, 133, 168, 199, 220, 234, 255, 251, 238,
                227, 206, 189, 170, 145, 124, 99, 77, 58, 40, 25, 14, 123, 255, 121, 12, 19, 129, 255, 132,
                22, 28, 141, 255, 140, 27, 29, 145, 255, 146, 37, 11, 5, 39, 156, 255, 153, 39, 4, 38,
                158, 255, 156, 45, 6, 8, 46, 162, 255, 161, 52, 10, 4, 8, 55, 166, 255, 167, 61, 16,
                6, 63, 199, 255, 158, 55, 14, 8, 5, 4, 21, 82, 193, 255, 172, 65, 14, 18, 120, 255,
                248, 167, 96, 40, 12, 9, 33, 94, 195, 255, 208, 122, 47, 11, 7, 32, 83, 160, 226, 255,
                223, 114, 14, 8, 18, 38, 78, 137, 201, 244, 255, 228, 179, 120, 70, 32, 12, 10, 21, 41,
                90, 145, 205, 239, 255, 248, 237, 207, 186, 161, 135, 97, 60, 37, 11, 25, 31, 61, 83, 112,
                146, 182, 209, 229, 242, 255, 250, 241, 236, 220, 206, 194, 172, 132, 78, 17, 23, 134, 255, 138,
                27, 3, 4, 28, 140, 255, 140, 29, 4, 5, 33, 145, 255, 146, 34, 5, 8, 41, 150, 255,
                152, 36, 5, 9, 42, 153, 255, 155, 41, 7, 7, 43, 158, 255, 155, 45, 10, 9, 46, 159,
                255, 151, 49, 12, 10, 49, 161, 255, 161, 45, 8, 14, 54, 161, 255, 166, 49, 10, 14, 55,
                167, 255, 163, 53, 12, 13, 59, 172, 255, 167, 62, 18, 4, 5, 21, 68, 176, 255, 175, 70,
                22, 6, 3, 10, 30, 81, 181, 255, 183, 78, 29, 9, 3, 7, 18, 46, 103, 199, 255, 184,
                91, 43, 18, 6, 6, 16, 35, 68, 128, 213, 255, 211, 139, 83, 45, 23, 12, 6, 8, 13,
                21, 33, 47, 66, 90, 127, 191, 255, 255, 193, 148, 123, 96, 74, 55, 40, 27, 19, 12, 8,
                5, 75, 255, 70, 12, 102, 255, 111, 14, 9, 117, 255, 122, 12, 19, 130, 255, 124, 17, 20,
                120, 255, 125, 22, 18, 123, 255, 137, 29, 4, 27, 158, 255, 145, 25, 31, 145, 255, 153, 45,
                7, 9, 47, 169, 255, 167, 52, 7, 5, 16, 61, 176, 255, 180, 60, 12, 21, 79, 187, 255,
                194, 84, 29, 12, 6, 26, 92, 207, 255, 191, 103, 51, 24, 15, 6, 6, 37, 98, 171, 255,
                219, 129, 48, 15, 10, 35, 77, 151, 229, 255, 203, 129, 64, 27, 8, 8, 17, 41, 81, 129,
                193, 235, 255, 230, 187, 130, 85, 51, 27, 10, 12, 16, 32, 52, 74, 108, 155, 197, 222, 233,
                244, 255, 226, 180, 141, 113, 82, 43, 18, 13, 40, 255, 50, 10, 58, 255, 64, 12, 48, 255,
                63, 98, 255, 82, 17, 92, 255, 107, 21, 13, 107, 255, 78, 25, 15, 11, 118, 255, 116, 23,
                14, 117, 255, 124, 27, 11, 33, 122, 255, 121, 11, 43, 160, 255, 185, 34, 34, 98, 255, 254,
                97, 18, 24, 83, 199, 255, 168, 99, 35, 47, 147, 255, 218, 116, 69, 21, 18, 17, 41, 104,
                229, 255, 204, 154, 74, 39, 21, 33, 76, 132, 197, 255, 243, 240, 160, 119, 76, 21, 21, 40,
                115, 218, 255, 210, 170, 86, 86, 87, 105, 104, 129, 106, 82, 58, 60, 87, 60, 30, 61, 255,
                54, 2, 68, 255, 67, 4, 83, 255, 66, 3, 80, 255, 93, 8, 6, 90, 255, 94, 4, 5,
                93, 255, 100, 10, 13, 111, 255, 101, 8, 10, 117, 255, 113, 7, 13, 113, 255, 125, 16, 26,
                139, 255, 130, 22, 28, 153, 255, 115, 22, 33, 144, 255, 155, 46, 12, 19, 72, 191, 255, 146,
                38, 11, 26, 75, 189, 255, 174, 69, 21, 7, 22, 59, 122, 206, 255, 237, 168, 90, 39, 12,
                9, 46, 102, 146, 182, 221, 238, 255, 234, 150, 61, 20, 9, 85, 255, 84, 14, 87, 255, 87,
                9, 109, 255, 94, 9, 8, 108, 255, 116, 15, 13, 111, 255, 119, 21, 4, 24, 121, 255, 116,
                15, 19, 124, 255, 142, 24, 7, 37, 142, 255, 114, 21, 10, 43, 157, 255, 148, 30, 5, 31,
                135, 255, 193, 58, 6, 13, 57, 192, 255, 172, 77, 22, 10, 30, 104, 209, 255, 193, 97, 47,
                15, 10, 26, 70, 149, 221, 255, 235, 118, 32, 9, 23, 46, 80, 143, 228, 255, 203, 151, 84,
                36, 21, 11, 15, 34, 68, 118, 181, 229, 255, 218, 192, 141, 76, 39, 13, 17, 55, 93, 152,
                217, 242, 255, 253, 231, 208, 174, 136, 105, 72, 49, 17, 38, 255, 36, 44, 255, 37, 6, 54,
                255, 42, 52, 255, 57, 7, 59, 255, 64, 8, 72, 255, 73, 6, 7, 84, 255, 75, 3, 71,
                255, 86, 2, 7, 91, 255, 96, 4, 14, 113, 255, 103, 13, 12, 102, 255, 111, 12, 13, 117,
                255, 130, 29, 4, 28, 136, 255, 168, 45, 16, 5, 6, 25, 90, 185, 255, 155, 22, 13, 48,
                76, 164, 255, 209, 135, 62, 22, 24, 67, 146, 237, 255, 228, 152, 80, 79, 52, 4, 86, 255,
                87, 3, 6, 93, 255, 89, 8, 10, 99, 255, 107, 8, 11, 107, 255, 106, 14, 18, 119, 255,
                116, 13, 12, 110, 255, 132, 21, 16, 122, 255, 131, 22, 3, 36, 157, 255, 139, 25, 7, 35,
                132, 255, 160, 29, 7, 44, 151, 255, 158, 48, 9, 5, 20, 62, 161, 255, 170, 62, 17, 5,
                8, 27, 72, 178, 255, 166, 69, 19, 6, 14, 46, 116, 191, 255, 194, 58, 7, 6, 11, 38,
                107, 203, 255, 199, 118, 50, 20, 9, 4, 10, 19, 33, 63, 106, 160, 210, 255, 229, 170, 111,
                61, 29, 13, 6, 6, 14, 32, 45, 65, 108, 168, 224, 255, 235, 182, 123, 73, 39, 16, 5,
                2, 46, 255, 8, 21, 255, 29, 27, 255, 24, 26, 255, 31, 35, 255, 32, 33, 255, 34, 42,
                255, 35, 41, 255, 43, 45, 255, 45, 4, 51, 255, 48, 58, 255, 56, 3, 5, 73, 255, 54,
                7, 71, 255, 86, 8, 18, 124, 255, 78, 4, 7, 10, 149, 255, 179, 95, 3, 15, 48, 224,
                255, 160, 104, 58, 41, 28, 66, 255, 67, 3, 76, 255, 79, 2, 6, 81, 255, 81, 6, 6,
                87, 255, 95, 5, 5, 97, 255, 88, 7, 8, 105, 255, 101, 7, 6, 95, 255, 112, 6, 15,
                107, 255, 120, 15, 20, 143, 255, 129, 22, 23, 131, 255, 174, 17, 8, 48, 167, 255, 141, 36,
                7, 12, 57, 168, 255, 191, 73, 16, 4, 12, 37, 117, 239, 255, 190, 98, 31, 6, 5, 17,
                48, 125, 225, 255, 204, 110, 42, 9, 21, 52, 102, 167, 231, 255, 245, 191, 117, 50, 13, 39,
                57, 77, 105, 134, 170, 187, 212, 237, 255, 246, 211, 145, 69, 31, 10, 73, 255, 75, 2, 5,
                86, 255, 81, 3, 4, 99, 255, 85, 5, 9, 97, 255, 99, 7, 18, 108, 255, 107, 8, 13,
                108, 255, 120, 18, 19, 112, 255, 122, 14, 20, 125, 255, 133, 23, 3, 28, 151, 255, 138, 24,
                39, 156, 255, 144, 31, 13, 54, 169, 255, 151, 48, 12, 6, 20, 73, 186, 255, 182, 69, 20,
                5, 8, 33, 98, 194, 255, 229, 123, 45, 15, 5, 19, 61, 139, 220, 255, 212, 110, 32, 7,
                9, 28, 60, 107, 172, 221, 255, 243, 204, 146, 76, 29, 19, 37, 73, 124, 185, 235, 255, 252,
                220, 194, 161, 137, 109, 74, 46, 25, 15, 8, 25, 255, 41, 51, 255, 49, 44, 255, 67, 7,
                72, 255, 56, 5, 96, 255, 65, 6, 80, 255, 89, 6, 6, 94, 255, 85, 6, 6, 97, 255,
                101, 9, 13, 120, 255, 106, 10, 22, 139, 255, 99, 8, 4, 40, 165, 255, 140, 30, 5, 37,
                134, 255, 197, 68, 15, 7, 23, 68, 174, 255, 190, 78, 30, 11, 13, 49, 115, 223, 255, 212,
                116, 45, 15, 19, 40, 101, 179, 249, 255, 218, 160, 134, 98, 59, 33, 11, 25, 75, 121, 157,
                172, 193, 216, 235, 238, 246, 249, 255, 201, 139, 90, 47, 24, 37, 255, 36, 38, 255, 40, 40,
                255, 42, 41, 255, 43, 45, 255, 46, 42, 255, 48, 53, 255, 47, 2, 58, 255, 56, 2, 63,
                255, 65, 2, 2, 70, 255, 72, 9, 71, 255, 77, 4, 80, 255, 83, 8, 11, 113, 255, 92,
                10, 8, 35, 159, 255, 129, 26, 18, 178, 255, 181, 74, 62, 33, 12, 5, 8, 40, 77, 142,
                253, 239, 255, 139, 102, 56, 14, 57, 255, 57, 64, 255, 63, 2, 71, 255, 76, 3, 4, 80,
                255, 80, 3, 4, 86, 255, 87, 4, 7, 96, 255, 100, 6, 7, 104, 255, 102, 10, 10, 98,
                255, 122, 12, 18, 129, 255, 123, 9, 9, 113, 255, 133, 27, 21, 125, 255, 166, 45, 8, 16,
                55, 165, 255, 206, 86, 15, 10, 48, 145, 217, 255, 194, 69, 8, 10, 62, 140, 221, 255, 233,
                151, 73, 29, 13, 10, 36, 113, 198, 254, 255, 243, 192, 121, 63, 27, 18, 11, 10, 22, 47,
                76, 83, 96, 139, 183, 210, 241, 255, 252, 237, 219, 180, 131, 67, 18, 51, 255, 56, 58, 255
        };
    }

    /**
     * Returns chunk 2 of the ST2_ALL_QLVL_DCMFS table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVL_DCMFS()} concatenates the chunks into {@link #ST2_ALL_QLVL_DCMFS}.
     */
    private static int[] makeST2_ALL_QLVL_DCMFS_2() {
        return new int[] {
                61, 65, 255, 68, 4, 70, 255, 69, 4, 70, 255, 85, 3, 4, 86, 255, 85, 4, 5, 90,
                255, 92, 3, 99, 255, 97, 5, 16, 134, 255, 137, 23, 17, 22, 134, 255, 155, 30, 19, 71,
                187, 255, 133, 26, 8, 51, 142, 255, 228, 76, 10, 13, 77, 193, 255, 213, 102, 9, 13, 23,
                58, 97, 186, 255, 209, 90, 14, 10, 109, 224, 255, 197, 124, 76, 38, 17, 13, 11, 20, 21,
                32, 51, 85, 127, 183, 217, 246, 255, 253, 236, 197, 130, 81, 44, 16, 51, 255, 57, 63, 255,
                61, 3, 72, 255, 67, 2, 73, 255, 82, 4, 6, 80, 255, 82, 7, 4, 77, 255, 90, 6,
                7, 95, 255, 88, 7, 5, 85, 255, 110, 6, 8, 114, 255, 109, 16, 19, 130, 255, 124, 25,
                26, 169, 255, 126, 23, 5, 57, 182, 255, 152, 54, 13, 32, 115, 216, 255, 182, 58, 7, 8,
                17, 48, 101, 187, 255, 208, 89, 9, 9, 11, 31, 73, 171, 241, 255, 216, 181, 146, 93, 39,
                23, 59, 123, 189, 240, 255, 218, 195, 208, 224, 223, 193, 142, 82, 44, 20, 53, 255, 52, 60,
                255, 56, 66, 255, 65, 3, 73, 255, 68, 4, 82, 255, 73, 8, 97, 255, 98, 9, 101, 255,
                99, 8, 14, 97, 255, 118, 9, 11, 133, 255, 103, 16, 3, 24, 122, 255, 165, 26, 25, 133,
                255, 168, 38, 6, 36, 156, 255, 191, 50, 6, 6, 26, 94, 205, 255, 172, 56, 13, 6, 25,
                47, 89, 178, 255, 196, 94, 43, 12, 9, 34, 76, 163, 240, 255, 230, 147, 43, 9, 12, 19,
                34, 47, 62, 95, 157, 204, 243, 255, 231, 200, 159, 117, 75, 47, 23, 12, 54, 255, 54, 63,
                255, 64, 72, 255, 69, 70, 255, 75, 8, 82, 255, 81, 4, 3, 82, 255, 85, 5, 5, 88,
                255, 92, 8, 6, 96, 255, 100, 10, 4, 113, 255, 94, 12, 5, 14, 123, 255, 103, 9, 11,
                176, 255, 120, 28, 24, 134, 255, 151, 26, 8, 55, 180, 255, 145, 5, 6, 34, 111, 216, 255,
                202, 103, 32, 6, 23, 84, 187, 255, 252, 215, 165, 114, 53, 16, 14, 24, 65, 109, 167, 225,
                255, 255, 245, 218, 169, 112, 24, 64, 255, 68, 2, 2, 70, 255, 72, 3, 4, 77, 255, 78,
                3, 6, 85, 255, 83, 4, 6, 89, 255, 88, 6, 8, 90, 255, 92, 5, 6, 95, 255, 94,
                7, 6, 98, 255, 96, 6, 7, 101, 255, 101, 10, 9, 102, 255, 105, 11, 15, 108, 255, 109,
                11, 17, 116, 255, 116, 17, 3, 25, 126, 255, 124, 23, 2, 6, 35, 131, 255, 147, 39, 8,
                16, 57, 163, 255, 170, 71, 23, 6, 4, 11, 22, 43, 71, 113, 165, 255, 214, 114, 63, 35,
                17, 7, 9, 17, 48, 134, 255, 112, 9, 55, 172, 255, 166, 62, 12, 10, 63, 172, 255, 153,
                56, 16, 13, 83, 202, 255, 170, 70, 17, 5, 12, 76, 202, 255, 192, 94, 26, 7, 27, 93,
                191, 255, 202, 87, 16, 6, 20, 54, 108, 186, 255, 216, 107, 24, 4, 24, 63, 135, 222, 255,
                221, 142, 58, 14, 9, 33, 107, 194, 255, 244, 171, 85, 30, 7, 9, 17, 34, 75, 141, 212,
                255, 234, 151, 75, 25, 6, 8, 17, 37, 72, 111, 162, 216, 255, 251, 202, 117, 50, 12, 17,
                34, 58, 85, 131, 178, 226, 255, 247, 195, 134, 84, 37, 10, 8, 22, 38, 70, 114, 165, 210,
                240, 255, 240, 207, 171, 124, 83, 49, 26, 11, 22, 55, 103, 161, 216, 251, 255, 243, 207, 162,
                117, 86, 67, 56, 43, 29, 17, 21, 36, 60, 87, 113, 148, 179, 209, 236, 252, 255, 243, 230,
                203, 175, 146, 105, 80, 52, 30, 16, 12, 8, 7, 21, 28, 41, 51, 71, 82, 100, 135, 160,
                178, 198, 214, 228, 243, 246, 255, 250, 233, 216, 185, 150, 102, 75, 63, 43, 41, 29, 17, 11,
                33, 132, 255, 160, 20, 10, 28, 62, 143, 255, 163, 15, 9, 55, 170, 255, 170, 49, 7, 8,
                60, 200, 255, 166, 79, 31, 10, 11, 42, 107, 196, 255, 194, 72, 10, 8, 35, 97, 196, 255,
                198, 92, 26, 4, 11, 48, 122, 214, 255, 208, 117, 47, 14, 7, 32, 76, 140, 213, 255, 234,
                156, 74, 23, 11, 40, 95, 170, 231, 255, 217, 145, 75, 38, 14, 8, 15, 39, 93, 163, 223,
                255, 236, 181, 110, 51, 17, 18, 64, 130, 197, 240, 255, 246, 203, 127, 54, 15, 10, 21, 41,
                83, 134, 191, 238, 255, 236, 184, 128, 77, 35, 12, 9, 22, 47, 77, 113, 159, 212, 250, 255,
                239, 202, 155, 105, 65, 29, 10, 17, 30, 55, 85, 121, 155, 192, 226, 244, 255, 246, 223, 180,
                132, 85, 46, 23, 14, 15, 30, 46, 69, 92, 122, 156, 193, 224, 246, 255, 246, 227, 195, 167,
                131, 93, 58, 37, 22, 12, 10, 17, 28, 39, 56, 74, 97, 125, 153, 180, 201, 222, 241, 251,
                255, 251, 237, 223, 200, 173, 147, 125, 105, 79, 61, 42, 31, 25, 22, 19, 20, 116, 255, 134,
                24, 5, 6, 53, 170, 255, 161, 46, 7, 5, 57, 196, 255, 169, 78, 34, 11, 20, 76, 185,
                255, 198, 81, 16, 4, 18, 44, 98, 174, 255, 217, 70, 9, 8, 30, 94, 193, 255, 191, 90,
                28, 6, 10, 44, 107, 207, 255, 190, 85, 24, 8, 47, 126, 212, 255, 220, 123, 36, 20, 66,
                137, 208, 255, 232, 134, 44, 6, 19, 53, 107, 185, 247, 255, 213, 148, 85, 42, 15, 10, 25,
                41, 66, 105, 156, 211, 254, 255, 207, 122, 54, 18, 15, 21, 40, 60, 99, 142, 199, 247, 255,
                244, 193, 115, 40, 13, 29, 53, 94, 148, 201, 246, 255, 227, 183, 136, 90, 47, 24, 8, 8,
                20, 33, 54, 78, 113, 157, 204, 237, 255, 245, 218, 166, 110, 66, 36, 20, 28, 35, 47, 79,
                110, 143, 185, 218, 244, 255, 248, 227, 196, 158, 115, 76, 45, 25, 12, 20, 24, 42, 64, 73,
                94, 122, 143, 168, 186, 210, 235, 245, 255, 246, 235, 222, 204, 174, 148, 120, 89, 57, 35, 17,
                6, 108, 255, 108, 17, 5, 40, 152, 255, 146, 41, 11, 37, 193, 255, 137, 55, 18, 7, 65,
                178, 255, 189, 74, 17, 10, 35, 83, 166, 255, 196, 59, 5, 12, 71, 188, 255, 193, 90, 23,
                21, 86, 202, 255, 190, 91, 29, 6, 30, 109, 213, 255, 192, 102, 42, 11, 14, 33, 79, 153,
                223, 255, 231, 153, 71, 18, 9, 30, 56, 97, 159, 224, 255, 223, 148, 74, 27, 7, 6, 21,
                53, 103, 175, 238, 255, 205, 118, 48, 14, 9, 12, 24, 38, 69, 111, 165, 224, 255, 232, 172,
                111, 70, 46, 23, 7, 9, 29, 74, 143, 209, 248, 255, 225, 194, 151, 106, 65, 43, 25, 9,
                15, 18, 31, 37, 53, 72, 106, 134, 165, 201, 229, 252, 255, 243, 216, 186, 152, 117, 93, 67,
                47, 21, 13, 8, 28, 63, 95, 141, 180, 211, 237, 253, 255, 245, 218, 195, 170, 139, 115, 89,
                69, 49, 26, 11, 13, 19, 24, 52, 90, 122, 162, 199, 229, 252, 255, 251, 248, 246, 240, 226,
                200, 169, 146, 116, 98, 77, 58, 33, 18, 5, 24, 133, 255, 126, 15, 3, 50, 168, 255, 167,
                45, 6, 10, 54, 163, 255, 185, 69, 13, 28, 88, 191, 255, 167, 46, 6, 52, 174, 255, 183,
                88, 35, 13, 22, 79, 181, 255, 207, 104, 36, 9, 9, 39, 101, 184, 255, 223, 107, 26, 14,
                51, 119, 209, 255, 209, 119, 44, 11, 13, 30, 68, 121, 190, 255, 255, 143, 30, 14, 53, 124,
                207, 255, 227, 149, 76, 30, 10, 10, 21, 54, 110, 173, 235, 255, 232, 165, 98, 48, 22, 15,
                32, 63, 115, 182, 240, 255, 235, 187, 135, 93, 55, 30, 16, 6, 28, 66, 131, 198, 244, 255,
                241, 205, 154, 108, 73, 47, 30, 20, 14, 10, 22, 45, 73, 118, 159, 201, 239, 255, 240, 193,
                139, 91, 53, 29, 16, 20, 31, 57, 75, 97, 133, 162, 198, 231, 246, 255, 245, 235, 201, 163,
                127, 97, 69, 43, 23, 10, 28, 36, 52, 57, 63, 70, 84, 95, 114, 144, 169, 196, 220, 242,
                252, 255, 243, 224, 197, 162, 131, 108, 85, 61, 50, 37, 27, 22, 17, 114, 255, 118, 14, 22,
                168, 255, 129, 41, 13, 6, 38, 123, 255, 173, 47, 8, 4, 28, 71, 157, 255, 177, 38, 5,
                8, 21, 45, 82, 157, 255, 196, 36, 20, 94, 205, 255, 186, 88, 28, 6, 9, 30, 92, 186,
                255, 218, 119, 40, 11, 11, 49, 146, 238, 255, 198, 118, 54, 18, 8, 28, 71, 149, 230, 255,
                225, 152, 70, 16, 13, 34, 83, 159, 236, 255, 211, 132, 65, 22, 6, 14, 44, 93, 160, 221,
                255, 234, 166, 89, 36, 9, 9, 20, 44, 95, 162, 221, 255, 241, 189, 121, 66, 30, 8, 9,
                29, 71, 132, 195, 242, 255, 231, 186, 133, 88, 53, 24, 7, 9, 15, 48, 104, 159, 207, 237,
                255, 248, 218, 172, 120, 75, 45, 24, 11, 23, 47, 83, 119, 151, 190, 222, 242, 255, 253, 248,
                230, 201, 164, 134, 106, 77, 52, 35, 16, 16, 16, 25, 44, 48, 68, 81, 110, 135, 161, 188,
                208, 230, 245, 255, 249, 236, 216, 200, 174, 152, 129, 94, 71, 39, 14, 4, 110, 255, 129, 30,
                7, 4, 12, 154, 255, 126, 48, 13, 14, 66, 179, 255, 178, 61, 10, 20, 71, 184, 255, 175,
                58, 10, 12, 64, 173, 255, 191, 88, 29, 8, 5, 15, 48, 114, 204, 255, 194, 94, 34, 10,
                18, 52, 125, 208, 255, 219, 134, 57, 21, 9, 11, 33, 80, 156, 234, 255, 198, 110, 47, 14,
                12, 34, 95, 167, 236, 255, 209, 131, 59, 26, 5, 12, 41, 91, 162, 231, 255, 226, 167, 109,
                58, 24, 5, 10, 16, 34, 65, 112, 172, 229, 255, 241, 188, 112, 51, 12, 18, 65, 142, 210,
                255, 254, 223, 181, 143, 107, 67, 42, 25, 11, 17, 33, 61, 101, 146, 192, 228, 253, 255, 218,
                172, 126, 81, 46, 23, 13, 9, 15, 25, 40, 60, 95, 133, 175, 217, 245, 255, 238, 206, 168,
                127, 91, 61, 41, 20, 14, 12, 34, 58, 97, 135, 172, 208, 239, 252, 255, 240, 224, 192, 170,
                147, 114, 93, 64, 53, 33, 30, 20, 17, 13, 16, 26, 37, 50, 71, 87, 103, 117, 135, 156,
                185, 205, 235, 250, 255, 253, 246, 225, 205, 176, 143, 108, 77, 48, 34, 20, 7, 98, 255, 104,
                23, 6, 6, 53, 162, 255, 171, 57, 12, 6, 22, 71, 169, 255, 187, 54, 4, 22, 72, 177,
                255, 181, 67, 14, 9, 35, 97, 200, 255, 197, 101, 39, 11, 20, 107, 228, 255, 189, 114, 58,
                28, 11, 11, 44, 126, 217, 255, 215, 124, 51, 15, 8, 48, 140, 234, 255, 211, 134, 64, 21,
                4, 8, 25, 70, 139, 219, 255, 213, 130, 54, 15, 14, 34, 76, 141, 214, 255, 222, 138, 65,
                23, 6, 14, 44, 102, 175, 238, 255, 215, 148, 92, 48, 26, 12, 5, 25, 87, 166, 229, 255,
                228, 170, 116, 74, 46, 31, 14, 7, 7, 28, 67, 123, 184, 235, 255, 234, 188, 134, 84, 48,
                27, 14, 7, 7, 17, 27, 43, 66, 97, 140, 188, 229, 255, 255, 223, 172, 119, 75, 39, 20,
                9, 11, 24, 36, 58, 84, 115, 151, 189, 224, 245, 255, 250, 230, 197, 162, 118, 83, 55, 31,
                15, 30, 48, 72, 104, 138, 172, 200, 228, 244, 255, 252, 238, 218, 197, 174, 155, 130, 109, 93
        };
    }

    /**
     * Returns chunk 3 of the ST2_ALL_QLVL_DCMFS table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVL_DCMFS()} concatenates the chunks into {@link #ST2_ALL_QLVL_DCMFS}.
     */
    private static int[] makeST2_ALL_QLVL_DCMFS_3() {
        return new int[] {
                82, 72, 60, 49, 40, 28, 17, 13, 3, 70, 255, 83, 14, 29, 172, 255, 129, 37, 5, 9,
                54, 159, 255, 173, 58, 13, 4, 21, 77, 179, 255, 184, 79, 23, 4, 13, 40, 99, 194, 255,
                196, 96, 32, 7, 5, 17, 43, 108, 194, 255, 224, 118, 28, 41, 128, 229, 255, 196, 110, 44,
                12, 25, 108, 208, 255, 239, 150, 57, 9, 8, 23, 57, 123, 209, 255, 219, 136, 64, 23, 7,
                15, 50, 125, 208, 255, 232, 169, 92, 32, 28, 77, 146, 220, 255, 221, 161, 103, 51, 19, 6,
                9, 29, 55, 98, 162, 228, 255, 226, 161, 88, 38, 11, 8, 15, 32, 59, 107, 175, 236, 255,
                230, 181, 119, 64, 30, 10, 7, 10, 25, 46, 70, 109, 159, 205, 242, 255, 236, 187, 130, 86,
                55, 32, 16, 10, 20, 36, 52, 79, 116, 150, 191, 227, 246, 255, 238, 209, 182, 153, 117, 95,
                66, 53, 35, 23, 13, 8, 11, 14, 23, 28, 35, 44, 59, 67, 85, 110, 139, 169, 198, 221,
                240, 252, 255, 235, 211, 174, 135, 99, 68, 48, 34, 30, 26, 20, 8, 119, 255, 129, 32, 8,
                25, 143, 255, 137, 34, 4, 16, 57, 151, 255, 160, 64, 16, 17, 73, 180, 255, 206, 94, 27,
                6, 8, 69, 187, 255, 201, 105, 39, 6, 14, 49, 116, 206, 255, 206, 114, 39, 4, 16, 56,
                119, 199, 255, 241, 133, 43, 7, 21, 66, 135, 218, 255, 212, 127, 57, 16, 9, 31, 76, 142,
                213, 255, 241, 178, 96, 41, 13, 13, 49, 115, 198, 252, 255, 210, 143, 81, 39, 14, 11, 24,
                50, 79, 112, 157, 215, 255, 232, 151, 68, 14, 16, 42, 83, 135, 181, 228, 255, 246, 208, 149,
                94, 44, 19, 8, 16, 34, 59, 92, 128, 166, 207, 237, 255, 244, 206, 154, 96, 47, 16, 10,
                16, 28, 44, 69, 101, 138, 174, 206, 235, 255, 241, 209, 166, 122, 81, 46, 20, 6, 21, 27,
                49, 68, 102, 136, 170, 208, 230, 242, 255, 251, 236, 209, 174, 143, 112, 82, 56, 37, 24, 14,
                17, 29, 44, 66, 92, 126, 160, 194, 220, 241, 255, 252, 244, 230, 211, 182, 151, 125, 102, 88,
                76, 67, 55, 42, 34, 18, 13, 12, 123, 255, 118, 27, 7, 5, 54, 180, 255, 154, 54, 10,
                15, 70, 172, 255, 187, 78, 21, 13, 80, 197, 255, 175, 74, 25, 9, 26, 96, 200, 255, 189,
                90, 31, 7, 17, 75, 191, 255, 201, 110, 50, 18, 8, 36, 115, 207, 255, 193, 97, 32, 11,
                14, 50, 120, 210, 255, 207, 105, 30, 13, 21, 50, 107, 199, 255, 210, 126, 51, 9, 7, 31,
                82, 157, 221, 255, 217, 112, 31, 6, 12, 30, 70, 134, 211, 255, 224, 151, 87, 41, 18, 17,
                50, 113, 179, 239, 255, 229, 188, 129, 82, 41, 18, 7, 7, 20, 65, 128, 187, 237, 255, 236,
                198, 139, 85, 45, 28, 11, 12, 25, 46, 78, 128, 179, 205, 238, 255, 242, 225, 185, 138, 85,
                45, 14, 10, 26, 42, 62, 99, 144, 197, 234, 255, 238, 228, 203, 175, 137, 105, 75, 57, 39,
                25, 9, 18, 45, 80, 133, 174, 207, 226, 244, 255, 249, 244, 222, 201, 172, 152, 114, 100, 82,
                60, 53, 38, 34, 28, 28, 16, 12, 11, 89, 255, 104, 21, 4, 3, 16, 45, 116, 255, 160,
                19, 31, 170, 255, 153, 54, 13, 35, 168, 255, 183, 77, 17, 20, 84, 196, 255, 195, 91, 25,
                24, 98, 206, 255, 190, 94, 30, 6, 6, 38, 121, 216, 255, 203, 115, 46, 11, 13, 46, 118,
                208, 255, 213, 122, 47, 12, 6, 25, 62, 124, 202, 255, 238, 158, 76, 26, 8, 20, 49, 97,
                160, 226, 255, 223, 142, 66, 21, 18, 48, 105, 174, 238, 255, 212, 134, 71, 28, 8, 15, 34,
                65, 112, 173, 226, 255, 238, 184, 122, 69, 35, 15, 10, 24, 50, 94, 150, 202, 235, 255, 251,
                213, 151, 92, 44, 17, 9, 21, 38, 62, 91, 128, 169, 211, 243, 255, 252, 223, 190, 147, 106,
                79, 57, 44, 32, 19, 10, 22, 35, 49, 76, 105, 138, 174, 215, 240, 252, 255, 246, 225, 191,
                158, 132, 101, 73, 46, 25, 10, 16, 29, 51, 76, 105, 131, 147, 169, 192, 204, 222, 235, 246,
                254, 255, 248, 231, 211, 192, 170, 148, 125, 101, 76, 60, 46, 32, 23, 19, 15, 9, 55, 161,
                255, 180, 61, 12, 18, 83, 198, 255, 176, 72, 18, 17, 77, 187, 255, 195, 90, 29, 8, 4,
                13, 40, 107, 200, 255, 194, 71, 9, 10, 37, 109, 208, 255, 194, 95, 31, 4, 17, 56, 120,
                201, 255, 212, 102, 26, 15, 56, 124, 202, 255, 228, 134, 50, 13, 4, 9, 24, 65, 132, 214,
                255, 212, 130, 64, 25, 7, 9, 26, 79, 156, 228, 255, 217, 139, 68, 28, 11, 8, 35, 85,
                152, 221, 255, 222, 149, 84, 39, 15, 9, 23, 50, 97, 161, 224, 255, 231, 168, 101, 54, 24,
                8, 6, 11, 25, 45, 81, 125, 183, 235, 255, 230, 182, 125, 79, 48, 30, 17, 5, 5, 12,
                20, 44, 73, 115, 171, 218, 245, 255, 246, 219, 162, 93, 33, 6, 9, 16, 23, 48, 73, 109,
                151, 192, 231, 255, 252, 218, 167, 117, 75, 43, 23, 13, 11, 20, 28, 39, 53, 73, 86, 111,
                138, 169, 199, 226, 248, 253, 255, 237, 211, 184, 150, 114, 88, 59, 34, 16, 12, 14, 16, 21,
                24, 32, 42, 56, 68, 84, 101, 126, 156, 186, 215, 235, 252, 255, 248, 238, 216, 189, 154, 124,
                96, 66, 43, 22, 9, 100, 255, 101, 9, 12, 105, 255, 102, 14, 17, 115, 255, 117, 22, 22,
                127, 255, 123, 26, 6, 29, 138, 255, 134, 36, 10, 9, 41, 146, 255, 151, 44, 9, 12, 53,
                159, 255, 153, 47, 10, 6, 22, 58, 163, 255, 150, 38, 4, 15, 67, 175, 255, 159, 63, 19,
                13, 71, 180, 255, 194, 78, 21, 10, 32, 89, 189, 255, 191, 95, 39, 14, 13, 41, 97, 189,
                255, 210, 119, 49, 12, 22, 60, 127, 215, 255, 216, 143, 80, 44, 25, 11, 6, 11, 23, 45,
                81, 129, 187, 239, 255, 222, 151, 89, 51, 21, 5, 28, 83, 136, 167, 203, 239, 255, 238, 219,
                190, 146, 110, 84, 69, 49, 35, 28, 16, 15, 11, 14, 16, 19, 22, 21, 34, 39, 54, 64,
                84, 119, 149, 176, 198, 227, 250, 255, 241, 224, 195, 176, 153, 121, 83, 58, 37, 20, 85, 255,
                86, 13, 4, 3, 30, 125, 255, 133, 31, 3, 29, 153, 255, 164, 54, 11, 41, 171, 255, 170,
                76, 32, 8, 12, 75, 186, 255, 199, 88, 26, 6, 8, 35, 91, 184, 255, 201, 87, 22, 10,
                43, 112, 207, 255, 205, 112, 44, 13, 11, 53, 134, 223, 255, 209, 121, 50, 16, 6, 20, 53,
                119, 203, 255, 227, 141, 66, 27, 11, 5, 11, 42, 87, 157, 230, 255, 217, 140, 69, 26, 9,
                5, 13, 26, 53, 95, 149, 209, 255, 232, 154, 64, 14, 6, 15, 48, 103, 170, 224, 255, 239,
                199, 143, 82, 34, 14, 34, 78, 134, 197, 244, 255, 230, 185, 133, 87, 48, 25, 6, 34, 66,
                120, 176, 227, 255, 251, 218, 173, 126, 82, 47, 26, 15, 6, 12, 19, 28, 45, 66, 95, 119,
                155, 188, 226, 247, 255, 244, 218, 183, 143, 110, 79, 51, 38, 20, 9, 9, 14, 12, 21, 31,
                41, 58, 82, 117, 139, 172, 196, 224, 238, 254, 254, 255, 246, 236, 218, 195, 167, 146, 119, 101,
                76, 61, 52, 47, 43, 45, 34, 37, 34, 34, 25, 13, 8, 27, 121, 255, 97, 6, 25, 138,
                255, 133, 23, 30, 144, 255, 144, 31, 5, 8, 41, 151, 255, 151, 38, 4, 4, 15, 55, 159,
                255, 162, 49, 6, 5, 22, 64, 163, 255, 179, 63, 12, 14, 69, 179, 255, 171, 66, 24, 6,
                20, 73, 179, 255, 190, 85, 25, 5, 14, 45, 108, 200, 255, 198, 89, 19, 17, 44, 111, 203,
                255, 204, 114, 52, 22, 9, 5, 15, 34, 71, 132, 203, 255, 208, 96, 26, 14, 38, 78, 144,
                213, 255, 241, 171, 91, 38, 12, 14, 32, 59, 111, 176, 240, 255, 214, 156, 90, 36, 13, 13,
                27, 51, 86, 139, 197, 242, 255, 228, 191, 144, 98, 60, 35, 18, 10, 12, 22, 29, 52, 78,
                110, 151, 193, 225, 248, 255, 236, 216, 191, 163, 136, 102, 73, 48, 31, 20, 11, 45, 87, 138,
                186, 225, 243, 255, 255, 243, 233, 216, 192, 174, 160, 150, 142, 129, 121, 109, 97, 86, 69, 59,
                49, 39, 31, 25, 17, 14, 11, 9, 8, 5, 90, 255, 100, 17, 4, 7, 32, 140, 255, 143,
                49, 16, 4, 3, 16, 57, 160, 255, 170, 53, 11, 13, 56, 171, 255, 165, 61, 19, 6, 8,
                24, 71, 171, 255, 174, 61, 16, 4, 5, 20, 66, 173, 255, 177, 70, 21, 4, 8, 26, 75,
                176, 255, 182, 71, 19, 4, 5, 23, 76, 183, 255, 185, 81, 26, 6, 9, 31, 85, 185, 255,
                192, 87, 29, 7, 8, 33, 91, 190, 255, 199, 99, 37, 11, 13, 42, 104, 200, 255, 193, 94,
                35, 10, 4, 16, 46, 109, 202, 255, 199, 106, 45, 16, 4, 6, 20, 53, 118, 207, 255, 206,
                116, 54, 21, 7, 11, 29, 66, 131, 212, 255, 213, 134, 72, 34, 14, 5, 7, 17, 34, 63,
                107, 170, 231, 255, 222, 161, 105, 64, 37, 21, 11, 5, 8, 12, 17, 27, 41, 60, 89, 130,
                185, 235, 255, 230, 178, 123, 82, 53, 35, 21, 13, 7, 3, 15, 83, 255, 36, 7, 101, 255,
                95, 6, 12, 114, 255, 87, 4, 6, 129, 255, 103, 13, 6, 123, 255, 131, 18, 18, 126, 255,
                132, 10, 6, 38, 133, 255, 147, 14, 4, 45, 165, 255, 171, 40, 21, 136, 255, 198, 61, 4,
                13, 55, 162, 255, 178, 50, 5, 7, 33, 93, 181, 255, 223, 88, 12, 17, 52, 107, 192, 255,
                212, 116, 40, 20, 62, 139, 216, 255, 220, 150, 75, 23, 6, 48, 130, 227, 255, 217, 143, 81,
                51, 32, 9, 15, 45, 89, 141, 197, 241, 255, 235, 193, 133, 76, 32, 9, 7, 15, 33, 56,
                85, 129, 168, 203, 229, 245, 255, 222, 174, 104, 59, 42, 27, 8, 72, 255, 69, 17, 89, 255,
                64, 6, 98, 255, 89, 3, 113, 255, 109, 22, 3, 26, 136, 255, 119, 5, 21, 132, 255, 130,
                16, 30, 154, 255, 148, 31, 5, 54, 164, 255, 184, 50, 3, 10, 69, 192, 255, 166, 54, 12,
                11, 67, 184, 255, 200, 84, 14, 17, 99, 213, 255, 215, 93, 11, 7, 36, 110, 205, 255, 200,
                103, 30, 6, 21, 68, 136, 224, 255, 212, 129, 55, 11, 20, 56, 111, 175, 231, 255, 233, 162,
                78, 24, 6, 10, 33, 68, 115, 176, 233, 255, 234, 180, 121, 60, 25, 8, 10, 23, 44, 81,
                126, 172, 215, 241, 255, 239, 209, 164, 124, 83, 46, 25, 19, 12, 59, 255, 60, 4, 4, 99,
                255, 91, 7, 3, 110, 255, 106, 24, 11, 118, 255, 126, 7, 4, 34, 120, 255, 124, 4, 21,
                128, 255, 124, 17, 31, 146, 255, 124, 15, 26, 160, 255, 156, 20, 43, 160, 255, 170, 27, 15,
                79, 204, 255, 168, 64, 14, 12, 40, 86, 173, 255, 227, 92, 13, 7, 24, 55, 121, 209, 255,
                206, 79, 8, 15, 47, 121, 217, 255, 198, 113, 42, 9, 12, 36, 78, 144, 219, 255, 225, 139,
                55, 20, 30, 51, 99, 162, 222, 255, 236, 180, 106, 42, 14, 15, 22, 50, 83, 123, 161, 204
        };
    }

    /**
     * Returns chunk 4 of the ST2_ALL_QLVL_DCMFS table literal.
     *
     * <p>One contiguous slice of the byte-for-byte C transcription; {@link #makeST2_ALL_QLVL_DCMFS()} concatenates the chunks into {@link #ST2_ALL_QLVL_DCMFS}.
     */
    private static int[] makeST2_ALL_QLVL_DCMFS_4() {
        return new int[] {
                241, 255, 240, 215, 171, 117, 67, 28, 43, 255, 52, 4, 83, 255, 80, 5, 100, 255, 82, 13,
                5, 109, 255, 117, 10, 29, 113, 255, 114, 5, 115, 255, 127, 12, 12, 128, 255, 129, 19, 19,
                146, 255, 135, 29, 11, 56, 175, 255, 176, 47, 4, 8, 33, 85, 183, 255, 173, 51, 7, 5,
                40, 134, 255, 245, 89, 8, 5, 12, 34, 89, 186, 255, 195, 91, 41, 7, 24, 112, 223, 255,
                204, 132, 63, 22, 6, 10, 25, 40, 73, 123, 186, 239, 255, 225, 168, 112, 67, 29, 6, 13,
                60, 130, 194, 242, 255, 229, 183, 135, 89, 57, 16, 11, 21, 60, 115, 190, 243, 255, 251, 238,
                211, 161, 115, 79, 40, 14, 3, 62, 255, 56, 3, 93, 255, 90, 3, 6, 94, 255, 111, 7,
                16, 117, 255, 90, 3, 94, 255, 123, 24, 15, 117, 255, 139, 21, 22, 123, 255, 153, 16, 35,
                153, 255, 149, 31, 9, 50, 145, 255, 179, 12, 35, 151, 255, 174, 56, 8, 18, 83, 193, 255,
                190, 75, 18, 7, 28, 89, 199, 255, 204, 113, 52, 17, 25, 101, 213, 255, 214, 127, 64, 29,
                13, 20, 65, 139, 215, 255, 208, 117, 46, 16, 16, 38, 75, 122, 182, 236, 255, 237, 184, 119,
                71, 28, 7, 27, 48, 62, 74, 96, 134, 180, 229, 254, 255, 215, 158, 110, 69, 41, 30, 16,
                3, 51, 255, 55, 79, 255, 73, 9, 5, 70, 255, 96, 4, 17, 104, 255, 93, 3, 7, 34,
                108, 255, 99, 11, 134, 255, 123, 17, 19, 124, 255, 153, 25, 28, 178, 255, 143, 37, 6, 6,
                50, 174, 255, 178, 45, 10, 60, 184, 255, 157, 46, 6, 15, 72, 181, 255, 190, 63, 7, 16,
                71, 179, 255, 206, 95, 25, 25, 105, 212, 255, 203, 113, 46, 8, 8, 43, 132, 217, 255, 228,
                147, 69, 27, 5, 29, 74, 141, 206, 246, 255, 232, 183, 125, 77, 38, 9, 18, 21, 43, 68,
                108, 155, 200, 238, 255, 244, 209, 171, 127, 75, 25, 37, 255, 69, 5, 66, 255, 79, 9, 9,
                109, 255, 105, 7, 10, 114, 255, 103, 4, 6, 105, 255, 129, 20, 4, 33, 147, 255, 130, 23,
                37, 154, 255, 164, 40, 7, 9, 58, 184, 255, 140, 32, 10, 68, 193, 255, 157, 44, 5, 10,
                68, 187, 255, 186, 85, 21, 8, 30, 89, 189, 255, 203, 82, 10, 16, 108, 227, 255, 194, 126,
                61, 24, 7, 17, 56, 126, 206, 255, 233, 153, 74, 25, 4, 18, 39, 86, 154, 226, 255, 219,
                151, 85, 40, 15, 17, 59, 127, 193, 246, 255, 227, 185, 135, 91, 55, 30, 17, 12, 15, 33,
                58, 91, 117, 151, 195, 240, 255, 246, 215, 164, 105, 55, 25, 40, 255, 61, 5, 2, 94, 255,
                98, 7, 15, 107, 255, 105, 14, 112, 255, 110, 8, 22, 134, 255, 135, 25, 10, 154, 255, 134,
                41, 8, 29, 157, 255, 155, 33, 29, 174, 255, 159, 47, 5, 6, 49, 169, 255, 153, 33, 10,
                56, 166, 255, 168, 48, 6, 11, 75, 195, 255, 170, 71, 23, 4, 58, 187, 255, 198, 105, 48,
                14, 23, 93, 200, 255, 204, 112, 44, 14, 9, 27, 62, 119, 202, 255, 233, 146, 65, 19, 10,
                24, 58, 108, 169, 228, 255, 237, 183, 111, 56, 15, 23, 54, 106, 165, 217, 251, 255, 226, 183,
                148, 109, 83, 63, 46, 27, 14, 22, 255, 43, 90, 255, 72, 3, 4, 93, 255, 101, 7, 14,
                115, 255, 118, 15, 28, 133, 255, 135, 21, 4, 34, 139, 255, 156, 18, 26, 165, 255, 138, 27,
                14, 141, 255, 186, 35, 6, 42, 155, 255, 166, 46, 6, 33, 152, 255, 201, 72, 8, 5, 54,
                170, 255, 182, 79, 17, 27, 80, 188, 255, 185, 67, 12, 8, 28, 84, 195, 255, 198, 95, 27,
                7, 24, 63, 134, 216, 255, 203, 110, 48, 15, 10, 26, 52, 108, 173, 238, 255, 223, 172, 112,
                68, 37, 19, 9, 15, 27, 39, 61, 87, 135, 187, 232, 255, 246, 195, 126, 71, 40, 28, 17,
                41, 255, 71, 6, 69, 255, 75, 10, 95, 255, 100, 11, 10, 112, 255, 136, 16, 5, 114, 255,
                140, 24, 32, 149, 255, 145, 23, 38, 147, 255, 173, 25, 45, 163, 255, 156, 39, 7, 55, 164,
                255, 200, 71, 12, 11, 83, 216, 255, 167, 61, 13, 10, 44, 95, 175, 255, 182, 45, 5, 39,
                109, 195, 255, 219, 124, 39, 8, 7, 37, 83, 147, 216, 255, 220, 127, 41, 8, 20, 43, 95,
                160, 213, 255, 220, 146, 73, 23, 12, 32, 68, 127, 193, 234, 255, 240, 194, 135, 83, 43, 18,
                24, 51, 93, 151, 206, 245, 255, 231, 194, 143, 102, 80, 60, 39, 17, 6, 55, 255, 63, 4,
                5, 100, 255, 89, 7, 10, 104, 255, 114, 11, 9, 125, 255, 108, 16, 16, 132, 255, 125, 18,
                9, 121, 255, 145, 35, 3, 20, 153, 255, 131, 22, 34, 151, 255, 144, 15, 10, 35, 140, 255,
                158, 34, 4, 57, 177, 255, 150, 16, 11, 50, 161, 255, 179, 69, 13, 12, 87, 200, 255, 203,
                108, 35, 8, 19, 100, 205, 255, 213, 114, 44, 8, 6, 21, 65, 157, 214, 255, 232, 163, 77,
                18, 13, 43, 95, 181, 255, 255, 221, 167, 103, 60, 31, 24, 80, 164, 217, 245, 255, 232, 186,
                141, 98, 65, 45, 31, 26, 13, 8, 27, 255, 54, 4, 11, 71, 255, 72, 84, 255, 91, 9,
                86, 255, 117, 11, 13, 126, 255, 129, 16, 14, 139, 255, 127, 18, 23, 154, 255, 146, 30, 29,
                149, 255, 153, 31, 6, 45, 149, 255, 186, 54, 7, 19, 78, 183, 255, 168, 43, 15, 82, 197,
                255, 161, 51, 9, 6, 30, 90, 191, 255, 200, 97, 29, 6, 10, 45, 124, 213, 255, 226, 122,
                37, 12, 38, 83, 147, 217, 255, 232, 167, 99, 58, 35, 14, 19, 37, 76, 130, 197, 245, 255,
                231, 175, 124, 72, 31, 9, 26, 62, 109, 149, 190, 216, 241, 255, 252, 222, 187, 148, 106, 68,
                42, 25, 14, 5, 93, 255, 102, 6, 9, 123, 255, 108, 11, 11, 116, 255, 129, 19, 4, 27,
                139, 255, 119, 5, 23, 144, 255, 132, 20, 38, 147, 255, 146, 15, 37, 151, 255, 167, 33, 8,
                47, 161, 255, 160, 49, 6, 8, 59, 180, 255, 170, 52, 8, 6, 61, 174, 255, 175, 67, 14,
                22, 78, 182, 255, 191, 79, 21, 13, 44, 110, 206, 255, 196, 105, 44, 17, 7, 24, 70, 150,
                235, 255, 221, 127, 25, 4, 11, 25, 68, 133, 207, 255, 229, 145, 69, 23, 9, 16, 32, 60,
                88, 132, 183, 234, 255, 243, 204, 144, 86, 38, 14, 10, 14, 19, 29, 47, 69, 97, 144, 197,
                235, 255, 243, 206, 148, 97, 48, 17, 48, 255, 45, 50, 255, 50, 55, 255, 59, 63, 255, 64,
                7, 72, 255, 74, 8, 6, 81, 255, 85, 7, 9, 96, 255, 89, 8, 2, 17, 95, 255, 81,
                3, 11, 114, 255, 101, 13, 11, 111, 255, 121, 14, 22, 126, 255, 131, 29, 28, 131, 255, 153,
                33, 44, 156, 255, 167, 62, 23, 4, 7, 13, 42, 109, 206, 255, 175, 68, 14, 7, 70, 149,
                214, 255, 229, 175, 112, 73, 43, 23, 14, 8, 11, 16, 21, 32, 50, 81, 135, 182, 231, 255,
                228, 183, 142, 83, 41, 10, 31, 255, 41, 3, 3, 67, 255, 72, 75, 255, 99, 6, 91, 255,
                111, 21, 8, 117, 255, 129, 16, 21, 123, 255, 134, 13, 27, 146, 255, 145, 30, 35, 166, 255,
                151, 34, 4, 4, 39, 146, 255, 170, 46, 10, 11, 66, 184, 255, 167, 49, 7, 8, 23, 77,
                170, 255, 182, 43, 13, 78, 190, 255, 216, 115, 26, 5, 29, 108, 210, 255, 200, 111, 40, 9,
                10, 57, 153, 243, 255, 191, 107, 44, 15, 11, 36, 65, 116, 173, 233, 255, 227, 164, 102, 54,
                20, 10, 10, 13, 29, 48, 91, 141, 191, 232, 254, 255, 237, 207, 167, 117, 84, 57, 46, 40,
                36, 32, 18, 6, 63, 255, 37, 70, 255, 66, 75, 255, 75, 3, 6, 83, 255, 81, 4, 12,
                94, 255, 94, 4, 14, 103, 255, 107, 8, 7, 110, 255, 106, 15, 14, 114, 255, 123, 17, 29,
                137, 255, 130, 10, 6, 31, 145, 255, 148, 38, 8, 12, 56, 160, 255, 139, 15, 12, 60, 169,
                255, 196, 67, 12, 30, 89, 201, 255, 175, 65, 11, 14, 45, 117, 215, 255, 203, 122, 55, 19,
                5, 9, 22, 49, 103, 172, 233, 255, 222, 178, 128, 71, 35, 16, 8, 52, 141, 214, 253, 255,
                239, 204, 171, 151, 132, 111, 91, 66, 49, 32, 20, 13, 8, 6, 4, 65, 255, 46, 4, 88,
                255, 98, 14, 9, 108, 255, 110, 14, 9, 111, 255, 110, 13, 13, 117, 255, 119, 19, 15, 122,
                255, 118, 17, 15, 122, 255, 124, 19, 19, 127, 255, 124, 17, 23, 131, 255, 134, 24, 3, 30,
                142, 255, 136, 26, 3, 28, 139, 255, 148, 34, 3, 5, 37, 149, 255, 150, 37, 5, 6, 44,
                157, 255, 157, 44, 7, 11, 55, 166, 255, 168, 60, 14, 9, 33, 92, 195, 255, 186, 90, 36,
                12, 7, 14, 29, 59, 115, 205, 255, 198, 109, 52, 24, 10
        };
    }

    /**
     * Returns the REG_COND table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #REG_COND} is initialized from this factory to split the table out of the class initializer.
     */
    private static float[] makeREG_COND() {
        return new float[] {0.15f, 0.12f};
    }

    /**
     * Returns the MEAN_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #MEAN_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static float[] makeMEAN_V() {
        return new float[] {0.110817075f, 0.17563242f, 0.30499664f, 0.53475165f, 0.6947636f, 0.88328713f, 1.0692836f, 1.2496489f, 1.4380369f, 1.6110579f, 1.8053242f, 2.0153108f, 2.2479281f, 2.47133f, 2.6959803f, 2.9006512f};
    }

    /**
     * Returns the MEAN_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #MEAN_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static float[] makeMEAN_UV() {
        return new float[] {0.09289457f, 0.25106147f, 0.48140478f, 0.6699562f, 0.85176575f, 1.045828f, 1.239984f, 1.4193453f, 1.5988212f, 1.7777773f, 1.9651974f, 2.1487794f, 2.3432646f, 2.529888f, 2.7280896f, 2.909268f};
    }

    /**
     * Returns the MIN_DIST_V table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #MIN_DIST_V} is initialized from this factory to split the table out of the class initializer.
     */
    private static float[] makeMIN_DIST_V() {
        return new float[] {0.011408983f, 0.006902917f, 0.013805822f, 0.014189318f, 0.012175977f, 0.014381051f, 0.016873825f, 0.015339851f, 0.016586185f, 0.017449021f, 0.01764083f, 0.018311977f, 0.02339325f, 0.022990376f, 0.025099665f, 0.020996332f, 0.06634474f};
    }

    /**
     * Returns the MIN_DIST_UV table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #MIN_DIST_UV} is initialized from this factory to split the table out of the class initializer.
     */
    private static float[] makeMIN_DIST_UV() {
        return new float[] {0.009779128f, 0.014285199f, 0.022242725f, 0.017161429f, 0.014572799f, 0.02250497f, 0.024518296f, 0.023009658f, 0.024543643f, 0.025502443f, 0.02806567f, 0.029816747f, 0.03298068f, 0.035377502f, 0.04033755f, 0.028186798f, 0.07401466f};
    }

    /**
     * Returns the QSTEP table literal.
     *
     * <p>Holds the byte-for-byte transcription of the corresponding C literal; the public field {@link #QSTEP} is initialized from this factory to split the table out of the class initializer.
     */
    private static float[][] makeQSTEP() {
        return new float[][] {
                    {0.17f, 0.3f},
                    {0.17f, 0.3f}
                };
    }

    /**
     * Prevents instantiation of this data-only table holder.
     */
    private LsfTables() {
        throw new AssertionError("no instances");
    }
}
