package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.PitchTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Open loop pitch lag estimator of the MLow speech encoder.
 *
 * <p>The estimator picks the per subframe integer pitch lags that the closed loop adaptive codebook search
 * later refines, and the block segmentation index that codes those lags in the bitstream. It operates on the
 * perceptually weighted speech held in the long term prediction buffer, padded with look ahead, not on the raw
 * residual: the caller filters each frame through the pitch perceptual weighting filter into that buffer before
 * the call. The search is a coarse to fine normalized cross correlation peak pick:
 * <ul>
 *   <li><b>High pass and half rate downsample.</b> The buffer is high passed at {@code -3 dB / 60 Hz}
 *       ({@code 1 - z^-1} over {@code 1 - 0.96 z^-1}), then decimated by two through a symmetric FIR to the
 *       {@value #STAGE1_FS_KHZ} kHz stage 1 rate.</li>
 *   <li><b>Stage 1 normalized correlation.</b> For each of the {@value #PITCH_NUM_SUBFRAMES} pitch subframes
 *       the lag energy {@code E1} (sliding window, {@link #calcE1}), the cross correlation {@code C} and the
 *       target energy {@code E2} are formed over the stage 1 lag range {@code [MINPITCH_STAGE1,
 *       MAXPITCH_STAGE1]}. {@code E} is the mean energy {@code (0.5 (sqrt E1 + sqrt E2))^2}.</li>
 *   <li><b>Upsample to the {@value #COARSE_FS_KHZ} kHz coarse grid.</b> {@code C} is band limited interpolated
 *       and {@code E} is linearly interpolated back to full rate, yielding the coarse normalized correlation
 *       {@code H = C / E} over {@value #NUMLAGS_COARSE} lags.</li>
 *   <li><b>Block track tournament.</b> Each lag block's coarse correlation peak feeds a weighted utility over
 *       the {@value #NUM_BLOCKTRACKS} candidate block tracks; the top {@code numstates1} survivors are kept by
 *       the leaf flag tournament {@link #getMaxiK}.</li>
 *   <li><b>Fine refinement.</b> For the lag blocks the survivors touch, {@code C}, {@code E} and {@code H} are
 *       recomputed at the input rate and fractionally upsampled by two, and each block segmentation's per
 *       segment lag is the peak of the energy weighted combined correlation.</li>
 *   <li><b>Final selection.</b> Each survivor block segmentation's utility is its overall normalized
 *       correlation, less a lag jitter penalty and the entropy coding rate estimate, plus a spectral
 *       harmonicity bonus and a previous lag continuity bonus; the highest utility segmentation wins.</li>
 * </ul>
 * The output is one lag index per pitch subframe, the corresponding fractional lags, the winning block
 * segmentation index, the best normalized pitch correlation, the dominant subframe average lag, and the
 * spectral harmonicity of that lag.
 *
 * <p>This estimator is stateful across the frames of a packet. It carries the previous frame's last lag and
 * its pitch correlation (for the continuity bonus) and the previous frame's last lag block and lag index (for
 * the conditional rate estimate). {@link #reset()} clears the lag block carry; an inactive voice frame clears
 * all carry. The {@code numstates1} survivor count and the {@link #lowRate} flag are fixed at construction from
 * the encoder complexity and bitrate.
 *
 * <p>Float reproducibility. The survivor tournaments compare normalized correlations that differ in the last
 * float bit, so the arithmetic of the shipped encoder is reproduced exactly. The correlation and energy dot
 * products ({@link #nrg}, {@link #dotProd}, {@link #dotProd20}, {@link #dotProd40}) accumulate with a two wide
 * paired reduction: the shipped dot products are built with fast math and SSE2 (not AVX), so each reduction is
 * an even lane and an odd lane running sum added once at the end, not a left to right scan. A scalar sum rounds
 * differently and, at the high cancellation of these correlations, flips a survivor index. The {@link #getMaxi}
 * and {@link #getMaxiK} tournaments reproduce the strict greater than comparison with the leaf flag toggle and
 * the {@code -FLT_MAX} removal followed by propagation back up the tournament. The high pass and resampling
 * filters, the running window energy and the bit estimate are bit faithful.
 *
 * <p>Scope is the shipped 16 kHz, 60 ms, mono high rate configuration: 20 ms frames, eight pitch subframes, the
 * {@value #NUM_BLOCKTRACKS} 20 ms block tracks and {@value #NUM_BLOCKSEGS} block segmentations of
 * {@link PitchTables}, and the high quality search path ({@link #lowComplexity} false, the band limited
 * correlation interpolation and the {@value #H_THRES_HQ} correlation gate). The decode side lag tables this
 * estimator shares are owned by {@link PitchTables}; the encoder only block track spans and the generated block
 * tracks are built once here.
 *
 * @implNote This implementation keeps the flat correlation buffers and reproduces the in place upsample
 * aliasing verbatim: the coarse rate {@code C}/{@code E} use the {@code numlagsC}/{@code numlagsE} strides, and
 * the fractional full rate {@code C}/{@code E} overwrite them from the high subframe down to the low one with
 * the {@code (PITCH_NUM_BLOCKS * 2 * PITCHBLOCK + offset)} stride, so the final search reads land on exactly
 * the same addresses. The low complexity branch (survivor count at most four) is retained for completeness but
 * never taken at the shipped complexity. The pitch multiple short weight bias is disabled in the shipped build
 * and is therefore omitted; the spectral harmonicity bias is enabled and ported in full.
 */
public final class OpenLoopPitch {
    /**
     * Pitch search sample rate in kHz.
     */
    private static final int FS_KHZ = 16;

    /**
     * Stage 1 (half rate) pitch search sample rate in kHz.
     */
    private static final int STAGE1_FS_KHZ = 8;

    /**
     * Coarse grid pitch search sample rate in kHz.
     */
    private static final int COARSE_FS_KHZ = 16;

    /**
     * Number of pitch subframes per 20 ms frame.
     */
    private static final int PITCH_NUM_SUBFRAMES = 8;

    /**
     * Minimum pitch period in milliseconds.
     */
    private static final int MINPITCH_MS = 2;

    /**
     * Maximum pitch period in milliseconds.
     */
    private static final int MAXPITCH_MS = 20;

    /**
     * Minimum pitch period in samples at the search rate.
     */
    private static final int MINPITCH_LEN = MINPITCH_MS * FS_KHZ;

    /**
     * Maximum pitch period in samples at the search rate.
     */
    private static final int MAXPITCH_LEN = MAXPITCH_MS * FS_KHZ;

    /**
     * Total interpolation delay of the resampling chain.
     */
    private static final int TOT_INTERP_DELAY = 6;

    /**
     * Smallest stage 1 lag searched.
     */
    private static final int MINPITCH_STAGE1 = MINPITCH_MS * STAGE1_FS_KHZ - TOT_INTERP_DELAY;

    /**
     * Largest stage 1 lag searched.
     */
    private static final int MAXPITCH_STAGE1 = MAXPITCH_MS * STAGE1_FS_KHZ + TOT_INTERP_DELAY;

    /**
     * Stage 1 lag subframe length in samples.
     */
    private static final int LAG_SUBFRLEN_STAGE1 = STAGE1_FS_KHZ * 40 / FS_KHZ;

    /**
     * Input rate lag subframe length in samples.
     */
    private static final int LAG_SUBFRLEN = 40;

    /**
     * Half block length in samples at the search rate.
     */
    private static final int PITCHBLOCK = 2 * FS_KHZ;

    /**
     * Full lag block size in fractional lag indices ({@code 2 * PITCHBLOCK}).
     */
    private static final int BLOCKSIZE = 2 * PITCHBLOCK;

    /**
     * Number of coarse lag blocks.
     */
    private static final int PITCH_NUM_BLOCKS = (MAXPITCH_MS - MINPITCH_MS) / 2;

    /**
     * Number of lags on the coarse grid.
     */
    private static final int NUMLAGS_COARSE = COARSE_FS_KHZ * (MAXPITCH_MS - MINPITCH_MS);

    /**
     * Smallest coarse grid lag.
     */
    private static final int MINPITCH_COARSE = COARSE_FS_KHZ * MINPITCH_MS;

    /**
     * Coarse grid half block length.
     */
    private static final int PITCHBLOCK_COARSE = 2 * COARSE_FS_KHZ;

    /**
     * Number of stage 1 lags before any upsampling.
     */
    private static final int NUMLAGS_STAGE1 = MAXPITCH_STAGE1 - MINPITCH_STAGE1 + 1;

    /**
     * Number of input rate lags.
     */
    private static final int NUMLAGS_FS = FS_KHZ * (MAXPITCH_MS - MINPITCH_MS);

    /**
     * Number of candidate 20 ms block tracks.
     */
    private static final int NUM_BLOCKTRACKS = 187;

    /**
     * Number of 20 ms block segmentations.
     */
    private static final int NUM_BLOCKSEGS = 217;

    /**
     * Half rate decimation FIR delay.
     */
    private static final int DOWNSAMP_DELAY = 7;

    /**
     * Band limited correlation interpolation FIR half length.
     */
    private static final int INTERPOL_DELAY_C = 4;

    /**
     * Lag jitter penalty weight.
     */
    private static final float DELTAWGHT = 0.1439f;

    /**
     * Per track short lag bias weight.
     */
    private static final float SHORTWGHT1 = 0.04f;

    /**
     * Spectral harmonicity bias weight.
     */
    private static final float SPEC_HARM_BIAS = 2.5f;

    /**
     * Previous lag continuity bonus weight.
     */
    private static final float PREVWGHT = 0.7981f;

    /**
     * Fractional span of the previous lag the continuity bonus covers.
     */
    private static final float PREVWGHT_SPAN = 0.15f;

    /**
     * Low rate entropy coding rate bias weight.
     */
    private static final float RATEWGHT_LR = 0.028f;

    /**
     * High rate entropy coding rate bias weight.
     */
    private static final float RATEWGHT_HR = 0.022f;

    /**
     * Block track delta penalty reduction factor.
     */
    private static final float REDUCTION_FACTOR = 0.7f;

    /**
     * Per fractional index lag jitter weight ({@code DELTAWGHT / BLOCKSIZE}).
     */
    private static final float PITCH_DELTAWGHT = DELTAWGHT / BLOCKSIZE;

    /**
     * Coarse correlation gate for the high quality fine search, applied when {@link #lowComplexity} is false.
     */
    private static final float H_THRES_HQ = 0.25f;

    /**
     * Spectral analysis half spectrum length.
     */
    private static final int F_LEN = 257;

    /**
     * Length of the spectral harmonicity result cache.
     */
    private static final int HARM_CACHE_LEN = 50;

    /**
     * Sentinel for an unfilled spectral harmonicity cache slot.
     */
    private static final float HARMONICITY_UNDEF = -10000.0f;

    /**
     * Number of harmonic peak and valley measurements in the spectral harmonicity.
     */
    private static final int NUM_HARMS = 4;

    /**
     * Segment length cache key shift.
     */
    private static final int CACHE_BITS_BLOCK = 4;

    /**
     * Subframe cache key shift.
     */
    private static final int CACHE_BITS_SEG_LEN = 3;

    /**
     * Total span of the lag index cache ({@code 1 << (3 + CACHE_BITS_SEG_LEN + CACHE_BITS_BLOCK)}).
     */
    private static final int LAGIND_CACHE_LEN = 1 << (3 + CACHE_BITS_SEG_LEN + CACHE_BITS_BLOCK);

    /**
     * Half rate decimation FIR.
     */
    private static final float[] DOWNSAMP_FILT = {
            -0.045472838f, 0.0f, 0.06366198f, 0.0f, -0.10610329f, 0.0f, 0.31830987f, 0.5f,
            0.31830987f, 0.0f, -0.10610329f, 0.0f, 0.06366198f, 0.0f, -0.045472838f
    };

    /**
     * Band limited correlation interpolation FIR.
     */
    private static final float[] INTERPOL_FILT_C = {
            -0.0024414062f, 0.023925781f, -0.119628906f, 0.59814453f,
            0.59814453f, -0.119628906f, 0.023925783f, -0.0024414062f
    };

    /**
     * High pass moving average numerator {@code {1, -1}}.
     */
    private static final float[] HP_B = {1.0f, -1.0f};

    /**
     * High pass auto regressive denominator {@code {1, -0.96}}.
     */
    private static final float[] HP_A = {1.0f, -0.96f};

    /**
     * Peak weights of the spectral harmonicity peak and valley folding.
     */
    private static final float[] MAG_PEAK_WEIGHTS = {1.0f, 10.0f, 1.0f};

    /**
     * Valley weights of the spectral harmonicity peak and valley folding.
     */
    private static final float[] MAG_VALLEY_WEIGHTS = {5.0f, 2.0f, 5.0f};

    /**
     * Block track to block segmentation span table, flattened to interleaved {@code [offset, count]} pairs read
     * as unsigned bytes.
     *
     * <p>Track {@code t} spans the block segmentations {@code [BLOCKSEGS_IX[2t], BLOCKSEGS_IX[2t] +
     * BLOCKSEGS_IX[2t + 1])}. This is the encoder only companion of the decode tables {@link PitchTables}
     * already holds, embedded here so the encode package needs no new accessor on the table layer.
     */
    private static final int[] BLOCKSEGS_IX = {
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
            127, 1, 128, 1, 129, 1, 130, 1, 131, 1, 132, 1, 133, 1, 134, 1,
            135, 1, 136, 1, 137, 1, 138, 1, 139, 1, 140, 1, 141, 1, 142, 1,
            143, 1, 144, 1, 145, 1, 146, 1, 147, 1, 148, 1, 149, 1, 150, 1,
            151, 1, 152, 1, 153, 1, 154, 1, 155, 1, 156, 3, 159, 2, 161, 1,
            162, 1, 163, 1, 164, 1, 165, 1, 166, 1, 167, 1, 168, 1, 169, 1,
            170, 1, 171, 1, 172, 1, 173, 1, 174, 1, 175, 1, 176, 1, 177, 1,
            178, 1, 179, 1, 180, 1, 181, 3, 184, 1, 185, 2, 187, 1, 188, 1,
            189, 1, 190, 1, 191, 1, 192, 1, 193, 1, 194, 1, 195, 1, 196, 1,
            197, 1, 198, 1, 199, 1, 200, 2, 202, 1, 203, 1, 204, 1, 205, 1,
            206, 1, 207, 1, 208, 1, 209, 1, 210, 1, 211, 1, 212, 1, 213, 1,
            214, 1, 215, 1, 216, 1,
    };

    /**
     * One reconstructed block track: the per subframe lag block, the mean block index, and the cumulative block
     * jitter.
     *
     * @param track       the lag block index for each of the {@value #PITCH_NUM_SUBFRAMES} subframes
     * @param meanblock   the segment length weighted mean block index, divided by the subframe count
     * @param trackdeltas the cumulative absolute block to block jitter across the track's segmentation
     */
    private record Blocktrack(int[] track, float meanblock, float trackdeltas) {
    }

    /**
     * The result of one open loop pitch estimate.
     *
     * @param lags         the fractional pitch lag for each subframe
     * @param laginds      the integer fractional lag index for each subframe
     * @param pitchCorr    the best normalized pitch correlation
     * @param blocksegIdx  the winning block segmentation index
     * @param avgLag       the dominant subframe average lag
     * @param harmStrength the spectral harmonicity of the average lag
     */
    public record Result(float[] lags, int[] laginds, float pitchCorr, int blocksegIdx,
                         float avgLag, float harmStrength) {
    }

    /**
     * The logger for {@link OpenLoopPitch}.
     */
    private static final System.Logger LOGGER = Log.get(OpenLoopPitch.class);

    /**
     * The shared 20 ms pitch decode data: block segmentations, the permutation, the rate coding cumulative mass
     * functions, and the first block ranges.
     */
    private final PitchTables.PitchData data;

    /**
     * The reconstructed 20 ms block tracks.
     */
    private final Blocktrack[] blocktracks;

    /**
     * The survivor count kept by the block track tournament.
     */
    private final int numstates1;

    /**
     * Whether the low complexity search path is taken ({@code numstates1 <= 4}).
     */
    private final boolean lowComplexity;

    /**
     * Whether the low rate entropy coding rate bias weight applies.
     */
    private final boolean lowRate;

    /**
     * The previous frame's last fractional lag.
     */
    private float prevLag;

    /**
     * The previous frame's best pitch correlation.
     */
    private float prevPitchCorr;

    /**
     * The previous frame's last lag block; {@code -1} when there is no previous frame in the packet or the carry
     * was reset.
     */
    private int prevLagblk;

    /**
     * The previous frame's last lag index; {@code -1} when there is no previous frame in the packet or the carry
     * was reset.
     */
    private int prevLagidx;

    /**
     * Reused per frame scratch for the stage 1 square root energies, sized {@code 16 * (MAXPITCH_MS -
     * MINPITCH_MS)}.
     *
     * <p>Its used span {@code [0, numlags)} is fully written and then square rooted in place before it is read
     * each subframe, and it is not retained past the frame, so this single owner thread buffer is reused instead
     * of reallocated.
     */
    private final float[] sqrtE1Scratch = new float[16 * (MAXPITCH_MS - MINPITCH_MS)];

    /**
     * Reused per frame scratch for the per block square root energies, sized {@code 2 * PITCHBLOCK + 1}.
     *
     * <p>Its used span {@code [0, PITCHBLOCK + 1)} is fully written and square rooted in place before it is read
     * for each processed block, and it is not retained past the frame, so this single owner thread buffer is
     * reused instead of reallocated.
     */
    private final float[] sqrtE1BlkScratch = new float[2 * 16 + 1];

    /**
     * Reused per frame scratch for the energy weighted combined correlation of the fine search, sized
     * {@code 2 * PITCHBLOCK}.
     *
     * <p>Zeroed in full and then accumulated before the argmax reads it on every cache miss, and not retained
     * past the frame, so this single owner thread buffer is reused instead of reallocated.
     */
    private final float[] hCombScratch = new float[2 * PITCHBLOCK];

    /**
     * Reused per frame scratch for the per block track utilities, sized {@value #NUM_BLOCKTRACKS}.
     *
     * <p>Fully written before the survivor tournament reads it, and not retained past the frame, so this single
     * owner thread buffer is reused instead of reallocated.
     */
    private final float[] utilsScratch = new float[NUM_BLOCKTRACKS];

    /**
     * Reused per frame scratch for the per subframe weights, sized {@value #PITCH_NUM_SUBFRAMES}.
     *
     * <p>Fully written by {@link #calcSfWeights} before it is read, and not retained past the frame, so this
     * single owner thread buffer is reused instead of reallocated.
     */
    private final float[] sfWghtScratch = new float[PITCH_NUM_SUBFRAMES];

    /**
     * Reused per frame scratch for the per subframe target energies, sized {@value #PITCH_NUM_SUBFRAMES}.
     *
     * <p>Fully written by {@link #calcCE2} before any read, and not retained past the frame, so this single
     * owner thread buffer is reused instead of reallocated.
     */
    private final float[] e2Scratch = new float[PITCH_NUM_SUBFRAMES];

    /**
     * Reused per frame scratch for the per survivor block segmentation indices, sized {@value #NUM_BLOCKSEGS}.
     *
     * <p>Written entry by entry as survivors are enumerated and read only at indices already written this frame,
     * and not retained past the frame, so this single owner thread buffer is reused instead of reallocated.
     */
    private final int[] blocksegsIxScratch = new int[NUM_BLOCKSEGS];

    /**
     * Reused per frame scratch for the fine search lag index cache, sized {@value #LAGIND_CACHE_LEN}.
     *
     * <p>Filled with the {@code -1} miss sentinel in full at the start of the fine search each frame before it is
     * read, and not retained past the frame, so this single owner thread buffer is reused instead of
     * reallocated.
     */
    private final int[] lagindCacheScratch = new int[LAGIND_CACHE_LEN];

    /**
     * Constructs an open loop pitch estimator for the high rate shipped configuration with the given survivor
     * count.
     *
     * <p>The block tracks are generated once from the {@link PitchTables} block segmentations. The estimator
     * starts in the reset state ({@code prevLag == prevPitchCorr == 0}, {@code prevLagblk == prevLagidx ==
     * -1}), ready for the first frame of a packet.
     *
     * @param numstates1 the survivor count kept by the block track tournament (30 at the shipped highest
     *                   complexity)
     * @param lowRate    whether the low rate rate bias weight applies
     */
    public OpenLoopPitch(int numstates1, boolean lowRate) {
        this.data = PitchTables.data20();
        this.numstates1 = numstates1;
        this.lowComplexity = numstates1 <= 4;
        this.lowRate = lowRate;
        this.blocktracks = genBlocktracks(data.blocksegs());
        reset();
    }

    /**
     * Clears the lag block carry ({@code prevLagblk = prevLagidx = -1}).
     *
     * <p>Must be called wherever the encoder resets the lag carry: after an unvoiced frame and at the last frame
     * of a packet. The previous lag continuity carry ({@link #prevLag}, {@link #prevPitchCorr}) is not touched;
     * the reset only clears the block and index carry.
     */
    public void reset() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "open loop pitch estimator reset");
        }
        this.prevLagblk = -1;
        this.prevLagidx = -1;
    }

    /**
     * Estimates the open loop pitch lags of one frame.
     *
     * <p>When {@code codedAsActiveVoice} is false the lags collapse to the minimum pitch, all indices are zero,
     * and the full carry ({@link #prevLag}, {@link #prevPitchCorr}, {@link #prevLagblk}, {@link #prevLagidx}) is
     * cleared. Otherwise the full coarse to fine search runs and the carry is updated for the next frame. The
     * {@code ltpBuf} holds the perceptually weighted speech of the frame plus the look ahead and resampling
     * delay padding, a slice of length {@code l}; {@code f2} is the frame's short term power spectrum from the
     * LPC analysis, {@value #F_LEN} bins. The {@code ltpBuf} is not modified.
     *
     * @param ltpBuf             the look ahead padded weighted speech buffer, at least {@code l} samples
     * @param l                  the number of valid samples in {@code ltpBuf}
     * @param lookAhead          the number of look ahead samples at the end of {@code ltpBuf}
     * @param f2                 the frame short term power spectrum, {@value #F_LEN} bins
     * @param codedAsActiveVoice whether the frame is coded as active voice
     * @param numsubfrs          the number of pitch subframes (always {@value #PITCH_NUM_SUBFRAMES} for the
     *                           shipped configuration)
     * @return the estimated lags, indices, correlation, block segmentation, average lag, and harmonicity
     */
    public Result estimate(float[] ltpBuf, int l, int lookAhead, float[] f2,
                           boolean codedAsActiveVoice, int numsubfrs) {
        var lags = new float[numsubfrs];
        var laginds = new int[numsubfrs];
        if (!codedAsActiveVoice) {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "open loop pitch estimate: inactive voice, using minimum pitch");
            }
            for (var i = 0; i < numsubfrs; i++) {
                lags[i] = MINPITCH_LEN;
            }
            prevLag = 0.0f;
            prevPitchCorr = 0.0f;
            prevLagblk = -1;
            prevLagidx = -1;
            return new Result(lags, laginds, 0.0f, 0, (float) MINPITCH_LEN, 0.0f);
        }
        var result = search(ltpBuf, l, lookAhead, f2, numsubfrs, lags, laginds);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "open loop pitch estimate: corr={0} blockseg={1} avgLag={2}",
                    result.pitchCorr(), result.blocksegIdx(), result.avgLag());
        }
        return result;
    }

    /**
     * Runs the full active voice coarse to fine pitch search after the inactive guard.
     *
     * @param ltpBuf    the look ahead padded weighted speech buffer
     * @param l         the number of valid samples in {@code ltpBuf}
     * @param lookAhead the number of look ahead samples
     * @param f2        the frame short term power spectrum
     * @param numsubfrs the number of pitch subframes
     * @param lags      the destination fractional lags
     * @param laginds   the destination fractional lag indices
     * @return the estimate result
     */
    private Result search(float[] ltpBuf, int l, int lookAhead, float[] f2, int numsubfrs,
                          float[] lags, int[] laginds) {
        var cBuf = (2 * FS_KHZ / STAGE1_FS_KHZ) * NUMLAGS_STAGE1 * PITCH_NUM_SUBFRAMES;
        var cArr = new float[cBuf];
        var hArr = new float[cBuf];
        var e1Arr = new float[cBuf];
        var eArr = new float[cBuf];
        var e2Arr = e2Scratch;
        var cCoarse = new float[NUMLAGS_COARSE * PITCH_NUM_SUBFRAMES];
        var hCoarse = new float[NUMLAGS_COARSE * PITCH_NUM_SUBFRAMES];
        var eCoarse = new float[NUMLAGS_COARSE * PITCH_NUM_SUBFRAMES];

        var offset = DOWNSAMP_DELAY;
        var stage1 = new float[(2 * FS_KHZ * 20) + MAXPITCH_LEN + 2 * DOWNSAMP_DELAY];
        var ltpHp = new float[(2 * FS_KHZ * 20) + MAXPITCH_LEN];

        var state = new float[2];
        // HP: -3 dB @ 60 Hz, to remove low frequency noise.
        hpArma1(ltpBuf, l, state, stage1, offset);
        System.arraycopy(stage1, offset, ltpHp, 0, l - lookAhead);

        var stage1Len = downsample(stage1, l + offset);

        calcE1Multi(e1Arr, stage1, stage1Len, numsubfrs, MINPITCH_STAGE1, MAXPITCH_STAGE1, LAG_SUBFRLEN_STAGE1);
        calcCE2(cArr, e2Arr, stage1, stage1Len, numsubfrs);

        var numlags = NUMLAGS_STAGE1;
        var sqrtE1 = sqrtE1Scratch;
        for (var sf = 0; sf < numsubfrs; sf++) {
            for (var i = 0; i < numlags; i++) {
                sqrtE1[i] = e1Arr[sf * numlags + i] + 1e-30f;
            }
            sqrtVec(sqrtE1, numlags);
            var sqrtE2 = (float) Math.sqrt(e2Arr[sf] + 1e-30f);
            for (var i = 0; i < numlags; i++) {
                var tmp = 0.5f * (sqrtE1[i] + sqrtE2);
                eArr[sf * numlags + i] = tmp * tmp;
            }
        }

        var minpitchC = MINPITCH_STAGE1;
        var numlagsC = numlags;
        var minpitchE = MINPITCH_STAGE1;
        var numlagsE = numlags;
        var cUp = lowComplexity
                ? upsampEFast(numsubfrs, minpitchC, numlagsC, cArr)
                : upsampCFast(numsubfrs, minpitchC, numlagsC, cArr);
        minpitchC = cUp[0];
        numlagsC = cUp[1];
        var eUp = upsampEFast(numsubfrs, minpitchE, numlagsE, eArr);
        minpitchE = eUp[0];
        numlagsE = eUp[1];

        var offsetC = MINPITCH_COARSE - minpitchC;
        var offsetE = MINPITCH_COARSE - minpitchE;
        for (var sf = 0; sf < numsubfrs; sf++) {
            for (var i = 0; i < NUMLAGS_COARSE; i++) {
                hArr[sf * NUMLAGS_COARSE + i] =
                        cArr[sf * numlagsC + offsetC + i] / eArr[sf * numlagsE + offsetE + i];
            }
            System.arraycopy(hArr, sf * NUMLAGS_COARSE, hCoarse, sf * NUMLAGS_COARSE, NUMLAGS_COARSE);
            System.arraycopy(cArr, sf * numlagsC + offsetC, cCoarse, sf * NUMLAGS_COARSE, NUMLAGS_COARSE);
            System.arraycopy(eArr, sf * numlagsE + offsetE, eCoarse, sf * NUMLAGS_COARSE, NUMLAGS_COARSE);
        }

        var hblk = new float[PITCH_NUM_SUBFRAMES][PITCH_NUM_BLOCKS];
        for (var sf = 0; sf < numsubfrs; sf++) {
            var blockPtr = sf * NUMLAGS_COARSE;
            for (var block = 0; block < PITCH_NUM_BLOCKS; block++) {
                hblk[sf][block] = maximum(hArr, blockPtr, PITCHBLOCK_COARSE);
                blockPtr += PITCHBLOCK_COARSE;
            }
        }

        var sfWght = sfWghtScratch;
        calcSfWeights(e2Arr, numsubfrs, sfWght);

        var utils = utilsScratch;
        for (var i = 0; i < blocktracks.length; i++) {
            var corr = 0.0f;
            var track = blocktracks[i].track();
            for (var sf = 0; sf < numsubfrs; sf++) {
                corr += hblk[sf][track[sf]] * sfWght[sf];
            }
            var shortlagbias1 =
                    (MAXPITCH_LEN / ((blocktracks[i].meanblock() + 1.5f) * PITCHBLOCK) - 1.0f) * SHORTWGHT1;
            utils[i] = 1.0f / (1.1f - corr)
                    - REDUCTION_FACTOR * PITCHBLOCK * PITCH_DELTAWGHT * blocktracks[i].trackdeltas()
                    + shortlagbias1;
        }
        var trackIdx = getMaxiK(utils, blocktracks.length, numstates1);

        // Recompute E1 at the input sampling rate over the full input rate lag range.
        calcE1Multi(e1Arr, ltpHp, l - lookAhead, numsubfrs, minpitchE, minpitchE + numlagsE - 1, LAG_SUBFRLEN);

        var uniqueblocks = new int[PITCH_NUM_SUBFRAMES];
        for (var i = 0; i < numstates1; i++) {
            var track = blocktracks[trackIdx[i]].track();
            for (var sf = 0; sf < numsubfrs; sf++) {
                uniqueblocks[sf] |= (1 << track[sf]);
            }
        }

        var hThres = lowComplexity ? 0.0f : H_THRES_HQ;
        offsetC = MINPITCH_MS * FS_KHZ - minpitchC;
        offsetE = MINPITCH_MS * FS_KHZ - minpitchE;
        var sqrtE1Blk = sqrtE1BlkScratch;
        for (var sf = 0; sf < numsubfrs; sf++) {
            var cPtr = offsetC + sf * numlagsC;
            var ePtr = offsetE + sf * numlagsE;
            var e1Ptr = offsetE + sf * numlagsE;
            var hPtr = sf * NUMLAGS_FS;
            var ltpPtr = l - lookAhead + (sf - numsubfrs) * LAG_SUBFRLEN;
            e2Arr[sf] = Math.max(dotProd40Self(ltpHp, ltpPtr), 1e-9f);
            var sqrtE2 = (float) Math.sqrt(e2Arr[sf] + 1e-30f);
            var mask = 1;
            for (var block = 0; block < PITCH_NUM_BLOCKS; block++) {
                if ((uniqueblocks[sf] & mask) != 0) {
                    for (var i = 0; i < PITCHBLOCK + 1; i++) {
                        sqrtE1Blk[i] = e1Arr[e1Ptr + block * PITCHBLOCK + i] + 1e-30f;
                    }
                    sqrtVec(sqrtE1Blk, PITCHBLOCK + 1);
                    for (var i = 0; i < PITCHBLOCK + 1; i++) {
                        var tmp = 0.5f * (sqrtE1Blk[i] + sqrtE2);
                        eArr[ePtr + block * PITCHBLOCK + i] = 0.5f * tmp * tmp;
                    }
                    for (var i = 0; i < PITCHBLOCK; i++) {
                        if (hArr[hPtr + block * PITCHBLOCK + i] > hThres) {
                            var lag = MINPITCH_LEN + block * PITCHBLOCK + i;
                            cArr[cPtr + block * PITCHBLOCK + i] =
                                    0.5f * dotProd40(ltpHp, ltpPtr, ltpHp, ltpPtr - lag);
                        }
                    }
                }
                mask <<= 1;
            }
        }

        // Fractionally upsample C and E and recompute H, high subframe to low to avoid overwrite.
        var fracStride = PITCH_NUM_BLOCKS * 2 * PITCHBLOCK + offsetC;
        for (var sf = numsubfrs - 1; sf >= 0; sf--) {
            var cPtr = offsetC + sf * numlagsC;
            var cPtrFrac = offsetC + sf * (PITCH_NUM_BLOCKS * 2 * PITCHBLOCK + offsetC);
            var ePtr = offsetE + sf * numlagsE;
            var ePtrFrac = offsetE + sf * (PITCH_NUM_BLOCKS * 2 * PITCHBLOCK + offsetE);
            var hPtr = sf * 2 * PITCHBLOCK * PITCH_NUM_BLOCKS;
            var mask = 1 << (PITCH_NUM_BLOCKS - 1);
            for (var block = PITCH_NUM_BLOCKS - 1; block >= 0; block--) {
                if ((uniqueblocks[sf] & mask) != 0) {
                    upsampECore(eArr, ePtr + block * PITCHBLOCK + PITCHBLOCK - 1,
                            eArr, ePtrFrac + block * 2 * PITCHBLOCK + 2 * PITCHBLOCK - 1, PITCHBLOCK);
                    if (lowComplexity) {
                        upsampECore(cArr, cPtr + block * PITCHBLOCK + PITCHBLOCK - 1,
                                cArr, cPtrFrac + block * 2 * PITCHBLOCK + 2 * PITCHBLOCK - 1, PITCHBLOCK);
                    } else {
                        upsampCCore(cArr, cPtr + block * PITCHBLOCK + PITCHBLOCK - 1,
                                cArr, cPtrFrac + block * 2 * PITCHBLOCK + 2 * PITCHBLOCK - 1, PITCHBLOCK);
                    }
                    var coutBase = cPtrFrac + block * 2 * PITCHBLOCK;
                    var eoutBase = ePtrFrac + block * 2 * PITCHBLOCK;
                    for (var i = 0; i < 2 * PITCHBLOCK; i++) {
                        hArr[hPtr + block * 2 * PITCHBLOCK + i] = cArr[coutBase + i] / eArr[eoutBase + i];
                    }
                }
                mask >>= 1;
            }
        }

        // Fine search: per survivor segmentation, the energy weighted combined correlation peak.
        var hComb = hCombScratch;
        var nlaginds = 0;
        var blocksegsIx = blocksegsIxScratch;
        var survLaginds = new int[NUM_BLOCKSEGS][PITCH_NUM_SUBFRAMES];
        var lagindCache = lagindCacheScratch;
        Arrays.fill(lagindCache, -1);
        for (var surv = 0; surv < numstates1; surv++) {
            var idx = trackIdx[surv];
            var spanStart = BLOCKSEGS_IX[2 * idx];
            var spanCount = BLOCKSEGS_IX[2 * idx + 1];
            for (var j = 0; j < spanCount; j++) {
                blocksegsIx[nlaginds] = spanStart + j;
                var seg = data.blocksegs()[blocksegsIx[nlaginds]];
                var blocks = seg.blocks();
                var seglens = seg.seglens();
                var startSf = 0;
                for (var n = 0; n < seg.nblocks(); n++) {
                    var lookupKey = (((startSf << CACHE_BITS_SEG_LEN) + seglens[n]) << CACHE_BITS_BLOCK) + blocks[n];
                    var bestI = lagindCache[lookupKey];
                    if (bestI == -1) {
                        Arrays.fill(hComb, 0.0f);
                        for (var sf = startSf; sf < startSf + seglens[n]; sf++) {
                            var hPtr = sf * 2 * PITCHBLOCK * PITCH_NUM_BLOCKS + blocks[n] * 2 * PITCHBLOCK;
                            for (var i = 0; i < 2 * PITCHBLOCK; i++) {
                                hComb[i] += hArr[hPtr + i] * e2Arr[sf];
                            }
                        }
                        bestI = getMaxi(hComb, 2 * PITCHBLOCK);
                        lagindCache[lookupKey] = bestI;
                    }
                    for (var sf = startSf; sf < startSf + seglens[n]; sf++) {
                        survLaginds[nlaginds][sf] = bestI + blocks[n] * 2 * PITCHBLOCK;
                    }
                    startSf += seglens[n];
                }
                nlaginds++;
            }
        }

        // Final search: utility per survivor, with the spectral harmonicity and previous lag bonuses.
        var bestUtil = 0.0f;
        var bestPitchcorr = 0.0f;
        var bestSurv = 0;
        var pitchRatewght = lowRate ? RATEWGHT_LR : RATEWGHT_HR;

        var f2w = new float[F_LEN];
        for (var i = 2; i < F_LEN; i++) {
            f2w[i] = f2[i] * (i + 3);
        }
        var maxIx = getMaxi(sfWght, numsubfrs);
        var harmCache = new float[HARM_CACHE_LEN];
        for (var surv = 0; surv < nlaginds; surv++) {
            var sumC = 0.0f;
            var sumE = 0.0f;
            for (var sf = 0; sf < numsubfrs; sf++) {
                var cPtr = offsetC + sf * (PITCH_NUM_BLOCKS * 2 * PITCHBLOCK + offsetC);
                var ePtr = offsetE + sf * (PITCH_NUM_BLOCKS * 2 * PITCHBLOCK + offsetE);
                sumC += cArr[cPtr + survLaginds[surv][sf]];
                sumE += eArr[ePtr + survLaginds[surv][sf]];
            }
            var rateBias = encodeLagsBits(blocksegsIx[surv], survLaginds[surv], prevLagblk, prevLagidx)
                           * pitchRatewght;

            var meanLag = survLaginds[surv][maxIx] * 0.5f + MINPITCH_LEN;
            var pitchcorr = sumC / sumE;
            var firstLag = 0.5f * survLaginds[surv][0] + MINPITCH_LEN;
            var prevLagBias = prevLagBias(firstLag);
            var spectralHarmBias =
                    SPEC_HARM_BIAS * spectralHarmonicity(meanLag, f2w, harmCache, surv == 0);

            var util = 1.0f / (1.1f - pitchcorr)
                       - PITCH_DELTAWGHT * sumDeltas(survLaginds[surv], numsubfrs)
                       + spectralHarmBias + prevLagBias - rateBias;
            if (surv == 0 || util > bestUtil) {
                bestUtil = util;
                bestSurv = surv;
            }
            if (surv == 0 || pitchcorr > bestPitchcorr) {
                bestPitchcorr = pitchcorr;
            }
        }

        for (var sf = 0; sf < numsubfrs; sf++) {
            lags[sf] = survLaginds[bestSurv][sf] * 0.5f + MINPITCH_LEN;
            laginds[sf] = survLaginds[bestSurv][sf];
        }
        var avgLag = survLaginds[bestSurv][maxIx] * 0.5f + MINPITCH_LEN;
        var harmStrength = spectralHarmonicity(avgLag, f2w, harmCache, false);

        prevLag = lags[numsubfrs - 1];
        prevPitchCorr = bestPitchcorr;
        prevLagidx = survLaginds[bestSurv][numsubfrs - 1];
        prevLagblk = prevLagidx / (2 * PITCHBLOCK);

        return new Result(lags, laginds, bestPitchcorr, blocksegsIx[bestSurv], avgLag, harmStrength);
    }

    /**
     * Generates the block tracks from the block segmentations.
     *
     * <p>Each track expands the first block segmentation of its span into a per subframe block index, and
     * accumulates the segment length weighted mean block and the cumulative block jitter. The mean block is
     * divided by the {@value #PITCH_NUM_SUBFRAMES} subframe count.
     *
     * @param blocksegs the reconstructed block segmentations indexed by block segmentation index
     * @return the {@value #NUM_BLOCKTRACKS} generated block tracks
     */
    private static Blocktrack[] genBlocktracks(PitchTables.Blockseg[] blocksegs) {
        var out = new Blocktrack[NUM_BLOCKTRACKS];
        for (var t = 0; t < NUM_BLOCKTRACKS; t++) {
            var seg = blocksegs[BLOCKSEGS_IX[2 * t]];
            var blocks = seg.blocks();
            var seglens = seg.seglens();
            var track = new int[PITCH_NUM_SUBFRAMES];
            var meanblock = 0.0f;
            var trackdeltas = 0.0f;
            var segIdx = 0;
            for (var b = 0; b < seg.nblocks(); b++) {
                for (var k = 0; k < seglens[b]; k++) {
                    track[segIdx++] = blocks[b];
                }
                meanblock += (float) blocks[b] * seglens[b];
                if (b != 0) {
                    trackdeltas += Math.abs(blocks[b - 1] - blocks[b]);
                }
            }
            meanblock /= PITCH_NUM_SUBFRAMES;
            out[t] = new Blocktrack(track, meanblock, trackdeltas);
        }
        return out;
    }

    /**
     * Decimates the high passed buffer by two through the symmetric FIR.
     *
     * <p>The center tap multiplies the delayed sample and the symmetric pairs fold around it. The first
     * {@value #DOWNSAMP_DELAY} stage 1 output samples are left zero (the caller zeroes them before the high pass
     * writes its output past the delay); this writes the decimated samples in place into the same buffer past
     * the delay region.
     *
     * @param buf the buffer holding the delayed high passed samples, decimated in place from the start
     * @param len the number of valid input samples
     * @return the number of decimated output samples
     */
    private static int downsample(float[] buf, int len) {
        var outLen = (len - 2 * DOWNSAMP_DELAY) / 2;
        for (var j = 0; j < outLen; j++) {
            var tmp = buf[2 * j + DOWNSAMP_DELAY] * DOWNSAMP_FILT[DOWNSAMP_DELAY];
            for (var i = 0; i < DOWNSAMP_DELAY; i += 2) {
                tmp += (buf[2 * j + i] + buf[2 * j + 2 * DOWNSAMP_DELAY - i]) * DOWNSAMP_FILT[i];
            }
            buf[j] = tmp;
        }
        return outLen;
    }

    /**
     * Computes the sliding window lag energy for one lag span.
     *
     * <p>The first entry is the energy of the {@code lagSubfrlen} sample window at the largest lag; each
     * subsequent entry slides the window by one lag, adding the new leading sample's square and removing the
     * trailing one's, floored at {@code 1e-9}.
     *
     * @implNote This implementation groups the running update as {@code E1[i-1] + (add - sub)}, the leading
     * minus trailing squared difference added to the carry in one parenthesized term, so the two squares are
     * subtracted before accumulating; the left to right {@code (E1[i-1] + add) - sub} rounds differently and
     * shifts the input rate lag energy that feeds the normalized pitch correlation.
     *
     * @param e1          the destination energy vector
     * @param ltpbuf      the buffer to read the windows from
     * @param t           the target offset
     * @param minpitch    the smallest lag
     * @param maxpitch    the largest lag
     * @param lagSubfrlen the energy window length
     */
    private static void calcE1(float[] e1, float[] ltpbuf, int t, int minpitch, int maxpitch, int lagSubfrlen) {
        var numlags = maxpitch - minpitch + 1;
        var reg = t - minpitch;
        e1[0] = Math.max(nrg(ltpbuf, reg, lagSubfrlen), 1e-9f);
        for (var i = 1; i < numlags; i++) {
            var add = ltpbuf[reg - i] * ltpbuf[reg - i];
            var sub = ltpbuf[reg + lagSubfrlen - i] * ltpbuf[reg + lagSubfrlen - i];
            e1[i] = Math.max(e1[i - 1] + (add - sub), 1e-9f);
        }
    }

    /**
     * Computes the per subframe lag energy.
     *
     * <p>One extended {@code E1} is computed over the lag span widened by the subframe count, then sliced per
     * subframe with the slice offset decremented by {@code lagSubfrlen} each subframe.
     *
     * @param e1          the destination per subframe energy vector, {@code numsubfrs * numlags} entries
     * @param ltpbuf      the buffer to read the windows from
     * @param ltpbufLen   the number of valid samples in {@code ltpbuf}
     * @param numsubfrs   the number of pitch subframes
     * @param minpitch    the smallest lag
     * @param maxpitch    the largest lag
     * @param lagSubfrlen the energy window length
     */
    private static void calcE1Multi(float[] e1, float[] ltpbuf, int ltpbufLen, int numsubfrs,
                                    int minpitch, int maxpitch, int lagSubfrlen) {
        var numlags = maxpitch - minpitch + 1;
        var maxpitchExt = maxpitch + (numsubfrs - 1) * lagSubfrlen;
        var numlagsExt = maxpitchExt - minpitch + 1;
        var t = ltpbufLen - lagSubfrlen;
        var e1Ext = new float[1024];
        calcE1(e1Ext, ltpbuf, t, minpitch, maxpitchExt, lagSubfrlen);
        var offset = numlagsExt - numlags;
        for (var sf = 0; sf < numsubfrs; sf++) {
            for (var i = 0; i < numlags; i++) {
                e1[sf * numlags + i] = e1Ext[offset + i];
            }
            offset -= lagSubfrlen;
        }
    }

    /**
     * Computes the per subframe stage 1 cross correlation and target energy.
     *
     * <p>Per subframe the target window is correlated against every lagged window over the stage 1 lag span, and
     * the target energy is the window's self correlation, floored at {@code 1e-9}.
     *
     * @param c         the destination cross correlation, {@code numsubfrs * NUMLAGS_STAGE1} entries
     * @param e2        the destination per subframe target energy
     * @param ltpbuf    the stage 1 buffer
     * @param ltpbufLen the number of valid samples in {@code ltpbuf}
     * @param numsubfrs the number of pitch subframes
     */
    private static void calcCE2(float[] c, float[] e2, float[] ltpbuf, int ltpbufLen, int numsubfrs) {
        var numLagsStage1 = NUMLAGS_STAGE1;
        var t = ltpbufLen - LAG_SUBFRLEN_STAGE1 * numsubfrs;
        for (var sf = 0; sf < numsubfrs; sf++) {
            var tgt = t;
            var reg = t - MINPITCH_STAGE1;
            for (var i = 0; i < numLagsStage1; i++) {
                c[sf * numLagsStage1 + i] = dotProd20(ltpbuf, tgt, ltpbuf, reg - i);
            }
            t += LAG_SUBFRLEN_STAGE1;
            e2[sf] = Math.max(dotProd20(ltpbuf, tgt, ltpbuf, tgt), 1.0e-9f);
        }
    }

    /**
     * Linearly upsamples the energy buffer by two in place.
     *
     * <p>Each subframe's energy slice is doubled by inserting the average of adjacent samples, written from the
     * high subframe down to avoid overwriting the next subframe's input. The minimum lag doubles and the lag
     * count becomes {@code (n - 1) * 2}.
     *
     * @param numsubfrs the number of pitch subframes
     * @param minpitch  the current smallest lag
     * @param numlags   the current lag count
     * @param e         the energy buffer, upsampled in place
     * @return the updated {@code {minpitch, numlags}}
     */
    private static int[] upsampEFast(int numsubfrs, int minpitch, int numlags, float[] e) {
        var nlagsIn = numlags;
        var nlagsOut = (nlagsIn - 1) * 2;
        for (var sf = numsubfrs - 1; sf >= 0; sf--) {
            var x = sf * nlagsIn + nlagsIn - 2;
            var y = sf * nlagsOut + nlagsOut - 1;
            upsampECore(e, x, e, y, nlagsIn - 1);
        }
        return new int[]{minpitch * 2, nlagsOut};
    }

    /**
     * Band limited upsamples the correlation buffer by two in place.
     *
     * <p>Each subframe's correlation slice is doubled by the {@value #INTERPOL_DELAY_C} tap symmetric
     * interpolation FIR, written from the high subframe down. The minimum lag doubles and the lag count becomes
     * {@code (n - INTERPOL_DELAY_C) * 2}.
     *
     * @param numsubfrs the number of pitch subframes
     * @param minpitch  the current smallest lag
     * @param numlags   the current lag count
     * @param c         the correlation buffer, upsampled in place
     * @return the updated {@code {minpitch, numlags}}
     */
    private static int[] upsampCFast(int numsubfrs, int minpitch, int numlags, float[] c) {
        var nlagsIn = numlags;
        var nlagsOut = (nlagsIn - INTERPOL_DELAY_C) * 2;
        for (var sf = numsubfrs - 1; sf >= 0; sf--) {
            var x = sf * nlagsIn + nlagsIn - 1 - INTERPOL_DELAY_C;
            var y = sf * nlagsOut + nlagsOut - 1;
            upsampCCore(c, x, c, y, nlagsIn - (INTERPOL_DELAY_C * 2 - 1));
        }
        return new int[]{minpitch * 2, nlagsOut};
    }

    /**
     * Linearly interpolates a slice by two.
     *
     * <p>Writes backward from {@code yOff}: the interpolated sample is the average of two adjacent inputs, the
     * copied sample is the input. The {@code x} pointer walks backward.
     *
     * @param x    the input buffer
     * @param xOff the offset of the highest input sample
     * @param y    the output buffer
     * @param yOff the offset of the highest output sample
     * @param len  the number of input samples to expand
     */
    private static void upsampECore(float[] x, int xOff, float[] y, int yOff, int len) {
        for (var i = 0; i < len; i++) {
            y[yOff--] = (x[xOff] + x[xOff + 1]) * 0.5f;
            y[yOff--] = x[xOff--];
        }
    }

    /**
     * Band limited interpolates a slice by two.
     *
     * <p>Writes backward from {@code yOff}: the interpolated sample is the symmetric FIR over the centered
     * window, the copied sample is the input. The {@code x} pointer walks backward.
     *
     * @param x    the input buffer
     * @param xOff the offset of the highest input sample
     * @param y    the output buffer
     * @param yOff the offset of the highest output sample
     * @param len  the number of input samples to expand
     */
    private static void upsampCCore(float[] x, int xOff, float[] y, int yOff, int len) {
        for (var i = 0; i < len; i++) {
            var tmp = 0.0f;
            for (var j = 0; j < INTERPOL_DELAY_C; j++) {
                tmp += (x[xOff + j - (INTERPOL_DELAY_C - 1)] + x[xOff + INTERPOL_DELAY_C - j]) * INTERPOL_FILT_C[j];
            }
            y[yOff--] = tmp;
            y[yOff--] = x[xOff--];
        }
    }

    /**
     * Computes the subframe weights from the target energies.
     *
     * <p>Each weight is the subframe's target energy over the sum of all target energies.
     *
     * @param e2        the per subframe target energies
     * @param numsubfrs the number of pitch subframes
     * @param sfWght    the destination weight vector
     */
    private static void calcSfWeights(float[] e2, int numsubfrs, float[] sfWght) {
        var sumE2 = 0.0f;
        for (var sf = 0; sf < numsubfrs; sf++) {
            sumE2 += e2[sf];
        }
        for (var sf = 0; sf < numsubfrs; sf++) {
            sfWght[sf] = e2[sf] / sumE2;
        }
    }

    /**
     * Computes the cumulative absolute lag index jitter across a survivor.
     *
     * @param laginds   the per subframe lag indices
     * @param numsubfrs the number of pitch subframes
     * @return the sum of absolute adjacent subframe lag index differences
     */
    private static int sumDeltas(int[] laginds, int numsubfrs) {
        var ret = 0;
        for (var i = 1; i < numsubfrs; i++) {
            ret += Math.abs(laginds[i] - laginds[i - 1]);
        }
        return ret;
    }

    /**
     * Computes the previous lag continuity bonus.
     *
     * <p>When the candidate first lag falls within {@link #PREVWGHT_SPAN} of the previous lag, the bonus is the
     * previous pitch correlation scaled by the proximity and {@link #PREVWGHT}; otherwise zero.
     *
     * @param lag the candidate first lag
     * @return the continuity bonus
     */
    private float prevLagBias(float lag) {
        var lagDiff = Math.abs(lag - prevLag);
        var diffThres = PREVWGHT_SPAN * prevLag;
        if (lagDiff < diffThres) {
            return prevPitchCorr * (1.0f - (lagDiff / diffThres)) * PREVWGHT;
        }
        return 0.0f;
    }

    /**
     * Estimates the bit cost of coding a survivor's lags.
     *
     * <p>This mirrors the rate the entropy coder would spend: the block segmentation index symbol (direct, or
     * the block transition plus windowed index for a multi frame continuation), the uniform first lag symbol
     * (six bits) where the segmentation does not chain onto the previous block, and the within block delta lag
     * symbols. Each non uniform symbol contributes {@code -log2((fh - fl) / ft)} of its windowed cumulative mass
     * function; the uniform first lag contributes a flat six bits.
     *
     * @param blocksegsIx the survivor's block segmentation index
     * @param laginds     the survivor's per subframe lag indices
     * @param prevBlk     the previous frame's last lag block, or {@code -1}
     * @param prevIdx     the previous frame's last lag index, or {@code -1}
     * @return the estimated bit cost
     */
    private float encodeLagsBits(int blocksegsIx, int[] laginds, int prevBlk, int prevIdx) {
        var nBits = 0.0f;
        var ixJulia = data.blocksegs2idx()[blocksegsIx] & 0xFF;
        var blocksize = BLOCKSIZE;
        var seg = data.blocksegs()[blocksegsIx];
        var blocks = seg.blocks();
        var seglens = seg.seglens();
        if (prevBlk < 0) {
            var cmf = data.blocksegIdxCmf();
            nBits += encodeWrap(cmf[ixJulia - 1], cmf[ixJulia], cmf[data.numBlocksegs()]);
        } else {
            var cmf = data.blockTransitionCmf()[prevBlk];
            nBits += encodeWrap(cmf[blocks[0]], cmf[blocks[0] + 1], cmf[PITCH_NUM_BLOCKS]);
            var range = data.firstBlockRange();
            var startIx = range[blocks[0] * 2] & 0xFF;
            var cmfLen = (range[blocks[0] * 2 + 1] & 0xFF) - startIx + 1;
            var idxCmf = data.blocksegIdxCmf();
            var base = idxCmf[startIx];
            nBits += encodeWrap(
                    idxCmf[ixJulia - 1] - base,
                    idxCmf[ixJulia] - base,
                    idxCmf[startIx + cmfLen] - base);
        }
        var blk = blocks[0];
        var deltaBlk = blk - prevBlk;
        var startSeg = 0;
        var lagindsIx = 0;
        var runPrevBlk = prevBlk;
        var runPrevIdx = prevIdx;
        if (!(prevBlk > -1 && deltaBlk >= -1 && deltaBlk <= 2)) {
            nBits += 6.0f;
            runPrevBlk = blk;
            runPrevIdx = laginds[lagindsIx];
            lagindsIx += seglens[0];
            startSeg = 1;
        }
        var deltaLagCmf = data.deltaLagCmfs()[1];
        for (var k = startSeg; k < seg.nblocks(); k++) {
            blk = blocks[k];
            var idx = laginds[lagindsIx];
            lagindsIx += seglens[k];
            deltaBlk = blk - runPrevBlk;
            var deltaIdx = idx - runPrevIdx;
            var prevLagidxMod = runPrevIdx - runPrevBlk * blocksize;
            var deltaRangeStart = -prevLagidxMod + deltaBlk * blocksize;
            var pCmf = deltaRangeStart + 2 * blocksize - 1;
            var ix = deltaIdx - deltaRangeStart;
            var base = deltaLagCmf[pCmf];
            nBits += encodeWrap(
                    deltaLagCmf[pCmf + ix] - base,
                    deltaLagCmf[pCmf + ix + 1] - base,
                    deltaLagCmf[pCmf + blocksize] - base);
            runPrevBlk = blk;
            runPrevIdx = idx;
        }
        return nBits;
    }

    /**
     * Estimates one symbol's bit cost.
     *
     * <p>Returns {@code -log2((fh - fl) / ft)}, the ideal code length of a symbol whose cumulative mass function
     * window is {@code [fl, fh)} of total {@code ft}.
     *
     * @param fl the symbol's low cumulative frequency
     * @param fh the symbol's high cumulative frequency
     * @param ft the total frequency
     * @return the estimated bit cost
     */
    private static float encodeWrap(int fl, int fh, int ft) {
        var num = (float) (fh - fl);
        var den = (float) ft;
        return -log2f(num / den);
    }

    /**
     * Computes the spectral harmonicity at a pitch lag.
     *
     * <p>Measures the ratio of harmonic peaks to valleys at the low frequencies of the weighted power spectrum
     * {@code f2w}. The result is cached per quantized harmonic index; the cache is reset when {@code resetCache}
     * is set. For pitch periods above roughly 60 Hz the peaks and valleys are gathered with parabolic weighting
     * windows and combined into a weighted mean log peak to valley ratio; below that the harmonicity is a flat
     * {@code 0.1}.
     *
     * @implNote This implementation reproduces the four float reassociations the fast math build performs on
     * this routine, each of which shifts the reported harmonicity (voicing strength) by up to tens of units in
     * the last place where the log peak to valley ratio approaches zero:
     * <ul>
     *   <li>The reciprocal {@code 1 / harm_width} is reduced to a single multiply {@code avgLag * ((1 /
     *       invF2StepHz) / 16000)}, since {@code harm_width = (16000 / avgLag) * invF2StepHz}; the precomputed
     *       constant rounds differently from a runtime divide.</li>
     *   <li>Each weight's {@code tmp - tmp * tmp} is factored to {@code t * (1 - t)}.</li>
     *   <li>The three tap peak and valley folds are reassociated to {@code (p2 + p0) + 10 * p1} and
     *       {@code ((p1 + p1) + 5 * p0) + 5 * p2}, with the middle valley weight of two emitted as
     *       {@code p1 + p1} rather than {@code 2 * p1}.</li>
     *   <li>The per harmonic log peak to valley ratio uses the verbatim {@link #logf}, not a rounded double
     *       precision logarithm.</li>
     * </ul>
     * The peak and valley energy numerator and denominator and the final harmonicity dot product and sum use the
     * same packed SSE reductions as the pitch correlations ({@link #dotProd}, {@link #sumVec}).
     *
     * @param avgLag     the lag whose harmonicity is measured
     * @param f2w        the frequency weighted power spectrum, {@value #F_LEN} bins
     * @param cache      the per harmonic index result cache
     * @param resetCache whether to clear the cache before this query
     * @return the spectral harmonicity, roughly in {@code [-1.5, 1.5]}
     */
    private static float spectralHarmonicity(float avgLag, float[] f2w, float[] cache, boolean resetCache) {
        if (resetCache) {
            for (var i = 0; i < cache.length; i++) {
                cache[i] = HARMONICITY_UNDEF;
            }
        }
        var invF2StepHz = 2 * (F_LEN - 1) / 16000.0f;
        var harmHz = 16000 / avgLag;
        var harmIx = (int) rintf(harmHz * 2 * invF2StepHz);
        if (cache[harmIx] > HARMONICITY_UNDEF) {
            return cache[harmIx];
        }
        var harmWidth = harmHz * invF2StepHz;
        var harmStrength = 0.1f;
        if (harmWidth > 1.97f) {
            var peakValleyMags = new float[2 * NUM_HARMS + 1];
            var weights = new float[20];
            for (var numHarm = 0; numHarm <= NUM_HARMS * 2; numHarm++) {
                var ixStart = 0.5f * numHarm * harmWidth;
                var ixEnd = ixStart + harmWidth;
                var idxStart = (int) Math.ceil(ixStart);
                var idxEnd = (int) Math.floor(ixEnd);
                var weightsLen = idxEnd - idxStart + 1;
                var invHarmWidth = avgLag * ((1.0f / invF2StepHz) / 16000.0f);
                for (var i = 0; i < weightsLen; i++) {
                    var t = (idxStart - ixStart + i) * invHarmWidth;
                    var tmp = t * (1.0f - t);
                    weights[i] = tmp * tmp;
                }
                var peakValleyNrg = dotProd(f2w, idxStart, weights, 0, weightsLen) / sumVec(weights, weightsLen);
                peakValleyMags[numHarm] = (float) Math.sqrt(peakValleyNrg + 1e-30f);
            }
            var magWeights = new float[NUM_HARMS];
            var magRatiosLog = new float[NUM_HARMS];
            for (var numHarm = 0; numHarm < NUM_HARMS; numHarm++) {
                var p0 = peakValleyMags[2 * numHarm];
                var p1 = peakValleyMags[2 * numHarm + 1];
                var p2 = peakValleyMags[2 * numHarm + 2];
                var magPeak = (p2 + p0) + MAG_PEAK_WEIGHTS[1] * p1;
                var magValley = ((p1 + p1) + MAG_VALLEY_WEIGHTS[0] * p0) + MAG_VALLEY_WEIGHTS[2] * p2;
                magRatiosLog[numHarm] = logf(magPeak / magValley);
                magWeights[numHarm] = (float) Math.sqrt(magPeak + magValley + 1e-30f);
            }
            harmStrength = dotProd(magWeights, 0, magRatiosLog, 0, NUM_HARMS) / sumVec(magWeights, NUM_HARMS);
        }
        cache[harmIx] = harmStrength;
        return harmStrength;
    }

    /**
     * Computes the energy of a window.
     *
     * <p>The sum of squares is built with fast math and SSE2, vectorizing into a four lane packed accumulator
     * over four consecutive samples per step, reduced horizontally as {@code (lane0 + lane2) + (lane1 + lane3)}
     * and finished with a left to right scalar tail for the remaining {@code n & 3} samples, exactly as
     * {@link #dotProd4Wide} does; a plain scalar sum rounds differently and flips a survivor through the
     * correlation tournament.
     *
     * @param x    the buffer
     * @param off  the offset of the first sample
     * @param n    the window length
     * @return the float window energy
     */
    private static float nrg(float[] x, int off, int n) {
        return dotProd4Wide(x, off, x, off, n);
    }

    /**
     * Applies the pitch preemphasis high pass, the {@link #HP_B}{@code /}{@link #HP_A} pole zero filter at the
     * head of {@link #estimate}.
     *
     * <p>Runs the monic moving average difference {@code y[i] = x[i] - x[i - 1]} (exact, reused from the shared
     * {@link Filters#ma1}) into the recursive {@link #hpAr1} pole. This is a dedicated encoder copy rather than
     * the shared {@link Filters#arma1} because the pitch filter is built with fast math, and the divergent fast
     * math reassociation lives in the auto regressive pole; {@link Filters#ar1} keeps the literal source order
     * for the decoder, so the pitch path needs its own fast math faithful pole. The moving average numerator has
     * only two taps per output and reassociates to itself, so it is shared unchanged.
     *
     * @param x      the input buffer, read from index zero
     * @param n      the number of samples to filter
     * @param state  the two element filter memory, element zero the moving average history and element one the
     *               auto regressive history, both updated to the last processed sample
     * @param y      the output buffer
     * @param yOff   the offset of the first output sample in {@code y}
     */
    private static void hpArma1(float[] x, int n, float[] state, float[] y, int yOff) {
        Filters.ma1(x, 0, n, HP_B, state, 0, y, yOff);
        hpAr1(y, yOff, n, -HP_A[1], state, 1, y, yOff);
    }

    /**
     * Applies the first order auto regressive pole of the pitch high pass with a fast math reassociation.
     *
     * <p>Computes the leaky recursion {@code y[k] = x[k] + ar1 * y[k - 1]} over {@code n} samples, threading the
     * previous output through {@code state[stateOff]}. The body unrolls five outputs at a time and expands each
     * in terms of the five input taps and the carried output, then reassociates each expansion. The per output
     * add trees are:
     * <ul>
     *   <li>{@code y[k]   = x0 + ar1 * yt}</li>
     *   <li>{@code y[k+1] = (x1 + ar1_2 * yt) + ar1 * x0}</li>
     *   <li>{@code y[k+2] = (x2 + ar1_3 * yt) + (ar1 * x1 + ar1_2 * x0)}</li>
     *   <li>{@code y[k+3] = ((x3 + ar1_4 * yt) + ar1_3 * x0) + (ar1 * x2 + ar1_2 * x1)}</li>
     *   <li>{@code y[k+4] = ((x4 + ar1_5 * yt) + ar1_3 * x1) + ((ar1 * x3 + ar1_2 * x2) + ar1_4 * x0)}</li>
     * </ul>
     * where {@code ar1_2 .. ar1_5} are the second through fifth powers of {@code ar1} formed by repeated
     * multiplication, and the trailing {@code n % 5} samples run the plain scalar recursion. This is a fast math
     * copy of {@link Filters#ar1} private to {@link OpenLoopPitch}: the shared filter writes these expansions in
     * literal source order for the decoder, which is energy matched rather than bit identical, so it differs in
     * the last float bit. The pitch correlation built from this filtered signal drives the survivor tournament,
     * so the difference propagates to the reported pitch correlation and on to the voiced decision and bitrate
     * allocation.
     *
     * @implNote This implementation reproduces the packed reassociation the fast math build emits for the five
     * wide unrolled body, so the filtered signal matches the shipped encoder bit for bit.
     *
     * @param x        the input buffer
     * @param xOff     the offset of the first input sample in {@code x}
     * @param n        the number of samples to filter
     * @param ar1      the pole coefficient {@code -coef[1]}
     * @param state    the filter memory vector
     * @param stateOff the offset of the carried output state element in {@code state}
     * @param y        the output buffer
     * @param yOff     the offset of the first output sample in {@code y}
     */
    private static void hpAr1(float[] x, int xOff, int n, float ar1, float[] state, int stateOff,
                              float[] y, int yOff) {
        var ar12 = ar1 * ar1;
        var ar13 = ar1 * ar12;
        var ar14 = ar1 * ar13;
        var ar15 = ar1 * ar14;
        var yt = state[stateOff];
        var k = 0;
        for (; k < n - 4; k += 5) {
            var x0 = x[xOff + k];
            var x1 = x[xOff + k + 1];
            var x2 = x[xOff + k + 2];
            var x3 = x[xOff + k + 3];
            var x4 = x[xOff + k + 4];
            y[yOff + k + 4] = ((x4 + ar15 * yt) + ar13 * x1) + ((ar1 * x3 + ar12 * x2) + ar14 * x0);
            y[yOff + k] = x0 + ar1 * yt;
            y[yOff + k + 1] = (x1 + ar12 * yt) + ar1 * x0;
            y[yOff + k + 2] = (x2 + ar13 * yt) + (ar1 * x1 + ar12 * x0);
            y[yOff + k + 3] = ((x3 + ar14 * yt) + ar13 * x0) + (ar1 * x2 + ar12 * x1);
            yt = y[yOff + k + 4];
        }
        for (; k < n; k++) {
            yt = x[xOff + k] + yt * ar1;
            y[yOff + k] = yt;
        }
        state[stateOff] = yt;
    }

    /**
     * Computes a variable length inner product.
     *
     * <p>Reproduces the four lane packed accumulation and the {@code (lane0 + lane2) + (lane1 + lane3)}
     * horizontal reduction the fast math build emits; see {@link #dotProd4Wide}.
     *
     * @param a    the first buffer
     * @param aOff the offset into the first buffer
     * @param b    the second buffer
     * @param bOff the offset into the second buffer
     * @param n    the length
     * @return the float inner product
     */
    private static float dotProd(float[] a, int aOff, float[] b, int bOff, int n) {
        return dotProd4Wide(a, aOff, b, bOff, n);
    }

    /**
     * Computes an inner product with the four lane packed reduction shared by the energy and correlation dot
     * products.
     *
     * <p>Four consecutive products accumulate into four lanes, then the lanes reduce horizontally as
     * {@code (lane0 + lane2) + (lane1 + lane3)}, and the remaining {@code n & 3} products are added left to right
     * scalar. The 20 tap and 40 tap pitch correlations have no tail; the harmonicity correlation does.
     *
     * @param a    the first buffer
     * @param aOff the offset into the first buffer
     * @param b    the second buffer
     * @param bOff the offset into the second buffer
     * @param n    the length
     * @return the float inner product
     */
    private static float dotProd4Wide(float[] a, int aOff, float[] b, int bOff, int n) {
        var l0 = 0.0f;
        var l1 = 0.0f;
        var l2 = 0.0f;
        var l3 = 0.0f;
        var n4 = n & ~3;
        for (var i = 0; i < n4; i += 4) {
            l0 += a[aOff + i] * b[bOff + i];
            l1 += a[aOff + i + 1] * b[bOff + i + 1];
            l2 += a[aOff + i + 2] * b[bOff + i + 2];
            l3 += a[aOff + i + 3] * b[bOff + i + 3];
        }
        var sum = (l0 + l2) + (l1 + l3);
        for (var i = n4; i < n; i++) {
            sum += a[aOff + i] * b[bOff + i];
        }
        return sum;
    }

    /**
     * Computes a 20 tap inner product with the fully unrolled reduction tree.
     *
     * <p>The fixed length 20 tap product unrolls to five four lane products {@code p0 .. p4} (the last being the
     * trailing four taps) reduced as {@code ((p0 + p1) + p4) + (p2 + p3)} into one four lane accumulator, then
     * {@code (lane0 + lane2) + (lane1 + lane3)} horizontally. This add tree differs from the running
     * {@link #dotProd4Wide} tree by one float bit and is load bearing because the stage 1 correlation it feeds
     * drives the survivor tournament.
     *
     * @param a    the first buffer
     * @param aOff the offset into the first buffer
     * @param b    the second buffer
     * @param bOff the offset into the second buffer
     * @return the float inner product over 20 taps
     */
    private static float dotProd20(float[] a, int aOff, float[] b, int bOff) {
        var p0 = vmul(a, aOff, b, bOff);
        var p1 = vmul(a, aOff + 4, b, bOff + 4);
        var p2 = vmul(a, aOff + 8, b, bOff + 8);
        var p3 = vmul(a, aOff + 12, b, bOff + 12);
        var p4 = vmul(a, aOff + 16, b, bOff + 16);
        var x = vadd(vadd(vadd(p0, p1), p4), vadd(p2, p3));
        return (x[0] + x[2]) + (x[1] + x[3]);
    }

    /**
     * Computes a 40 tap inner product with the ten group packed reduction, the input rate pitch correlation.
     *
     * <p>The fast math build fully unrolls the 40 tap product into ten four lane products {@code p0 .. p9}
     * (group {@code g} multiplies taps {@code 4g .. 4g + 3} of the two buffers) and reduces them lane wise with
     * the fixed unbalanced add tree {@code (((p3 + p4) + p7) + ((p5 + p6) + p9)) + (((p1 + p2) + p8) + p0)}, then
     * collapses the four lanes to {@code (lane0 + lane2) + (lane1 + lane3)}. This add tree differs from both
     * {@link #dotProd4Wide} (the running four lane product) and a plain scalar sum; its last float bit feeds the
     * {@code C}/{@code E} the final search reads, so the reported pitch correlation matches the shipped value
     * bit for bit.
     *
     * @param a    the first buffer
     * @param aOff the offset into the first buffer
     * @param b    the second buffer
     * @param bOff the offset into the second buffer
     * @return the float inner product over 40 taps
     */
    private static float dotProd40(float[] a, int aOff, float[] b, int bOff) {
        var p0 = vmul(a, aOff, b, bOff);
        var p1 = vmul(a, aOff + 4, b, bOff + 4);
        var p2 = vmul(a, aOff + 8, b, bOff + 8);
        var p3 = vmul(a, aOff + 12, b, bOff + 12);
        var p4 = vmul(a, aOff + 16, b, bOff + 16);
        var p5 = vmul(a, aOff + 20, b, bOff + 20);
        var p6 = vmul(a, aOff + 24, b, bOff + 24);
        var p7 = vmul(a, aOff + 28, b, bOff + 28);
        var p8 = vmul(a, aOff + 32, b, bOff + 32);
        var p9 = vmul(a, aOff + 36, b, bOff + 36);
        var left = vadd(vadd(vadd(p3, p4), p7), vadd(vadd(p5, p6), p9));
        var right = vadd(vadd(vadd(p1, p2), p8), p0);
        var x = vadd(left, right);
        return (x[0] + x[2]) + (x[1] + x[3]);
    }

    /**
     * Computes a 40 tap self energy with the ten group packed reduction, the input rate target energy
     * {@code E2}.
     *
     * <p>The self energy use of the 40 tap product reduces to a different schedule than the cross correlation
     * {@link #dotProd40} even though the arithmetic is the same: the fast math build squares each four tap group
     * into products {@code p0 .. p9} and reduces them lane wise with the add tree
     * {@code (((p0 + p1) + p6) + p9) + (((p2 + p3) + p7) + ((p4 + p5) + p8))}, then collapses the four lanes to
     * {@code (lane0 + lane2) + (lane1 + lane3)}. Reproducing this distinct tree makes {@code E2}, and therefore
     * the normalized correlation denominator the final search reads, bit exact.
     *
     * @param a   the buffer
     * @param off the offset of the first sample
     * @return the float self energy over 40 taps
     */
    private static float dotProd40Self(float[] a, int off) {
        var p0 = vmul(a, off, a, off);
        var p1 = vmul(a, off + 4, a, off + 4);
        var p2 = vmul(a, off + 8, a, off + 8);
        var p3 = vmul(a, off + 12, a, off + 12);
        var p4 = vmul(a, off + 16, a, off + 16);
        var p5 = vmul(a, off + 20, a, off + 20);
        var p6 = vmul(a, off + 24, a, off + 24);
        var p7 = vmul(a, off + 28, a, off + 28);
        var p8 = vmul(a, off + 32, a, off + 32);
        var p9 = vmul(a, off + 36, a, off + 36);
        var left = vadd(vadd(vadd(p0, p1), p6), p9);
        var right = vadd(vadd(vadd(p2, p3), p7), vadd(vadd(p4, p5), p8));
        var x = vadd(left, right);
        return (x[0] + x[2]) + (x[1] + x[3]);
    }

    /**
     * Multiplies four consecutive sample pairs into a four lane vector.
     *
     * @param a    the first buffer
     * @param aOff the offset of the first sample in {@code a}
     * @param b    the second buffer
     * @param bOff the offset of the first sample in {@code b}
     * @return the four products as a four element vector
     */
    private static float[] vmul(float[] a, int aOff, float[] b, int bOff) {
        return new float[]{a[aOff] * b[bOff], a[aOff + 1] * b[bOff + 1],
                a[aOff + 2] * b[bOff + 2], a[aOff + 3] * b[bOff + 3]};
    }

    /**
     * Adds two four lane vectors.
     *
     * @param x the first vector
     * @param y the second vector
     * @return the lane wise sum as a four element vector
     */
    private static float[] vadd(float[] x, float[] y) {
        return new float[]{x[0] + y[0], x[1] + y[1], x[2] + y[2], x[3] + y[3]};
    }

    /**
     * Sums a window with the four lane packed reduction.
     *
     * <p>The sum is built with fast math and SSE2, so it does not stay a left to right scan: the first element
     * seeds a scalar accumulator, the remaining {@code len - 1} elements accumulate into four packed lanes
     * ({@code lane_j} running over element indices {@code 1 + 4c + j}), the lanes reduce horizontally as
     * {@code (lane0 + lane2) + (lane1 + lane3)}, that reduction is added to the seed, and the trailing
     * {@code (len - 1) & 3} elements are added left to right scalar. A plain left to right sum rounds
     * differently from {@code len == 13} up and shifts the spectral harmonicity peak to valley normalization,
     * which drives the harmonicity (voicing strength) the final pitch search reports.
     *
     * @param x   the buffer
     * @param len the window length
     * @return the float window sum
     */
    private static float sumVec(float[] x, int len) {
        var seed = x[0];
        var m = len - 1;
        var m4 = m & ~3;
        var l0 = 0.0f;
        var l1 = 0.0f;
        var l2 = 0.0f;
        var l3 = 0.0f;
        for (var i = 0; i < m4; i += 4) {
            l0 += x[1 + i];
            l1 += x[1 + i + 1];
            l2 += x[1 + i + 2];
            l3 += x[1 + i + 3];
        }
        var sum = seed + ((l0 + l2) + (l1 + l3));
        for (var i = 1 + m4; i < len; i++) {
            sum += x[i];
        }
        return sum;
    }

    /**
     * Takes the element wise square root of a window in place.
     *
     * @param x   the buffer
     * @param len the window length
     */
    private static void sqrtVec(float[] x, int len) {
        for (var i = 0; i < len; i++) {
            x[i] = (float) Math.sqrt(x[i]);
        }
    }

    /**
     * Returns the maximum of a window.
     *
     * @param x   the buffer
     * @param off the offset of the first sample
     * @param len the window length
     * @return the float maximum
     */
    private static float maximum(float[] x, int off, int len) {
        var xMax = x[off];
        for (var i = 1; i < len; i++) {
            if (x[off + i] > xMax) {
                xMax = x[off + i];
            }
        }
        return xMax;
    }

    /**
     * Returns the index of the maximum of a window through the pairwise tournament.
     *
     * <p>The values are folded in halves, the winning leaf is found, and the index is recovered by walking the
     * tournament back down, taking the larger sibling at each level and resolving the final odd sibling. The
     * comparison is strict greater than so ties keep the lower index.
     *
     * @param x     the buffer (read from index zero)
     * @param xLen  the window length
     * @return the index of the maximum
     */
    private static int getMaxi(float[] x, int xLen) {
        var buf = new float[160];
        var numHalves = 0;
        var len = (xLen + 1) >> 1;
        for (var n = 0; n < xLen - len; n++) {
            buf[n] = Math.max(x[n], x[n + len]);
        }
        buf[xLen - len] = x[xLen - len];
        var bufPtr = 0;
        while ((len & 1) == 0) {
            bufPtr += len;
            len >>= 1;
            for (var n = 0; n < len; n++) {
                buf[bufPtr + n] = Math.max(buf[bufPtr - 2 * len + n], buf[bufPtr - len + n]);
            }
            numHalves++;
        }
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
            bufPtr -= 2 * len;
            if (buf[bufPtr + i] < buf[bufPtr + i + len]) {
                i += len;
            }
            len <<= 1;
        }
        if (i + len < xLen && x[i] < x[i + len]) {
            i += len;
        }
        return i;
    }

    /**
     * Returns the indices of the {@code k} highest values through the leaf flag tournament.
     *
     * <p>The values are folded in halves once; each of the {@code k} extractions finds the current winning leaf,
     * recovers its index, records the leaf flag toggle so a leaf's two children are reported in order, then
     * removes the winner (by writing {@code -FLT_MAX}) and propagates the affected path back up the tournament.
     * The strict greater than comparison keeps the lower index on ties.
     *
     * @param x    the values
     * @param xLen the number of values
     * @param k    the number of indices to extract
     * @return the {@code k} indices of the highest values, in extraction order
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
            var xtmp = -Float.MAX_VALUE;
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
     * Reciprocal table for the {@link #logf} argument reduction, the first lookup table.
     *
     * <p>Entry {@code idx} is the {@code float} bit pattern of the reciprocal that scales the reduced mantissa
     * argument of {@link #logf}, recovered with {@link Float#intBitsToFloat(int)}. The bit patterns are
     * reproduced verbatim from the platform runtime.
     */
    private static final int[] LOGF_RECIP = {
            0x40000000, 0x3ffe03f8, 0x3ffc0fc1, 0x3ffa232d, 0x3ff83e10, 0x3ff6603e,
            0x3ff4898d, 0x3ff2b9d6, 0x3ff0f0f1, 0x3fef2eb7, 0x3fed7304, 0x3febbdb3,
            0x3fea0ea1, 0x3fe865ac, 0x3fe6c2b4, 0x3fe52598, 0x3fe38e39, 0x3fe1fc78,
            0x3fe07038, 0x3fdee95c, 0x3fdd67c9, 0x3fdbeb62, 0x3fda740e, 0x3fd901b2,
            0x3fd79436, 0x3fd62b81, 0x3fd4c77b, 0x3fd3680d, 0x3fd20d21, 0x3fd0b6a0,
            0x3fcf6475, 0x3fce168a, 0x3fcccccd, 0x3fcb8728, 0x3fca4588, 0x3fc907da,
            0x3fc7ce0c, 0x3fc6980c, 0x3fc565c8, 0x3fc43730, 0x3fc30c31, 0x3fc1e4bc,
            0x3fc0c0c1, 0x3fbfa030, 0x3fbe82fa, 0x3fbd6910, 0x3fbc5264, 0x3fbb3ee7,
            0x3fba2e8c, 0x3fb92144, 0x3fb81703, 0x3fb70fbb, 0x3fb60b61, 0x3fb509e7,
            0x3fb40b41, 0x3fb30f63, 0x3fb21643, 0x3fb11fd4, 0x3fb02c0b, 0x3faf3ade,
            0x3fae4c41, 0x3fad602b, 0x3fac7692, 0x3fab8f6a, 0x3faaaaab, 0x3fa9c84a,
            0x3fa8e83f, 0x3fa80a81, 0x3fa72f05, 0x3fa655c4, 0x3fa57eb5, 0x3fa4a9cf,
            0x3fa3d70a, 0x3fa3065e, 0x3fa237c3, 0x3fa16b31, 0x3fa0a0a1, 0x3f9fd80a,
            0x3f9f1166, 0x3f9e4cad, 0x3f9d89d9, 0x3f9cc8e1, 0x3f9c09c1, 0x3f9b4c70,
            0x3f9a90e8, 0x3f99d723, 0x3f991f1a, 0x3f9868c8, 0x3f97b426, 0x3f97012e,
            0x3f964fda, 0x3f95a025, 0x3f94f209, 0x3f944581, 0x3f939a86, 0x3f92f114,
            0x3f924925, 0x3f91a2b4, 0x3f90fdbc, 0x3f905a38, 0x3f8fb824, 0x3f8f177a,
            0x3f8e7835, 0x3f8dda52, 0x3f8d3dcb, 0x3f8ca29c, 0x3f8c08c1, 0x3f8b7034,
            0x3f8ad8f3, 0x3f8a42f8, 0x3f89ae41, 0x3f891ac7, 0x3f888889, 0x3f87f781,
            0x3f8767ab, 0x3f86d905, 0x3f864b8a, 0x3f85bf37, 0x3f853408, 0x3f84a9fa,
            0x3f842108, 0x3f839930, 0x3f83126f, 0x3f828cc0, 0x3f820821, 0x3f81848e,
            0x3f810204, 0x3f808081, 0x3f800000,
    };

    /**
     * High part of the {@link #logf} reduced interval logarithm, the second lookup table.
     *
     * <p>Entry {@code idx} is the {@code float} bit pattern of the high part of the logarithm of the reduction
     * point of {@link #logf}, recovered with {@link Float#intBitsToFloat(int)} and accumulated with the high
     * part of {@code e * ln2}. The bit patterns are reproduced verbatim from the platform runtime.
     */
    private static final int[] LOGF_HI = {
            0x00000000, 0x3bff0000, 0x3c7e0000, 0x3cbdc000, 0x3cfc1000, 0x3d1cf000,
            0x3d3ba000, 0x3d5a1000, 0x3d785000, 0x3d8b2000, 0x3d9a0000, 0x3da8d000,
            0x3db78000, 0x3dc61000, 0x3dd49000, 0x3de2f000, 0x3df13000, 0x3dff6000,
            0x3e06b000, 0x3e0db000, 0x3e14a000, 0x3e1b8000, 0x3e226000, 0x3e293000,
            0x3e2ff000, 0x3e36b000, 0x3e3d5000, 0x3e43f000, 0x3e4a9000, 0x3e511000,
            0x3e579000, 0x3e5e1000, 0x3e647000, 0x3e6ae000, 0x3e713000, 0x3e778000,
            0x3e7dc000, 0x3e820000, 0x3e851000, 0x3e882000, 0x3e8b3000, 0x3e8e4000,
            0x3e914000, 0x3e944000, 0x3e974000, 0x3e9a3000, 0x3e9d3000, 0x3ea02000,
            0x3ea30000, 0x3ea5f000, 0x3ea8d000, 0x3eabb000, 0x3eae8000, 0x3eb16000,
            0x3eb43000, 0x3eb70000, 0x3eb9c000, 0x3ebc9000, 0x3ebf5000, 0x3ec21000,
            0x3ec4d000, 0x3ec78000, 0x3eca3000, 0x3ecce000, 0x3ecf9000, 0x3ed24000,
            0x3ed4e000, 0x3ed78000, 0x3eda2000, 0x3edcc000, 0x3edf5000, 0x3ee1e000,
            0x3ee47000, 0x3ee70000, 0x3ee99000, 0x3eec1000, 0x3eeea000, 0x3ef12000,
            0x3ef3a000, 0x3ef61000, 0x3ef89000, 0x3efb0000, 0x3efd7000, 0x3effe000,
            0x3f012000, 0x3f025000, 0x3f039000, 0x3f04c000, 0x3f05f000, 0x3f072000,
            0x3f084000, 0x3f097000, 0x3f0aa000, 0x3f0bc000, 0x3f0cf000, 0x3f0e1000,
            0x3f0f4000, 0x3f106000, 0x3f118000, 0x3f12a000, 0x3f13c000, 0x3f14e000,
            0x3f160000, 0x3f172000, 0x3f183000, 0x3f195000, 0x3f1a7000, 0x3f1b8000,
            0x3f1c9000, 0x3f1db000, 0x3f1ec000, 0x3f1fd000, 0x3f20e000, 0x3f21f000,
            0x3f230000, 0x3f241000, 0x3f252000, 0x3f263000, 0x3f273000, 0x3f284000,
            0x3f295000, 0x3f2a5000, 0x3f2b5000, 0x3f2c6000, 0x3f2d6000, 0x3f2e6000,
            0x3f2f7000, 0x3f307000, 0x3f317000,
    };

    /**
     * Low part of the {@link #logf} reduced interval logarithm, the third lookup table.
     *
     * <p>Entry {@code idx} is the {@code float} bit pattern of the low part of the logarithm of the reduction
     * point of {@link #logf}, recovered with {@link Float#intBitsToFloat(int)} and accumulated with the low part
     * of {@code e * ln2}. The bit patterns are reproduced verbatim from the platform runtime.
     */
    private static final int[] LOGF_LO = {
            0x00000000, 0x3429ac41, 0x35a8b0fc, 0x368d83ea, 0x361b0e78, 0x3687b9fe,
            0x3631ec65, 0x36dd7119, 0x35c30045, 0x379b7751, 0x37ebcb0d, 0x37839f83,
            0x37528ae5, 0x37a2eb18, 0x36da7495, 0x36a91eb7, 0x3783b715, 0x371131db,
            0x383f3e68, 0x38156a97, 0x38297c0f, 0x387e100f, 0x3815b665, 0x37e5e3a1,
            0x38183853, 0x35fe719d, 0x38448108, 0x38503290, 0x373539e8, 0x385e0ff1,
            0x3864a740, 0x3786742d, 0x387be3cd, 0x3685ad3e, 0x3803b715, 0x37adcbdc,
            0x380c36af, 0x371652d3, 0x38927139, 0x38c5fcd7, 0x38ae55d5, 0x3818c169,
            0x38a0fde7, 0x38ad09ef, 0x3862bae1, 0x38eecd4c, 0x3798aad2, 0x37421a1a,
            0x38c5e10e, 0x37bf2aee, 0x382d872d, 0x37ee2e8a, 0x38dedfac, 0x3802f2b9,
            0x38481e9b, 0x380eaa2b, 0x38ebfb5d, 0x38255fdd, 0x38783b82, 0x3851da1e,
            0x374e1b05, 0x388f439b, 0x38ca0e10, 0x38cac08b, 0x3891f65f, 0x378121cb,
            0x386c9a9a, 0x38949923, 0x38777bcc, 0x37b12d26, 0x38a6ced3, 0x38ebd3e6,
            0x38fbe3cd, 0x38d785c2, 0x387e7e00, 0x38f392c5, 0x37d40983, 0x38081a7c,
            0x3784c3ad, 0x38cce923, 0x380f5faf, 0x3891fd38, 0x38ac47bc, 0x3897042b,
            0x392952d2, 0x396fced4, 0x37f97073, 0x385e9eae, 0x3865c84a, 0x38130ba3,
            0x3979cf16, 0x3938cac9, 0x38c3d2f4, 0x39755dec, 0x38e6b467, 0x395c0fb8,
            0x383ebce0, 0x38dcd192, 0x39186bdf, 0x392de74c, 0x392f0944, 0x391bff61,
            0x38e9ed44, 0x38686dc8, 0x396b99a7, 0x39099c89, 0x37a27673, 0x390bdaa3,
            0x397069ab, 0x388449ff, 0x39013538, 0x392dc268, 0x3947f423, 0x394ff17c,
            0x3945e10e, 0x3929e8f5, 0x38f85db0, 0x38735f99, 0x396c08db, 0x3909e600,
            0x37b4996f, 0x391233cc, 0x397cead9, 0x38adb5cd, 0x3920261a, 0x3958ee36,
            0x35aa4905, 0x37cbd11e, 0x3805fdf4,
    };

    /**
     * Computes a single precision natural logarithm feeding the spectral harmonicity peak to valley ratio.
     *
     * <p>This is a verbatim port of the platform {@code logf}, whose result is the per harmonic log peak to
     * valley ratio of {@link #spectralHarmonicity}; the weighted mean of those logs is the reported harmonicity
     * (voicing strength), so the routine must match bit for bit. A rounded double precision logarithm differs by
     * up to one unit in the last place, and near a near unity peak to valley ratio (where the logarithm
     * approaches zero) that single bit deviation is amplified into the tens of bits relative error observed on
     * the reported harmonicity. The routine splits {@code x} into a mantissa table index from the top mantissa
     * bits (with the rounding bit folded in) and an exponent {@code e}; the reduced argument
     * {@code r = recip[idx] * (idx_value - mantissa)} drives the cubic {@code r + r^2 (r/3 + 1/2)}, and the
     * result reconstructs {@code log(x)} from the two split logarithm tables {@link #LOGF_HI} and
     * {@link #LOGF_LO} and the high and low parts of {@code e * ln2}. Inputs within {@code 1/16} of one take a
     * dedicated series in {@code s = (x - 1) / (x + 1)}.
     *
     * @param x the strictly positive argument
     * @return {@code logf(x)} in single precision
     */
    private static float logf(float x) {
        var bits = Float.floatToRawIntBits(x);
        if ((bits & 0x7fffffff) >= 0x7f800000 || x <= 0.0f) {
            return (float) Math.log(x);
        }
        var diff = x - 1.0f;
        var absDiff = Float.intBitsToFloat(Float.floatToRawIntBits(diff) & 0x7fffffff);
        if (absDiff < 0.0625f) {
            var s = diff / (2.0f + diff);
            var diffTimesS = diff * s;
            var twoS = s + s;
            var twoSSquared = twoS * twoS;
            var twoSCubed = twoS * twoSSquared;
            var poly = twoSSquared * 0.012500000186264515f;
            poly = poly + 0.0833333358168602f;
            poly = poly * twoSCubed;
            poly = poly - diffTimesS;
            return diff + poly;
        }
        var exponent = (float) ((bits >>> 23) - 0x7f);
        var combined = (bits & 0x7f0000) + ((bits & 0x8000) << 1);
        var idx = combined >>> 16;
        var mantissa = Float.intBitsToFloat((bits & 0x7fffff) | 0x3f000000);
        var reduced = Float.intBitsToFloat(combined | 0x3f000000) - mantissa;
        reduced = reduced * Float.intBitsToFloat(LOGF_RECIP[idx]);
        var poly = reduced * 0.3333333432674408f;
        var reducedSquared = reduced * reduced;
        poly = poly + 0.5f;
        poly = poly * reducedSquared;
        var mantissaLog = reduced + poly;
        var low = 3.194618329871446e-05f * exponent;
        low = low - mantissaLog;
        low = low + Float.intBitsToFloat(LOGF_LO[idx]);
        var high = 0.693115234375f * exponent;
        high = high + Float.intBitsToFloat(LOGF_HI[idx]);
        return high + low;
    }

    /**
     * Computes the base 2 logarithm in single precision.
     *
     * @param x the operand
     * @return the single precision base 2 logarithm of {@code x}
     */
    private static float log2f(float x) {
        return (float) (Math.log(x) / Math.log(2.0));
    }

    /**
     * Rounds to the nearest integer, ties away from zero, in single precision.
     *
     * @param x the operand
     * @return the single precision round of {@code x}
     */
    private static float rintf(float x) {
        return (float) (x < 0.0f ? Math.ceil(x - 0.5f) : Math.floor(x + 0.5f));
    }
}
