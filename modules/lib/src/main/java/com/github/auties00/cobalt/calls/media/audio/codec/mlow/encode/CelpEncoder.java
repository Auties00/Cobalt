package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.MiscTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Per subframe analysis by synthesis code excited linear prediction (CELP) encoder for the MLow speech codec.
 *
 * <p>This class is the analysis by synthesis core of the MLow encoder: the inner loop that, for one subframe,
 * forms the perceptually weighted target signal and the impulse response autocorrelation of the weighted
 * synthesis filter, drives the closed loop adaptive codebook gain search ({@link AcbSearch}), the algebraic
 * fixed codebook pulse search ({@link FcbSearch}), and the joint gain quantizer ({@link GainQuantizer}), and
 * commits the per subframe parameters the bitstream carries. It reconstructs each subframe's excitation exactly
 * as the decoder will, so the adaptive codebook history threaded to the next subframe is the same one the
 * decoder reconstructs. The open loop pitch lags ({@link OpenLoopPitch}) and the quantized line spectral
 * frequencies ({@link EncodeFrontEnd}, {@link EncoderLsfInterp}) are computed upstream and supplied per frame.
 *
 * <p>The per subframe routine {@link #encodeSubframe} proceeds in order:
 * <ol>
 * <li>The impulse response of the weighted synthesis filter is built by running the perceptual weighting
 * response through the short term synthesis filter, windowing it by the Hanning taper, and autocorrelating it
 * into the symmetric weighting column and its flipped form.</li>
 * <li>The weighted target cross correlation is the symmetric Toeplitz product of the flipped weighting column
 * against the LPC residual.</li>
 * <li>The zero input response of the weighted filter from the carried filter memory yields the residual
 * weighted error floor and augments the weighted target.</li>
 * <li>For a voiced subframe the long term prediction basis is synthesized from the adaptive codebook history;
 * {@link AcbSearch} picks the adaptive codebook gain and forms the fixed codebook target.</li>
 * <li>{@link FcbSearch} places the pulses jointly for the primary and forward error correction rate points,
 * then {@link GainQuantizer} commits the gains; for a voiced subframe the adaptive codebook gain is decided
 * again jointly with the fixed codebook gain.</li>
 * <li>The subframe excitation is reconstructed (fixed codebook plus dequantized adaptive codebook), the
 * adaptive codebook ring advances, and the weighting filter zero input response memory updates.</li>
 * </ol>
 *
 * <p>The frame entry {@link #encodeFrame} loops the subframes of one 20 ms frame in order, threading the
 * conditional coding predictors and the filter memory the per subframe routine carries, and assembles the full
 * per frame voiced or unvoiced parameter set: the per subframe adaptive codebook gain indices, the fixed
 * codebook gain indices, and the stacked signed pulses (indexed by absolute sample position with each entry the
 * signed pulse magnitude at that position). The line spectral and pitch parameters the frame entry passes
 * through unchanged are produced upstream; this class adds the excitation parameters only. The conditional
 * coding predictors reset at the packet boundary.
 *
 * <p>Scope is the 16 kHz, 60 ms, mono low band active voice path with prediction order {@value #LPC_ORDER},
 * perceptual response length {@value #HR_PERC_RESP_LEN}, the joint gain rate distortion branch always taken, and
 * the zero input response accounted. Both rate classes are supported: the high rate (9600 bps) path runs four
 * 5 ms fixed codebook subframes per 20 ms frame with the pitch sharpening fixed codebook branch inactive, and
 * the low rate (6000 bps) path runs two 10 ms fixed codebook subframes per 20 ms frame with the delayed
 * decision fixed codebook search biased by {@value #PITCH_SHARPENING_COEF} and the reconstructed voiced
 * excitation sharpened; the {@link #lowRate} flag set at construction selects the path. The high band, comfort
 * noise (DTX), and forward error correction encoding at a separate bitrate are out of scope: this class tracks
 * the forward error correction rate point the searches produce but the frame entry commits only the primary
 * rate point. One encoder carries the state of a single logical stream; construct one per stream, feed it every
 * frame in order, and call {@link #reset()} between independent streams. This type is stateful and is not thread
 * safe.
 *
 * @implNote This implementation owns the per subframe filter and codebook state: the adaptive codebook ring, the
 * weighting filter zero input response memory and its LPC synthesis tail, the conditional coding predictors per
 * rate point, the subframe counter, and the per encoder random pulse signatures. The signatures are seeded once
 * at construction by a fixed pseudo random sequence (a linear congruential generator seeded at {@code 1},
 * reproduced by {@link #seedSignatures()}) because the delayed decision fixed codebook dedup keys on them and
 * the survivor tournament is reproducible only when the seed matches. The synthesis helpers are located here
 * rather than reused from
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.CelpSynthesizer} because the encoder ring
 * is shorter than the decode ring (the decoder keeps two pitch cycles, the encoder one), so the index arithmetic
 * differs; the arithmetic itself is identical to the decode synthesis, so the reconstructed excitation the
 * encoder carries equals what the decoder produces. All accumulations are single precision; the short term
 * synthesis and moving average primitives are reused from {@link Filters}.
 */
public final class CelpEncoder {
    /**
     * Linear prediction order of the MLow short term filter.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Number of adaptive codebook gain taps.
     */
    private static final int ACBG_M = 2;

    /**
     * Maximum low band subframe length in samples; the flipped weighting column is centered at this index and
     * the random signature table is this long.
     */
    private static final int MAX_SF_LEN = 160;

    /**
     * Maximum perceptual response length ({@code 32 + 1}).
     */
    private static final int MAX_L_RESP = 33;

    /**
     * Perceptual response length of the high rate high complexity path ({@code 16 * 2}).
     */
    private static final int HR_PERC_RESP_LEN = 32;

    /**
     * Length in samples of one pitch lag subframe (2.5 ms at 16 kHz).
     */
    private static final int LAG_SUBFRLEN = 40;

    /**
     * Maximum pitch lag in samples (20 ms at 16 kHz); the adaptive codebook ring reaches back this far.
     */
    private static final int MAXPITCH_LEN = 320;

    /**
     * Half width of the fractional lag interpolation kernel.
     */
    private static final int LTP_INTERPOL_DELAY = 8;

    /**
     * Number of jointly tracked rate points.
     */
    private static final int MAX_RATES = 2;

    /**
     * The forward error correction rate point index.
     */
    private static final int IDX_FEC = 0;

    /**
     * The primary rate point index.
     */
    private static final int IDX_MAIN = 1;

    /**
     * The adaptive codebook scale applied to the long term contribution when forming the fixed codebook target.
     */
    private static final float RATE_ACB_SCALE = 0.9f;

    /**
     * The pitch sharpening feedback coefficient applied on the low rate path; multiplied by the low rate flag,
     * so {@code 0} on the high rate path.
     */
    private static final float PITCH_SHARPENING_COEF = 0.9881f;

    /**
     * The mask applied to each draw of the seeded random word, the largest value the generator returns.
     */
    private static final int RAND_MAX = 0x7fff;

    /**
     * The linear congruential multiplier of the seeded random word generator.
     */
    private static final long RAND_MULT = 214013L;

    /**
     * The linear congruential increment of the seeded random word generator.
     */
    private static final long RAND_INC = 2531011L;

    /**
     * The logger for {@link CelpEncoder}.
     */
    private static final System.Logger LOGGER = Log.get(CelpEncoder.class);

    /**
     * The closed loop adaptive codebook gain search.
     */
    private final AcbSearch acbSearch;

    /**
     * The algebraic fixed codebook pulse search; stateful scratch, one per stream.
     */
    private final FcbSearch fcbSearch;

    /**
     * The joint adaptive codebook and fixed codebook gain quantizer.
     */
    private final GainQuantizer gainQuantizer;

    /**
     * The per encoder random pulse signatures; seeded once at construction and threaded into every fixed
     * codebook delayed decision search.
     */
    private final long[] sgntrs;

    /**
     * The adaptive codebook history ring; length {@code fcbSubfrlen + MAXPITCH_LEN + LTP_INTERPOL_DELAY}. Holds
     * the reconstructed post fixed codebook excitation of recent subframes so the long term predictor can
     * reference it.
     */
    private final float[] acbState;

    /**
     * The active length of the adaptive codebook ring for the configured subframe length.
     */
    private final int acbStateLen;

    /**
     * The fixed codebook subframe length in samples.
     */
    private final int fcbSubfrlen;

    /**
     * Whether the low rate analysis by synthesis path is active.
     *
     * <p>{@code true} for the 6000 bps 60 ms path (two 10 ms subframes per frame), {@code false} for the 9600 bps
     * high rate path (four 5 ms subframes). Selects the low rate adaptive codebook gain codebook and cost rows in
     * {@link AcbSearch} and {@link GainQuantizer}, the delayed decision fixed codebook search with the
     * {@value #PITCH_SHARPENING_COEF} pitch sharpening bias, and the post synthesis pitch sharpening applied to a
     * voiced subframe's reconstructed fixed codebook excitation.
     */
    private final boolean lowRate;

    /**
     * The number of fixed codebook subframes per packet; the conditional coding predictors reset when the
     * subframe counter reaches this.
     */
    private final int subfrPerPacket;

    /**
     * The weighting filter zero input response memory backing array; the live window starts {@value #LPC_ORDER}
     * samples in.
     */
    private final float[] stateWghtBuf;

    /**
     * The LPC synthesis tail of the weighting filter zero input response memory; {@value #LPC_ORDER} samples
     * carried across subframes.
     */
    private final float[] stateErrLpcSyn;

    /**
     * The Hanning impulse response window; {@value #HR_PERC_RESP_LEN} samples for the high rate path.
     */
    private final float[] hanningWin;

    /**
     * The previous subframe's committed adaptive codebook gain index per rate point; {@code -1} at the packet
     * boundary or for an unvoiced subframe.
     */
    private final int[] prevAcbIdx;

    /**
     * The previous subframe's committed fixed codebook gain index per rate point; {@code -1} at the packet
     * boundary or for an unvoiced subframe.
     */
    private final int[] prevFcbIdx;

    /**
     * The subframe counter within the current packet.
     */
    private int subfrCnt;

    /**
     * The reconstructed post fixed codebook subframe excitation of the most recent {@link #encodeSubframe} call;
     * the high band reconstruction (out of scope here) reads it per subframe.
     */
    private final float[] excLpc;

    /**
     * Reused per subframe scratch for the reversed impulse response autocorrelation buffer, sized
     * {@code 2 * MAX_L_RESP - 1}.
     *
     * <p>Its used span {@code [1, 2 * MAX_L_RESP - 1)} is fully written every subframe (the history zeroing loop
     * and the reversed response copy) before {@link #percFiltMa} reads it, and it is not retained past the
     * subframe, so this single owner thread buffer is reused instead of reallocated.
     */
    private final float[] impLpcRevScratch = new float[2 * MAX_L_RESP - 1];

    /**
     * Reused per subframe scratch for the perceptually weighted zero input response tail, sized
     * {@value #HR_PERC_RESP_LEN}.
     *
     * <p>Fully overwritten by {@link #percFiltMa} each subframe before it is read, and not retained past the
     * subframe, so this single owner thread buffer is reused instead of reallocated.
     */
    private final float[] zirLpcScratch = new float[HR_PERC_RESP_LEN];

    /**
     * Reused per subframe scratch for the zero input response weighting column, sized {@code 2 * MAX_L_RESP - 1}.
     *
     * <p>Its used span {@code [MAX_L_RESP - 1, 2 * MAX_L_RESP - 1)} is fully written by {@link #percFiltMa} each
     * subframe before {@link #reverse} and the dot products read it, and it is not retained past the subframe, so
     * this single owner thread buffer is reused instead of reallocated.
     */
    private final float[] htZirScratch = new float[2 * MAX_L_RESP - 1];

    /**
     * Reused per subframe scratch for the two tap long term prediction basis, sized {@code MAX_SF_LEN * ACBG_M}.
     *
     * <p>Only read on the voiced path, where {@link #synLtpBasis} fully writes its used span
     * {@code [0, 2 * fcbSubfrlen)} before the adaptive codebook search and synthesis read it; not retained past
     * the subframe, so this single owner thread buffer is reused instead of reallocated.
     */
    private final float[] acbBasisScratch = new float[MAX_SF_LEN * ACBG_M];

    /**
     * Reused per subframe scratch for the per rate point weighted energy thresholds, sized {@value #MAX_RATES}.
     *
     * <p>Both entries are written every subframe before the fixed codebook search reads them, and the buffer is
     * not retained past the subframe, so this single owner thread buffer is reused instead of reallocated.
     */
    private final float[] wnrgPerPulseScratch = new float[MAX_RATES];

    /**
     * The per frame excitation parameter set the analysis by synthesis core produces.
     *
     * <p>{@code pulses} is the stacked signed pulse array indexed by absolute sample position within the frame,
     * each entry the signed pulse magnitude at that position (summed over coincident pulses); {@code sfPulses}
     * is the per fixed codebook subframe pulse count; {@code acbgIdx} the per subframe adaptive codebook gain
     * index ({@code -1} for an unvoiced or pulseless subframe); {@code fcbgIdx} the per subframe fixed codebook
     * gain index ({@code -1} for a pulseless subframe); and {@code nPulses} the total pulse count over the frame.
     *
     * @param nPulses  the total fixed codebook pulse count over the frame
     * @param pulses   the stacked signed pulses indexed by absolute sample position, {@code framelen} entries
     * @param sfPulses the per subframe pulse count, one entry per fixed codebook subframe
     * @param acbgIdx  the per subframe adaptive codebook gain index, one entry per fixed codebook subframe
     * @param fcbgIdx  the per subframe fixed codebook gain index, one entry per fixed codebook subframe
     */
    public record FrameExcitation(int nPulses, short[] pulses, int[] sfPulses, int[] acbgIdx, int[] fcbgIdx) {
    }

    /**
     * The per subframe excitation parameters the analysis by synthesis core commits for the primary rate point.
     *
     * @param nPulses the fixed codebook pulse count for this subframe
     * @param pulses  the signed pulse list, each entry {@code +-(position + 1)}; only the first
     *                {@link #nPulses()} entries are valid
     * @param acbgIdx the committed adaptive codebook gain index; {@code -1} for an unvoiced subframe and for a
     *                voiced subframe with no pulses, the sentinel the parameter serializer never reads (the gain
     *                symbols are gated on a nonzero pulse count)
     * @param fcbgIdx the committed fixed codebook gain index; {@code -1} for a subframe with no pulses
     */
    public record SubframeExcitation(int nPulses, short[] pulses, int acbgIdx, int fcbgIdx) {
    }

    /**
     * Constructs an analysis by synthesis CELP encoder for the configured fixed codebook subframe length and
     * packet structure.
     *
     * <p>Seeds the random pulse signatures, allocates the adaptive codebook ring and the zero input response
     * memory, computes the Hanning impulse response window, and resets the conditional coding predictors to the
     * packet boundary state. The search primitives are freshly allocated; the fixed codebook search holds per
     * stream scratch, so one encoder must not be shared across threads.
     *
     * @param fcbSubfrlen    the fixed codebook subframe length in samples (80 for the high rate 5 ms subframe,
     *                       160 for the low rate 10 ms subframe)
     * @param subfrPerPacket the number of fixed codebook subframes per packet
     * @param lowRate        {@code true} for the low rate analysis by synthesis path, {@code false} for the high
     *                       rate path
     */
    public CelpEncoder(int fcbSubfrlen, int subfrPerPacket, boolean lowRate) {
        this.acbSearch = new AcbSearch();
        this.fcbSearch = new FcbSearch();
        this.gainQuantizer = new GainQuantizer();
        this.fcbSubfrlen = fcbSubfrlen;
        this.subfrPerPacket = subfrPerPacket;
        this.lowRate = lowRate;
        this.acbStateLen = fcbSubfrlen + MAXPITCH_LEN + LTP_INTERPOL_DELAY;
        this.acbState = new float[acbStateLen];
        this.stateWghtBuf = new float[LPC_ORDER + MAX_SF_LEN];
        this.stateErrLpcSyn = new float[LPC_ORDER];
        this.hanningWin = buildHanningWindow(HR_PERC_RESP_LEN);
        this.prevAcbIdx = new int[]{-1, -1};
        this.prevFcbIdx = new int[]{-1, -1};
        this.excLpc = new float[fcbSubfrlen];
        this.sgntrs = seedSignatures();
        this.subfrCnt = 0;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "celp encoder constructed: fcbSubfrlen={0} subfrPerPacket={1} lowRate={2}",
                    fcbSubfrlen, subfrPerPacket, lowRate);
        }
    }

    /**
     * Returns this encoder to its freshly constructed state.
     *
     * <p>Zeroes the adaptive codebook ring and the zero input response memory, resets the conditional coding
     * predictors and the subframe counter to the packet boundary state, and resets the fixed codebook search
     * scratch. The random signatures are not reseeded; they are a fixed per encoder property. Call this between
     * independent streams.
     */
    public void reset() {
        Arrays.fill(acbState, 0.0f);
        Arrays.fill(stateWghtBuf, 0.0f);
        Arrays.fill(stateErrLpcSyn, 0.0f);
        prevAcbIdx[IDX_FEC] = prevAcbIdx[IDX_MAIN] = -1;
        prevFcbIdx[IDX_FEC] = prevFcbIdx[IDX_MAIN] = -1;
        subfrCnt = 0;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "celp encoder reset");
        }
    }

    /**
     * Returns the reconstructed post fixed codebook excitation of the most recent {@link #encodeSubframe} call.
     *
     * <p>Exposed for the high band reconstruction and to inspect the synthesized excitation. The returned array
     * is the live backing store; the next {@link #encodeSubframe} overwrites it.
     *
     * @return the live subframe excitation, {@code fcbSubfrlen} entries
     */
    public float[] excLpc() {
        return excLpc;
    }

    /**
     * Overrides the conditional coding predictors before the next subframe.
     *
     * <p>The predictors are normally threaded by the per subframe routine. A harness that drives one subframe in
     * isolation seeds them with the values held when that subframe was originally encoded, so the conditional
     * cost rows match. Production callers never use this; the predictors evolve internally.
     *
     * @param prevAcb the previous adaptive codebook gain index for both rate points, or {@code -1}
     * @param prevFcb the previous fixed codebook gain index for both rate points, or {@code -1}
     */
    void setConditionalPredictors(int prevAcb, int prevFcb) {
        prevAcbIdx[IDX_FEC] = prevAcbIdx[IDX_MAIN] = prevAcb;
        prevFcbIdx[IDX_FEC] = prevFcbIdx[IDX_MAIN] = prevFcb;
    }

    /**
     * Encodes one 20 ms frame's excitation.
     *
     * <p>Loops the fixed codebook subframes in order, calling {@link #encodeSubframe} for each, and assembles
     * the stacked per frame parameter set. The per subframe inputs are supplied by the caller: the LPC residual,
     * the per subframe interpolated LPC filters and perceptual weighting responses, the pitch lags, and the
     * bitrate control survivor budgets. The conditional coding predictors and the filter memory are threaded
     * internally across the subframes.
     *
     * @param voiced          {@code true} for a voiced frame
     * @param numsubfrs       the number of fixed codebook subframes in the frame
     * @param lagSfPerFcbSf   the number of lag subframes per fixed codebook subframe
     * @param reslpc          the whole frame LPC residual, {@code numsubfrs * fcbSubfrlen} entries
     * @param predcoefs       the per subframe interpolated LPC filters, {@code predcoefs[sf]} of length
     *                        {@value #LPC_ORDER}{@code  + 1}
     * @param percWghtResp    the per subframe perceptual weighting responses, {@code percWghtResp[sf]} of length
     *                        at least the perceptual response length
     * @param lags            the per lag subframe pitch lags for the whole frame
     * @param survPerSubframe the per pulse stage survivor budgets; reused across subframes
     * @param fcbPulsesMax    the per rate point maximum pulse count for each subframe, indexed
     *                        {@code fcbPulsesMax[sf][rate]}
     * @param subfrImportance the per rate point subframe importance for each subframe, indexed
     *                        {@code subfrImportance[sf][rate]}
     * @return the assembled per frame excitation parameter set
     */
    public FrameExcitation encodeFrame(boolean voiced, int numsubfrs, int lagSfPerFcbSf, float[] reslpc,
                                       float[][] predcoefs, float[][] percWghtResp, float[] lags,
                                       short[] survPerSubframe, short[][] fcbPulsesMax, float[][] subfrImportance) {
        var framelen = numsubfrs * fcbSubfrlen;
        var pulses = new short[framelen];
        var sfPulses = new int[numsubfrs];
        var acbgIdx = new int[numsubfrs];
        var fcbgIdx = new int[numsubfrs];
        var nPulsesTotal = 0;
        for (var sf = 0; sf < numsubfrs; sf++) {
            var lagind = sf * lagSfPerFcbSf;
            var subLags = new float[lagSfPerFcbSf];
            System.arraycopy(lags, lagind, subLags, 0, lagSfPerFcbSf);
            var maxPulses = fcbPulsesMax[sf];
            var importance = subfrImportance[sf];
            var se = encodeSubframe(voiced, reslpc, sf * fcbSubfrlen, predcoefs[sf],
                    percWghtResp[sf], subLags, importance, maxPulses, survPerSubframe);
            sfPulses[sf] = se.nPulses();
            acbgIdx[sf] = se.acbgIdx();
            fcbgIdx[sf] = se.fcbgIdx();
            nPulsesTotal += se.nPulses();
            var subPulses = se.pulses();
            for (var i = 0; i < se.nPulses(); i++) {
                var signed = subPulses[i];
                var sign = 1 + 2 * (signed >> 15);
                var pos = (signed * sign) - 1;
                pulses[sf * fcbSubfrlen + pos] += (short) sign;
            }
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "celp encode frame: voiced={0} numsubfrs={1} nPulses={2}",
                    voiced, numsubfrs, nPulsesTotal);
        }
        return new FrameExcitation(nPulsesTotal, pulses, sfPulses, acbgIdx, fcbgIdx);
    }

    /**
     * Encodes one subframe's excitation by analysis by synthesis.
     *
     * <p>Builds the impulse response autocorrelation and the perceptually weighted target, accounts the zero
     * input response, runs the adaptive codebook gain search (voiced only), the fixed codebook pulse search, and
     * the gain quantizer, reconstructs the excitation, and advances the adaptive codebook ring and the weighting
     * filter memory. The conditional coding predictors are advanced or reset at the packet boundary.
     *
     * @param voiced          {@code true} for a voiced subframe
     * @param reslpc          the whole frame LPC residual backing array
     * @param resOff          the offset of this subframe's residual within {@code reslpc}
     * @param predcoef        the subframe's interpolated LPC filter, {@value #LPC_ORDER}{@code  + 1} taps
     * @param percWghtResp    the subframe's perceptual weighting response, at least the perceptual response
     *                        length
     * @param lags            the per lag subframe pitch lags for this subframe, {@code lagSfPerFcbSf} entries
     * @param subfrImportance the per rate point subframe importance, {@value #MAX_RATES} entries
     * @param fcbPulsesMax    the per rate point maximum pulse count, {@value #MAX_RATES} entries
     * @param survPerSubframe the per pulse stage survivor budget
     * @return the committed primary rate point excitation parameters for this subframe
     */
    public SubframeExcitation encodeSubframe(boolean voiced, float[] reslpc, int resOff, float[] predcoef,
                                             float[] percWghtResp, float[] lags, float[] subfrImportance,
                                             short[] fcbPulsesMax, short[] survPerSubframe) {
        var lResp = HR_PERC_RESP_LEN;

        // Impulse response of the weighted synthesis filter: the weighting response run through the synthesis
        // filter, then windowed by Hanning.
        var impLpcBuf = new float[LPC_ORDER + MAX_L_RESP];
        hpAr16(percWghtResp, 0, lResp, predcoef, impLpcBuf, LPC_ORDER);
        var impLpc = LPC_ORDER;
        for (var i = 0; i < lResp; i++) {
            impLpcBuf[impLpc + i] *= hanningWin[i];
        }

        // Weighting column: autocorrelate the impulse response, then flip it into the symmetric column.
        var impLpcRevBuf = impLpcRevScratch;
        var impLpcRev = MAX_L_RESP - 1;
        for (var i = 0; i < lResp; i++) {
            impLpcRevBuf[impLpcRev + i] = impLpcBuf[impLpc + lResp - 1 - i];
        }
        // Zero the (lResp - 1) history samples preceding the reversed response.
        for (var i = 0; i < lResp - 1; i++) {
            impLpcRevBuf[impLpcRev - 1 - i] = 0.0f;
        }
        var phi = new float[fcbSubfrlen];
        percFiltMa(impLpcRevBuf, impLpcRev, lResp, impLpcBuf, impLpc, lResp, phi, 0);
        reverse(phi, lResp);
        // phi[lResp .. fcbSubfrlen) is already zero from allocation.

        var phiFlip = new float[2 * MAX_SF_LEN + 1];
        phiFlip[MAX_SF_LEN] = phi[0];
        for (var i = 0; i < lResp + 1; i++) {
            phiFlip[MAX_SF_LEN - i] = phi[i];
            phiFlip[MAX_SF_LEN + i] = phi[i];
        }

        // Weighted target cross correlation: the symmetric Toeplitz weighting times the LPC residual.
        var dLpc = new float[fcbSubfrlen];
        multSymToepl2(phiFlip, MAX_SF_LEN - lResp + 1, lResp, reslpc, resOff, dLpc, 0, fcbSubfrlen);

        // Zero input response accounting.
        float werrIn;
        var zirLpc = zirLpcScratch;
        {
            var zirTmpBuf = new float[fcbSubfrlen + 2 * MAX_L_RESP - 1];
            var zirTmp = MAX_L_RESP - 1;
            var htZirBuf = htZirScratch;
            var htZir = MAX_L_RESP - 1;
            // Clear the response samples; the preceding history comes from the weighting filter memory tail.
            for (var i = 0; i < lResp; i++) {
                zirTmpBuf[zirTmp + i] = 0.0f;
            }
            var stateLen = Math.max(LPC_ORDER, lResp - 1);
            var stateWght = LPC_ORDER;
            for (var i = 0; i < stateLen; i++) {
                zirTmpBuf[zirTmp - stateLen + i] = stateWghtBuf[stateWght + fcbSubfrlen - stateLen + i];
            }
            // Short term synthesis filter in place over the response window.
            ar16InPlace(zirTmpBuf, zirTmp, lResp, predcoef);
            percFiltMa(zirTmpBuf, zirTmp, lResp, percWghtResp, 0, lResp, zirLpc, 0);

            for (var i = 0; i < lResp; i++) {
                zirTmpBuf[zirTmp + i] = zirLpc[lResp - 1 - i];
            }
            for (var i = 0; i < lResp - 1; i++) {
                zirTmpBuf[zirTmp - 1 - i] = 0.0f;
            }
            percFiltMa(zirTmpBuf, zirTmp, lResp, impLpcBuf, impLpc, lResp, htZirBuf, htZir);
            reverse(htZirBuf, htZir, lResp);
            werrIn = voiced
                    ? dotProd(dLpc, 0, reslpc, resOff, fcbSubfrlen)
                      + 2.0f * dotProd(htZirBuf, htZir, reslpc, resOff, lResp)
                      + nrg(zirLpc, 0, lResp)
                    : 0.0f;
            for (var i = 0; i < lResp; i++) {
                dLpc[i] += htZirBuf[htZir + i];
            }
        }

        var nLags = fcbSubfrlen / LAG_SUBFRLEN;
        var acbBasis = acbBasisScratch;
        float[] dTarget;
        var acbgIdxMain = -1;
        AcbSearch.AcbParams acbParams;
        if (voiced) {
            synLtpBasis(lags, nLags, acbStateLen, acbBasis);
            var acb = acbSearch.search(phiFlip, lResp, acbBasis, dLpc, werrIn, fcbSubfrlen, lowRate,
                    prevAcbIdx[IDX_MAIN]);
            acbgIdxMain = acb.acbIdx();
            dTarget = acb.dLtp();
            acbParams = acb.params();
        } else {
            dTarget = dLpc;
            acbParams = new AcbSearch.AcbParams(werrIn, new float[ACBG_M * ACBG_M], new float[ACBG_M],
                    new float[ACBG_M * fcbSubfrlen]);
        }

        // Weighted target energy for the per pulse weighted energy threshold.
        var wtgtTmpBuf = new float[fcbSubfrlen + 2 * MAX_L_RESP - 1];
        var wtgtTmp = MAX_L_RESP - 1;
        var wtgt = new float[fcbSubfrlen + MAX_L_RESP];
        System.arraycopy(reslpc, resOff, wtgtTmpBuf, wtgtTmp, fcbSubfrlen);
        if (voiced) {
            var acbGain = acbDequant(lowRate, acbgIdxMain);
            var acb = new float[fcbSubfrlen];
            acbSynthesize(fcbSubfrlen, acbBasis, acbGain, acb, 0.0f);
            for (var i = 0; i < fcbSubfrlen; i++) {
                wtgtTmpBuf[wtgtTmp + i] += -RATE_ACB_SCALE * acb[i];
            }
        }
        for (var i = 0; i < lResp; i++) {
            wtgtTmpBuf[wtgtTmp + fcbSubfrlen + i] = 0.0f;
        }
        for (var i = 0; i < lResp - 1; i++) {
            wtgtTmpBuf[wtgtTmp - 1 - i] = 0.0f;
        }
        percFiltMa(wtgtTmpBuf, wtgtTmp, fcbSubfrlen + lResp, impLpcBuf, impLpc, lResp, wtgt, 0);
        for (var i = 0; i < lResp; i++) {
            wtgt[i] += zirLpc[i];
        }
        var nrgWtgt = nrg(wtgt, 0, fcbSubfrlen + lResp);
        var wnrgPerPulse = wnrgPerPulseScratch;
        for (var r = 0; r < MAX_RATES; r++) {
            wnrgPerPulse[r] = nrgWtgt / (subfrImportance[r] + 1.0e-3f);
        }

        var iLag = (int) lags[nLags - 1];
        int[] fcbPulsesMaxInt = {fcbPulsesMax[IDX_FEC], fcbPulsesMax[IDX_MAIN]};
        FcbSearch.Result fcb;
        if (fcbPulsesMax[IDX_MAIN] > 0) {
            if (fcbPulsesMax[IDX_MAIN] - 1 > 0 && survPerSubframe[fcbPulsesMax[IDX_MAIN] - 2] == 1 && !lowRate) {
                fcb = fcbSearch.search(dTarget, wnrgPerPulse, fcbPulsesMaxInt, phi, phiFlip, lResp, fcbSubfrlen);
            } else {
                fcb = fcbSearch.searchDeldec(dTarget, PITCH_SHARPENING_COEF * (lowRate ? 1 : 0), iLag,
                        wnrgPerPulse, fcbPulsesMaxInt, survPerSubframe, phi, phiFlip, lResp, fcbSubfrlen, sgntrs);
            }
        } else {
            fcb = new FcbSearch.Result(new short[MAX_RATES][40], new int[MAX_RATES], new float[MAX_RATES],
                    new float[MAX_RATES], new float[MAX_RATES]);
        }

        var acbIdxOut = new int[]{acbgIdxMain, acbgIdxMain};
        var gainIdxOut = new int[]{-1, -1};
        var excFcbPerRate = new float[MAX_RATES][];
        for (var r = 0; r < MAX_RATES; r++) {
            var excFcb = fcbSynthesize(fcb.pulses()[r], fcb.nPulses()[r], fcbSubfrlen);
            if (fcb.nPulses()[r] > 0) {
                float fcbgain;
                if (voiced) {
                    // A low rate voiced subframe sharpens the reconstructed pulse excitation before the joint
                    // gain decision, so the gain quantizer sees the sharpened excitation. The high rate path
                    // leaves it untouched.
                    if (lowRate) {
                        pitchSharp(excFcb, iLag, fcbSubfrlen);
                    }
                    var vg = gainQuantizer.quantizeVoiced(acbParams, fcb.fcbWnrg()[r],
                            fcb.gainFromSearch()[r], excFcb, dLpc, fcbSubfrlen, lowRate,
                            prevAcbIdx[r], prevFcbIdx[r]);
                    acbIdxOut[r] = vg.acbIdx();
                    gainIdxOut[r] = vg.fcbIdx();
                    fcbgain = vg.fcbGain();
                } else {
                    gainIdxOut[r] = GainQuantizer.quantizeUnvoiced(fcb.gainFromSearch()[r]);
                    fcbgain = gainQuantizer.unvoicedGain(gainIdxOut[r]);
                }
                for (var i = 0; i < fcbSubfrlen; i++) {
                    excFcb[i] *= fcbgain;
                }
            }
            excFcbPerRate[r] = excFcb;
        }

        // Reconstruct the primary rate point excitation and update the adaptive codebook ring.
        System.arraycopy(excFcbPerRate[IDX_MAIN], 0, excLpc, 0, fcbSubfrlen);
        var resLtp = new float[fcbSubfrlen];
        System.arraycopy(reslpc, resOff, resLtp, 0, fcbSubfrlen);
        if (voiced) {
            var acbGain = acbDequant(lowRate, acbIdxOut[IDX_MAIN]);
            var acb = new float[fcbSubfrlen];
            acbSynthesize(fcbSubfrlen, acbBasis, acbGain, acb, 0.0f);
            for (var i = 0; i < fcbSubfrlen; i++) {
                excLpc[i] += acb[i];
                resLtp[i] -= acb[i];
            }
        }
        System.arraycopy(acbState, fcbSubfrlen, acbState, 0, acbStateLen - 2 * fcbSubfrlen);
        System.arraycopy(excLpc, 0, acbState, acbStateLen - 2 * fcbSubfrlen, fcbSubfrlen);

        // Update the weighting filter zero input response memory.
        var lpcResErr = new float[fcbSubfrlen];
        for (var i = 0; i < fcbSubfrlen; i++) {
            lpcResErr[i] = reslpc[resOff + i] - excLpc[i];
        }
        System.arraycopy(stateErrLpcSyn, 0, stateWghtBuf, 0, LPC_ORDER);
        ar16(lpcResErr, 0, fcbSubfrlen, predcoef, stateWghtBuf, LPC_ORDER);
        System.arraycopy(stateWghtBuf, LPC_ORDER + fcbSubfrlen - LPC_ORDER, stateErrLpcSyn, 0, LPC_ORDER);

        // Advance or reset the conditional coding predictors.
        subfrCnt++;
        if (subfrCnt == subfrPerPacket) {
            prevAcbIdx[IDX_FEC] = prevAcbIdx[IDX_MAIN] = -1;
            prevFcbIdx[IDX_FEC] = prevFcbIdx[IDX_MAIN] = -1;
            subfrCnt = 0;
        } else {
            for (var r = 0; r < MAX_RATES; r++) {
                prevAcbIdx[r] = voiced ? acbIdxOut[r] : -1;
                prevFcbIdx[r] = voiced ? gainIdxOut[r] : -1;
            }
        }

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "celp encode subframe: voiced={0} nPulses={1} acbIdx={2} fcbIdx={3}",
                    voiced, fcb.nPulses()[IDX_MAIN], acbIdxOut[IDX_MAIN], gainIdxOut[IDX_MAIN]);
        }
        return new SubframeExcitation(fcb.nPulses()[IDX_MAIN], fcb.pulses()[IDX_MAIN],
                acbIdxOut[IDX_MAIN], gainIdxOut[IDX_MAIN]);
    }

    /**
     * Scatters signed pulses into a zeroed excitation buffer.
     *
     * <p>Each entry of {@code pulses} is {@code +-(position + 1)}; the sign is the high bit and the magnitude
     * minus one is the position. Coincident pulses accumulate.
     *
     * @param pulses      the signed pulse list, {@code nPulses} valid entries
     * @param nPulses     the pulse count
     * @param fcbSubfrlen the subframe length in samples
     * @return a freshly allocated excitation buffer with the scattered unit pulses
     */
    private static float[] fcbSynthesize(short[] pulses, int nPulses, int fcbSubfrlen) {
        var fcb = new float[fcbSubfrlen];
        for (var n = 0; n < nPulses; n++) {
            var sign = 1 + 2 * (pulses[n] >> 15);
            var pos = (pulses[n] * sign) - 1;
            fcb[pos] += sign;
        }
        return fcb;
    }

    /**
     * Sharpens an excitation in place by a one pole pitch periodic feedback.
     *
     * <p>For each sample at or past {@code lag}, adds {@value #PITCH_SHARPENING_COEF} times the sample one lag
     * earlier ({@code x[i] += x[i - lag] * PITCH_SHARPENING_COEF}), accentuating the pitch periodicity of the
     * reconstructed fixed codebook excitation. Run only on the low rate voiced path before the joint gain
     * decision; the high rate path never calls it. The decode side twin is
     * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.CelpSynthesizer}, which sharpens the
     * residual the same way so the encoder and decoder reconstruct identical voiced excitation.
     *
     * @param x   the excitation to sharpen in place
     * @param lag the pitch lag in samples
     * @param len the subframe length in samples
     */
    private static void pitchSharp(float[] x, int lag, int len) {
        for (var i = lag; i < len; i++) {
            x[i] += x[i - lag] * PITCH_SHARPENING_COEF;
        }
    }

    /**
     * Applies the general perceptual moving average filter for the high rate path.
     *
     * <p>Delegates to {@link Filters#ma(float[], int, int, float[], int, float[], int)}: the {@code coefLen - 1}
     * history samples precede the window in the same backing array.
     *
     * @param x       the input buffer with {@code coefLen - 1} history samples before {@code xOff}
     * @param xOff    the offset of the first filtered sample
     * @param n       the number of samples to filter
     * @param coef    the filter coefficients backing array
     * @param coefOff the offset of the first coefficient
     * @param coefLen the coefficient count
     * @param y       the output buffer
     * @param yOff    the offset of the first output sample
     */
    private static void percFiltMa(float[] x, int xOff, int n, float[] coef, int coefOff, int coefLen,
                                   float[] y, int yOff) {
        if (coefOff == 0) {
            Filters.ma(x, xOff, n, coef, coefLen, y, yOff);
            return;
        }
        var c = new float[coefLen];
        System.arraycopy(coef, coefOff, c, 0, coefLen);
        Filters.ma(x, xOff, n, c, coefLen, y, yOff);
    }

    /**
     * Applies the 16th order short term synthesis filter with {@value #LPC_ORDER} zero history before the window,
     * producing output into a buffer whose history precedes it.
     *
     * @param x        the input buffer (the residual)
     * @param xOff     the offset of the first input sample
     * @param n        the subframe length
     * @param coef     the {@value #LPC_ORDER}{@code  + 1} LPC taps
     * @param y        the output buffer with {@value #LPC_ORDER} history samples before {@code yOff}
     * @param yOff     the offset of the first output sample
     */
    private static void ar16(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        hpAr16(x, xOff, n, coef, y, yOff);
    }

    /**
     * Runs the 16th order short term synthesis filter in place over a window with {@value #LPC_ORDER} history
     * samples before it.
     *
     * <p>Input and output alias; {@link Filters#ar16(float[], int, int, float[], float[], int)} reads
     * {@code y[n - 16 .. n - 1]} and writes {@code y[n]}, and because each output depends only on prior outputs
     * and the current input, running it with the same array for input and output reproduces the aliased
     * recursion exactly.
     *
     * @param buf the buffer holding {@value #LPC_ORDER} history samples before {@code off} and the input over
     *            {@code [off, off + n)}, overwritten with the filtered output
     * @param off the offset of the first filtered sample
     * @param n   the number of samples to filter
     * @param coef the {@value #LPC_ORDER}{@code  + 1} LPC taps
     */
    private static void ar16InPlace(float[] buf, int off, int n, float[] coef) {
        hpAr16(buf, off, n, coef, buf, off);
    }

    /**
     * Applies the monic 16th order autoregressive synthesis pole with a fixed operand grouping.
     *
     * <p>Computes the recursion {@code y[n] = x[n] - sum(coef[16 - i] * y[n - 16 + i], i = 0 .. 15)}, that is
     * {@code y[n] = x[n] - sum(coef[k] * y[n - k], k = 1 .. 16)} with {@code coef[0]} the monic {@code 1.0f},
     * reading the 16 history outputs from {@code y[yOff - 16 .. yOff - 1]} in the same backing array. The sixteen
     * taps are accumulated in the exact grouping below rather than in source order, so the single precision
     * result is bit identical to the reference implementation:
     * <ul>
     *   <li>{@code t = x[n]}</li>
     *   <li>{@code t = t - coef[14] * y[n-14]}</li>
     *   <li>{@code t = t - coef[2]  * y[n-2]}</li>
     *   <li>{@code t = t - coef[12] * y[n-12]}</li>
     *   <li>{@code t = t - coef[4]  * y[n-4]}</li>
     *   <li>{@code t = t - (coef[8]*y[n-8] + (coef[1]*y[n-1] + (coef[6]*y[n-6] + coef[9]*y[n-9])))}</li>
     *   <li>{@code t = t - (coef[15]*y[n-15] + (coef[16]*y[n-16] + ((coef[10]*y[n-10] + coef[13]*y[n-13]) + coef[5]*y[n-5])))}</li>
     *   <li>{@code y[n] = t - (coef[3]*y[n-3] + (coef[11]*y[n-11] + coef[7]*y[n-7]))}</li>
     * </ul>
     * Each subexpression keeps the exact left to right operand order the grouping requires. This filter drives
     * the weighted synthesis impulse response, the zero input response, and the weighting filter memory update of
     * the voiced analysis by synthesis search, so its rounding propagates through the whole excitation search.
     *
     * @implNote This implementation reproduces the operand grouping with plain {@code *}, {@code +}, and
     * {@code -}; no fused multiply add is used, so the single precision arithmetic matches the reference bit for
     * bit.
     *
     * @param x    the input buffer
     * @param xOff the offset of the first input sample in {@code x}
     * @param n    the number of samples to filter
     * @param coef the {@value #LPC_ORDER}{@code  + 1} LPC taps; {@code coef[0]} is the monic {@code 1.0f}
     * @param y    the output buffer with {@value #LPC_ORDER} history outputs before {@code yOff}
     * @param yOff the offset of the first output sample in {@code y}; at least {@value #LPC_ORDER}
     */
    private static void hpAr16(float[] x, int xOff, int n, float[] coef, float[] y, int yOff) {
        for (var sample = 0; sample < n; sample++) {
            var b = yOff + sample;
            float ym1  = y[b - 1],  ym2  = y[b - 2],  ym3  = y[b - 3],  ym4  = y[b - 4];
            float ym5  = y[b - 5],  ym6  = y[b - 6],  ym7  = y[b - 7],  ym8  = y[b - 8];
            float ym9  = y[b - 9],  ym10 = y[b - 10], ym11 = y[b - 11], ym12 = y[b - 12];
            float ym13 = y[b - 13], ym14 = y[b - 14], ym15 = y[b - 15], ym16 = y[b - 16];
            var t = x[xOff + sample];
            t = t - coef[14] * ym14;
            t = t - coef[2] * ym2;
            t = t - coef[12] * ym12;
            t = t - coef[4] * ym4;
            t = t - (coef[8] * ym8 + (coef[1] * ym1 + (coef[6] * ym6 + coef[9] * ym9)));
            t = t - (coef[15] * ym15 + (coef[16] * ym16 + (coef[10] * ym10 + coef[13] * ym13 + coef[5] * ym5)));
            t = t - (coef[3] * ym3 + (coef[11] * ym11 + coef[7] * ym7));
            y[b] = t;
        }
    }

    /**
     * Builds the two tap symmetric long term prediction basis from the adaptive codebook ring.
     *
     * <p>Identical to the decode side synthesis, on the encoder's own shorter ring; see the class note.
     *
     * @param lags     the per lag subframe pitch lags, {@code numLags} entries
     * @param numLags  the number of lag subframes
     * @param stateLen the active length of the adaptive codebook ring
     * @param acbBasis the interleaved center and side basis output
     */
    private void synLtpBasis(float[] lags, int numLags, int stateLen, float[] acbBasis) {
        var pEnd = stateLen - numLags * LAG_SUBFRLEN;
        for (var subfr = 0; subfr < numLags; subfr++) {
            var iLag = (int) Math.floor(lags[subfr]);
            var centerOut = subfr * LAG_SUBFRLEN;
            var sideOut = (numLags + subfr) * LAG_SUBFRLEN;
            if (iLag == lags[subfr]) {
                for (var i = 0; i < LAG_SUBFRLEN; i++) {
                    acbState[pEnd + i] = acbState[pEnd + i - iLag];
                }
                System.arraycopy(acbState, pEnd, acbBasis, centerOut, LAG_SUBFRLEN);
                var a = pEnd - iLag - 1;
                var b = pEnd - iLag + 1;
                for (var i = 0; i < LAG_SUBFRLEN; i++) {
                    acbBasis[sideOut + i] = acbState[a + i] + acbState[b + i];
                }
            } else {
                var first = dotProd(acbState, pEnd - 1 - iLag - LTP_INTERPOL_DELAY,
                        MiscTables.INTERPOL_KERNEL, 0, 2 * LTP_INTERPOL_DELAY);
                interpol(acbState, pEnd - iLag - LTP_INTERPOL_DELAY, acbState, pEnd, LAG_SUBFRLEN);
                var last = dotProd(acbState, pEnd + LAG_SUBFRLEN - iLag - LTP_INTERPOL_DELAY,
                        MiscTables.INTERPOL_KERNEL, 0, 2 * LTP_INTERPOL_DELAY);
                System.arraycopy(acbState, pEnd, acbBasis, centerOut, LAG_SUBFRLEN);
                acbBasis[sideOut] = first + acbState[pEnd + 1];
                for (var i = 0; i < LAG_SUBFRLEN - 2; i++) {
                    acbBasis[sideOut + 1 + i] = acbState[pEnd + i] + acbState[pEnd + 2 + i];
                }
                acbBasis[sideOut + LAG_SUBFRLEN - 1] = acbState[pEnd + LAG_SUBFRLEN - 2] + last;
            }
            pEnd += LAG_SUBFRLEN;
        }
    }

    /**
     * Combines the basis vectors into the adaptive codebook contribution.
     *
     * <p>Identical to the decode side synthesis. The {@code highBoost} is always {@code 0} in the encoder, so
     * {@link #adjustAcbGains} does nothing.
     *
     * @param subfrLen  the subframe length in samples
     * @param acbBasis  the interleaved center and side basis vectors
     * @param acbG      the adaptive codebook gains, {@value #ACBG_M} entries; mutated by the high boost
     * @param acb       the contribution output, {@code subfrLen} entries written
     * @param highBoost the adaptive codebook high boost amount, always {@code 0} here
     */
    private static void acbSynthesize(int subfrLen, float[] acbBasis, float[] acbG, float[] acb, float highBoost) {
        adjustAcbGains(acbG, highBoost);
        for (var i = 0; i < subfrLen; i++) {
            acb[i] = acbBasis[i] * acbG[0];
        }
        for (var i = 0; i < subfrLen; i++) {
            acb[i] += acbBasis[subfrLen + i] * acbG[1];
        }
    }

    /**
     * Applies the high frequency boost to the adaptive codebook gains.
     *
     * <p>A zero boost leaves the gains unchanged, which is the only case the encoder takes.
     *
     * @param acbG      the two adaptive codebook gains, mutated in place
     * @param highBoost the boost amount; {@code 0} leaves the gains unchanged
     */
    private static void adjustAcbGains(float[] acbG, float highBoost) {
        if (highBoost == 0.0f) {
            return;
        }
        var f0 = acbG[0] + 2.0f * acbG[1];
        var f1 = acbG[0] - acbG[1];
        var absF1New = Math.min(Math.abs(f1) + highBoost, Math.abs(f0));
        f1 *= absF1New / (Math.abs(f1) + 1e-12f);
        acbG[0] = (f0 + 2.0f * f1) / 3.0f;
        acbG[1] = (f0 - f1) / 3.0f;
    }

    /**
     * Dequantizes one subframe's adaptive codebook gains from their index.
     *
     * @param lowRate {@code true} for the low rate codebook
     * @param acbIdx  the adaptive codebook gain index
     * @return a freshly allocated {@value #ACBG_M} entry array of real tap gains
     */
    private static float[] acbDequant(boolean lowRate, int acbIdx) {
        var cb = lowRate ? MiscTables.ACB_GAINS_LR_Q14 : MiscTables.ACB_GAINS_HR_Q14;
        var acbG = new float[ACBG_M];
        var scQ14 = 1.0f / (1 << 14);
        for (var m = 0; m < ACBG_M; m++) {
            acbG[m] = cb[acbIdx * ACBG_M + m] * scQ14;
        }
        return acbG;
    }

    /**
     * Computes the symmetric eight tap fractional delay interpolation with a fixed operand grouping.
     *
     * <p>Each output sample is the sum of eight folded tap products {@code t[i] = (x[n + i] + x[n + 15 - i]) *
     * kernel[i]}, {@code i = 0 .. 7}. The eight products are accumulated as two grouped chains combined at the
     * end rather than in source order, so the single precision result is bit identical to the reference:
     * {@snippet :
     * ret = (((t[0] + t[1]) + t[4]) + t[6]) + (((t[2] + t[3]) + t[5]) + t[7]);
     * }
     * The grouping is load bearing on the encoder's voiced analysis by synthesis search: this filter interpolates
     * the fractional lag adaptive codebook ring inside {@link #synLtpBasis}, so a one ULP shift in any
     * interpolated sample propagates through the adaptive codebook basis and the carried ring into every later
     * subframe. The decoder keeps the source order {@link Filters#interpol} and
     * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.CelpSynthesizer}; only this encoder
     * local copy carries the grouped order.
     *
     * @implNote This implementation builds the first chain as {@code t0; +=t1; +=t4; +=t6} and the second as
     * {@code t2; +=t3; +=t5; +=t7}, then adds the two chains; no fused multiply add is used, so the single
     * precision arithmetic matches the reference bit for bit.
     *
     * @param x    the input array
     * @param xOff the offset of the first input sample of output sample 0
     * @param y    the output array
     * @param yOff the offset of the first output sample
     * @param n    the number of output samples
     */
    private static void interpol(float[] x, int xOff, float[] y, int yOff, int n) {
        var kernel = MiscTables.INTERPOL_KERNEL;
        for (var m = 0; m < n; m++) {
            var b = xOff + m;
            var t0 = (x[b] + x[b + 15]) * kernel[0];
            var t1 = (x[b + 1] + x[b + 14]) * kernel[1];
            var t2 = (x[b + 2] + x[b + 13]) * kernel[2];
            var t3 = (x[b + 3] + x[b + 12]) * kernel[3];
            var t4 = (x[b + 4] + x[b + 11]) * kernel[4];
            var t5 = (x[b + 5] + x[b + 10]) * kernel[5];
            var t6 = (x[b + 6] + x[b + 9]) * kernel[6];
            var t7 = (x[b + 7] + x[b + 8]) * kernel[7];
            y[yOff + m] = (((t0 + t1) + t4) + t6) + (((t2 + t3) + t5) + t7);
        }
    }

    /**
     * Multiplies a symmetric Toeplitz weighting against a vector.
     *
     * @param c     the symmetric weighting column array
     * @param cBase the offset of the column's first valid sample
     * @param lResp the half bandwidth
     * @param x     the input vector array
     * @param xOff  the offset of the input vector
     * @param y     the output array
     * @param yOff  the offset of the output vector
     * @param n     the output length
     */
    private static void multSymToepl2(float[] c, int cBase, int lResp, float[] x, int xOff,
                                      float[] y, int yOff, int n) {
        var idx = 0;
        var len = lResp;
        for (; idx < lResp - 1; idx++) {
            y[yOff + idx] = dotProd(c, cBase + lResp - 1 - idx, x, xOff, len++);
        }
        len = 2 * lResp;
        for (; idx < n - lResp; idx++) {
            y[yOff + idx] = dotProd(c, cBase, x, xOff + idx - lResp + 1, len);
        }
        for (; idx < n; idx++) {
            y[yOff + idx] = dotProd(c, cBase, x, xOff + idx - lResp + 1, --len);
        }
    }

    /**
     * Reverses a window of an array in place.
     *
     * @param x the array
     * @param l the window length, reversed from index zero
     */
    private static void reverse(float[] x, int l) {
        reverse(x, 0, l);
    }

    /**
     * Reverses a window of an array in place.
     *
     * @param x   the array
     * @param off the offset of the first window sample
     * @param l   the window length
     */
    private static void reverse(float[] x, int off, int l) {
        for (var i = 0; i < l / 2; i++) {
            var tmp = x[off + i];
            x[off + i] = x[off + l - i - 1];
            x[off + l - i - 1] = tmp;
        }
    }

    /**
     * Computes the squared magnitude energy of a window.
     *
     * <p>Delegates to {@link #dotProd} so the weighted error floor and the weighted target threshold round
     * exactly as the four lane reduction the dot product uses.
     *
     * @param x   the array
     * @param off the offset of the first window sample
     * @param n   the window length
     * @return the accumulated single precision energy
     */
    private static float nrg(float[] x, int off, int n) {
        return dotProd(x, off, x, off, n);
    }

    /**
     * Computes a single precision dot product over a window of two arrays.
     *
     * <p>The accumulation is a four lane reduction: four partial sums each gather the products at indices
     * congruent to their lane modulo four, are combined as {@code (s0 + s2) + (s1 + s3)}, and the trailing
     * {@code len mod 4} products are added sequentially. The lane order changes the rounding relative to a left
     * to right sum, so it is load bearing for the weighted target and the weighted error floor that drive the
     * search tournaments, and is reproduced here exactly to match the search primitives.
     *
     * @param a    the first array
     * @param aOff the offset into the first array
     * @param b    the second array
     * @param bOff the offset into the second array
     * @param len  the number of elements
     * @return the accumulated single precision dot product
     */
    private static float dotProd(float[] a, int aOff, float[] b, int bOff, int len) {
        var s0 = 0.0f;
        var s1 = 0.0f;
        var s2 = 0.0f;
        var s3 = 0.0f;
        var m = len & ~3;
        var i = 0;
        for (; i < m; i += 4) {
            s0 += a[aOff + i] * b[bOff + i];
            s1 += a[aOff + i + 1] * b[bOff + i + 1];
            s2 += a[aOff + i + 2] * b[bOff + i + 2];
            s3 += a[aOff + i + 3] * b[bOff + i + 3];
        }
        var acc = (s0 + s2) + (s1 + s3);
        for (; i < len; i++) {
            acc += a[aOff + i] * b[bOff + i];
        }
        return acc;
    }

    /**
     * Pi as the single precision constant the Hanning argument is formed with.
     */
    private static final float SMPL_PI = 3.1415926535897f;

    /**
     * Builds the Hanning impulse response window.
     *
     * <p>Entry {@code i} is {@code sin(SMPL_PI * (percRespLen + i + 1) * scale)} with
     * {@code scale == 1.0f / (2 * HR_PERC_RESP_LEN + 1)}, the multiplicative argument formed entirely in single
     * precision (left to right, {@code SMPL_PI} a {@code float}) and the sine then taken in single precision. The
     * argument is held in a {@code float} before the sine so its rounding matches the reference, and the single
     * precision sine is reproduced by rounding {@link StrictMath#sin(double)} of that argument back to
     * {@code float}.
     *
     * @implNote This implementation forms the argument in {@code float} and uses {@link StrictMath#sin(double)}
     * rather than {@link Math#sin(double)} on a {@code double} argument. Computing the argument in {@code double}
     * (with {@link Math#PI}) or taking a {@code double} sine diverges on several entries of the covered scope,
     * and the weighted synthesis impulse response, hence the whole analysis by synthesis search, carries that
     * difference into the bitstream.
     *
     * @param percRespLen the perceptual response length
     * @return a freshly allocated {@code percRespLen} entry window
     */
    private static float[] buildHanningWindow(int percRespLen) {
        var win = new float[percRespLen];
        var scale = 1.0f / (2 * HR_PERC_RESP_LEN + 1);
        for (var i = 0; i < percRespLen; i++) {
            var arg = SMPL_PI * (percRespLen + i + 1) * scale;
            win[i] = (float) StrictMath.sin((double) arg);
        }
        return win;
    }

    /**
     * Seeds the per encoder random pulse signatures.
     *
     * <p>Packs {@code reps} draws of a linear congruential generator (each {@code nBitsRand} bits) into each
     * 64 bit signature, where {@code nBitsRand} is 15 bits and {@code reps} is four draws per signature. The
     * generator is {@code seed = seed * RAND_MULT + RAND_INC} returning bits 16 to 30, seeded at {@code 1}, so
     * the signature table is deterministic and matches the value the delayed decision fixed codebook dedup
     * expects.
     *
     * @return a freshly allocated {@value #MAX_SF_LEN} entry signature table
     */
    private static long[] seedSignatures() {
        var table = new long[MAX_SF_LEN];
        var nBitsRand = 15;
        var reps = 64 / nBitsRand;
        var seed = 1L;
        for (var i = 0; i < MAX_SF_LEN; i++) {
            seed = seed * RAND_MULT + RAND_INC;
            var tmp = (seed >>> 16) & RAND_MAX;
            for (var r = 1; r < reps; r++) {
                seed = seed * RAND_MULT + RAND_INC;
                tmp <<= nBitsRand;
                tmp += (seed >>> 16) & RAND_MAX;
            }
            table[i] = tmp;
        }
        return table;
    }
}
