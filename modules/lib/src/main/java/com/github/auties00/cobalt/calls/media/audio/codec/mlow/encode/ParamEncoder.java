package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeEncoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.LsfDequantizer;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Codebook;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Stage1;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.LsfCodebooks.Stage2;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.MiscTables;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.NrgResTables;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.PitchTables;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.PitchTables.Blockseg;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.PitchTables.PitchData;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.PulseTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Serializes the low band parameter set of one internal frame for the MLow speech codec, the exact inverse
 * of {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.param.ParamDecoder}.
 *
 * <p>Per internal frame this range encodes the full low band parameter set in exactly the order, and with
 * exactly the conditional coding state, that the decoder reads it back:
 * <ol>
 * <li>the voicing flag (only when the frame is coded as active voice), against the voicing CMF selected by the
 * frame number and the previous frame's voicing;</li>
 * <li>the stage 1 and per coefficient stage 2 LSF indices, conditional on the previous frame when the
 * conditional coding flag survives;</li>
 * <li>the LSF interpolation index, only for a multi subframe frame coded as active voice;</li>
 * <li>the fixed codebook excitation pulses ({@link PulseEncoder}), unless the frame is a SID frame in which
 * case the pulse counts are zero;</li>
 * <li>then either the voiced gain set (per subframe adaptive codebook gain index and, for subframes with
 * pulses, the fixed codebook gain index) followed by the pitch lags, or the unvoiced residual energy set
 * (frame energy index, shape index, per subframe fixed codebook gain offsets).</li>
 * </ol>
 *
 * <p>Write order is load bearing: the range coder is a single serial stream, so emitting any symbol out of
 * order, or against the wrong CMF, desynchronizes every later symbol in the frame and every later frame in the
 * packet. The conditional coding flag is threaded exactly as the decoder expects: it enters each frame from
 * the caller ({@code false} for the first frame of a packet, {@code true} for later frames whose voicing
 * matches the previous frame), and when it is {@code false} the per frame predictors (previous adaptive
 * codebook index, previous fixed codebook index, previous residual energy index, and the pitch lag carry) are
 * reset.
 *
 * <p>This serializer is stateful across the frames of a packet and across the packets of one continuous encode
 * session. Construct one per logical stream and feed it every frame in order. {@link #reset()} returns it to
 * the freshly constructed state.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono low band path. The high band parameter encode is deliberately not
 * invoked here; at 16 kHz the high band is absent. This type is internal to the MLow encode implementation and
 * is intentionally not exported from the module.
 *
 * @implNote This implementation encodes the voicing, LSF, interpolation, pulse, gain, and pitch lag symbols
 * against the same CMF tables the decode leaf decoders read. The stage 1 and stage 2 LSF CMFs come from
 * {@link LsfCodebooks} (the decode ready {@link Stage1#cmf()}, {@link Stage1#cmfCond()}, and
 * {@link Stage2#cmf()}); the pulse, residual energy, and pitch lag CMFs come from the same shared tables. The
 * voiced gain encode selects the conditional CMF row by {@code prevAcbIdx + 1}, accumulates the mean quantized
 * gain in fixed point, and chooses the absolute versus delta fixed codebook gain branch by the presence of a
 * previous index; the mean gain divided by the subframe count selects the pitch lag delta mode exactly as the
 * decoder recomputes it. The lag encode applies the block transition windowing for multi frame packets and the
 * uniform first lag emission, with every CMF span taken at the same relative offset the decoder resolves
 * against.
 */
public final class ParamEncoder {
    /**
     * Number of taps per adaptive codebook gain codebook vector.
     */
    private static final int ACBG_M = MiscTables.ACBG_M;

    /**
     * Number of adaptive codebook gain symbols; the conditional CMF row has one more entry than this.
     */
    private static final int ACBG_N = MiscTables.ACBG_N;

    /**
     * Number of symbols in the voiced fixed codebook gain absolute CMF.
     */
    private static final int FCBG_V_N = 34;

    /**
     * Linear prediction order of the MLow short term filter; the number of stage 2 LSF coefficients encoded
     * per frame.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Number of coarse pitch lag blocks; the total span of a block transition CMF row.
     */
    private static final int PITCH_NUM_BLOCKS = 9;

    /**
     * The within block lag block size in lag indices, fixed at 64.
     */
    private static final int BLOCKSIZE = 64;

    /**
     * The logger for {@link ParamEncoder}.
     */
    private static final System.Logger LOGGER = Log.get(ParamEncoder.class);

    /**
     * The shared prebuilt pulse coding CMF families.
     */
    private final PulseTables.Tables pulseTables;

    /**
     * The shared decode ready two stage LSF codebook, supplying the stage 1 and stage 2 LSF CMFs.
     */
    private final Codebook lsfCodebook;

    /**
     * The shared 20 ms pitch encode data.
     */
    private final PitchData pitchData;

    /**
     * The previous adaptive codebook gain index; {@code -1} when conditional coding was reset.
     */
    private int prevAcbIdx;

    /**
     * The previous fixed codebook gain index; {@code -1} when conditional coding was reset, which selects the
     * absolute fixed codebook gain encode.
     */
    private int prevFcbIdx;

    /**
     * The previous residual energy index predictor; reset but never read on the low band path.
     */
    private int prevNrgresIdx;

    /**
     * The previous frame's last lag block; {@code -1} when there is no previous frame in this packet or
     * conditional coding was reset.
     */
    private int prevLagblk;

    /**
     * The previous frame's last lag index; {@code -1} when there is no previous frame in this packet or
     * conditional coding was reset.
     */
    private int prevLagidx;

    /**
     * Constructs a low band parameter serializer over the shared MLow encode tables and clears its state.
     *
     * <p>The pulse, LSF, and pitch tables are the shared instances; the cross frame predictors start in the
     * reset state, ready to encode the first frame of the first packet.
     */
    public ParamEncoder() {
        this.pulseTables = PulseTables.build();
        this.lsfCodebook = LsfCodebooks.load();
        this.pitchData = PitchTables.data20();
        reset();
    }

    /**
     * Returns this serializer to its freshly constructed state.
     *
     * <p>Clears the conditional coding predictors, the pitch lag carry, and the previous frame voicing flag.
     * Call this between independent encode sessions; do not call it between the packets of one continuous
     * stream, which must thread state.
     */
    public void reset() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "param encoder reset");
        }
        this.prevAcbIdx = -1;
        this.prevFcbIdx = -1;
        this.prevNrgresIdx = -1;
        this.prevLagblk = -1;
        this.prevLagidx = -1;
    }

    /**
     * Serializes the full low band parameter set of one internal frame.
     *
     * <p>Emits, in order, the voicing flag, the LSF indices, the LSF interpolation index, the excitation
     * pulses, and then the voiced or unvoiced gain set, advancing {@code encoder} past every symbol. The
     * {@code condCoding} argument is the conditional coding flag computed for this frame ({@code false} for
     * the first frame of a packet, {@code true} otherwise when the voicing matches the previous frame); when
     * it is {@code false} the cross frame predictors are reset before the LSF and gain encode read them.
     *
     * @param encoder    the range encoder positioned at the frame's first symbol
     * @param params     the frame's quantized low band parameters
     * @param framelen   the frame length in samples
     * @param numSubfr   the number of subframes in the frame
     * @param codedAsActiveVoice {@code true} when the frame is coded as if it may contain voiced energy
     * @param condCoding the conditional coding flag on entry to this frame
     * @param lowRate    {@code true} for the low rate mode, {@code false} for high rate
     * @param frameNum   the zero based index of this frame within the packet
     * @param prevVoiced the previous frame's voicing flag; ignored for the first frame of a packet
     * @param sid        {@code true} for a SID frame
     */
    public void encodeFrame(MlowRangeEncoder encoder, LbQuantParams params, int framelen, int numSubfr,
                            boolean codedAsActiveVoice, boolean condCoding, boolean lowRate, int frameNum,
                            int prevVoiced, boolean sid) {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "param encode frame {0}: numSubfr={1} activeVoice={2} condCoding={3} lowRate={4} sid={5}",
                    frameNum, numSubfr, codedAsActiveVoice, condCoding, lowRate, sid);
        }
        var voiced = params.voiced() ? 1 : 0;
        var lowRateIx = lowRate ? 1 : 0;

        if (codedAsActiveVoice) {
            var cmf = MiscTables.VUV_CMFS[frameNum == 0 ? 0 : prevVoiced == 0 ? 1 : 2];
            MlowEntropyWrapper.encodeUpdate(encoder, cmf, voiced);
        }

        if (!condCoding) {
            prevAcbIdx = -1;
            prevFcbIdx = -1;
            prevNrgresIdx = -1;
            prevLagblk = -1;
            prevLagidx = -1;
        }

        var lsfIdx = params.lsfIdx();
        var st1 = lsfCodebook.stage1(voiced);
        var stage1Cmf = condCoding ? st1.cmfCond() : st1.cmf();
        MlowEntropyWrapper.encodeUpdate(encoder, stage1Cmf, lsfIdx[0]);
        var st2 = lsfCodebook.stage2(voiced, lowRateIx, lsfIdx[0]);
        for (var i = 0; i < LPC_ORDER; i++) {
            MlowEntropyWrapper.encodeUpdate(encoder, st2.cmf()[i], lsfIdx[i + 1]);
        }

        if (codedAsActiveVoice && numSubfr > 1) {
            MlowEntropyWrapper.encodeUpdate(encoder, MiscTables.LSF_INTERP_CMF, params.lsfInterpolIdx());
        }

        var sfPulses = new int[numSubfr];
        if (!sid) {
            PulseEncoder.encode(encoder, pulseTables, params.pulses(), framelen, numSubfr, lowRate,
                    params.voiced(), codedAsActiveVoice, sfPulses);
        }

        if (codedAsActiveVoice && params.voiced()) {
            encodeVoiced(encoder, params, sfPulses, numSubfr, framelen / 40, lowRate);
        } else {
            encodeUnvoiced(encoder, params, sfPulses, numSubfr);
        }
    }

    /**
     * Encodes the voiced per subframe gain set and the pitch lags.
     *
     * <p>For each subframe emits the adaptive codebook gain index against the conditional CMF row selected by
     * {@code prevAcbIdx + 1}, accumulates the mean quantized gain from the rate selected codebook, and, when
     * the subframe carries pulses, emits the fixed codebook gain index either absolutely (the first one of a
     * conditional run) or as a signed delta from the previous index. The accumulated mean divided by the
     * subframe count selects the pitch lag delta mode, then {@link #encodeLags} emits the block segmentation
     * and lag indices.
     *
     * @param encoder       the range encoder positioned at the first adaptive codebook gain symbol
     * @param params        the frame's quantized parameters
     * @param sfPulses      the per subframe pulse counts from the pulse encode, gating the fixed codebook gain
     *                      emits
     * @param numSubfr      the number of subframes in the frame
     * @param pitchNumSubfr the number of pitch (lag) subframes
     * @param lowRate       {@code true} for the low rate gain codebooks and CMFs
     */
    private void encodeVoiced(MlowRangeEncoder encoder, LbQuantParams params, int[] sfPulses, int numSubfr,
                              int pitchNumSubfr, boolean lowRate) {
        long meanAcbgQ14 = 0;
        var acbgCbk = lowRate ? MiscTables.ACB_GAINS_LR_Q14 : MiscTables.ACB_GAINS_HR_Q14;
        var acbgIdx = params.acbgIdx();
        var fcbgIdx = params.fcbgIdx();
        for (var sf = 0; sf < numSubfr; sf++) {
            var cmf = lowRate ? MiscTables.acbGainsCmfLr(prevAcbIdx + 1) : MiscTables.acbGainsCmfHr(prevAcbIdx + 1);
            MlowEntropyWrapper.encodeUpdate(encoder, cmf, acbgIdx[sf]);
            prevAcbIdx = acbgIdx[sf];
            meanAcbgQ14 += acbgCbk[prevAcbIdx * ACBG_M] + 2 * acbgCbk[prevAcbIdx * ACBG_M + 1];
            if (sfPulses[sf] > 0) {
                if (prevFcbIdx == -1) {
                    MlowEntropyWrapper.encodeUpdate(encoder, MiscTables.fcbgVCmf(), fcbgIdx[sf]);
                } else {
                    var delta = fcbgIdx[sf] - prevFcbIdx;
                    var minDelta = -prevFcbIdx;
                    var maxDelta = (FCBG_V_N - 1) - prevFcbIdx;
                    var dcmf = MiscTables.fcbgVDeltaCmf();
                    var base = FCBG_V_N - 1;
                    var sub = dcmf[base + minDelta] & 0xFFFFFFFFL;
                    encoder.encode((dcmf[base + delta] & 0xFFFFFFFFL) - sub,
                            (dcmf[base + delta + 1] & 0xFFFFFFFFL) - sub,
                            (dcmf[base + maxDelta + 1] & 0xFFFFFFFFL) - sub);
                }
                prevFcbIdx = fcbgIdx[sf];
            }
        }
        var meanInt = (int) (meanAcbgQ14 / numSubfr);

        var mode = 2;
        var thr = PitchTables.acbgainThr20Q14();
        if (meanInt < thr[0]) {
            mode = 0;
        } else if (meanInt < thr[1]) {
            mode = 1;
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "param encode voiced: deltaLagMode={0} blocksegsIx={1}",
                    mode, params.blocksegsIx());
        }
        encodeLags(encoder, params.blocksegsIx(), params.laginds(), mode);
        var seg = pitchData.blocksegs()[params.blocksegsIx()];
        prevLagblk = seg.blocks()[seg.nblocks() - 1];
        prevLagidx = params.laginds()[pitchNumSubfr - 1];
    }

    /**
     * Encodes the pitch lag block segmentation and per segment lag indices.
     *
     * <p>Emits the block segmentation index (directly for the first frame of a packet, or as a block
     * transition plus a windowed segmentation index for a multi frame continuation), then the first segment's
     * lag (uniformly, when the segmentation does not chain onto the previous frame's block) and each remaining
     * segment's delta lag against the running previous lag. Every CMF span is taken with the same relative
     * offset the decoder resolves against.
     *
     * @param encoder     the range encoder positioned at the block segmentation index symbol(s)
     * @param blocksegsIx the block segmentation index of this frame
     * @param laginds     the per pitch subframe integer lag indices
     * @param mode        the within block delta lag CMF class selected by the mean quantized gain
     */
    private void encodeLags(MlowRangeEncoder encoder, int blocksegsIx, int[] laginds, int mode) {
        var ixJulia = pitchData.blocksegs2idx()[blocksegsIx] & 0xFF;
        var seg = pitchData.blocksegs()[blocksegsIx];
        var numBlocksegs = pitchData.numBlocksegs();

        if (prevLagblk < 0) {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "param encode lags: first frame of packet, uniform blockseg index");
            }
            var cmf = pitchData.blocksegIdxCmf();
            encoder.encode(cmf[ixJulia - 1] & 0xFFFFFFFFL, cmf[ixJulia] & 0xFFFFFFFFL,
                    cmf[numBlocksegs] & 0xFFFFFFFFL);
        } else {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "param encode lags: block transition from prevLagblk={0}", prevLagblk);
            }
            var transCmf = pitchData.blockTransitionCmf()[prevLagblk];
            var block0 = seg.blocks()[0];
            encoder.encode(transCmf[block0] & 0xFFFFFFFFL, transCmf[block0 + 1] & 0xFFFFFFFFL,
                    transCmf[PITCH_NUM_BLOCKS] & 0xFFFFFFFFL);
            var range = pitchData.firstBlockRange();
            var startIx = range[block0 * 2] & 0xFF;
            var rangeEnd = range[block0 * 2 + 1] & 0xFF;
            var cmfLen = rangeEnd - startIx + 1;
            var cmf = pitchData.blocksegIdxCmf();
            var sub = cmf[startIx] & 0xFFFFFFFFL;
            var rel = ixJulia - startIx - 1;
            encoder.encode((cmf[startIx + rel] & 0xFFFFFFFFL) - sub,
                    (cmf[startIx + rel + 1] & 0xFFFFFFFFL) - sub,
                    (cmf[startIx + cmfLen] & 0xFFFFFFFFL) - sub);
        }

        var blk = seg.blocks()[0];
        var deltaBlk = blk - prevLagblk;
        var startSeg = 0;
        var lagindsIx = 0;
        if (!(prevLagblk > -1 && deltaBlk >= -1 && deltaBlk <= 2)) {
            var idxMod = laginds[lagindsIx] - blk * BLOCKSIZE;
            encoder.encode(idxMod, idxMod + 1, BLOCKSIZE);
            prevLagblk = blk;
            prevLagidx = laginds[lagindsIx];
            lagindsIx += seg.seglens()[0];
            startSeg = 1;
        }

        var deltaLagCmf = pitchData.deltaLagCmfs()[mode];
        var blocks = seg.blocks();
        var seglens = seg.seglens();
        for (var k = startSeg; k < seg.nblocks(); k++) {
            blk = blocks[k];
            var idx = laginds[lagindsIx];
            lagindsIx += seglens[k];
            deltaBlk = blk - prevLagblk;
            var deltaIdx = idx - prevLagidx;
            var prevLagidxMod = prevLagidx - prevLagblk * BLOCKSIZE;
            var deltaRangeStart = -prevLagidxMod + deltaBlk * BLOCKSIZE;
            var windowStart = deltaRangeStart + 2 * BLOCKSIZE - 1;
            var ix = deltaIdx - deltaRangeStart;
            var sub = deltaLagCmf[windowStart] & 0xFFFFFFFFL;
            encoder.encode((deltaLagCmf[windowStart + ix] & 0xFFFFFFFFL) - sub,
                    (deltaLagCmf[windowStart + ix + 1] & 0xFFFFFFFFL) - sub,
                    (deltaLagCmf[windowStart + BLOCKSIZE] & 0xFFFFFFFFL) - sub);
            prevLagblk = blk;
            prevLagidx = idx;
        }
    }

    /**
     * Encodes the unvoiced residual energy set.
     *
     * <p>Emits the frame level residual energy index (and, for multi subframe frames, the shape index) against
     * the gain and shape CMFs for the subframe count, then for each subframe that carries pulses emits the
     * fixed codebook gain offset index against a window of the offset CMF selected by subframe count bin and
     * pulse count bin, with the window start derived from the subframe's reconstructed Q14 decibel energy
     * exactly as the decoder derives it.
     *
     * @param encoder  the range encoder positioned at the frame level residual energy symbol
     * @param params   the frame's quantized parameters
     * @param sfPulses the per subframe pulse counts, gating the fixed codebook gain offset emits
     * @param numSubfr the number of subframes in the frame
     */
    private void encodeUnvoiced(MlowRangeEncoder encoder, LbQuantParams params, int[] sfPulses, int numSubfr) {
        var tableIx = numSubfrToIdx(numSubfr);
        if (numSubfr == 1) {
            MlowEntropyWrapper.encodeUpdate(encoder, NrgResTables.gain1Cmf(), params.nrgresFrameQi());
        } else if (numSubfr == 2) {
            MlowEntropyWrapper.encodeUpdate(encoder, NrgResTables.gain2Cmf(), params.nrgresFrameQi());
            MlowEntropyWrapper.encodeUpdate(encoder, NrgResTables.shapeCb2Cmf(), params.nrgresShapeQi());
        } else {
            MlowEntropyWrapper.encodeUpdate(encoder, NrgResTables.gain4Cmf(), params.nrgresFrameQi());
            MlowEntropyWrapper.encodeUpdate(encoder, NrgResTables.shapeCb4Cmf(), params.nrgresShapeQi());
        }

        var dbqQ14 = params.nrgresDbqQ14();
        var fcbgIdx = params.fcbgIdx();
        for (var i = 0; i < numSubfr; i++) {
            if (sfPulses[i] > 0) {
                var nrgresDbq = (dbqQ14[i] + (1 << 13)) >> 14;
                nrgresDbq = Math.min(Math.max(nrgresDbq, NrgResTables.RES_NRG_MIN_DB), NrgResTables.RES_NRG_MAX_DB);
                var minOffset = -nrgresDbq;
                var maxOffset = UV_GAIN_IDX_LEN - nrgresDbq;
                var cmfLen = maxOffset - minOffset + 2;
                var cmfIx = Math.min(sfPulses[i] / N_PULSES_STEP, FCB_G_OFFSET_CMFS - 1);
                var cmf = NrgResTables.fcbgOffsetCmf(tableIx, cmfIx);
                var sub = cmf[minOffset] & 0xFFFFFFFFL;
                var idx = fcbgIdx[i];
                encoder.encode((cmf[minOffset + idx] & 0xFFFFFFFFL) - sub,
                        (cmf[minOffset + idx + 1] & 0xFFFFFFFFL) - sub,
                        (cmf[minOffset + cmfLen - 1] & 0xFFFFFFFFL) - sub);
            }
        }
    }

    /**
     * Pulse count bin step; the per subframe pulse count is divided by this to pick the fixed codebook gain
     * offset CMF column.
     */
    private static final int N_PULSES_STEP = 10;

    /**
     * Number of fixed codebook gain offset CMF columns.
     */
    private static final int FCB_G_OFFSET_CMFS = 4;

    /**
     * Length in decibel steps of the unvoiced fixed codebook gain index range.
     */
    private static final int UV_GAIN_IDX_LEN = 90;

    /**
     * Maps a subframe count to its table bin.
     *
     * @param numSubfr the subframe count; must be 1, 2, or 4
     * @return the table bin: 0 for one, 1 for two, 2 for four subframes
     * @throws IllegalArgumentException if {@code numSubfr} is not 1, 2, or 4
     */
    private static int numSubfrToIdx(int numSubfr) {
        return switch (numSubfr) {
            case 1 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            default -> throw new IllegalArgumentException("numSubfr must be 1, 2, or 4: " + numSubfr);
        };
    }
}
