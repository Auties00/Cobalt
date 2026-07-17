package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

/**
 * Assorted decode-path tables from {@code smpl_tables.c} that the MLow 16 kHz, 60 ms, mono SMPL decoder
 * reads: voiced gain codebooks and their range-coding distributions, the voicing and LSF interpolation
 * distributions, the LTP interpolation kernel, the analysis filterbank coefficients, the LSF
 * interpolation weights, the postfilter and pulse-shaping coefficients, the packet-loss-concealment
 * coefficients, and the comfort-noise LPC seed.
 *
 * <p>This class is the decode-only subset of {@code smpl_tables.c}. The entries that
 * {@code smpl_tables.c} carries solely for the encoder, the bitrate controller, the 32 kHz and 48 kHz
 * upsampler, or the high-band extension are not ported; see the class-level note below for the exact
 * deferral list. Three families live here:
 * <ul>
 * <li><b>Voiced gain decode.</b> {@link #ACB_GAINS_LR_Q14} and {@link #ACB_GAINS_HR_Q14} are the
 * adaptive-codebook (pitch) gain codebooks read directly during voiced gain decode; the conditional
 * range-coding distributions {@link #acbGainsCmfLr(int)} and {@link #acbGainsCmfHr(int)}, and the
 * fixed-codebook voiced-gain distributions {@link #fcbgVCmf()} and {@link #fcbgVDeltaCmf()}, are built
 * from packed DCMFs.</li>
 * <li><b>Raw decode distributions.</b> {@link #VUV_CMFS} (voicing) and {@link #LSF_INTERP_CMF} (LSF
 * interpolation index) are already in cumulative-mass-function form in the source and are passed
 * straight to the range decoder, so they are transcribed as-is rather than rebuilt.</li>
 * <li><b>Float synthesis coefficients.</b> The remaining floating-point tables drive the decode-side
 * synthesis filters (LTP interpolation, filterbank split, LSF interpolation, postfilter tilt, unvoiced
 * pulse shaping, packet-loss concealment, comfort noise).</li>
 * </ul>
 *
 * <p>The {@code _Q14} suffix denotes a value scaled by {@code 2^14} (real value times {@code 16384}).
 * The DCMF tables are expanded into runtime CMFs by {@link CmfBuilder#dcmfToCmf(byte[])} at
 * construction; the raw CMF tables are used directly. See {@link CmfBuilder} for the bit-exact transform
 * contract.
 *
 * @implNote This implementation transcribes the static literals byte-for-byte from {@code smpl_tables.c}.
 * The voiced-gain inverse-probability cost tables ({@code acbg_inv_prob_*}, {@code fcbg_v_inv_prob},
 * {@code fcbg_v_delta_inv_prob}) that {@code smpl_celp.c} derives from these distributions are
 * encoder-only rate-distortion weights and are intentionally not ported. The following
 * {@code smpl_tables.c} entries are also deferred because the 16 kHz mono decode path never reads them:
 * the high-band extension coefficients {@code smpl_hb_wght_coef}, {@code smpl_hb_post_coef},
 * {@code smpl_lb_wght_coef} (high-band, out of the 16 kHz scope); the 32 kHz and 48 kHz upsampler
 * coefficients {@code smpl_ap_coefs_32_48} and {@code smpl_fir_coefs_32_48} (above-16 kHz output only);
 * the encoder perceptual-weighting and rate-control tables {@code smpl_perc_emph_pitch},
 * {@code smpl_perc_emph_v}, {@code smpl_perc_emph_uv}, {@code smpl_low_rate_thr},
 * {@code smpl_fcb_tot_surv_20ms_max}, {@code smpl_vuv_weights}, {@code smpl_rate_control_model_comp5},
 * and {@code smpl_rate_control_thrs_comp5} (encoder/bitrate-controller only). The
 * {@code smpl_max_pulses_per_frame} table is read by both the encoder and the pulse decoder; it is
 * ported here as {@link #MAX_PULSES_PER_FRAME} because the decode path needs it.
 */
public final class MiscTables {
    /**
     * Number of entries per adaptive-codebook gain codebook vector, the {@code SMPL_ACBG_M} constant. Each
     * vector holds a leading and a trailing tap gain.
     */
    public static final int ACBG_M = 2;

    /**
     * Number of adaptive-codebook gain codebook vectors, the {@code SMPL_ACBG_N} constant.
     */
    public static final int ACBG_N = 16;

    /**
     * Low-rate adaptive-codebook (pitch) gain codebook, Q14, the {@code smpl_cb_acbgains_lr_Q14} table,
     * laid out as 16 vectors of 2 entries.
     *
     * <p>Indexed as {@code ACB_GAINS_LR_Q14[acbIdx * 2 + tap]}, each entry is the real tap gain times
     * {@code 2^14}. Voiced gain decode reads this directly to accumulate the mean adaptive-codebook gain.
     */
    public static final short[] ACB_GAINS_LR_Q14 = {
            2812, 2484,
            0, 0,
            -362, 2465,
            -337, 703,
            3033, 1474,
            13536, 220,
            -2630, 9226,
            6032, 3499,
            -220, 441,
            7661, 4243,
            11521, 0,
            1430, 779,
            4495, 2724,
            15535, 343,
            -779, 1559,
            480, 481
    };

    /**
     * High-rate adaptive-codebook (pitch) gain codebook, Q14, the {@code smpl_cb_acbgains_hr_Q14} table,
     * laid out as 16 vectors of 2 entries.
     *
     * <p>Indexed as {@code ACB_GAINS_HR_Q14[acbIdx * 2 + tap]}, each entry is the real tap gain times
     * {@code 2^14}. Voiced gain decode reads this directly to accumulate the mean adaptive-codebook gain.
     */
    public static final short[] ACB_GAINS_HR_Q14 = {
            16039, 91,
            0, 0,
            4310, 4930,
            -1431, 2862,
            2893, 0,
            8009, 4075,
            2754, 4223,
            8367, 354,
            4640, 1254,
            -176, 2734,
            -1222, 5017,
            -476, 1506,
            11351, 567,
            1243, 0,
            10601, 22,
            14088, 108
    };

    /**
     * Voicing decision distribution, already in cumulative-mass-function form, the {@code smpl_vuv_cmfs}
     * table, indexed {@code [context][symbol]} with three contexts of three entries.
     *
     * <p>Context 0 is unconditional (first frame), context 1 is previous-frame-unvoiced, context 2 is
     * previous-frame-voiced. Each row is a 3-entry CMF passed directly to the range decoder to read the
     * voicing flag, exactly as {@code smpl_decode_lb_params} does.
     */
    public static final int[][] VUV_CMFS = {
            {0, 5, 17},
            {0, 7, 9},
            {0, 1, 10}
    };

    /**
     * LSF interpolation index distribution, already in cumulative-mass-function form, the
     * {@code smpl_lsf_interp_cmf} table, three entries.
     *
     * <p>Passed directly to the range decoder to read the LSF interpolation index for multi-subframe
     * voiced frames, exactly as {@code smpl_decode_lb_params} does.
     */
    public static final int[] LSF_INTERP_CMF = {0, 5, 7};

    /**
     * Long-term-prediction (pitch) interpolation kernel, the {@code smpl_interpol_kernel} table, 16 taps
     * (two times the eight-tap delay).
     *
     * <p>This is the symmetric fractional-delay interpolation filter the CELP synthesis applies when the
     * pitch lag has a sub-sample fractional part; the decoder dot-products it against the excitation
     * history.
     */
    public static final float[] INTERPOL_KERNEL = {
            -6.3925986e-6f, 0.00011064114f, -0.0009153038f, 0.00484772f, -0.018698348f, 0.05759091f, -0.15997477f, 0.6170455f,
            0.61704546f, -0.15997475f, 0.057590906f, -0.018698348f, 0.00484772f, -0.0009153038f, 0.000110641144f, -6.392598e-6f
    };

    /**
     * Packet-loss-concealment excitation injection coefficients, the {@code smpl_plc_inject_coef} table.
     *
     * <p>Applied to the regenerated excitation during concealment of a lost frame.
     */
    public static final float[] PLC_INJECT_COEF = {0.5f, -0.5f};

    /**
     * Comfort-noise emphasis coefficients, the {@code smpl_cng_emph_coef} table, a first-order
     * de-emphasis {@code (1, a)} pair.
     *
     * <p>Applied to the comfort-noise excitation during discontinuous-transmission (DTX) silence.
     */
    public static final float[] CNG_EMPH_COEF = {1.0f, -0.9f};

    /**
     * Low-band analysis-filterbank coefficients, the {@code smpl_filterbankL_coef} table, a 3-tap
     * all-pole section.
     *
     * <p>The decoder uses the low-band and high-band filterbank pair to split or recombine the synthesis
     * signal around the band boundary.
     */
    public static final float[] FILTERBANK_L_COEF = {1.0f, 0.60797656f, 0.036630828f};

    /**
     * High-band analysis-filterbank coefficients, the {@code smpl_filterbankH_coef} table, a 3-tap
     * all-pole section.
     *
     * <p>The companion of {@link #FILTERBANK_L_COEF} for the high-band path of the filterbank split.
     */
    public static final float[] FILTERBANK_H_COEF = {1.0f, 1.1034178f, 0.2197291f};

    /**
     * Single-subframe LSF interpolation weight, the {@code smpl_lsf_interpol_1} scalar.
     *
     * <p>Blends the previous-frame LSFs with the current decoded LSFs for a 1-subframe frame.
     */
    public static final float LSF_INTERPOL_1 = 0.95f;

    /**
     * Two-subframe LSF interpolation weights, the {@code smpl_lsf_interpol_2} table, indexed
     * {@code [interpolationIndex][subframe]}.
     *
     * <p>Selected by the decoded LSF interpolation index, these blend the previous and current LSFs
     * across the two subframes.
     */
    public static final float[][] LSF_INTERPOL_2 = {
            {0.75f, 1.0f},
            {0.4f, 0.95f}
    };

    /**
     * Four-subframe LSF interpolation weights, the {@code smpl_lsf_interpol_4} table, indexed
     * {@code [interpolationIndex][subframe]}.
     *
     * <p>Selected by the decoded LSF interpolation index, these blend the previous and current LSFs
     * across the four subframes.
     */
    public static final float[][] LSF_INTERPOL_4 = {
            {0.55f, 0.88f, 1.0f, 1.0f},
            {0.3f, 0.65f, 0.95f, 1.0f}
    };

    /**
     * Single-subframe DTX LSF interpolation weight, the {@code smpl_lsf_interpol_dtx_1} scalar.
     *
     * <p>The discontinuous-transmission counterpart of {@link #LSF_INTERPOL_1} used when reconstructing
     * LSFs from a silence-insertion-descriptor frame.
     */
    public static final float LSF_INTERPOL_DTX_1 = 0.25f;

    /**
     * Two-subframe DTX LSF interpolation weights, the {@code smpl_lsf_interpol_dtx_2} table, one weight
     * per subframe.
     *
     * <p>The discontinuous-transmission counterpart of {@link #LSF_INTERPOL_2}.
     */
    public static final float[] LSF_INTERPOL_DTX_2 = {0.15f, 0.3f};

    /**
     * Four-subframe DTX LSF interpolation weights, the {@code smpl_lsf_interpol_dtx_4} table, one weight
     * per subframe.
     *
     * <p>The discontinuous-transmission counterpart of {@link #LSF_INTERPOL_4}.
     */
    public static final float[] LSF_INTERPOL_DTX_4 = {0.1f, 0.157f, 0.2f, 0.3f};

    /**
     * Postfilter spectral-tilt coefficients, the {@code smpl_post_tilt_coefs} table, indexed
     * {@code [lowRate][tap]} with a high-rate row and a low-rate row.
     *
     * <p>The decoder applies the rate-selected first-order tilt to the synthesized output to shape the
     * spectrum.
     */
    public static final float[][] POST_TILT_COEFS = {
            {1.0f, 0.0f},
            {0.84f, 0.16f}
    };

    /**
     * Unvoiced pulse-shaping coefficients, the {@code smpl_uv_pulse_shaping_coefs} table, indexed
     * {@code [lowRate][branch][tap]}.
     *
     * <p>The first index selects the rate (0 high, 1 low), the second selects one of two shaping
     * branches, and the third is the two-tap coefficient pair the decoder convolves with the unvoiced
     * pulse excitation.
     */
    public static final float[][][] UV_PULSE_SHAPING_COEFS = {
            {{1.0f, 0.0f}, {1.0f, 0.0f}},
            {{0.5f, 0.1665f}, {1.0f, -0.333f}}
    };

    /**
     * Maximum pulse count per frame, the {@code smpl_max_pulses_per_frame} table, indexed
     * {@code [lowRate][voicingClass]}.
     *
     * <p>The first index selects the rate (0 high, 1 low); the second selects the voicing class in
     * {@code {background noise, unvoiced, voiced}}. The pulse decoder scales the selected value by
     * {@code framelen / 320} to bound the number of decoded pulses.
     */
    public static final int[][] MAX_PULSES_PER_FRAME = {
            {80, 160, 160},
            {16, 32, 32}
    };

    /**
     * Packet-loss-concealment comfort-noise LPC seed, the {@code smpl_plc_cng_init} table, one entry per
     * LPC coefficient (LPC order 16).
     *
     * <p>The concealment path seeds its line-spectral-frequency state from these values when no prior
     * voiced LPC is available.
     */
    public static final float[] PLC_CNG_INIT = {
            0.065961175f, 0.21926339f, 0.40487507f, 0.59738964f,
            0.7911506f, 0.98644555f, 1.1819322f, 1.3775148f,
            1.573289f, 1.7692552f, 1.9650295f, 2.1610913f,
            2.357345f, 2.5532153f, 2.7495646f, 2.94601f
    };

    /**
     * Low-rate adaptive-codebook gain conditional DCMF, the {@code smpl_acbgains_dcmf_lr} table, 17 rows
     * of 16 symbols. Each row is conditioned on the previous adaptive-codebook index. Expanded into
     * {@link #acbGainsCmfLr(int)} at construction.
     */
    private static final byte[] ACB_GAINS_DCMF_LR = unsignedBytes(
            103, 70, 48, 3, 122, 135, 47, 192, 2, 255, 99, 96, 186, 194, 4, 28,
            161, 90, 76, 3, 181, 60, 37, 219, 2, 132, 81, 146, 255, 43, 3, 36,
            114, 222, 55, 6, 203, 34, 42, 154, 6, 255, 33, 209, 225, 78, 6, 45,
            198, 161, 110, 8, 239, 26, 35, 162, 4, 117, 42, 214, 255, 33, 6, 72,
            55, 255, 124, 55, 124, 55, 55, 55, 55, 78, 55, 215, 111, 55, 55, 167,
            154, 136, 77, 4, 220, 33, 38, 166, 2, 144, 50, 196, 255, 43, 4, 41,
            56, 21, 19, 3, 48, 255, 38, 220, 2, 225, 107, 31, 122, 227, 2, 11,
            63, 38, 23, 4, 77, 85, 58, 190, 4, 255, 53, 53, 145, 138, 4, 14,
            95, 47, 33, 2, 110, 146, 53, 255, 2, 219, 79, 73, 198, 122, 2, 15,
            84, 255, 84, 84, 147, 84, 84, 84, 84, 120, 84, 120, 84, 84, 84, 84,
            73, 58, 25, 1, 95, 99, 52, 175, 1, 255, 48, 69, 151, 184, 1, 15,
            105, 32, 43, 2, 84, 225, 34, 255, 2, 156, 129, 49, 189, 124, 3, 19,
            152, 230, 89, 6, 253, 28, 40, 153, 2, 195, 31, 255, 249, 58, 5, 61,
            138, 84, 54, 3, 173, 96, 45, 247, 2, 176, 83, 128, 255, 69, 2, 26,
            22, 17, 8, 1, 23, 106, 26, 88, 1, 182, 37, 18, 50, 255, 1, 6,
            218, 174, 228, 65, 186, 65, 65, 92, 65, 65, 65, 255, 174, 65, 65, 174,
            117, 255, 101, 16, 180, 20, 33, 94, 10, 131, 20, 222, 143, 38, 15, 105);

    /**
     * High-rate adaptive-codebook gain conditional DCMF, the {@code smpl_acbgains_dcmf_hr} table, 17 rows
     * of 16 symbols. Each row is conditioned on the previous adaptive-codebook index. Expanded into
     * {@link #acbGainsCmfHr(int)} at construction.
     */
    private static final byte[] ACB_GAINS_DCMF_HR = unsignedBytes(
            254, 105, 212, 26, 110, 255, 202, 93, 152, 121, 110, 43, 150, 20, 81, 176,
            255, 28, 100, 5, 26, 184, 61, 29, 36, 26, 28, 9, 61, 4, 27, 116,
            121, 255, 161, 39, 195, 215, 191, 75, 186, 178, 119, 82, 68, 41, 43, 56,
            188, 65, 243, 15, 74, 255, 205, 79, 123, 84, 95, 26, 139, 13, 67, 154,
            81, 219, 173, 70, 219, 165, 234, 102, 231, 255, 191, 119, 87, 60, 62, 59,
            106, 255, 182, 49, 242, 196, 233, 95, 247, 228, 152, 96, 81, 45, 54, 61,
            236, 55, 178, 10, 56, 255, 131, 54, 85, 58, 59, 18, 93, 9, 43, 133,
            123, 95, 224, 24, 113, 202, 255, 105, 186, 134, 135, 38, 141, 18, 82, 111,
            126, 97, 204, 34, 126, 186, 255, 141, 210, 147, 149, 46, 165, 22, 113, 122,
            96, 156, 185, 42, 188, 178, 255, 116, 248, 199, 157, 66, 109, 29, 69, 75,
            102, 207, 194, 57, 224, 193, 255, 107, 253, 242, 180, 95, 97, 44, 60, 64,
            105, 119, 202, 39, 140, 189, 255, 110, 207, 173, 165, 54, 119, 24, 75, 85,
            74, 255, 142, 59, 214, 150, 182, 76, 194, 215, 138, 122, 61, 56, 41, 45,
            200, 53, 255, 17, 66, 238, 222, 109, 129, 78, 101, 21, 227, 11, 110, 243,
            74, 255, 128, 50, 187, 149, 154, 63, 165, 184, 115, 101, 52, 47, 37, 34,
            159, 66, 232, 26, 86, 196, 255, 146, 171, 113, 134, 31, 245, 16, 145, 190,
            255, 29, 182, 7, 33, 235, 115, 55, 59, 37, 47, 11, 139, 6, 60, 234);

    /**
     * Number of symbols in the voiced fixed-codebook-gain DCMF, the {@code SMPL_FCBG_V_N} constant.
     */
    private static final int FCBG_V_N = 34;

    /**
     * Number of symbols in the voiced fixed-codebook-gain delta DCMF, the {@code SMPL_FCBG_V_DELTA_N}
     * constant.
     */
    private static final int FCBG_V_DELTA_N = 67;

    /**
     * Voiced fixed-codebook-gain absolute DCMF, the {@code smpl_fcbg_v_dcmf} table, 34 symbols. Read when
     * no previous fixed-codebook gain is available. Expanded into {@link #fcbgVCmf()} at construction.
     */
    private static final byte[] FCBG_V_DCMF = unsignedBytes(
            107, 12, 17, 25, 31, 41, 52, 65, 83, 103, 122, 146, 169, 191, 210, 227,
            240, 249, 255, 253, 246, 229, 200, 161, 120, 82, 51, 29, 14, 6, 2, 2,
            2, 2);

    /**
     * Voiced fixed-codebook-gain delta DCMF, the {@code smpl_fcbg_v_delta_dcmf} table, 67 symbols. Read
     * to decode a signed delta from the previous fixed-codebook gain index. Expanded into
     * {@link #fcbgVDeltaCmf()} at construction.
     */
    private static final byte[] FCBG_V_DELTA_DCMF = unsignedBytes(
            1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 6, 8, 10, 12, 12,
            12, 13, 14, 14, 14, 13, 12, 11, 10, 9, 8, 9, 15, 33, 65, 119,
            196, 255, 220, 144, 90, 57, 36, 23, 17, 14, 12, 12, 12, 13, 12, 12,
            12, 12, 12, 11, 11, 10, 9, 7, 6, 4, 3, 2, 1, 1, 1, 1,
            1, 1, 1);

    /**
     * CallRuntime conditional CMFs for the low-rate adaptive-codebook gain, built row by row from
     * {@link #ACB_GAINS_DCMF_LR}; 17 rows indexed by {@code previousAcbIndex + 1}.
     */
    private static final int[][] ACB_GAINS_CMF_LR = buildRowCmfs(ACB_GAINS_DCMF_LR, ACBG_N);

    /**
     * CallRuntime conditional CMFs for the high-rate adaptive-codebook gain, built row by row from
     * {@link #ACB_GAINS_DCMF_HR}; 17 rows indexed by {@code previousAcbIndex + 1}.
     */
    private static final int[][] ACB_GAINS_CMF_HR = buildRowCmfs(ACB_GAINS_DCMF_HR, ACBG_N);

    /**
     * CallRuntime CMF for the voiced fixed-codebook-gain absolute index, built from {@link #FCBG_V_DCMF}.
     */
    private static final int[] FCBG_V_CMF = CmfBuilder.dcmfToCmf(FCBG_V_DCMF);

    /**
     * CallRuntime CMF for the voiced fixed-codebook-gain delta, built from {@link #FCBG_V_DELTA_DCMF}.
     */
    private static final int[] FCBG_V_DELTA_CMF = CmfBuilder.dcmfToCmf(FCBG_V_DELTA_DCMF);

    /**
     * Prevents instantiation of this constants holder.
     */
    private MiscTables() {
        throw new AssertionError("no instances");
    }

    /**
     * Returns the low-rate adaptive-codebook gain conditional CMF for the given previous-index context,
     * an {@code int[17]} strictly increasing array.
     *
     * <p>The context is {@code previousAcbIndex + 1}, so context 0 is the no-previous-index case (a fresh
     * conditional-coding run) and contexts 1 through 16 condition on the prior decoded index, exactly as
     * {@code decode_lb_voiced} selects with {@code (pPD->prev_acb_idx + 1) * (SMPL_ACBG_N + 1)}.
     *
     * @param context the previous-index context, {@code previousAcbIndex + 1}, in {@code [0, 16]}
     * @return the selected low-rate adaptive-codebook gain CMF, shared and never modified
     * @throws ArrayIndexOutOfBoundsException if {@code context} is outside {@code [0, 16]}
     */
    public static int[] acbGainsCmfLr(int context) {
        return ACB_GAINS_CMF_LR[context];
    }

    /**
     * Returns the high-rate adaptive-codebook gain conditional CMF for the given previous-index context,
     * an {@code int[17]} strictly increasing array.
     *
     * <p>The context is {@code previousAcbIndex + 1}, with the same meaning as in
     * {@link #acbGainsCmfLr(int)}.
     *
     * @param context the previous-index context, {@code previousAcbIndex + 1}, in {@code [0, 16]}
     * @return the selected high-rate adaptive-codebook gain CMF, shared and never modified
     * @throws ArrayIndexOutOfBoundsException if {@code context} is outside {@code [0, 16]}
     */
    public static int[] acbGainsCmfHr(int context) {
        return ACB_GAINS_CMF_HR[context];
    }

    /**
     * Returns the runtime CMF for the voiced fixed-codebook-gain absolute index, an {@code int[35]}
     * strictly increasing array.
     *
     * @return the voiced fixed-codebook-gain absolute CMF, shared and never modified
     */
    public static int[] fcbgVCmf() {
        return FCBG_V_CMF;
    }

    /**
     * Returns the runtime CMF for the voiced fixed-codebook-gain delta, an {@code int[68]} strictly
     * increasing array.
     *
     * <p>The decoder windows into this array starting at {@code -previousFcbIndex} so the delta is bounded
     * to the valid index range, exactly as {@code decode_lb_voiced} does with
     * {@code &pCelp->fcbgains_v_delta_cmf[SMPL_FCBG_V_N - 1] + min_delta}.
     *
     * @return the voiced fixed-codebook-gain delta CMF, shared and never modified
     */
    public static int[] fcbgVDeltaCmf() {
        return FCBG_V_DELTA_CMF;
    }

    /**
     * Builds an array of per-row CMFs from a flat DCMF table whose rows are stored back to back,
     * mirroring the row-stepping loop in {@code smpl_init_celp_Tbls}.
     *
     * @param flat   the flat DCMF table, a whole number of rows of {@code rowLen} bytes each
     * @param rowLen the number of symbols per row
     * @return a freshly allocated {@code int[rows][]} of built CMFs, one per row
     */
    private static int[][] buildRowCmfs(byte[] flat, int rowLen) {
        var rows = flat.length / rowLen;
        var out = new int[rows][];
        for (var r = 0; r < rows; r++) {
            out[r] = CmfBuilder.dcmfToCmf(flat, r * rowLen, rowLen);
        }
        return out;
    }

    /**
     * Packs a list of unsigned-byte literals into a {@code byte[]}, narrowing each {@code int} in
     * {@code [0, 255]} to a signed {@code byte} that {@link CmfBuilder} later unmasks with
     * {@code & 0xFF}.
     *
     * @param values the unsigned byte values, each in {@code [0, 255]}
     * @return a freshly allocated {@code byte[]} of the same length, each entry the narrowed input
     */
    private static byte[] unsignedBytes(int... values) {
        var out = new byte[values.length];
        for (var i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }
}
