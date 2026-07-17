package com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeDecoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.PitchTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Decodes the per subframe integer pitch lags of an MLow voiced low band frame.
 *
 * <p>MLow codes the pitch lag as a coarse block segmentation plus within block deltas, not as one lag per
 * subframe. The decode proceeds in three stages, all against the {@link PitchTables} cumulative mass
 * functions:
 * <ul>
 * <li><b>Block segmentation index.</b> For the first frame of a packet (no previous lag block) the index
 * is read directly against the block segmentation index cumulative mass function. For later frames of a
 * multi frame packet it is read in two steps: a block transition symbol conditioned on the previous
 * frame's last lag block, then a windowed block segmentation index restricted to the segmentations whose
 * first block matches the transition. That index is mapped back to the reconstructed block segmentation
 * index through the permutation table, selecting one {@link PitchTables.Blockseg}.</li>
 * <li><b>First lag.</b> When the segmentation does not chain onto the previous frame's block (or there is
 * no previous frame) the first segment's lag is read as a uniform symbol over the block size and offset
 * into the chosen first block.</li>
 * <li><b>Delta lags.</b> Each remaining segment's lag is a delta against the running previous lag, read
 * against the within block delta lag cumulative mass function selected by the frame's mean quantized
 * adaptive codebook gain class, windowed by the block transition.</li>
 * </ul>
 * The output is one integer lag index per pitch subframe ({@link #PITCH_NUM_SUBFRAMES} of them).
 *
 * <p>This decoder is stateful across the frames of a packet: it carries the previous frame's last lag
 * block and lag index, which condition the block transition and delta lag decode of the next frame.
 * {@link #reset()} clears that carry and must be called whenever the lag carry is invalidated, namely at
 * the start of a packet and on any frame decoded with conditional coding disabled.
 *
 * @implNote This implementation fixes the lag block size at {@link #BLOCKSIZE} = 64 positions and windows
 * the delta lag decode over {@code blocksize + 1} symbols starting at
 * {@code deltaRangeStart + 2 * blocksize - 1}, where
 * {@code deltaRangeStart = -(prevLagidx - prevLagblk * blocksize) + (blk - prevLagblk) * blocksize}. The
 * permutation table that maps the encoded index back to the reconstructed segmentation is inverted by a
 * linear scan; the table is short and the inversion runs once per frame. The block transition path is only
 * reachable for the 20 ms multi frame packets of the shipped 16 kHz, 60 ms configuration, whose packet
 * carries three frames.
 */
public final class PitchLagDecoder {
    /**
     * The logger for {@link PitchLagDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(PitchLagDecoder.class);

    /**
     * The within block lag block size in lag indices, {@code 2 * 16 * 2} = 64.
     *
     * <p>A lag index runs over one block of this many positions; the uniform first lag symbol and the
     * delta lag window are both this size.
     */
    private static final int BLOCKSIZE = 64;

    /**
     * The number of pitch subframes a 20 ms frame's lags span.
     */
    private static final int PITCH_NUM_SUBFRAMES = 8;

    /**
     * The shared 20 ms pitch decode tables for eight pitch subframes.
     */
    private final PitchTables.PitchData data;

    /**
     * The previous frame's last lag block; {@code -1} when there is no previous frame in this packet or
     * conditional coding was disabled.
     */
    private int prevLagblk;

    /**
     * The previous frame's last lag index; {@code -1} when there is no previous frame in this packet or
     * conditional coding was disabled.
     */
    private int prevLagidx;

    /**
     * The block segmentation index decoded by the most recent {@link #decodeLags} call; {@code -1} before
     * any lag decode.
     *
     * <p>The lags alone do not determine the segmentation (two segments can share a coarse block yet carry
     * distinct lags), so this records the decoded segmentation index for callers that need to reconstruct
     * the encode side segmentation index, such as an encoder round trip cross check.
     */
    private int lastBlocksegsIx = -1;

    /**
     * Constructs a pitch lag decoder over the 20 ms decode tables and clears its cross frame carry.
     *
     * <p>The decoder starts in the reset state ({@code prevLagblk == prevLagidx == -1}), ready to decode
     * the first frame of a packet.
     */
    public PitchLagDecoder() {
        this.data = PitchTables.data20();
        reset();
    }

    /**
     * Clears the cross frame carry, setting {@code prevLagblk} and {@code prevLagidx} back to {@code -1}.
     *
     * <p>Must be called whenever the lag carry is invalidated: at the start of a packet and on any frame
     * decoded with conditional coding disabled. After a reset the next decoded frame takes the no previous
     * block path (direct block segmentation index, uniform first lag).
     */
    public void reset() {
        this.prevLagblk = -1;
        this.prevLagidx = -1;
        this.lastBlocksegsIx = -1;
    }

    /**
     * Returns the block segmentation index decoded by the most recent {@link #decodeLags} call.
     *
     * <p>Together with the decoded lags this reconstructs the encode side segmentation index a parameter
     * serializer must emit again. It is {@code -1} before any lag decode and is not part of the synthesis
     * contract; the synthesis path needs only the lags themselves.
     *
     * @return the most recently decoded block segmentation index, or {@code -1} before any decode
     */
    public int lastBlocksegsIx() {
        return lastBlocksegsIx;
    }

    /**
     * Decodes the per subframe integer pitch lags of one voiced low band frame.
     *
     * <p>Reads the block segmentation index (conditioned on the cross frame carry), then the first lag and
     * the delta lags, and expands them into one lag per pitch subframe. The {@code meanAcbgQ14} argument
     * is the frame's mean quantized adaptive codebook gain in Q14, computed by the gain decode that runs
     * earlier in the frame; it selects the delta lag cumulative mass function class. The cross frame carry
     * is updated so the next call can decode a multi frame continuation.
     *
     * @param decoder     the range decoder positioned at the pitch lag parameters
     * @param meanAcbgQ14 the frame's mean quantized adaptive codebook gain in Q14
     * @return the {@link #PITCH_NUM_SUBFRAMES} integer lag indices
     */
    public int[] decodeLags(MlowRangeDecoder decoder, int meanAcbgQ14) {
        var ixJulia = decodeBlocksegIndex(decoder);
        var blocksegsIx = blocksegIxFromJulia(ixJulia);
        lastBlocksegsIx = blocksegsIx;
        var seg = data.blocksegs()[blocksegsIx];

        var laginds = new int[PITCH_NUM_SUBFRAMES];
        var blk = seg.blocks()[0];
        var deltaBlk = blk - prevLagblk;
        var startSeg = 0;
        var lagindsIx = 0;
        if (!(prevLagblk > -1 && deltaBlk >= -1 && deltaBlk <= 2)) {
            var lagind = MlowEntropyWrapper.decodeUniform(decoder, BLOCKSIZE) + blk * BLOCKSIZE;
            for (var j = 0; j < seg.seglens()[0]; j++) {
                laginds[lagindsIx++] = lagind;
            }
            prevLagblk = blk;
            prevLagidx = lagind;
            startSeg = 1;
        }

        var mode = selectMode(meanAcbgQ14);
        var deltaLagCmf = data.deltaLagCmfs()[mode];
        var blocks = seg.blocks();
        var seglens = seg.seglens();
        for (var k = startSeg; k < seg.nblocks(); k++) {
            blk = blocks[k];
            deltaBlk = blk - prevLagblk;
            var prevLagidxMod = prevLagidx - prevLagblk * BLOCKSIZE;
            var deltaRangeStart = -prevLagidxMod + deltaBlk * BLOCKSIZE;
            var windowStart = deltaRangeStart + 2 * BLOCKSIZE - 1;
            var idx = MlowEntropyWrapper.decodeUpdate(decoder, deltaLagCmf, windowStart, BLOCKSIZE + 1);
            var lagind = idx + deltaRangeStart + prevLagidx;
            for (var j = 0; j < seglens[k]; j++) {
                laginds[lagindsIx++] = lagind;
            }
            prevLagblk = blk;
            prevLagidx = lagind;
        }
        return laginds;
    }

    /**
     * Decodes the one based block segmentation index at the top of the lag decode.
     *
     * <p>For the no previous block case the index is the symbol decoded against the block segmentation
     * index cumulative mass function plus one. For a multi frame continuation it is decoded in two steps:
     * a block transition symbol conditioned on the previous frame's last lag block selects the current
     * first block, then a windowed block segmentation index restricted to that block's first block range
     * is read and offset back into the full index space.
     *
     * @param decoder the range decoder positioned at the block segmentation index symbol or symbols
     * @return the one based block segmentation index
     */
    private int decodeBlocksegIndex(MlowRangeDecoder decoder) {
        if (prevLagblk < 0) {
            return MlowEntropyWrapper.decodeUpdate(decoder, data.blocksegIdxCmf()) + 1;
        }
        var block0 = MlowEntropyWrapper.decodeUpdate(decoder, data.blockTransitionCmf()[prevLagblk]);
        var range = data.firstBlockRange();
        var startIx = range[block0 * 2] & 0xFF;
        var rangeEnd = range[block0 * 2 + 1] & 0xFF;
        var cmfLen = rangeEnd - startIx + 2;
        return MlowEntropyWrapper.decodeUpdate(decoder, data.blocksegIdxCmf(), startIx, cmfLen) + startIx + 1;
    }

    /**
     * Inverts the block segmentation index permutation by a linear scan.
     *
     * <p>Finds the reconstructed segmentation index whose one based encoded value equals {@code ixJulia}.
     * The permutation table is read as unsigned bytes.
     *
     * @param ixJulia the one based block segmentation index
     * @return the reconstructed block segmentation index in {@code [0, numBlocksegs)}
     * @throws IllegalStateException if no index maps to {@code ixJulia}
     */
    private int blocksegIxFromJulia(int ixJulia) {
        var map = data.blocksegs2idx();
        for (var i = 0; i < map.length; i++) {
            if ((map[i] & 0xFF) == ixJulia) {
                return i;
            }
        }
        if (Log.ERROR) {
            LOGGER.log(Level.ERROR, "mlow pitch lag decode: no block-segmentation index maps to {0}", ixJulia);
        }
        throw new IllegalStateException("no block-segmentation index maps to native index " + ixJulia);
    }

    /**
     * Selects the within block delta lag cumulative mass function class from the mean quantized adaptive
     * codebook gain.
     *
     * <p>Returns 0, 1, or 2 by comparing {@code meanAcbgQ14} against the two 20 ms thresholds: class 0
     * below the first threshold, class 1 below the second, class 2 otherwise. Higher gain (more strongly
     * voiced) maps to a higher class.
     *
     * @param meanAcbgQ14 the frame's mean quantized adaptive codebook gain in Q14
     * @return the delta lag cumulative mass function class index in {@code [0, 2]}
     */
    private static int selectMode(int meanAcbgQ14) {
        var thr = PitchTables.acbgainThr20Q14();
        if (meanAcbgQ14 < thr[0]) {
            return 0;
        }
        if (meanAcbgQ14 < thr[1]) {
            return 1;
        }
        return 2;
    }
}
