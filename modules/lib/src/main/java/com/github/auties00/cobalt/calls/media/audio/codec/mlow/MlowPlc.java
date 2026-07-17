package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf.LpcInterpolator;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.MiscTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * The MLow packet loss concealment (PLC) and comfort noise generation (CNG) state machine for the low band
 * of the {@code smpl} decoder.
 *
 * <p>The concealment subsystem serves two roles that thread across every packet of a decode. On a good frame
 * it maintains the recovery state the next lost frame would extrapolate from: the last good LPC filter and
 * line spectral frequencies, the pitch lag, running rings of recent adaptive codebook gains and residual
 * energies, and a small ring of low energy background noise candidates for comfort noise. On a lost frame it
 * fabricates a plausible continuation: {@link #concealCelp} repeats the last pitch cycle with a gradually
 * lowered gain and drifting lag, {@link #decayExc} injects shaped noise on voiced excitation or mutes
 * unvoiced excitation, {@link #blendLtp} crossfades the adaptive codebook history to hide lag instability,
 * and {@link #addComfortNoise} overlays a spectrally matched noise floor. {@link #bweRecover} and
 * {@link #adaptLsf} smooth the first good frame after a loss burst.
 *
 * <p>All state is single writer per decode stream, held on one instance constructed alongside the
 * {@link MlowDecoder} it serves. The scope is the 16 kHz low band; the high band ({@code fs > 16000})
 * concealment and the silence descriptor (SID/DTX) concealment paths are not handled here, matching the
 * {@link MlowDecoder} low band scope.
 *
 * @implNote This implementation reproduces the float accumulation order of the concealment kernels through
 * the shared {@link Filters} kernels and the small per class copies here ({@link #nrg}, {@link #getEnv},
 * {@link #sigmoid}, {@link #bweExpand}, {@link #genRandPulses}). The concealment output is a plausible
 * continuation synthesis validated by signal to noise ratio rather than bit for bit, within the float
 * tolerance carried from the synthesis and filter stages.
 */
public final class MlowPlc {
    /** The logger for {@link MlowPlc}. */
    private static final System.Logger LOGGER = Log.get(MlowPlc.class);

    /** Linear prediction order of the low band short term filter. */
    private static final int LPC_ORDER = 16;

    /** Maximum pitch lag in samples, {@code 20 ms * 16 kHz}. */
    private static final int MAX_PITCH_LAG = 320;

    /** Minimum pitch lag in samples, {@code 2 ms * 16 kHz}. */
    private static final int MIN_PITCH_LAG = 32;

    /** Minimum subframe length in samples, {@code 5 ms * 16 kHz}. */
    private static final int MIN_SF_LEN = 80;

    /** Maximum subframe length in samples, {@code 10 ms * 16 kHz}. */
    private static final int MAX_SF_LEN = 160;

    /** Length of one pitch lag subframe in samples. */
    private static final int LAG_SUBFRLEN = 40;

    /** Entries per adaptive codebook gain vector. */
    private static final int ACBG_M = 2;

    /** Half width of the fractional lag interpolation kernel. */
    private static final int LTP_INTERPOL_DELAY = 8;

    /** Total postfilter delay in samples, {@code 8 + 40}. */
    private static final int TOT_POSTFILT_DELAY = 48;

    /** Comfort noise candidate ring size. */
    private static final int CNG_NO_CANDIDATES = 3;

    /** Low band selector. */
    static final int BAND_LB = 0;

    /** The loss flag value marking a concealed frame. */
    static final int FLAG_PACKET_LOST = 1;

    /** Voiced LPC bandwidth expansion base. */
    private static final float PLC_BWE_V = 0.995f;

    /** Unvoiced LPC bandwidth expansion base. */
    private static final float PLC_BWE_UV = 0.95f;

    /** Per subframe pitch lag drift multiplier. */
    private static final float PLC_LAG_DRIFT = 1.01f;

    /** Unvoiced excitation attenuation. */
    private static final float PLC_EXC_ATTEN = 0.90f;

    /** Adaptive codebook center tap clamp minimum. */
    private static final float PLC_ACB_MIN = 0.7f;

    /** Adaptive codebook center tap clamp maximum. */
    private static final float PLC_ACB_MAX = 0.95f;

    /** LTP blend sigmoid slope, {@code -0.2f * LAG_SUBFRLEN}. */
    private static final float PLC_BLEND_SHAPE = -0.2f * LAG_SUBFRLEN;

    /** LTP blend sigmoid offset, {@code 75.0f / LAG_SUBFRLEN}. */
    private static final float PLC_BLEND_OFFSET = 75.0f / LAG_SUBFRLEN;

    /** Comfort signal ratio threshold to adapt from CNG rather than PLC LSFs. */
    private static final float COMFORT_SIGNAL_THRESHOLD = 100.0f;

    /** Early attenuation window in milliseconds. */
    private static final int PLC_EARLY_ATTEN_MS = 40;

    /** Early attenuation alpha power. */
    private static final float PLC_EARLY_ATTEN_POW = 3.0f;

    /** Early attenuation floor gain. */
    private static final float PLC_EARLY_ATTEN_GAIN = 0.99f;

    /** Late attenuation sigmoid scale. */
    private static final float PLC_LATE_ATTEN_SCALE = 1.0f;

    /** Late attenuation sigmoid shape. */
    private static final float PLC_LATE_ATTEN_SHAPE = 0.5f;

    /** Late attenuation sigmoid offset. */
    private static final float PLC_LATE_ATTEN_OFFSET = 10.0f;

    /** Minimum bandwidth expansion applied during recovery. */
    private static final float RECOVER_MIN_BWE = 0.95f;

    /** Recovery measurement window length in milliseconds. */
    private static final int RECOVER_LEN_MS = 60;

    /** Envelope smoother coefficient for voiced noise injection. */
    private static final float PLC_INJECT_SMTH = 0.999f;

    /** Voiced noise injection gain. */
    private static final float PLC_INJECT_GAIN = 0.8f;

    /** Impulse length for the recovery energy measurement. */
    private static final int PLC_IMP_LEN = 12;

    /** Pseudo random generator multiplier. */
    private static final int RAND_MULTIPLIER = 196314165;

    /** Pseudo random generator increment. */
    private static final int RAND_INCREMENT = 907633515;

    /** Pseudo random pulse output scale. */
    private static final float RAND_SCALE = 8.1e-10f;

    /** The two tap noise injection high pass coefficients. */
    private static final float[] PLC_INJECT_COEF = MiscTables.PLC_INJECT_COEF;

    /** The two tap comfort noise emphasis coefficients. */
    private static final float[] CNG_EMPH_COEF = MiscTables.CNG_EMPH_COEF;

    /** The fractional lag interpolation kernel. */
    private static final float[] INTERPOL_KERNEL = MiscTables.INTERPOL_KERNEL;

    /** The initial comfort noise LSF vector. */
    private static final float[] PLC_CNG_INIT = MiscTables.PLC_CNG_INIT;

    /** Count of consecutive concealed subframes since the last good frame. */
    private int lossCountSubfr;

    /** Whether the previous frame was a concealed (lost, non SID) frame. */
    private boolean isPlcFramePrev;

    /** Whether the last received packet was a silence descriptor. */
    private boolean tocSid;

    /** LCG seed for the voiced excitation noise injection PRNG. */
    private int randSeed;

    /** Envelope smoother carry state for the voiced noise injection. */
    private float smthState;

    /** Ring of recent limited adaptive codebook gains, one per fcb subframe. */
    private final float[] acbBuf = new float[2 * MAX_PITCH_LAG / MIN_SF_LEN];

    /** Ring of recent per subframe residual energies. */
    private final float[] excNrgBuf = new float[MAX_PITCH_LAG / MIN_SF_LEN];

    /** Ring of recent pitch lags at 2.5 ms granularity. */
    private final float[] lagBuf = new float[2 * MAX_PITCH_LAG / LAG_SUBFRLEN];

    /** Length of the current uninterrupted packet loss burst, in milliseconds. */
    private int lossLenMs;

    /** Energy ratio of comfort noise over signal, tracked for the recovery LSF adaptation. */
    private float comfSigRatio;

    /** Milliseconds elapsed since recovery began. */
    private int recoveryLenMs;

    /** Last good LPC filter, saved for the recovery impulse energy measurement. */
    private final float[] aLast = new float[LPC_ORDER + 1];

    /** Voicing flag of the last good frame. */
    private boolean voicedLast;

    /** Impulse response energy of {@link #aLast}. */
    private float nrgLast;

    /** Voicing flag copied from the last good low band params. */
    private boolean voiced;

    /** Last residual energy quant value in Q14. */
    private int lastNrgresQ14;

    /** Last per subframe pulse count. */
    private int lastSubfrPulses;

    /** Last fixed codebook gain index. */
    private int lastFcbIdx;

    /** Last good LSF vector. */
    private final float[] lsf = new float[LPC_ORDER];

    /** Last good LPC filter, bandwidth expanded per concealed subframe. */
    private final float[] a = new float[LPC_ORDER + 1];

    /** Subsample pitch lag, drifted upward each concealed subframe. */
    private float preciseLag;

    /** Running multiplicative attenuation applied to unvoiced excitation during loss. */
    private float excAttenuation;

    /** Low band comfort noise AR synthesis filter memory. */
    private final float[] cngStateLb = new float[LPC_ORDER];

    /** Ring of comfort noise candidate low band LSF vectors. */
    private final float[][] cngLsfsLb = new float[CNG_NO_CANDIDATES][LPC_ORDER];

    /** Per candidate low band energy. */
    private final float[] cngNrgLb = new float[CNG_NO_CANDIDATES];

    /** Per candidate whole frame energy. */
    private final float[] cngFrameNrg = new float[CNG_NO_CANDIDATES];

    /** Write index into the candidate ring. */
    private int cngCandidatesTail;

    /** Number of valid candidates so far. */
    private int cngNumCandidates;

    /** Index of the currently selected lowest energy candidate. */
    private int cngBestCandidateIdx;

    /** LCG seed for the comfort noise PRNG. */
    private int cngRandSeed;

    /** Low band comfort noise delay line. */
    private final float[] cngLbDelayBuf = new float[TOT_POSTFILT_DELAY];

    /** Deemphasis carry state for the comfort noise energy measurement. */
    private final float[] cngStateEmph = new float[1];

    /**
     * Constructs a concealment state machine primed with a low level comfort noise candidate.
     */
    public MlowPlc() {
        cngFrameNrg[0] = 1e-8f;
        cngNrgLb[0] = 1e-10f;
        System.arraycopy(PLC_CNG_INIT, 0, cngLsfsLb[0], 0, LPC_ORDER);
        cngBestCandidateIdx = 0;
        cngCandidatesTail = 1;
        cngNumCandidates = 1;
        lastNrgresQ14 = -90 * (1 << 14);
        tocSid = false;
        excAttenuation = 1.0f;
    }

    /**
     * Resets the per packet concealment counters when a good packet is received.
     *
     * @param sid whether the received packet is a silence descriptor
     */
    public void reset(boolean sid) {
        lossCountSubfr = 0;
        tocSid = sid;
    }

    /**
     * Returns this concealment state to its freshly constructed state across a stream discontinuity.
     */
    public void resetStream() {
        lossCountSubfr = 0;
        isPlcFramePrev = false;
        tocSid = false;
        randSeed = 0;
        smthState = 0.0f;
        Arrays.fill(acbBuf, 0.0f);
        Arrays.fill(excNrgBuf, 0.0f);
        Arrays.fill(lagBuf, 0.0f);
        lossLenMs = 0;
        comfSigRatio = 0.0f;
        recoveryLenMs = 0;
        Arrays.fill(aLast, 0.0f);
        voicedLast = false;
        nrgLast = 0.0f;
        voiced = false;
        lastSubfrPulses = 0;
        lastFcbIdx = 0;
        Arrays.fill(lsf, 0.0f);
        Arrays.fill(a, 0.0f);
        preciseLag = 0.0f;
        excAttenuation = 1.0f;
        Arrays.fill(cngStateLb, 0.0f);
        Arrays.fill(cngNrgLb, 0.0f);
        Arrays.fill(cngFrameNrg, 0.0f);
        for (var row : cngLsfsLb) {
            Arrays.fill(row, 0.0f);
        }
        Arrays.fill(cngLbDelayBuf, 0.0f);
        cngStateEmph[0] = 0.0f;
        cngRandSeed = 0;
        // prime the initial comfort noise candidate as a fresh construction does
        cngFrameNrg[0] = 1e-8f;
        cngNrgLb[0] = 1e-10f;
        System.arraycopy(PLC_CNG_INIT, 0, cngLsfsLb[0], 0, LPC_ORDER);
        cngBestCandidateIdx = 0;
        cngCandidatesTail = 1;
        cngNumCandidates = 1;
        lastNrgresQ14 = -90 * (1 << 14);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls mlow plc: stream state reset");
    }

    /**
     * Returns the last good frame's voicing, the packet level voicing a concealed packet inherits.
     *
     * @return whether the last good frame was voiced
     */
    public boolean voiced() {
        return voiced;
    }

    /**
     * Records the recovery state a future concealed frame extrapolates from. Called on every decoded good
     * frame.
     *
     * @param frameVoiced    whether the frame is voiced
     * @param lastNrgresDbq  the last subframe's residual energy Q14 value
     * @param lastPulses     the last subframe's pulse count
     * @param lastFcbGainIdx the last subframe's fixed codebook gain index
     * @param acbGains       the per subframe adaptive codebook gains, {@code numSubframes * ACBG_M} entries
     * @param lastA          the last subframe's LPC filter, {@code LPC_ORDER + 1} entries
     * @param lastLsf        the last subframe's LSF vector, {@code LPC_ORDER} entries
     * @param lags           the frame's pitch lags, {@code lagsPerFrame} entries
     * @param lagsPerFrame   the number of lag subframes per frame
     * @param numSubframes   the number of subframes in the frame
     * @param fcbSubfrlen    the subframe length in samples
     */
    public void updateCelp(boolean frameVoiced, int lastNrgresDbq, int lastPulses, int lastFcbGainIdx,
                           float[] acbGains, float[] lastA, float[] lastLsf, float[] lags, int lagsPerFrame,
                           int numSubframes, int fcbSubfrlen) {
        voiced = frameVoiced;
        lastNrgresQ14 = lastNrgresDbq;
        lastSubfrPulses = lastPulses;
        lastFcbIdx = lastFcbGainIdx;
        System.arraycopy(lastLsf, 0, lsf, 0, LPC_ORDER);
        System.arraycopy(lastA, 0, a, 0, LPC_ORDER + 1);
        preciseLag = lags[lagsPerFrame - 1];

        if (lossCountSubfr > 0 || tocSid) {
            var numElems = MAX_PITCH_LAG / fcbSubfrlen;
            Arrays.fill(acbBuf, 0, numElems, 0.0f);
            Arrays.fill(excNrgBuf, 0, numElems, 0.0f);
            Arrays.fill(lagBuf, 0, 2 * numElems, 0.0f);
        }

        var bufLeft = 2 * MAX_PITCH_LAG / LAG_SUBFRLEN - lagsPerFrame;
        System.arraycopy(lagBuf, lagsPerFrame, lagBuf, 0, bufLeft);
        System.arraycopy(lags, 0, lagBuf, bufLeft, lagsPerFrame);

        var toAdd = Math.min(numSubframes, MAX_PITCH_LAG / fcbSubfrlen);
        bufLeft = MAX_PITCH_LAG / fcbSubfrlen - toAdd;
        System.arraycopy(acbBuf, toAdd, acbBuf, 0, bufLeft);
        for (var i = 0; i < toAdd; i++) {
            acbBuf[bufLeft + i] = limitAcbgains(acbGains, i * ACBG_M, fcbSubfrlen);
        }
    }

    /**
     * Slides the residual energy ring forward by one subframe. Called on every subframe of a good frame
     * during synthesis.
     *
     * @param resNrg   the subframe's excitation energy
     * @param subfrlen the subframe length in samples
     */
    public void updateNrg(float resNrg, int subfrlen) {
        var len = MAX_PITCH_LAG / subfrlen;
        System.arraycopy(excNrgBuf, 1, excNrgBuf, 0, len - 1);
        excNrgBuf[len - 1] = resNrg;
    }

    /**
     * Decays or noise injects one subframe's excitation during a loss, and resets the attenuation on a good
     * frame.
     *
     * @param resLpc    the excitation buffer
     * @param off       the offset of the subframe within {@code resLpc}
     * @param subfrlen  the subframe length in samples
     * @param reset     whether to reset the attenuation (a good frame) rather than decay (a lost frame)
     * @param subVoiced whether the excitation is voiced
     */
    public void decayExc(float[] resLpc, int off, int subfrlen, boolean reset, boolean subVoiced) {
        if (reset) {
            excAttenuation = 1.0f;
            return;
        }
        if (subVoiced) {
            var tgtNrg = nrg(resLpc, off, subfrlen);
            var noise = new float[subfrlen];
            genRandPulses(noise, subfrlen);

            // modulate the random pulses by the excitation envelope: the envelope times noise product lands in
            // scratch, then the moving average high pass filters that product back into noise
            var scratch = new float[subfrlen];
            getEnv(resLpc, off, subfrlen, PLC_INJECT_SMTH, scratch);
            for (var i = 0; i < subfrlen; i++) {
                scratch[i] *= noise[i];
            }

            var tempState = new float[]{0.0f};
            Filters.ma1(scratch, 0, subfrlen, PLC_INJECT_COEF, tempState, 0, noise, 0);
            var nrgRatio = (float) Math.sqrt(tgtNrg / (nrg(noise, 0, subfrlen) + 1.0e-30f));
            var scale = nrgRatio * PLC_INJECT_GAIN;
            for (var i = 0; i < subfrlen; i++) {
                noise[i] *= scale;
                resLpc[off + i] += noise[i];
            }
        } else {
            excAttenuation *= subfrlen == MIN_SF_LEN ? PLC_EXC_ATTEN : PLC_EXC_ATTEN * PLC_EXC_ATTEN;
            for (var i = 0; i < subfrlen; i++) {
                resLpc[off + i] *= excAttenuation;
            }
        }
    }

    /**
     * Fabricates the concealed low band parameters of one lost frame from the recovery state. Fills the per
     * subframe LPC filters, LSF vectors, adaptive codebook gains, and pitch lags the synthesis loop then
     * renders.
     *
     * @param aOut         the per subframe LPC output, {@code numSubframes * (LPC_ORDER + 1)} entries
     * @param lsfsOut      the per subframe LSF output, {@code numSubframes * LPC_ORDER} entries
     * @param acbGainsOut  the per subframe adaptive codebook gain output, {@code numSubframes * ACBG_M} entries
     * @param lagsOut      the frame's pitch lags, one per lag subframe
     * @param numSubframes the number of subframes in the frame
     * @param subfrlen     the subframe length in samples
     * @return the concealed voicing and per subframe residual energy, pulse, and gain index parameters
     */
    public ConcealedParams concealCelp(float[] aOut, float[] lsfsOut, float[] acbGainsOut, float[] lagsOut,
                                       int numSubframes, int subfrlen) {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls mlow plc: concealing frame, subframes={0} voiced={1} lossCount={2}",
                    numSubframes, voiced, lossCountSubfr);
        }
        var lenBuffers = MAX_PITCH_LAG / subfrlen;
        var nPulses = lastSubfrPulses * numSubframes;
        var subfrlenComp = (float) subfrlen / MIN_SF_LEN;
        var lagsPerSubframe = subfrlen / LAG_SUBFRLEN;

        for (var i = 0; i < numSubframes; i++) {
            lossCountSubfr += 1;

            bweExpand(a, 0, LPC_ORDER, (float) Math.pow(voiced ? PLC_BWE_V : PLC_BWE_UV, subfrlenComp));
            aOut[i * (LPC_ORDER + 1)] = 1.0f;
            System.arraycopy(a, 1, aOut, i * (LPC_ORDER + 1) + 1, LPC_ORDER);
            System.arraycopy(lsf, 0, lsfsOut, i * LPC_ORDER, LPC_ORDER);

            preciseLag = Math.min(preciseLag * (float) Math.pow(PLC_LAG_DRIFT, subfrlenComp), MAX_PITCH_LAG);
            var lagRounded = roundTo(preciseLag, 0.5f, 0.0f);
            for (var j = 0; j < lagsPerSubframe; j++) {
                lagsOut[i * lagsPerSubframe + j] = lagRounded;
            }

            if (voiced) {
                var preciseLagSpan = (int) Math.ceil(preciseLag / (float) subfrlen);
                var highestNrgIdx = 0;
                var highestNrg = -1.0f;
                for (var j = lenBuffers - preciseLagSpan; j < lenBuffers; j++) {
                    if (excNrgBuf[j] > highestNrg) {
                        highestNrg = excNrgBuf[j];
                        highestNrgIdx = j;
                    }
                }
                var gain = acbBuf[highestNrgIdx];
                var lateAtten = PLC_LATE_ATTEN_SCALE
                                * sigmoid((-lossCountSubfr + PLC_LATE_ATTEN_OFFSET) * PLC_LATE_ATTEN_SHAPE)
                                + (1.0f - PLC_LATE_ATTEN_SCALE);
                gain *= (float) Math.pow(lateAtten, subfrlenComp);
                if (lossCountSubfr <= PLC_EARLY_ATTEN_MS * 16 / subfrlen) {
                    var alpha = (float) lossCountSubfr * subfrlen / (PLC_EARLY_ATTEN_MS * 16.0f);
                    alpha = (float) Math.pow(alpha, PLC_EARLY_ATTEN_POW);
                    gain = (float) Math.pow(PLC_EARLY_ATTEN_GAIN, subfrlenComp) * (1 - alpha) + gain * alpha;
                }
                acbGainsOut[i * ACBG_M] = gain;
                for (var j = 1; j < ACBG_M; j++) {
                    acbGainsOut[i * ACBG_M + j] = 0.0f;
                }
            }
        }
        return new ConcealedParams(voiced, nPulses, lastNrgresQ14, lastSubfrPulses, lastFcbIdx);
    }

    /**
     * Crossfades the adaptive codebook history to hide pitch lag instability at the start of a concealment.
     * Performs no work when the last good frame was unvoiced.
     *
     * @param acbState the decoder's adaptive codebook history ring
     * @param lag      the concealed pitch lag of the first subframe
     */
    public void blendLtp(float[] acbState, float lag) {
        if (!voiced) {
            return;
        }
        var acbStateLen = 2 * MAX_PITCH_LAG + LTP_INTERPOL_DELAY;
        var acbEnd = acbStateLen;

        var ceilLag = (int) Math.ceil(lag);
        var pitchCycle1 = acbEnd - ceilLag;
        var pitchCycle2 = new float[MAX_PITCH_LAG];
        var doubleLag = (int) (2.0f * lag);
        if ((float) Math.floor(lag) == lag) {
            System.arraycopy(acbState, acbEnd - doubleLag, pitchCycle2, 0, (int) lag);
        } else {
            Filters.interpol(acbState, acbEnd - doubleLag + 1 - LTP_INTERPOL_DELAY, pitchCycle2, 0, ceilLag,
                    INTERPOL_KERNEL);
        }

        var lookback = 2.0f * lag / LAG_SUBFRLEN;
        var lagInstability = 0.0f;
        var lagBufLen = 2 * MAX_PITCH_LAG / LAG_SUBFRLEN;
        for (var i = 0; i < (int) Math.floor(lookback); i++) {
            lagInstability += Math.abs(lagBuf[lagBufLen - 1 - i] - lag);
        }
        lagInstability += Math.abs(lagBuf[lagBufLen - (int) Math.ceil(lookback)] - lag)
                * (lookback - (float) Math.floor(lookback));

        var ltpBlendCoef = sigmoid(PLC_BLEND_SHAPE * (lagInstability - PLC_BLEND_OFFSET)) / 2.0f;
        for (var i = 0; i < ceilLag; i++) {
            acbState[acbStateLen - ceilLag + i] =
                    ltpBlendCoef * pitchCycle2[i] + (1.0f - ltpBlendCoef) * acbState[pitchCycle1 + i];
        }
    }

    /**
     * Tracks the length of the current packet loss burst. Called once per packet.
     *
     * @param lostFlag    the loss flag of the packet
     * @param packetLenMs the packet length in milliseconds
     */
    public void updateLossInfo(int lostFlag, int packetLenMs) {
        if (lostFlag == FLAG_PACKET_LOST && !tocSid) {
            lossLenMs += packetLenMs;
        } else {
            lossLenMs = 0;
        }
    }

    /**
     * Adapts the previous frame LSF vector toward the concealment or comfort noise spectrum on the first good
     * frame after a loss.
     *
     * @param lsfPrev  the previous frame LSF vector to adapt in place
     * @param lpcOrder the linear prediction order
     */
    public void adaptLsf(float[] lsfPrev, int lpcOrder) {
        if (lossLenMs > 0) {
            if (comfSigRatio > COMFORT_SIGNAL_THRESHOLD) {
                System.arraycopy(cngLsfsLb[cngBestCandidateIdx], 0, lsfPrev, 0, lpcOrder);
            } else {
                System.arraycopy(lsf, 0, lsfPrev, 0, lpcOrder);
            }
        }
    }

    /**
     * Records the last good LPC filter and voicing for the recovery bandwidth expansion measurement. Called
     * when a frame is concealed.
     *
     * @param frameVoiced whether the concealed frame is voiced
     */
    public void updateRecoveryInfo(boolean frameVoiced) {
        System.arraycopy(a, 0, aLast, 0, LPC_ORDER + 1);
        voicedLast = frameVoiced;
        recoveryLenMs = 0;
    }

    /**
     * Smooths the spectral envelope of the first good voiced frames after a voiced loss burst. Runs only on
     * voiced to voiced transitions.
     *
     * @param frameVoiced  whether the current good frame is voiced
     * @param a            the current frame's per subframe LPC filters to bandwidth expand in place
     * @param numSubframes the number of subframes
     * @param framelenMs   the frame length in milliseconds
     */
    public void bweRecover(boolean frameVoiced, float[][] a, int numSubframes, int framelenMs) {
        if (!(frameVoiced && voicedLast)) {
            return;
        }
        var lostPrevious = lossLenMs > 0;
        var recoveryCont = recoveryLenMs > 0 && recoveryLenMs < RECOVER_LEN_MS;
        if (lostPrevious) {
            var impulse = new float[PLC_IMP_LEN + LPC_ORDER];
            impulse[LPC_ORDER] = 1.0f;
            Filters.ar16(impulse, LPC_ORDER, PLC_IMP_LEN, aLast, impulse, LPC_ORDER);
            nrgLast = nrg(impulse, LPC_ORDER, PLC_IMP_LEN);
        }
        if (lostPrevious || recoveryCont) {
            var impulse = new float[PLC_IMP_LEN + LPC_ORDER];
            impulse[LPC_ORDER] = 1.0f;
            Filters.ar16(impulse, LPC_ORDER, PLC_IMP_LEN, a[numSubframes - 1], impulse, LPC_ORDER);
            var postGain = nrg(impulse, LPC_ORDER, PLC_IMP_LEN);

            var logRatio = 0.5f * (float) Math.log10(postGain / (nrgLast + 1e-12f));
            var bwe = 1.0f - (1.0f - RECOVER_MIN_BWE) * Math.max(Math.min(logRatio, 1.0f), 0.0f);
            for (var s = 0; s < numSubframes; s++) {
                bweExpand(a[s], 0, LPC_ORDER, bwe);
            }
            recoveryLenMs += framelenMs;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "calls mlow plc: recovery smoothing, recoveryLenMs={0}", recoveryLenMs);
            }
        }
    }

    /**
     * Records a background noise candidate on an unvoiced frame for later comfort noise generation, low band
     * path. Called once per frame.
     *
     * @param y            the frame's synthesized output
     * @param yOff         the offset of the frame within {@code y}
     * @param frameVoiced  whether the frame is voiced
     * @param vad          whether the packet is voice active
     * @param numSubframes the number of subframes in the frame
     * @param subfrlen     the subframe length in samples
     * @param nrgres       the per subframe linear residual energy, {@code numSubframes} entries
     * @param lsfs         the per subframe LSF vectors, {@code numSubframes * LPC_ORDER} entries
     */
    public void updateCng(float[] y, int yOff, boolean frameVoiced, boolean vad, int numSubframes, int subfrlen,
                          float[] nrgres, float[] lsfs) {
        if (frameVoiced) {
            cngStateEmph[0] = y[yOff + numSubframes * subfrlen - 1];
            return;
        }

        var subfrIdx = 0;
        var lowestNrgSubfr = 1e30f;
        var nrgFrame = 0.0f;
        var yEmph = new float[subfrlen];
        for (var i = 0; i < numSubframes; i++) {
            Filters.ma1(y, yOff + i * subfrlen, subfrlen, CNG_EMPH_COEF, cngStateEmph, 0, yEmph, 0);
            var nrgSubfr = nrg(yEmph, 0, subfrlen);
            nrgFrame += nrgSubfr;
            if (nrgSubfr < lowestNrgSubfr) {
                subfrIdx = i;
                lowestNrgSubfr = nrgSubfr;
            }
        }

        if (!vad) {
            var tail = cngCandidatesTail;
            cngFrameNrg[tail] = nrgFrame;
            cngNrgLb[tail] = nrgres[subfrIdx] / (float) subfrlen;
            System.arraycopy(lsfs, LPC_ORDER * subfrIdx, cngLsfsLb[tail], 0, LPC_ORDER);
            tail = (tail + 1) % CNG_NO_CANDIDATES;
            cngCandidatesTail = tail;
            cngNumCandidates = Math.min(cngNumCandidates + 1, CNG_NO_CANDIDATES);

            var candidateLowestNrg = 1e30f;
            var bestIdx = 0;
            for (var i = 0; i < cngNumCandidates; i++) {
                if (cngFrameNrg[i] < candidateLowestNrg) {
                    candidateLowestNrg = cngFrameNrg[i];
                    bestIdx = i;
                }
            }
            cngBestCandidateIdx = bestIdx;
        }
    }

    /**
     * Carries the comfort noise deemphasis state across a concealed frame.
     *
     * <p>On a concealed frame no background noise candidate is recorded, but the emphasis filter state is
     * advanced to the frame's last synthesized sample so the next good frame's candidate energy measurement
     * continues from the right value rather than from stale pre loss state.
     *
     * @param lastSample the last sample of the concealed frame's synthesis
     */
    public void carryConcealStateEmph(float lastSample) {
        cngStateEmph[0] = lastSample;
    }

    /**
     * Overlays the spectrally matched comfort noise floor on the low band output during and just after a
     * loss, low band path. Called once per packet after the harmonic postfilter. Performs no work when
     * neither the current nor previous frame is concealed.
     *
     * @param y        the packet's output buffer
     * @param yOff     the offset of the packet within {@code y}
     * @param yLen     the packet length in samples
     * @param lostFlag the loss flag of the packet
     */
    public void addComfortNoise(float[] y, int yOff, int yLen, int lostFlag) {
        var isPlcFrame = lostFlag == FLAG_PACKET_LOST && !tocSid;

        if (isPlcFrame != isPlcFramePrev && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "calls mlow plc: comfort noise {0} -> {1}", isPlcFramePrev, isPlcFrame);
        }

        if (isPlcFrame || isPlcFramePrev) {
            var noiseBuf = new float[yLen + LPC_ORDER];
            var noiseOff = LPC_ORDER;
            if (isPlcFrame) {
                var filterA = new float[LPC_ORDER + 1];
                nlsf2aStabilize(cngLsfsLb[cngBestCandidateIdx], filterA);
                var scale = (float) Math.sqrt(cngNrgLb[cngBestCandidateIdx] + 1e-30f);
                genRandPulsesCng(noiseBuf, noiseOff, yLen);
                for (var i = 0; i < yLen; i++) {
                    noiseBuf[noiseOff + i] *= scale;
                }
                System.arraycopy(cngStateLb, 0, noiseBuf, noiseOff - LPC_ORDER, LPC_ORDER);
                Filters.ar16(noiseBuf, noiseOff, yLen, filterA, noiseBuf, noiseOff);
                System.arraycopy(noiseBuf, noiseOff + yLen - LPC_ORDER, cngStateLb, 0, LPC_ORDER);
            } else {
                Arrays.fill(noiseBuf, noiseOff, noiseOff + yLen, 0.0f);
            }

            var noiseOut = new float[yLen];
            System.arraycopy(cngLbDelayBuf, 0, noiseOut, 0, TOT_POSTFILT_DELAY);
            System.arraycopy(noiseBuf, noiseOff, noiseOut, TOT_POSTFILT_DELAY, yLen - TOT_POSTFILT_DELAY);
            System.arraycopy(noiseBuf, noiseOff + yLen - TOT_POSTFILT_DELAY, cngLbDelayBuf, 0, TOT_POSTFILT_DELAY);

            if (lossLenMs == 0) {
                for (var i = 0; i < TOT_POSTFILT_DELAY; i++) {
                    noiseOut[TOT_POSTFILT_DELAY + i] *= (float) i * (1.0f / TOT_POSTFILT_DELAY);
                }
            }
            if (lossLenMs > 0 && (lostFlag != FLAG_PACKET_LOST || tocSid)) {
                for (var i = 0; i < TOT_POSTFILT_DELAY; i++) {
                    noiseOut[i] *= 1.0f - (float) i * (1.0f / TOT_POSTFILT_DELAY);
                }
            }

            comfSigRatio = nrg(noiseOut, 0, yLen) / (nrg(y, yOff, yLen) + 1e-12f);
            for (var i = 0; i < yLen; i++) {
                y[yOff + i] += noiseOut[i];
            }
        } else {
            comfSigRatio = 0.0f;
        }
        isPlcFramePrev = isPlcFrame;
    }

    /**
     * The concealed low band parameters {@link #concealCelp} produces for the synthesis loop.
     *
     * @param voiced       whether the concealed frame is voiced
     * @param nPulses      the frame's total pulse count
     * @param nrgresDbqQ14 the per subframe residual energy Q14 value (identical across subframes)
     * @param sfPulses     the per subframe pulse count (identical across subframes)
     * @param fcbgIdx      the per subframe fixed codebook gain index (identical across subframes)
     */
    public record ConcealedParams(boolean voiced, int nPulses, int nrgresDbqQ14, int sfPulses, int fcbgIdx) {
    }

    /**
     * Returns the clamped adaptive codebook center tap gain the concealment history stores for one subframe.
     *
     * @param acbGains the adaptive codebook gains
     * @param off      the offset of the subframe's gains within {@code acbGains}
     * @param subfrlen the subframe length in samples
     * @return the clamped center tap gain
     */
    private float limitAcbgains(float[] acbGains, int off, int subfrlen) {
        var ctrTap = 0.0f;
        var subfrlenComp = (float) subfrlen / MIN_SF_LEN;
        for (var i = 0; i < ACBG_M; i++) {
            ctrTap += acbGains[off + i] * ((i > 0 ? 1 : 0) + 1);
        }
        ctrTap = Math.abs(ctrTap);
        ctrTap = (float) Math.pow(ctrTap, preciseLag / (subfrlenComp * subfrlen));
        ctrTap = Math.min(Math.max(ctrTap, PLC_ACB_MIN), PLC_ACB_MAX);
        return ctrTap;
    }

    /**
     * Returns the sum of squares of a signal window.
     *
     * @param x   the signal
     * @param off the window offset
     * @param n   the window length
     * @return the energy
     */
    private static float nrg(float[] x, int off, int n) {
        var sum = 0.0f;
        for (var i = 0; i < n; i++) {
            sum += x[off + i] * x[off + i];
        }
        return sum;
    }

    /**
     * Writes the smoothed root energy envelope of an excitation window.
     *
     * @param exc      the excitation signal
     * @param off      the window offset
     * @param len      the window length
     * @param smthCoef the smoothing coefficient
     * @param env      the envelope output, {@code len} entries
     */
    private void getEnv(float[] exc, int off, int len, float smthCoef, float[] env) {
        smthCoef *= smthCoef;
        var state = smthState + 1e-8f;
        state *= state;
        var gainCoef = 1.0f - smthCoef;
        var smthCoef2 = smthCoef * smthCoef;
        var gainSmthCoef = gainCoef * smthCoef;
        for (var i = 0; i < len - 3; i += 4) {
            var tmp0 = exc[off + i] * exc[off + i] + exc[off + i + 1] * exc[off + i + 1];
            var tmp1 = exc[off + i + 2] * exc[off + i + 2] + exc[off + i + 3] * exc[off + i + 3];
            var y1 = gainCoef * tmp1 + gainSmthCoef * tmp0 + smthCoef2 * state;
            var y0 = gainCoef * tmp0 + smthCoef * state;
            env[i] = env[i + 1] = (float) Math.sqrt(y0);
            env[i + 2] = env[i + 3] = (float) Math.sqrt(y1);
            state = y1;
        }
        smthState = env[len - 1];
    }

    /**
     * Fills a buffer with pseudo random pulses from the concealment PRNG.
     *
     * @param out the output buffer
     * @param len the number of pulses
     */
    private void genRandPulses(float[] out, int len) {
        randSeed = genRandPulses(out, 0, len, randSeed);
    }

    /**
     * Fills a buffer with pseudo random pulses from the comfort noise PRNG.
     *
     * @param out the output buffer
     * @param off the output offset
     * @param len the number of pulses
     */
    private void genRandPulsesCng(float[] out, int off, int len) {
        cngRandSeed = genRandPulses(out, off, len, cngRandSeed);
    }

    /**
     * Fills {@code out[off, off+len)} with pseudo random pulses and returns the advanced seed. Four pulses
     * are drawn per seed step from the four bytes of the new seed.
     *
     * @param out  the output buffer
     * @param off  the output offset
     * @param len  the number of pulses
     * @param seed the LCG seed on entry
     * @return the LCG seed after generation
     */
    private static int genRandPulses(float[] out, int off, int len, int seed) {
        var i = 0;
        for (; i < len - 3; i += 4) {
            seed = RAND_INCREMENT + seed * RAND_MULTIPLIER;
            out[off + i] = RAND_SCALE * seed;
            out[off + i + 1] = RAND_SCALE * (seed << 8);
            out[off + i + 2] = RAND_SCALE * (seed << 16);
            out[off + i + 3] = RAND_SCALE * (seed << 24);
        }
        for (; i < len; i++) {
            seed = RAND_INCREMENT + seed * RAND_MULTIPLIER;
            out[off + i] = RAND_SCALE * seed;
        }
        return seed;
    }

    /**
     * Returns the numerically guarded logistic sigmoid.
     *
     * @param x the argument
     * @return the logistic value in {@code [0, 1]}
     */
    private static float sigmoid(float x) {
        if (x > 80.0f) {
            return 1.0f;
        }
        if (x < -80.0f) {
            return 0.0f;
        }
        return (float) (1.0 / (1.0 + Math.exp(-x)));
    }

    /**
     * Bandwidth expands an LPC filter in place.
     *
     * @param filter the LPC coefficients, index zero the monic {@code 1.0f}
     * @param off    the offset of the filter within {@code filter}
     * @param order  the linear prediction order
     * @param bwe    the bandwidth expansion factor
     */
    private static void bweExpand(float[] filter, int off, int order, float bwe) {
        if (bwe <= 0.0f) {
            for (var i = 1; i <= order; i++) {
                filter[off + i] = 0.0f;
            }
            return;
        }
        var c = bwe;
        for (var i = 1; i <= order; i++) {
            filter[off + i] *= c;
            c *= bwe;
        }
    }

    /**
     * Rounds a value to the nearest multiple of a step about an offset.
     *
     * @param x      the value
     * @param step   the quantization step
     * @param offset the quantization offset
     * @return the rounded value
     */
    private static float roundTo(float x, float step, float offset) {
        return step * roundHalfAwayFromZero((x - offset) / step) + offset;
    }

    /**
     * Rounds to the nearest integer, breaking ties away from zero. The JDK {@link Math#round} rounds half up,
     * which diverges on negative ties, so the sign is carried explicitly.
     *
     * @param v the value
     * @return the rounded value
     */
    private static float roundHalfAwayFromZero(float v) {
        return (float) Math.copySign(Math.floor(Math.abs(v) + 0.5f), v);
    }

    /**
     * Converts a stabilized LSF vector to LPC coefficients.
     *
     * @param lsfIn the LSF vector, {@code LPC_ORDER} entries
     * @param aOut  the LPC output, {@code LPC_ORDER + 1} entries with index zero the monic {@code 1.0f}
     */
    private static void nlsf2aStabilize(float[] lsfIn, float[] aOut) {
        var result = LpcInterpolator.nlsf2aStabilize(lsfIn);
        System.arraycopy(result, 0, aOut, 0, LPC_ORDER + 1);
    }
}
