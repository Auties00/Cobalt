package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

/**
 * Residual-energy (resnrg) quantization tables for the MLow unvoiced decode path, the port of
 * {@code smpl_nrgres_tables.c}.
 *
 * <p>When an MLow frame is decoded as unvoiced (or as conditionally unvoiced background noise), the
 * excitation is a scaled fixed codebook pulse train whose per-subframe energy is transmitted as a
 * quantized residual energy in decibels. This class carries every static table that decode reads to
 * reconstruct that energy: the frame-level gain step sizes, the multi-subframe shape codebooks, and the
 * delta cumulative mass functions (DCMFs) the range decoder uses to read the frame gain index, the shape
 * index, and the per-subframe fixed-codebook gain offset.
 *
 * <p>The decode flow, mirroring {@code decode_lb_unvoiced} in {@code smpl_param_coding.c}, is keyed by
 * the subframe count of the frame (1, 2, or 4):
 * <ul>
 * <li>The frame gain index {@code nrgres_frame_qi} is range-decoded against {@link #gain1Cmf()},
 * {@link #gain2Cmf()}, or {@link #gain4Cmf()} per subframe count.</li>
 * <li>For 2 and 4 subframes a shape index {@code nrgres_shape_qi} is range-decoded against
 * {@link #shapeCb2Cmf()} or {@link #shapeCb4Cmf()}, then used to index {@link #SHAPE_CB_2_Q10} or
 * {@link #SHAPE_CB_4_Q10} for the per-subframe Q10 decibel deltas.</li>
 * <li>The dequantized frame energy is {@code nrgres_frame_qi * smpl_nrg_step_db_Q14[table_ix]} (see
 * {@link #NRG_STEP_DB_Q14}) offset by {@link #RES_NRG_MIN_DB}, then the shape delta is added.</li>
 * <li>For each subframe with pulses, a fixed-codebook gain offset index is range-decoded against a
 * window of one of the {@link #FCBG_OFFSET_CMF fixed-codebook gain offset} CMFs, selected by subframe
 * count, pulse count bin, and the clamped per-subframe energy.</li>
 * </ul>
 *
 * <p>The {@code *_Q10} and {@code *_Q14} suffixes denote fixed-point scaling: a {@code Q10} value is the
 * real value times {@code 2^10}, a {@code Q14} value times {@code 2^14}. All DCMF tables are expanded
 * into runtime CMFs by {@link CmfBuilder#dcmfToCmf(byte[])} at construction; see {@link CmfBuilder} for
 * the bit-exact transform contract.
 *
 * @implNote This implementation transcribes the static {@code int16_t}/{@code uint8_t} table literals
 * byte-for-byte from {@code smpl_nrgres_tables.c}. The shape and gain codebook data are signed 16-bit
 * and carried in {@code short[]}; every DCMF is {@code uint8_t} in C and carried in a {@code byte[]}
 * whose elements {@link CmfBuilder#dcmfToCmf(byte[])} reads with an {@code 0xFF} mask, so the high-bit
 * bytes (values above 127) round-trip unsigned. The built CMF arrays are validated to match a C
 * {@code smpl_dcmf_to_cmf} dump exactly.
 */
public final class NrgResTables {
    /**
     * Minimum residual energy in decibels, the {@code SMPL_RES_NRG_MIN_DB} constant.
     *
     * <p>This is the origin the dequantized frame energy is offset from and the lower clamp applied to
     * each per-subframe energy before the fixed-codebook gain offset is decoded.
     */
    public static final int RES_NRG_MIN_DB = -85;

    /**
     * Maximum residual energy in decibels, the {@code SMPL_RES_NRG_MAX_DB} constant.
     *
     * <p>This is the upper clamp applied to each per-subframe energy before the fixed-codebook gain
     * offset is decoded.
     */
    public static final int RES_NRG_MAX_DB = 0;

    /**
     * Frame-energy quantization step size in decibels (Q14), the {@code smpl_nrg_step_db_Q14} table,
     * indexed by subframe-count bin (0 for 1 subframe, 1 for 2 subframes, 2 for 4 subframes).
     *
     * <p>The dequantized frame energy in Q14 decibels is {@code nrgres_frame_qi * NRG_STEP_DB_Q14[bin]}
     * offset by {@code RES_NRG_MIN_DB << 14}. The values are the real step sizes times {@code 2^14}.
     */
    public static final short[] NRG_STEP_DB_Q14 = {
            16384, 19843, 16686
    };

    /**
     * Residual-energy shape codebook for 4-subframe frames, Q10 decibels, the
     * {@code nrgres_shape_CB_4_Q10} table, laid out as 98 vectors of 4 entries.
     *
     * <p>Indexed as {@code SHAPE_CB_4_Q10[shapeIdx * 4 + subframe]}, each entry is the Q10 decibel
     * offset added to the dequantized frame energy for that subframe. Values are signed.
     */
    public static final short[] SHAPE_CB_4_Q10 = {
            -2515, -2238, 2632, 2121,
            790, 3973, -2872, -1891,
            -533, 2847, 1453, -3767,
            -6174, -402, 2668, 3908,
            -1623, -1458, 153, 2928,
            -1254, 3197, -476, -1467,
            1803, -1086, 270, -987,
            1952, -66, -1257, -629,
            161, 19, -85, -96,
            4833, 3147, -105, -7875,
            -1320, 1377, -1156, 1099,
            3398, -2247, 1485, -2637,
            -3031, 2756, 1841, -1566,
            -1487, 2202, -2668, 1954,
            5518, -5344, 522, -696,
            8400, -3123, -6235, 958,
            5152, -2444, -2811, 102,
            2513, -82, 1181, -3612,
            -561, -197, -1074, 1832,
            -294, -1250, -1839, 3383,
            5126, 522, -782, -4866,
            -7760, -5178, -1840, 14779,
            -1119, 6007, -1489, -3399,
            -4567, -2543, 1855, 5255,
            53, -1626, 67, 1506,
            -12256, -7706, -1982, 21943,
            3549, -969, -1096, -1484,
            -10824, 2981, 2204, 5639,
            -229, 1106, 945, -1821,
            -9237, 10157, 1616, -2537,
            4916, -199, -2177, -2540,
            6673, 984, -3355, -4302,
            -7130, -4677, 8925, 2882,
            445, 2762, -348, -2859,
            -196, -1859, 1761, 294,
            2725, -2093, -966, 334,
            -3908, -308, 3675, 541,
            735, 890, -2516, 891,
            504, 1631, -1157, -977,
            -17817, 2119, 7104, 8594,
            -2056, 1897, -198, 356,
            292, -4544, -287, 4538,
            -1455, -304, 603, 1156,
            -18259, -12643, 15247, 15655,
            4177, 1778, -1815, -4140,
            1425, 576, -294, -1707,
            -1301, 5132, 2838, -6669,
            -4727, -3148, -905, 8781,
            -650, 152, -4654, 5152,
            13746, 2320, -6259, -9807,
            -1356, 396, 3789, -2829,
            2337, 1947, -29, -4256,
            6033, 820, -5730, -1123,
            -1795, 1091, 1080, -377,
            2208, -1921, -3314, 3027,
            9688, 5218, -3754, -11152,
            3814, -3941, -6183, 6310,
            -1017, -2391, 4393, -984,
            10944, -1182, -5011, -4751,
            -4640, 7201, -218, -2343,
            -1278, 4720, -4212, 770,
            2777, 1333, -5944, 1833,
            -16066, 8107, 5165, 2795,
            2530, -5020, 6073, -3582,
            -2111, -7534, 4575, 5070,
            -8702, -3762, 4050, 8414,
            1335, -997, -1567, 1229,
            9348, 1534, -3959, -6922,
            2440, 1153, -2175, -1418,
            -2715, -4538, -4478, 11730,
            569, -885, 2032, -1716,
            3529, -91, -3218, -219,
            2157, -4121, 191, 1772,
            -2123, -1968, -1355, 5446,
            1475, -354, 3651, -4772,
            1654, -3521, 2726, -859,
            2393, 6820, -2958, -6255,
            -3861, 1365, 1177, 1319,
            7614, -1638, -2789, -3187,
            -3628, -2635, 6902, -639,
            1925, 2295, -1451, -2769,
            -3683, 4517, -981, 147,
            -1260, -529, 2339, -550,
            3013, 639, -1050, -2602,
            3651, 1959, -3218, -2391,
            6267, 3124, -2926, -6464,
            -8180, 3900, 4191, 89,
            -3372, -611, 1042, 2941,
            -2510, 856, -925, 2579,
            -11667, -8436, 10605, 9498,
            6427, -2733, 1887, -5581,
            1581, -1722, -328, 469,
            2011, 1989, -3606, -394,
            -1014, 2197, -1200, 17,
            1544, -2555, 765, 247,
            1188, -183, 1966, -2972,
            -6057, 3480, -2284, 4860,
            -25659, 8466, 8891, 8303
    };

    /**
     * Residual-energy shape codebook for 2-subframe frames, Q10 decibels, the
     * {@code nrgres_shape_CB_2_Q10} table, laid out as 22 vectors of 2 entries.
     *
     * <p>Indexed as {@code SHAPE_CB_2_Q10[shapeIdx * 2 + subframe]}, each entry is the Q10 decibel
     * offset added to the dequantized frame energy for that subframe. Values are signed.
     */
    public static final short[] SHAPE_CB_2_Q10 = {
            -9672, 9672,
            -4437, 4437,
            -907, 907,
            3610, -3610,
            -2857, 2857,
            9345, -9345,
            -2220, 2220,
            420, -420,
            -3565, 3565,
            -1577, 1577,
            -184, 184,
            2973, -2973,
            2389, -2389,
            1774, -1774,
            -7315, 7315,
            4405, -4405,
            -5630, 5630,
            -12927, 12927,
            -17376, 17376,
            1107, -1107,
            7058, -7058,
            5481, -5481
    };

    /**
     * Delta cumulative mass function for the 4-subframe shape index, the {@code nrgres_shape_CB_4_dcmf}
     * table, 98 symbols. Expanded into {@link #shapeCb4Cmf()} at construction.
     */
    private static final byte[] SHAPE_CB_4_DCMF = unsignedBytes(
            34, 13, 12, 37, 46, 23, 45, 74, 255, 19, 10, 11, 19, 9, 10, 11, 19, 14, 40, 19, 24, 34, 18,
            41, 42, 28, 38, 24, 33, 17, 43, 32, 27, 16, 24, 18, 20, 17, 48, 23, 17, 16, 94, 29, 49, 99,
            10, 33, 10, 17, 12, 32, 16, 22, 19, 17, 14, 17, 19, 20, 7, 11, 24, 8, 14, 35, 29, 22, 25, 17,
            16, 22, 12, 32, 11, 11, 15, 29, 26, 21, 30, 16, 28, 66, 23, 27, 21, 51, 20, 33, 11, 7, 7, 15,
            5, 5, 15, 20);

    /**
     * Delta cumulative mass function for the 2-subframe shape index, the {@code nrgres_shape_CB_2_dcmf}
     * table, 22 symbols. Expanded into {@link #shapeCb2Cmf()} at construction.
     */
    private static final byte[] SHAPE_CB_2_DCMF = unsignedBytes(
            47, 52, 137, 58, 61, 21, 73, 240, 55, 95, 255, 68, 83, 108, 48, 50, 49, 45, 35, 155, 31, 41);

    /**
     * Delta cumulative mass function for the 1-subframe frame gain index, the {@code smpl_nrgres_gain_1_dcmf}
     * table, 86 symbols. Expanded into {@link #gain1Cmf()} at construction.
     */
    private static final byte[] GAIN_1_DCMF = unsignedBytes(
            145, 199, 204, 202, 211, 218, 233, 243, 247, 248, 250, 250, 253, 255, 251, 249, 244, 237,
            233, 227, 224, 221, 222, 218, 210, 206, 204, 202, 203, 201, 202, 203, 205, 209, 214, 220,
            225, 233, 234, 237, 240, 243, 246, 247, 248, 246, 246, 246, 241, 234, 226, 221, 209, 194,
            178, 160, 144, 123, 106, 88, 73, 59, 46, 36, 26, 18, 12, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8);

    /**
     * Delta cumulative mass function for the 2-subframe frame gain index, the {@code smpl_nrgres_gain_2_dcmf}
     * table, 71 symbols. Expanded into {@link #gain2Cmf()} at construction.
     */
    private static final byte[] GAIN_2_DCMF = unsignedBytes(
            139, 194, 196, 200, 209, 225, 239, 243, 246, 246, 253, 255, 253, 249, 244, 239, 233, 231,
            230, 228, 219, 216, 214, 214, 213, 211, 214, 215, 220, 223, 230, 234, 237, 240, 242, 244,
            245, 245, 246, 242, 237, 230, 220, 210, 190, 170, 148, 125, 104, 83, 64, 49, 36, 22, 14, 8,
            8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8);

    /**
     * Delta cumulative mass function for the 4-subframe frame gain index, the {@code smpl_nrgres_gain_4_dcmf}
     * table, 84 symbols. Expanded into {@link #gain4Cmf()} at construction.
     */
    private static final byte[] GAIN_4_DCMF = unsignedBytes(
            143, 195, 199, 198, 205, 215, 229, 240, 245, 244, 249, 249, 254, 255, 253, 251, 247, 242,
            237, 233, 231, 230, 231, 223, 218, 216, 215, 214, 212, 210, 211, 216, 213, 219, 220, 227,
            232, 233, 236, 237, 240, 238, 242, 241, 242, 241, 238, 235, 230, 219, 216, 205, 189, 172,
            154, 136, 117, 100, 82, 66, 53, 42, 31, 20, 15, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8);

    /**
     * Number of subframe-count bins for the fixed-codebook gain offset CMFs, the
     * {@code SMPL_FCB_G_OFFSET_CMFS} constant. One bin per pulse-count range.
     */
    private static final int FCB_G_OFFSET_CMFS = 4;

    /**
     * Number of symbols in each fixed-codebook gain offset DCMF, the {@code SMPL_FCB_G_OFFSET_STEPS}
     * constant, {@code (0 - (-85)) - (-90 - 0) + 1 = 176}. The full decibel offset range the decoder
     * windows into per subframe.
     */
    private static final int FCB_G_OFFSET_STEPS = 176;

    /**
     * Delta cumulative mass functions for the per-subframe fixed-codebook gain offset, the
     * {@code smpl_fcbg_offset_dcmf} table, indexed {@code [subframeCountBin][pulseCountBin]}, each a
     * flat 176-symbol DCMF.
     *
     * <p>The first index is the subframe-count bin (0 for 1 subframe, 1 for 2 subframes, 2 for 4
     * subframes). The second index is the pulse-count bin, {@code min(nPulses / 10, 3)}. Decode windows
     * into the expanded CMF (see {@link #FCBG_OFFSET_CMF}) starting at {@code -nrgres_dbq} for a length
     * derived from the clamped per-subframe energy. Expanded at construction.
     */
    private static final byte[][][] FCBG_OFFSET_DCMF = {
            {
                    unsignedBytes(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 3, 3, 3, 3, 5, 8, 13, 19, 31, 53, 93, 161, 228, 255, 243, 226, 210, 186, 152, 114, 84, 63, 50, 41, 31, 19, 10, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3),
                    constByte(255, FCB_G_OFFSET_STEPS),
                    constByte(255, FCB_G_OFFSET_STEPS),
                    constByte(255, FCB_G_OFFSET_STEPS)
            },
            {
                    unsignedBytes(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 4, 7, 7, 9, 12, 17, 20, 27, 34, 47, 64, 89, 142, 216, 255, 242, 204, 167, 135, 110, 88, 71, 58, 48, 39, 31, 25, 20, 16, 11, 8, 5, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
                    unsignedBytes(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 4, 6, 8, 10, 15, 22, 31, 51, 100, 192, 255, 226, 154, 95, 57, 35, 21, 13, 8, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
                    constByte(255, FCB_G_OFFSET_STEPS),
                    constByte(255, FCB_G_OFFSET_STEPS)
            },
            {
                    unsignedBytes(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 6, 25, 18, 21, 23, 26, 31, 37, 47, 61, 87, 142, 216, 255, 232, 174, 122, 88, 64, 48, 37, 28, 22, 17, 14, 11, 9, 7, 5, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
                    unsignedBytes(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 6, 6, 9, 12, 15, 44, 95, 61, 70, 83, 99, 113, 135, 174, 228, 255, 220, 156, 104, 68, 45, 30, 21, 14, 11, 7, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
                    unsignedBytes(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 4, 6, 8, 10, 18, 26, 27, 36, 48, 65, 97, 157, 231, 255, 196, 106, 49, 26, 16, 10, 6, 4, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
                    unsignedBytes(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 6, 9, 13, 18, 25, 36, 55, 96, 175, 255, 253, 163, 70, 29, 18, 11, 8, 6, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
            }
    };

    /**
     * CallRuntime CMF for the 4-subframe shape index, built from {@link #SHAPE_CB_4_DCMF}.
     */
    private static final int[] SHAPE_CB_4_CMF = CmfBuilder.dcmfToCmf(SHAPE_CB_4_DCMF);

    /**
     * CallRuntime CMF for the 2-subframe shape index, built from {@link #SHAPE_CB_2_DCMF}.
     */
    private static final int[] SHAPE_CB_2_CMF = CmfBuilder.dcmfToCmf(SHAPE_CB_2_DCMF);

    /**
     * CallRuntime CMF for the 1-subframe frame gain index, built from {@link #GAIN_1_DCMF}.
     */
    private static final int[] GAIN_1_CMF = CmfBuilder.dcmfToCmf(GAIN_1_DCMF);

    /**
     * CallRuntime CMF for the 2-subframe frame gain index, built from {@link #GAIN_2_DCMF}.
     */
    private static final int[] GAIN_2_CMF = CmfBuilder.dcmfToCmf(GAIN_2_DCMF);

    /**
     * CallRuntime CMF for the 4-subframe frame gain index, built from {@link #GAIN_4_DCMF}.
     */
    private static final int[] GAIN_4_CMF = CmfBuilder.dcmfToCmf(GAIN_4_DCMF);

    /**
     * CallRuntime CMFs for the per-subframe fixed-codebook gain offset, built from {@link #FCBG_OFFSET_DCMF},
     * indexed {@code [subframeCountBin][pulseCountBin]}.
     */
    private static final int[][][] FCBG_OFFSET_CMF = buildFcbgOffsetCmf();

    /**
     * Prevents instantiation of this constants holder.
     */
    private NrgResTables() {
        throw new AssertionError("no instances");
    }

    /**
     * Returns the runtime CMF for the 4-subframe shape index, an {@code int[99]} strictly increasing
     * array.
     *
     * @return the 4-subframe shape index CMF, shared and never modified
     */
    public static int[] shapeCb4Cmf() {
        return SHAPE_CB_4_CMF;
    }

    /**
     * Returns the runtime CMF for the 2-subframe shape index, an {@code int[23]} strictly increasing
     * array.
     *
     * @return the 2-subframe shape index CMF, shared and never modified
     */
    public static int[] shapeCb2Cmf() {
        return SHAPE_CB_2_CMF;
    }

    /**
     * Returns the runtime CMF for the 1-subframe frame gain index, an {@code int[87]} strictly
     * increasing array.
     *
     * @return the 1-subframe frame gain index CMF, shared and never modified
     */
    public static int[] gain1Cmf() {
        return GAIN_1_CMF;
    }

    /**
     * Returns the runtime CMF for the 2-subframe frame gain index, an {@code int[72]} strictly
     * increasing array.
     *
     * @return the 2-subframe frame gain index CMF, shared and never modified
     */
    public static int[] gain2Cmf() {
        return GAIN_2_CMF;
    }

    /**
     * Returns the runtime CMF for the 4-subframe frame gain index, an {@code int[85]} strictly
     * increasing array.
     *
     * @return the 4-subframe frame gain index CMF, shared and never modified
     */
    public static int[] gain4Cmf() {
        return GAIN_4_CMF;
    }

    /**
     * Returns the runtime CMF for the per-subframe fixed-codebook gain offset, selected by subframe-count
     * bin and pulse-count bin, an {@code int[177]} strictly increasing array.
     *
     * <p>The decoder windows into the returned array starting at {@code -nrgres_dbq} for a length derived
     * from the clamped per-subframe energy, exactly as {@code decode_lb_unvoiced} does with
     * {@code &pQNRD->fcbg_offset_cmf[table_ix][cmfIx][min_offset]}.
     *
     * @param subframeCountBin the subframe-count bin: 0 for 1 subframe, 1 for 2 subframes, 2 for 4
     *                         subframes
     * @param pulseCountBin    the pulse-count bin {@code min(nPulses / 10, 3)}, in {@code [0, 3]}
     * @return the selected fixed-codebook gain offset CMF, shared and never modified
     * @throws ArrayIndexOutOfBoundsException if either index is outside its valid range
     */
    public static int[] fcbgOffsetCmf(int subframeCountBin, int pulseCountBin) {
        return FCBG_OFFSET_CMF[subframeCountBin][pulseCountBin];
    }

    /**
     * Builds the three-by-four matrix of fixed-codebook gain offset CMFs from {@link #FCBG_OFFSET_DCMF},
     * mirroring the nested loop in {@code smpl_load_nrgresq_data}.
     *
     * @return a freshly allocated {@code int[3][4][]} of built CMFs
     */
    private static int[][][] buildFcbgOffsetCmf() {
        var out = new int[FCBG_OFFSET_DCMF.length][FCB_G_OFFSET_CMFS][];
        for (var subframeBin = 0; subframeBin < FCBG_OFFSET_DCMF.length; subframeBin++) {
            for (var pulseBin = 0; pulseBin < FCB_G_OFFSET_CMFS; pulseBin++) {
                out[subframeBin][pulseBin] = CmfBuilder.dcmfToCmf(FCBG_OFFSET_DCMF[subframeBin][pulseBin]);
            }
        }
        return out;
    }

    /**
     * Packs a list of unsigned-byte literals into a {@code byte[]}, narrowing each {@code int} in
     * {@code [0, 255]} to a signed {@code byte} that the readers later unmask with {@code & 0xFF}.
     *
     * <p>This keeps the transcribed DCMF literals readable as the {@code uint8_t} values they are in the
     * C source while satisfying the Java {@code byte[]} type; values above 127 become negative
     * {@code byte} values and round-trip unsigned through the {@code 0xFF} masks in {@link CmfBuilder}.
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

    /**
     * Builds a {@code byte[]} of {@code length} entries all equal to the given unsigned byte, the
     * compact form of a saturated DCMF row.
     *
     * <p>Several fixed-codebook gain offset DCMF rows are a single repeated value (for example the
     * all-{@code 255} pulse-count bins that are never selected at their subframe count); this expands
     * such a row without spelling out 176 identical literals.
     *
     * @param value  the unsigned byte value in {@code [0, 255]} to fill with
     * @param length the row length, {@link #FCB_G_OFFSET_STEPS}
     * @return a freshly allocated {@code byte[length]} filled with the narrowed value
     */
    private static byte[] constByte(int value, int length) {
        var out = new byte[length];
        var v = (byte) value;
        for (var i = 0; i < length; i++) {
            out[i] = v;
        }
        return out;
    }
}
