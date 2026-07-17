package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeDecoder;

/**
 * Pitch-lag decode tables for the MLow speech decoder and the loaders that expand them into the runtime
 * cumulative mass functions (CMFs) the lag decoder consumes, the port of the decode-side tables in
 * {@code smpl_pitch_tables.c} and the table-construction half of {@code smpl_load_pitch_tables} in
 * {@code smpl_pitch.c}.
 *
 * <p>MLow codes the per-subframe pitch lag as a coarse block index plus a within-block delta. The
 * decoder reads, in order:
 * <ul>
 * <li>A block-segmentation index, drawn against the {@code blockseg_idx} CMF built from
 * {@link #BLOCKSEG_IDX_DCMF_20} (20 ms frames) or {@link #BLOCKSEG_IDX_DCMF_10} (10 ms frames). The
 * index selects one of the precomputed block tracks.</li>
 * <li>The block segmentation itself, a small list of coarse lag blocks and their segment lengths,
 * which is range-coded into {@link #BLOCKSEGS} / {@link #BLOCKSEGS_10} at codec build time and decoded
 * once at init by the pitch decode logic (a uniform-symbol stream, no probability table). The byte
 * buffers and their companion index and range tables ({@link #BLOCKSEGS2IDX}, {@link #BLOCKSEGS_IX},
 * {@link #FIRSTBLOCK_RANGE}, and their 10 ms variants) are static data and are transcribed here.</li>
 * <li>A within-block delta lag, drawn against one of the three {@code delta_lag} CMFs built from
 * {@link #DELTA_LAG_DCMFS}, selected by the adaptive-codebook gain quantization against the
 * {@link #ACBGAIN_THR_20_Q14} / {@link #ACBGAIN_THR_10_Q14} thresholds.</li>
 * <li>For multi-frame packets only, a block-transition symbol drawn against the
 * {@link #BLOCK_TRANSITION_DCMF_20} CMFs that couples the lag block of consecutive 20 ms frames.</li>
 * </ul>
 *
 * <p>Every probability table here is a delta cumulative mass function expanded by the shared
 * {@link CmfBuilder#dcmfToCmf(byte[])} transform; this class adds no new CMF arithmetic. The
 * lag-block-resolution constants follow the native fixed pitch range: a 2 ms to 20 ms lag window in 2
 * ms blocks gives {@link #PITCH_NUM_BLOCKS} coarse blocks.
 *
 * <p>This class ports only the lag decode tables. The pitch estimator
 * ({@code smpl_create_pitch_estimator} and the search tables in {@code smpl_pitch_util.c}) is
 * encoder-only and is intentionally not ported. High-band and above-16 kHz pitch tables are out of
 * scope for the shipped 16 kHz, 60 ms, mono configuration and are likewise not ported.
 *
 * @implNote This implementation transcribes the {@code smpl_pitch_*} decode tables byte-for-byte and
 * builds their CMFs with the same {@link CmfBuilder#dcmfToCmf(byte[])} call sequence as
 * {@code smpl_load_pitch_tables}. The native loader concatenates the 20 ms and 10 ms
 * {@code blockseg_idx} CMFs into one buffer with a one-slot gap between them; this port returns them as
 * two independent arrays instead, which is observationally identical because each CMF is only ever read
 * within its own {@code [0, len)} span. The {@code int16_t} adaptive-codebook gain thresholds are kept
 * as {@code int} since they are compared, never bit-packed.
 */
public final class PitchTables {
    /**
     * Number of coarse pitch-lag blocks, the native {@code PITCH_NUM_BLOCKS}
     * ({@code (SMPL_MAXPITCH_MS - SMPL_MINPITCH_MS) / SMPL_PITCHBLOCK_MS}, that is
     * {@code (20 - 2) / 2}).
     */
    static final int PITCH_NUM_BLOCKS = 9;

    /**
     * Number of distinct block segmentations for 20 ms frames, the native {@code NUM_BLOCKSEGS}.
     *
     * <p>This is the symbol count of the 20 ms {@code blockseg_idx} model and the length of
     * {@link #BLOCKSEG_IDX_DCMF_20} and {@link #BLOCKSEGS2IDX}.
     */
    static final int NUM_BLOCKSEGS = 217;

    /**
     * Number of distinct block segmentations for 10 ms frames, the native {@code NUM_BLOCKSEGS_10}.
     */
    static final int NUM_BLOCKSEGS_10 = 44;

    /**
     * Number of block tracks for 20 ms frames, the native {@code NUM_BLOCKTRACKS}.
     *
     * <p>This is the row count of {@link #BLOCKSEGS_IX}, the table mapping each track to its span of
     * block segmentations.
     */
    static final int NUM_BLOCKTRACKS = 187;

    /**
     * Number of block tracks for 10 ms frames, the native {@code NUM_BLOCKTRACKS_10}.
     */
    static final int NUM_BLOCKTRACKS_10 = 37;

    /**
     * Number of within-block delta-lag CMFs, the native {@code N_DELTALAG_CMFS}.
     */
    static final int N_DELTALAG_CMFS = 3;

    /**
     * Symbol count of each delta-lag DCMF row, the native {@code LEN_DELTALAG_CMF - 1}.
     *
     * <p>The built CMF has one more entry than this ({@code LEN_DELTALAG_CMF}, that is {@code 320}).
     */
    static final int DELTALAG_DCMF_LEN = 319;

    /**
     * Range-coded block-segmentation stream for 20 ms frames, the native {@code smpl_pitch_blocksegs}.
     *
     * <p>This is not a probability table; it is a uniform-symbol bitstream that the pitch decode logic
     * feeds to the range decoder at init to reconstruct the {@link #NUM_BLOCKSEGS} block segmentations
     * (each a list of coarse lag blocks and per-block segment lengths). Stored as raw bytes; read in
     * native byte order by the range decoder.
     */
    static final byte[] BLOCKSEGS = {
            (byte) 170, (byte) 179, 85, (byte) 224, (byte) 235, 28, 117, 50, (byte) 232, (byte) 211, 127, 120, 41, 24, 75, (byte) 252,
            (byte) 175, (byte) 221, (byte) 189, (byte) 151, 40, (byte) 253, (byte) 142, 77, 115, (byte) 182, 44, 0, 10, 47, 67, (byte) 163,
            (byte) 173, 34, (byte) 225, 62, 63, 95, (byte) 158, (byte) 187, (byte) 137, (byte) 254, (byte) 206, 41, 6, 34, (byte) 236, (byte) 206,
            (byte) 191, (byte) 181, (byte) 240, 66, 80, 96, (byte) 227, (byte) 148, (byte) 223, 85, 44, 42, (byte) 245, (byte) 229, (byte) 228, (byte) 234,
            (byte) 137, 79, 98, (byte) 237, 77, 94, (byte) 177, (byte) 171, 23, (byte) 226, 23, (byte) 142, (byte) 246, (byte) 227, (byte) 200, 79,
            36, (byte) 244, 24, 36, 20, 114, (byte) 255, (byte) 141, (byte) 144, (byte) 145, (byte) 173, 67, (byte) 169, 6, 21, 66,
            123, (byte) 129, 61, (byte) 184, (byte) 231, 43, 19, 18, 55, (byte) 204, 84, (byte) 183, (byte) 247, (byte) 220, (byte) 243, (byte) 164,
            (byte) 155, (byte) 207, (byte) 140, 108, (byte) 242, (byte) 177, 109, (byte) 247, (byte) 187, (byte) 227, (byte) 210, 51, 42, 122, 19, (byte) 216,
            18, (byte) 185, (byte) 154, 89, 72, (byte) 182, 105, 30, (byte) 149, 16, 108, 25, (byte) 211, (byte) 175, (byte) 154, 21,
            19, 91, 31, 70, 88, (byte) 189, (byte) 143, (byte) 217, (byte) 136, (byte) 225, (byte) 222, (byte) 235, (byte) 231, 64, 101, 32,
            (byte) 171, 41, (byte) 187, (byte) 229, (byte) 132, (byte) 222, (byte) 166, 17, (byte) 155, 99, 67, (byte) 186, 69, 127, (byte) 200, (byte) 240,
            (byte) 234, 10, 5, 86, 58, 21, (byte) 187, 48, 122, 52, 98, 4, 94, 52, (byte) 165, 113,
            70, (byte) 188, (byte) 246, (byte) 197, 30, 2, 9, 124, (byte) 244, (byte) 138, (byte) 253, (byte) 163, (byte) 224, (byte) 135, 38, (byte) 162,
            58, (byte) 147, (byte) 193, 90, (byte) 206, (byte) 242, 74, 10, 23, 45, (byte) 177, 101, (byte) 138, 113, 9, (byte) 129,
            122, 106, 77, (byte) 207, (byte) 160, 55, (byte) 175, 83, (byte) 158, (byte) 228, (byte) 171, 93, (byte) 171, (byte) 162, 64, (byte) 178,
            27, (byte) 165, 57, 65, 44, (byte) 133, 116, 80, 27, 21, (byte) 137, 39, (byte) 226, (byte) 148, (byte) 243, 13,
            44, 30, (byte) 217, 19, (byte) 252, (byte) 220, (byte) 131, (byte) 209, 46, (byte) 240, 95, (byte) 192, 37, (byte) 169, (byte) 251, 66,
            (byte) 220, (byte) 154, 64, (byte) 228, (byte) 199, (byte) 213, (byte) 235, (byte) 178, 14, 102, 62, (byte) 170, (byte) 178, (byte) 210, (byte) 231, (byte) 134,
            43, 44, (byte) 157, 127, (byte) 221, (byte) 226, (byte) 237, 94, (byte) 162, 112, (byte) 236, 27, (byte) 238, (byte) 231, (byte) 175, 25,
            (byte) 244, (byte) 248, (byte) 149, (byte) 209, 106, (byte) 151, (byte) 237, 17, 104, 39, 90, (byte) 224, (byte) 160, (byte) 221, 94, (byte) 176,
            126, (byte) 169, (byte) 166, (byte) 245, 45, 30, (byte) 150, 2, (byte) 249, (byte) 141, 36, (byte) 176, 105, (byte) 230, 98, 35,
            (byte) 232, 104, 75, 47, 95, (byte) 245, 53, 123, (byte) 250, 122, (byte) 143, (byte) 216, 46, (byte) 210, (byte) 200, 77,
            51, (byte) 128, 32, (byte) 149, (byte) 128, (byte) 238, 121, (byte) 226, (byte) 241, 35, 87, (byte) 220, (byte) 186, (byte) 180, (byte) 231, (byte) 159,
            13, (byte) 199, (byte) 247, (byte) 177, (byte) 240, 40, 39, 21, 23, (byte) 136, 50, (byte) 194, 101, (byte) 249, (byte) 203, (byte) 207,
            (byte) 133, 116, 26, 81, 41, 112, 79, 8, (byte) 144, (byte) 244, (byte) 146, (byte) 254, (byte) 144, (byte) 165, (byte) 203, (byte) 251,
            126, (byte) 132, (byte) 211, 47, (byte) 220, (byte) 169, (byte) 167, 66, (byte) 186, 73, (byte) 134, 86, (byte) 133, 42, 102, (byte) 239,
            17, 50, 90, 17, 78, 124, 93, 120, 49, 116, (byte) 165, (byte) 141, (byte) 196, 96, (byte) 172, (byte) 215,
            (byte) 153, 22, 39, 122, (byte) 149, 116, 88, 114, (byte) 175, 70, 56, (byte) 206, 48, (byte) 185, 8, (byte) 233,
            (byte) 254, (byte) 135, (byte) 241, (byte) 185, (byte) 225, 94, (byte) 234, (byte) 195, 36, 75, (byte) 136, 125, 41, (byte) 254, (byte) 141, (byte) 215,
            30, 35, 51, (byte) 215, 7, (byte) 214, 63, 84, (byte) 215, 24, (byte) 173, (byte) 139, (byte) 182, 75, (byte) 199, (byte) 189,
            97, 22, (byte) 152, (byte) 255, 10, (byte) 156, (byte) 169, 56, (byte) 236, (byte) 145, 115, 81, 33, 127, (byte) 216, 1,
            (byte) 158, (byte) 162, 30, 82, 46, 67, (byte) 185, 3, (byte) 195, (byte) 181, (byte) 132, 64, 68, 58, (byte) 214, 71,
            (byte) 242, 24, (byte) 236, (byte) 169, 117, 93, (byte) 139, 73, 71, 58, 106, (byte) 130, (byte) 195, (byte) 217, 126, (byte) 132,
            6, 100, 48, 66, 19, (byte) 248, 24, (byte) 208, 34, (byte) 184, (byte) 197, (byte) 177, (byte) 205, (byte) 148, 93, 25,
            67, 64, (byte) 186, (byte) 237, 25, (byte) 233, 5, 112, 57, (byte) 174, 17, (byte) 162, (byte) 141, (byte) 204, 87, 59,
            (byte) 244, 40, (byte) 216, (byte) 188, (byte) 135, 0, (byte) 233, 48, 95, 14, (byte) 255, (byte) 210, (byte) 212, (byte) 219, 97, (byte) 132,
            47, (byte) 149, 53, (byte) 209, 34, (byte) 183, (byte) 177, 91, 75, 28, (byte) 231, 38, 55, 17, 45, 90,
            5, 30, (byte) 163, (byte) 167, 87, 32, 18, (byte) 155, 24, 82, (byte) 226, (byte) 140, 22, (byte) 232, (byte) 186, (byte) 254,
            53, 40, (byte) 212, 16, (byte) 175, 4, (byte) 150, 11, 64, 14, 58, 90, (byte) 190, 69, (byte) 158, 125,
            4, 59, (byte) 133, 7, (byte) 219, 76, 67, 107, (byte) 190, (byte) 235, 115, 73, (byte) 209, 21, (byte) 182, 7,
            (byte) 186, 99, (byte) 168, (byte) 183, 110, 22, (byte) 155, (byte) 182, 68, (byte) 211, (byte) 201, 107, (byte) 235, (byte) 171, (byte) 128, (byte) 199,
            (byte) 181, (byte) 186, (byte) 128, 23, (byte) 163, 51, 48, 104, 121, 34, 30, (byte) 247, 67, (byte) 205, 102, (byte) 247,
            22, (byte) 254, (byte) 255, (byte) 220,
    };

    /**
     * Range-coded block-segmentation stream for 10 ms frames, the native {@code smpl_pitch_blocksegs_10}.
     *
     * <p>The 10 ms counterpart of {@link #BLOCKSEGS}, reconstructing {@link #NUM_BLOCKSEGS_10}
     * segmentations.
     */
    static final byte[] BLOCKSEGS_10 = {
            43, (byte) 229, 80, 10, 14, (byte) 230, (byte) 203, (byte) 239, 101, 122, 102, (byte) 203, (byte) 235, (byte) 196, 119, 82,
            48, (byte) 181, 22, 16, 53, (byte) 161, 29, (byte) 243, 110, 120, 11, (byte) 221, (byte) 185, 118, (byte) 152, 4,
            77, 93, (byte) 184, 53, 66, (byte) 159, 79, (byte) 224, 118, 101, 16, 96, 67, 48, 32, 97,
            0, 68, 81, 46, 80, 45, 15, (byte) 176, 103, (byte) 201, 47, (byte) 246, (byte) 230, (byte) 140, 69, 35,
            (byte) 176, (byte) 132, (byte) 234, 69, 127, 73, (byte) 216, (byte) 244, 2, (byte) 141, (byte) 178, 73, (byte) 176, 14, (byte) 155, 18,
            (byte) 238, (byte) 184,
    };

    /**
     * Permutation from C block-segmentation index to the native Julia index for 20 ms frames, the
     * native {@code smpl_pitch_blocksegs2idx}.
     *
     * <p>Read as unsigned bytes. The decoder uses this to translate the decoded block-segmentation
     * index into the index space the rest of the lag model expects.
     */
    static final byte[] BLOCKSEGS2IDX = {
            1, 17, 33, 49, 50, 2, 34, 3, 4, 5, 21, 6, 7, 8, 9, 25,
            10, 11, 12, 13, 29, 14, 15, 47, 16, 48, 18, 19, 20, 22, 23, 24,
            26, 27, 28, 30, 31, 45, 32, 35, 36, 37, 38, 39, 40, 41, 42, 43,
            44, 46, 51, 59, 105, 121, 52, 53, 54, 55, 56, 57, 58, 60, 61, 62,
            63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 106, 76, 107,
            77, 78, 93, 79, 80, 81, 82, 97, 83, 84, 85, 86, 101, 87, 88, 89,
            120, 90, 91, 109, 92, 94, 95, 96, 98, 99, 100, 102, 103, 104, 108, 110,
            111, 112, 113, 114, 115, 116, 117, 118, 119, 122, 126, (byte) 156, 123, 124, 125, 127,
            (byte) 128, (byte) 129, (byte) 130, (byte) 131, (byte) 132, (byte) 133, (byte) 134, (byte) 135, (byte) 136, (byte) 137, (byte) 138, (byte) 139, (byte) 140, (byte) 141, (byte) 142, (byte) 143,
            (byte) 144, (byte) 145, (byte) 146, (byte) 147, (byte) 148, (byte) 149, (byte) 150, (byte) 151, (byte) 152, (byte) 153, (byte) 154, (byte) 155, (byte) 157, (byte) 160, (byte) 167, (byte) 158,
            (byte) 173, (byte) 159, (byte) 161, (byte) 162, (byte) 163, (byte) 164, (byte) 165, (byte) 166, (byte) 168, (byte) 169, (byte) 170, (byte) 171, (byte) 172, (byte) 174, (byte) 175, (byte) 176,
            (byte) 177, (byte) 178, (byte) 179, (byte) 180, (byte) 181, (byte) 182, (byte) 186, (byte) 200, (byte) 183, (byte) 184, (byte) 194, (byte) 185, (byte) 187, (byte) 188, (byte) 189, (byte) 190,
            (byte) 191, (byte) 192, (byte) 193, (byte) 195, (byte) 196, (byte) 197, (byte) 198, (byte) 199, (byte) 201, (byte) 208, (byte) 202, (byte) 203, (byte) 204, (byte) 205, (byte) 206, (byte) 207,
            (byte) 209, (byte) 210, (byte) 211, (byte) 212, (byte) 213, (byte) 214, (byte) 215, (byte) 216, (byte) 217,
    };

    /**
     * Permutation from C block-segmentation index to the native Julia index for 10 ms frames, the
     * native {@code smpl_pitch_blocksegs2idx_10}.
     */
    static final byte[] BLOCKSEGS2IDX_10 = {
            1, 3, 2, 4, 5, 6, 7, 10, 8, 9, 11, 12, 13, 14, 15, 16,
            17, 20, 18, 19, 21, 22, 23, 24, 25, 26, 27, 28, 34, 29, 30, 31,
            32, 33, 35, 36, 37, 38, 39, 41, 40, 42, 43, 44,
    };

    /**
     * Block-track to block-segmentation span table for 20 ms frames, the native
     * {@code smpl_pitch_blocksegs_ix}, flattened to {@code [offset0, count0, offset1, count1, ...]}.
     *
     * <p>Each {@link #NUM_BLOCKTRACKS} tracks contributes a pair: the first block-segmentation index
     * in the track and the number of segmentations it spans. Read as unsigned bytes.
     */
    static final byte[] BLOCKSEGS_IX = {
            0, 5, 5, 2, 7, 1, 8, 1, 9, 2, 11, 1, 12, 1, 13, 1,
            14, 2, 16, 1, 17, 1, 18, 1, 19, 2, 21, 1, 22, 2, 24, 2,
            26, 1, 27, 1, 28, 1, 29, 1, 30, 1, 31, 1, 32, 1, 33, 1,
            34, 1, 35, 1, 36, 2, 38, 1, 39, 1, 40, 1, 41, 1, 42, 1,
            43, 1, 44, 1, 45, 1, 46, 1, 47, 1, 48, 1, 49, 1, 50, 4,
            54, 1, 55, 1, 56, 1, 57, 1, 58, 1, 59, 1, 60, 1, 61, 1,
            62, 1, 63, 1, 64, 1, 65, 1, 66, 1, 67, 1, 68, 1, 69, 1,
            70, 1, 71, 1, 72, 1, 73, 1, 74, 1, 75, 1, 76, 2, 78, 2,
            80, 1, 81, 2, 83, 1, 84, 1, 85, 1, 86, 2, 88, 1, 89, 1,
            90, 1, 91, 2, 93, 1, 94, 1, 95, 2, 97, 1, 98, 2, 100, 1,
            101, 1, 102, 1, 103, 1, 104, 1, 105, 1, 106, 1, 107, 1, 108, 1,
            109, 1, 110, 1, 111, 1, 112, 1, 113, 1, 114, 1, 115, 1, 116, 1,
            117, 1, 118, 1, 119, 1, 120, 1, 121, 3, 124, 1, 125, 1, 126, 1,
            127, 1, (byte) 128, 1, (byte) 129, 1, (byte) 130, 1, (byte) 131, 1, (byte) 132, 1, (byte) 133, 1, (byte) 134, 1,
            (byte) 135, 1, (byte) 136, 1, (byte) 137, 1, (byte) 138, 1, (byte) 139, 1, (byte) 140, 1, (byte) 141, 1, (byte) 142, 1,
            (byte) 143, 1, (byte) 144, 1, (byte) 145, 1, (byte) 146, 1, (byte) 147, 1, (byte) 148, 1, (byte) 149, 1, (byte) 150, 1,
            (byte) 151, 1, (byte) 152, 1, (byte) 153, 1, (byte) 154, 1, (byte) 155, 1, (byte) 156, 3, (byte) 159, 2, (byte) 161, 1,
            (byte) 162, 1, (byte) 163, 1, (byte) 164, 1, (byte) 165, 1, (byte) 166, 1, (byte) 167, 1, (byte) 168, 1, (byte) 169, 1,
            (byte) 170, 1, (byte) 171, 1, (byte) 172, 1, (byte) 173, 1, (byte) 174, 1, (byte) 175, 1, (byte) 176, 1, (byte) 177, 1,
            (byte) 178, 1, (byte) 179, 1, (byte) 180, 1, (byte) 181, 3, (byte) 184, 1, (byte) 185, 2, (byte) 187, 1, (byte) 188, 1,
            (byte) 189, 1, (byte) 190, 1, (byte) 191, 1, (byte) 192, 1, (byte) 193, 1, (byte) 194, 1, (byte) 195, 1, (byte) 196, 1,
            (byte) 197, 1, (byte) 198, 1, (byte) 199, 1, (byte) 200, 2, (byte) 202, 1, (byte) 203, 1, (byte) 204, 1, (byte) 205, 1,
            (byte) 206, 1, (byte) 207, 1, (byte) 208, 1, (byte) 209, 1, (byte) 210, 1, (byte) 211, 1, (byte) 212, 1, (byte) 213, 1,
            (byte) 214, 1, (byte) 215, 1, (byte) 216, 1,
    };

    /**
     * Block-track to block-segmentation span table for 10 ms frames, the native
     * {@code smpl_pitch_blocksegs_ix_10}, flattened to interleaved {@code [offset, count]} pairs.
     */
    static final byte[] BLOCKSEGS_IX_10 = {
            0, 2, 2, 1, 3, 1, 4, 1, 5, 1, 6, 2, 8, 1, 9, 1,
            10, 1, 11, 1, 12, 1, 13, 1, 14, 1, 15, 1, 16, 2, 18, 1,
            19, 1, 20, 1, 21, 1, 22, 1, 23, 1, 24, 1, 25, 1, 26, 3,
            29, 1, 30, 1, 31, 1, 32, 1, 33, 1, 34, 2, 36, 1, 37, 1,
            38, 2, 40, 1, 41, 1, 42, 1, 43, 1,
    };

    /**
     * First-block range table for 20 ms frames, the native {@code smpl_pitch_firstblock_range},
     * flattened to interleaved {@code [low, high]} pairs, one per coarse block.
     *
     * <p>Maps each of the {@link #PITCH_NUM_BLOCKS} coarse lag blocks to the inclusive range of
     * block-track indices whose first block equals it. Read as unsigned bytes.
     */
    static final byte[] FIRSTBLOCK_RANGE = {
            0, 49, 50, 120, 121, (byte) 155, (byte) 156, (byte) 180, (byte) 181, (byte) 199, (byte) 200, (byte) 207, (byte) 208, (byte) 211, (byte) 212, (byte) 214,
            (byte) 215, (byte) 216,
    };

    /**
     * First-block range table for 10 ms frames, the native {@code smpl_pitch_firstblock_range_10},
     * flattened to interleaved {@code [low, high]} pairs.
     */
    static final byte[] FIRSTBLOCK_RANGE_10 = {
            0, 5, 6, 15, 16, 25, 26, 33, 34, 37, 38, 40, 41, 41, 42, 42,
            43, 43,
    };

    /**
     * Block-segmentation-index DCMF for 20 ms frames, the native {@code smpl_pitch_blockseg_idx_DCMF_20}.
     *
     * <p>Read as unsigned bytes; expanded to the 20 ms {@code blockseg_idx} CMF by
     * {@link #loadBlocksegIdxCmf20()}.
     */
    static final byte[] BLOCKSEG_IDX_DCMF_20 = {
            65, 8, 7, 6, 6, 5, 6, 5, 13, 5, 6, 5, 18, 6, 22, 19,
            96, 8, 6, 7, 7, 5, 6, 6, 17, 5, 6, 6, 26, 5, 23, 26,
            96, 8, 7, 7, 7, 5, 7, 7, 23, 5, 6, 5, 21, 6, 20, 21,
            25, 25, (byte) 255, 12, 12, 10, 31, 8, 34, 30, (byte) 160, 11, 10, 9, 9, 5,
            8, 7, 17, 5, 6, 6, 25, 8, 24, 22, 19, 16, 6, 17, 5, 5,
            5, 12, 7, 7, 5, 7, 8, 6, 9, 22, 17, 5, 16, 5, 5, 5,
            14, 8, 7, 5, 7, 9, 6, 9, 52, 22, 20, 5, 18, 5, 5, 5,
            16, 9, 9, 5, 8, 8, 7, 8, 65, (byte) 133, 9, 30, 32, 102, 9, 8,
            10, 28, 7, 28, 33, 28, 24, 9, 21, 9, 11, 11, 20, 19, 7, 16,
            6, 6, 5, 14, 6, 7, 5, 8, 9, 9, 9, 70, (byte) 138, 35, 15, 92,
            9, 25, 28, 24, 24, 11, 112, 9, 9, 12, 27, 8, 14, 30, 18, 19,
            6, 20, 11, 10, 10, 117, 32, 31, 15, 75, 11, 26, 26, 19, 20, 11,
            21, 12, 8, 21, 14, 9, 10, 84, 87, 32, 28, 15, 20, 22, 11, 58,
            68, 30, 27, 14, 64, 26, 26, 58, 24,
    };

    /**
     * Block-segmentation-index DCMF for 10 ms frames, the native {@code smpl_pitch_blockseg_idx_DCMF_10}.
     *
     * <p>Read as unsigned bytes; expanded to the 10 ms {@code blockseg_idx} CMF by
     * {@link #loadBlocksegIdxCmf10()}.
     */
    static final byte[] BLOCKSEG_IDX_DCMF_10 = {
            (byte) 163, 31, 91, 11, 20, 29, (byte) 255, 39, 26, (byte) 186, 14, 25, 33, 24, 17, 12,
            (byte) 192, 38, 33, (byte) 142, 12, 25, 33, 28, 22, 13, (byte) 184, (byte) 136, 46, 31, 27, 23,
            13, (byte) 132, (byte) 161, (byte) 131, 38, 39, (byte) 139, 36, 92, 116, 115, 96,
    };

    /**
     * Within-block delta-lag DCMFs, the native {@code smpl_pitch_delta_lag_DCMFs}, stored as
     * {@link #N_DELTALAG_CMFS} rows of {@link #DELTALAG_DCMF_LEN} bytes flattened row-major.
     *
     * <p>Read as unsigned bytes. {@link #loadDeltaLagCmfs()} expands each row into a CMF of
     * {@code DELTALAG_DCMF_LEN + 1} entries. The three rows correspond to the delta model selected by
     * the quantized adaptive-codebook gain.
     */
    static final byte[] DELTA_LAG_DCMFS = {
            (byte) 255, (byte) 219, (byte) 184, (byte) 157, (byte) 141, (byte) 131, 124, 118, 115, 114, 113, 111, 110, 109, 107, 104,
            101, 101, 103, 105, 106, 107, 106, 104, 102, 101, 100, 100, 100, 98, 98, 100,
            100, 100, 100, 101, 99, 99, 98, 98, 98, 96, 95, 96, 97, 96, 95, 96,
            94, 94, 93, 93, 93, 93, 92, 92, 92, 92, 90, 90, 89, 91, 98, 113,
            114, 94, 83, 77, 73, 70, 68, 68, 67, 67, 66, 65, 64, 64, 63, 62,
            62, 63, 62, 62, 61, 62, 61, 61, 61, 62, 61, 62, 62, 63, 64, 65,
            64, 65, 66, 68, 68, 69, 70, 72, 73, 76, 75, 77, 78, 81, 82, 86,
            87, 89, 91, 95, 96, 100, 102, 108, 110, 119, 122, (byte) 134, (byte) 142, (byte) 162, (byte) 196, (byte) 251,
            (byte) 209, (byte) 181, (byte) 162, (byte) 154, (byte) 141, (byte) 136, 126, 123, 114, 113, 108, 105, 100, 98, 93, 92,
            87, 85, 83, 82, 78, 76, 74, 74, 71, 70, 67, 67, 66, 64, 64, 63,
            61, 62, 60, 60, 59, 58, 57, 57, 58, 57, 57, 56, 55, 55, 55, 56,
            57, 57, 57, 57, 58, 59, 60, 59, 59, 60, 62, 64, 67, 74, 88, 103,
            94, 89, 88, 88, 88, 89, 90, 92, 93, 93, 93, 93, 93, 94, 93, 92,
            93, 93, 92, 90, 89, 91, 93, 93, 92, 92, 89, 87, 88, 89, 89, 89,
            89, 92, 93, 92, 91, 91, 89, 90, 92, 92, 90, 89, 90, 92, 92, 94,
            94, 94, 95, 97, 98, 97, 96, 97, 97, 98, 100, 104, 108, 111, 111, 112,
            108, 104, 103, 103, 102, 102, 102, 101, 99, 100, 100, 101, 104, 106, 104, 102,
            101, 100, 99, 99, 100, 103, 105, 103, 102, 102, 102, 101, 100, 101, 101, 101,
            99, 97, 94, 93, 93, 93, 95, 96, 98, 99, 98, 97, 95, 92, 92, 94,
            96, 98, 98, 97, 96, 96, 96, 98, 102, 108, 115, 124, (byte) 134, (byte) 148, (byte) 159, (byte) 161,
            (byte) 144, 125, 113, 103, 96, 92, 90, 90, 89, 87, 85, 84, 82, 82, 81, 81,
            82, 83, 83, 83, 80, 78, 77, 75, 74, 75, 76, 77, 79, 79, 78, 78,
            79, 80, 80, 79, 78, 76, 75, 75, 75, 74, 72, 70, 69, 69, 68, 68,
            68, 69, 69, 68, 67, 66, 65, 64, 65, 64, 63, 62, 63, 65, 67, 58,
            51, 44, 40, 38, 37, 37, 35, 34, 33, 33, 33, 32, 31, 31, 31, 30,
            30, 31, 31, 30, 30, 30, 30, 30, 30, 31, 32, 32, 33, 33, 34, 34,
            34, 35, 36, 36, 37, 38, 40, 40, 41, 42, 44, 45, 47, 49, 51, 53,
            55, 57, 61, 64, 68, 71, 76, 81, 89, 96, 109, 124, (byte) 152, (byte) 198, (byte) 255, (byte) 243,
            (byte) 204, (byte) 170, (byte) 149, (byte) 130, 118, 107, 99, 92, 87, 80, 76, 71, 68, 64, 62, 58,
            56, 53, 51, 49, 48, 46, 45, 43, 42, 41, 41, 39, 39, 38, 37, 37,
            37, 36, 36, 35, 34, 34, 34, 34, 35, 34, 34, 33, 34, 35, 36, 36,
            37, 38, 38, 39, 40, 40, 41, 41, 42, 44, 46, 49, 53, 58, 64, 66,
            66, 67, 68, 70, 71, 69, 68, 70, 72, 72, 71, 71, 71, 71, 71, 71,
            71, 69, 67, 67, 68, 68, 69, 69, 70, 69, 69, 69, 69, 69, 68, 68,
            67, 67, 68, 69, 71, 71, 71, 71, 72, 73, 73, 73, 72, 71, 71, 71,
            71, 71, 72, 72, 74, 76, 78, 80, 81, 80, 80, 82, 87, 91, 93, 89,
            85, 82, 81, 81, 83, 85, 87, 89, 91, 93, 95, 96, 98, 99, 97, 95,
            93, 93, 94, 96, 98, 99, 99, 99, 97, 96, 96, 96, 96, 95, 92, 90,
            89, 87, 86, 84, 82, 81, 81, 82, 83, 82, 82, 81, 81, 81, 81, 81,
            82, 82, 83, 84, 87, 89, 92, 96, 100, 105, 111, 119, (byte) 129, (byte) 137, 122, 112,
            99, 89, 82, 77, 73, 71, 69, 67, 65, 63, 62, 60, 59, 58, 58, 59,
            59, 59, 60, 60, 59, 58, 56, 54, 53, 53, 54, 55, 57, 58, 58, 57,
            56, 54, 54, 53, 52, 51, 49, 49, 48, 48, 47, 46, 47, 47, 47, 46,
            46, 45, 44, 43, 42, 41, 39, 37, 36, 36, 37, 37, 36, 33, 26, 21,
            17, 16, 14, 13, 13, 11, 11, 11, 11, 10, 10, 10, 9, 9, 10, 10,
            10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 12,
            12, 13, 13, 14, 14, 15, 15, 15, 16, 16, 18, 19, 19, 20, 21, 23,
            25, 26, 29, 32, 35, 39, 44, 50, 59, 72, 93, (byte) 128, (byte) 193, (byte) 255, (byte) 217, (byte) 154,
            112, 88, 72, 61, 52, 47, 41, 38, 34, 31, 29, 26, 25, 24, 22, 21,
            20, 19, 18, 18, 17, 16, 16, 15, 15, 14, 14, 14, 14, 13, 13, 13,
            13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 13, 13, 13,
            13, 13, 13, 14, 14, 15, 16, 16, 17, 19, 20, 23, 27, 34, 37, 39,
            40, 42, 42, 42, 43, 45, 45, 46, 47, 47, 48, 49, 49, 49, 49, 49,
            48, 47, 47, 46, 46, 47, 48, 49, 49, 49, 49, 49, 48, 47, 47, 47,
            47, 48, 49, 50, 51, 52, 52, 51, 51, 50, 50, 51, 51, 53, 54, 55,
            55, 54, 54, 55, 56, 56, 57, 57, 58, 61, 63, 66, 68, 68, 67, 66,
            67, 67, 67, 67, 68, 69, 70, 71, 71, 71, 71, 71, 72, 74, 75, 75,
            75, 75, 75, 75, 74, 72, 72, 74, 75, 76, 75, 72, 69, 66, 64, 62,
            61, 60, 59, 58, 58, 58, 59, 60, 61, 61, 61, 61, 62, 62, 63, 65,
            67, 68, 68, 68, 67, 66, 67, 68, 71, 75, 81, 88, 93,
    };

    /**
     * Block-transition DCMFs for 20 ms multi-frame packets, the native
     * {@code smpl_pitch_block_transition_DCMF_20}, stored as {@link #PITCH_NUM_BLOCKS} rows of
     * {@link #PITCH_NUM_BLOCKS} bytes flattened row-major.
     *
     * <p>Read as unsigned bytes. Row {@code i} is the transition model from lag block {@code i} of the
     * previous frame to the lag block of the current frame; {@link #loadBlockTransitionCmfs20()}
     * expands each row into a CMF.
     */
    static final byte[] BLOCK_TRANSITION_DCMF_20 = {
            (byte) 255, 109, 62, 59, 49, 46, 46, 46, 42, 35, (byte) 255, 45, 36, 37, 21, 18,
            17, 13, 40, 62, (byte) 255, 77, 40, 43, 28, 24, 20, 37, 54, 52, (byte) 255, 72,
            34, 26, 29, 24, 46, 71, 40, 71, (byte) 255, 73, 37, 32, 28, 75, 78, 77,
            74, 109, (byte) 255, 86, 62, 47, (byte) 141, (byte) 135, 116, 119, 124, (byte) 150, (byte) 255, (byte) 138, 100, (byte) 155,
            (byte) 156, 121, (byte) 157, (byte) 134, (byte) 137, (byte) 142, (byte) 255, (byte) 140, (byte) 174, (byte) 152, (byte) 128, (byte) 163, (byte) 143, (byte) 139, (byte) 139, (byte) 158,
            (byte) 255,
    };

    /**
     * Quantized adaptive-codebook gain thresholds for 10 ms frames in Q14, the native
     * {@code smpl_pitch_acbgain_thr_10_Q14}.
     *
     * <p>The two thresholds partition the adaptive-codebook gain into three classes, selecting which of
     * the three {@link #DELTA_LAG_DCMFS} rows drives the within-block delta-lag decode.
     */
    static final int[] ACBGAIN_THR_10_Q14 = {9859, 13955};

    /**
     * Quantized adaptive-codebook gain thresholds for 20 ms frames in Q14, the native
     * {@code smpl_pitch_acbgain_thr_20_Q14}.
     */
    static final int[] ACBGAIN_THR_20_Q14 = {10007, 14085};

    /**
     * Prevents instantiation of this stateless table holder.
     */
    private PitchTables() {
        throw new AssertionError("no instances");
    }

    /**
     * Builds the 20 ms block-segmentation-index CMF, the {@code blockseg_idx_CMF} for 20 ms frames in
     * {@code smpl_load_pitch_tables}.
     *
     * @return a freshly allocated CMF of {@code NUM_BLOCKSEGS + 1} entries
     */
    static int[] loadBlocksegIdxCmf20() {
        return CmfBuilder.dcmfToCmf(BLOCKSEG_IDX_DCMF_20);
    }

    /**
     * Builds the 10 ms block-segmentation-index CMF, the {@code blockseg_idx_CMF} for 10 ms frames in
     * {@code smpl_load_pitch_tables}.
     *
     * @return a freshly allocated CMF of {@code NUM_BLOCKSEGS_10 + 1} entries
     */
    static int[] loadBlocksegIdxCmf10() {
        return CmfBuilder.dcmfToCmf(BLOCKSEG_IDX_DCMF_10);
    }

    /**
     * Builds the three within-block delta-lag CMFs, the {@code delta_lag_CMFs} in
     * {@code smpl_load_pitch_tables}.
     *
     * <p>Returns a {@link #N_DELTALAG_CMFS}-element array, each a CMF of {@code DELTALAG_DCMF_LEN + 1}
     * entries built from the corresponding row of {@link #DELTA_LAG_DCMFS} via
     * {@link CmfBuilder#dcmfToCmf(byte[], int, int)}.
     *
     * @return a freshly allocated {@code int[N_DELTALAG_CMFS][]} of delta-lag CMFs
     */
    static int[][] loadDeltaLagCmfs() {
        var cmfs = new int[N_DELTALAG_CMFS][];
        for (var i = 0; i < N_DELTALAG_CMFS; i++) {
            cmfs[i] = CmfBuilder.dcmfToCmf(DELTA_LAG_DCMFS, i * DELTALAG_DCMF_LEN, DELTALAG_DCMF_LEN);
        }
        return cmfs;
    }

    /**
     * Builds the block-transition CMFs for 20 ms multi-frame packets, the {@code block_transition_CMF_20}
     * in {@code smpl_load_pitch_tables}.
     *
     * <p>Returns a {@link #PITCH_NUM_BLOCKS}-element array, each a CMF of {@code PITCH_NUM_BLOCKS + 1}
     * entries built from the corresponding row of {@link #BLOCK_TRANSITION_DCMF_20} via
     * {@link CmfBuilder#dcmfToCmf(byte[], int, int)}.
     *
     * @return a freshly allocated {@code int[PITCH_NUM_BLOCKS][]} of block-transition CMFs
     */
    static int[][] loadBlockTransitionCmfs20() {
        var cmfs = new int[PITCH_NUM_BLOCKS][];
        for (var i = 0; i < PITCH_NUM_BLOCKS; i++) {
            cmfs[i] = CmfBuilder.dcmfToCmf(BLOCK_TRANSITION_DCMF_20, i * PITCH_NUM_BLOCKS, PITCH_NUM_BLOCKS);
        }
        return cmfs;
    }

    /**
     * Number of uniform symbols coding a block segmentation's block count, the native {@code N_LEN} in
     * {@code decode_blocksegs}; the decoded value plus one is the number of blocks in the segmentation.
     */
    private static final int BLOCKSEGS_N_LEN = 6;

    /**
     * Number of uniform symbols coding each coarse lag block, the native {@code N_BLOCK} in
     * {@code decode_blocksegs}; one of the {@link #PITCH_NUM_BLOCKS} coarse blocks.
     */
    private static final int BLOCKSEGS_N_BLOCK = 9;

    /**
     * Number of uniform symbols coding each segment length, the native {@code N_SEGLEN} in
     * {@code decode_blocksegs}; the decoded value plus one is the segment length in subframes.
     */
    private static final int BLOCKSEGS_N_SEGLEN = 4;

    /**
     * Length in bytes of the 20 ms block-segmentation stream consumed by the range decoder, the native
     * {@code NUM_BLOCKSEGS_BYTES}.
     */
    private static final int BLOCKSEGS_BYTES = 676;

    /**
     * The reconstructed 20 ms pitch decode data, built once and shared, the native {@code data20ms} of
     * {@code PITCH_Tables}.
     */
    private static final PitchData DATA_20 = buildData20();

    /**
     * One reconstructed block segmentation: a list of coarse lag blocks and their per-block segment
     * lengths, the native {@code PITCH_blocksegs}.
     *
     * <p>A block segmentation describes how the pitch lag tracks across the subframes of a frame: each of
     * the {@code nblocks} entries is a coarse lag {@code block} held for {@code seglen} consecutive
     * subframes. The native code stores fixed-size {@code blocks[8]} and {@code seglens[8]} arrays with a
     * separate {@code nblocks} count; this record carries them as exact-length arrays whose length equals
     * the block count, so {@code blocks.length} is the native {@code nblocks}.
     *
     * @param blocks  the coarse lag block index for each segment, length equal to the block count
     * @param seglens the number of consecutive subframes each segment spans, parallel to {@code blocks}
     */
    public record Blockseg(int[] blocks, int[] seglens) {
        /**
         * Returns the number of blocks (segments) in this segmentation, the native {@code nblocks}.
         *
         * @return the block count, equal to {@code blocks().length}
         */
        public int nblocks() {
            return blocks.length;
        }
    }

    /**
     * The runtime pitch-lag decode data for one frame length, the decode-relevant subset of the native
     * {@code PITCH_data} returned by {@code smpl_get_pitch_data}.
     *
     * <p>This bundles everything {@code decode_lb_voiced} reads to turn the bitstream into per-subframe
     * integer pitch lags: the reconstructed block segmentations (decoded once from the range-coded
     * {@link #BLOCKSEGS} stream), the permutation from C block-segmentation index to native (Julia) index,
     * the first-block range table, and the four CMF families (block-segmentation index, within-block
     * delta lag, and block transition for multi-frame packets). The high-band and 10 ms variants are out
     * of scope for the shipped 16 kHz, 60 ms, mono configuration and are not exposed.
     *
     * @param blocksegs         the reconstructed block segmentations, indexed by C block-segmentation
     *                          index
     * @param blocksegs2idx     the permutation from C block-segmentation index to native index, read as
     *                          unsigned bytes; {@code blocksegs2idx[c]} is the native index of C index
     *                          {@code c}
     * @param blocksegIdxCmf    the block-segmentation index CMF, scanned (optionally windowed) to decode
     *                          the native index
     * @param deltaLagCmfs      the three within-block delta-lag CMFs, selected by the quantized
     *                          adaptive-codebook gain class
     * @param blockTransitionCmf the per-previous-block transition CMFs for multi-frame packets, indexed by
     *                          the previous frame's last lag block
     * @param firstBlockRange   the first-block range table, flattened to interleaved {@code [low, high]}
     *                          pairs read as unsigned bytes
     * @param numBlocksegs      the number of block segmentations, the native {@code num_blocksegs}
     */
    public record PitchData(
            Blockseg[] blocksegs,
            byte[] blocksegs2idx,
            int[] blocksegIdxCmf,
            int[][] deltaLagCmfs,
            int[][] blockTransitionCmf,
            byte[] firstBlockRange,
            int numBlocksegs) {
    }

    /**
     * Returns the quantized adaptive-codebook gain thresholds for 20 ms frames in Q14, the native
     * {@code smpl_pitch_acbgain_thr_20_Q14}.
     *
     * <p>The two returned thresholds partition the mean quantized adaptive-codebook gain into the three
     * classes that select which within-block delta-lag CMF the lag decoder uses. The returned array is
     * shared and must not be mutated.
     *
     * @return the two 20 ms adaptive-codebook gain thresholds in Q14, shared and never modified
     */
    public static int[] acbgainThr20Q14() {
        return ACBGAIN_THR_20_Q14;
    }

    /**
     * Returns the reconstructed 20 ms pitch-lag decode data, the native
     * {@code smpl_get_pitch_data(SMPL_PITCH_NUM_SUBFRAMES)}.
     *
     * <p>The returned {@link PitchData} is built once at class initialization and shared; callers must
     * not mutate its arrays.
     *
     * @return the shared 20 ms pitch decode data
     */
    public static PitchData data20() {
        return DATA_20;
    }

    /**
     * Builds the 20 ms pitch decode data, the {@code data20ms} half of {@code smpl_load_pitch_tables}.
     *
     * <p>Reconstructs the {@link #NUM_BLOCKSEGS} block segmentations by running a range decoder over the
     * {@link #BLOCKSEGS} stream exactly as {@code ec_dec_init} plus the {@code decode_blocksegs} loop do,
     * then bundles them with the permutation, range, and CMF tables this port already builds.
     *
     * @return a freshly built {@link PitchData} for 20 ms frames
     */
    private static PitchData buildData20() {
        var segs = decodeBlocksegs(BLOCKSEGS, BLOCKSEGS_BYTES, NUM_BLOCKSEGS);
        return new PitchData(
                segs,
                BLOCKSEGS2IDX,
                loadBlocksegIdxCmf20(),
                loadDeltaLagCmfs(),
                loadBlockTransitionCmfs20(),
                FIRSTBLOCK_RANGE,
                NUM_BLOCKSEGS);
    }

    /**
     * Reconstructs the block segmentations from a range-coded stream, the {@code decode_blocksegs} loop in
     * {@code smpl_pitch.c} driven by one {@code ec_dec_init}.
     *
     * <p>Each segmentation is read as a sequence of uniform symbols: a block count (decoded value plus
     * one), then for each block a coarse lag block in {@code [0, N_BLOCK)} and a segment length (decoded
     * value plus one). All symbols share one range decoder seeded over the whole stream, so the
     * segmentations must be decoded in order.
     *
     * @param stream the range-coded block-segmentation byte stream
     * @param length the number of valid stream bytes, the native {@code NUM_BLOCKSEGS_BYTES}
     * @param count  the number of segmentations to decode, the native {@code NUM_BLOCKSEGS}
     * @return a freshly allocated array of {@code count} reconstructed segmentations
     */
    private static Blockseg[] decodeBlocksegs(byte[] stream, int length, int count) {
        var decoder = new MlowRangeDecoder(stream, 0, length);
        var out = new Blockseg[count];
        for (var i = 0; i < count; i++) {
            var len = decodeUniform(decoder, BLOCKSEGS_N_LEN) + 1;
            var blocks = new int[len];
            var seglens = new int[len];
            for (var j = 0; j < len; j++) {
                blocks[j] = decodeUniform(decoder, BLOCKSEGS_N_BLOCK);
                seglens[j] = decodeUniform(decoder, BLOCKSEGS_N_SEGLEN) + 1;
            }
            out[i] = new Blockseg(blocks, seglens);
        }
        return out;
    }

    /**
     * Decodes one value from a uniform distribution over {@code [0, n)}, the table-load-time inline of
     * {@code smpl_ec_decode_uniform}.
     *
     * <p>Mirrors
     * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper#decodeUniform}
     * but is duplicated here so the table layer does not depend on the symbol-helper layer; the block
     * segmentation stream is the only place a table loader runs the range decoder.
     *
     * @param decoder the range decoder to advance
     * @param n       the number of equiprobable values; must be at least 1
     * @return the decoded value in {@code [0, n)}
     */
    private static int decodeUniform(MlowRangeDecoder decoder, int n) {
        var cmfLow = decoder.decode(n);
        decoder.update(cmfLow, cmfLow + 1, n);
        return (int) cmfLow;
    }
}
