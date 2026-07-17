package com.github.auties00.cobalt.calls.media.audio.codec.mlow.postfilter;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.dsp.Pffft;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.filter.Filters;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Runs the deterministic MLow decode postfilter chain (the harmonic, LPC, and high pass postfilters) over the
 * synthesized low band before the {@code int16} conversion.
 *
 * <p>The MLow byte exact CELP kernel ({@code MlowDecoder}) reproduces the feature dump decode path, which
 * compiles every postfilter out and substitutes a fixed second order high pass; the shipping decoder the live
 * WhatsApp client runs takes the postfilter on branch. That branch, per internal 20 ms frame and in this
 * order, runs an optional LPC postfilter (an {@code A(z/g1)/A(z/g2)} pole zero short term shaper with a
 * nyquist tilt gain), then a pitch adaptive high pass postfilter (a low frequency de emphasis cascade whose
 * resonance tracks the pitch), and after all frames of the packet a harmonic postfilter (a pitch comb that
 * reinforces the periodicity through a pitch tracking low pass). The tilt postfilter is the LPC postfilter's
 * mutually exclusive low rate alternative and, being interleaved with the residual synthesis, is applied by
 * the kernel before this chain, so it is already present in the synthesis handed here. This chain is the
 * signal dependent level lift and harmonic shaping that a flat output gain on the postfilter off kernel only
 * crudely approximates.
 *
 * <p>The entry point is {@link #process(float[], int, float[][][], int, int, float[], float[], boolean,
 * boolean, boolean)}: it consumes the kernel's pre postfilter synthesis for one packet (every internal
 * frame's post {@code ar16}, post tilt samples concatenated, the {@code yBuf}), the per frame and per subframe
 * decode parameters (LPC coefficient sets, pitch lags, voicing, rate), and returns the postfiltered packet in
 * place. State is threaded across the frames of a packet and across packets of a stream: the LPC postfilter's
 * moving average and auto regressive memories, the high pass postfilter's cascade memories and previous pitch
 * lag, and the harmonic postfilter's comb delay line and previous lag all persist between calls. Construct one
 * instance per logical stream and feed it every packet in order; {@link #reset()} returns the chain to its
 * freshly constructed state.
 *
 * <p>Scope is the SMPL 16 kHz, mono, low band path with {@code SMPL_LPC_ORDER == 16}. The high band
 * (32/48 kHz) synthesis the chain also feeds through its nyquist gain output, the comfort noise injection, and
 * the packet loss concealment are out of this chain's responsibility. By default the LPC postfilter is
 * disabled (the {@code SMPL_LPC_PSTF_MODE_WB_OFF_SWB_ON} default disables it for the 16 kHz band), so the low
 * rate tilt postfilter is the active short term shaper and this chain runs the high pass postfilter then the
 * harmonic postfilter; the LPC postfilter path is ported and selected by the {@code lpcPostfilterEnabled}
 * argument for completeness. This type is stateful per stream and is not thread safe.
 *
 * @implNote This implementation threads the three postfilters' state as separate fields: the LPC postfilter
 * carries a {@code SMPL_LPC_ORDER + 1} moving average memory and a {@code SMPL_LPC_ORDER} auto regressive
 * memory; the high pass postfilter carries a low emphasis pre and post pair, a four tap second order memory,
 * the previous pitch lag, and the previous frame input for the lag change overlap add; the harmonic
 * postfilter carries the {@code SMPL_MAXPITCH_LEN + packet + SMPL_HARM_POSTF_DELAY} comb delay line, the
 * {@code 2 * SMPL_HARM_POSTF_FB_DELAY} diff carry, the running low pass comb coefficients, the previous lag,
 * and the filtered flag. The harmonic postfilter applies the lag with a one subframe delay (the lag for
 * subframe {@code i} is the lag decoded for subframe {@code i - 1}) and writes its output back into the
 * synthesis with the {@code SMPL_HARM_POSTF_FB_DELAY} group shift. Every postfilter is reproduced in strict
 * single precision: Java performs no fused multiply add contraction in plain {@code float} arithmetic and
 * evaluates left to right, so the arithmetic is strictly ordered single precision. The residual against the
 * reference decoder is the fast math reassociation of the reference build, well below the {@code int16}
 * quantization, not a structural difference. The LPC postfilter's nyquist tilt gain is computed through the
 * same {@value #LPC_POST_IMPZ_LEN}-point real FFT ({@link Pffft}) as the reference.
 */
public final class MlowDecodePostfilter {
    /**
     * The logger for {@link MlowDecodePostfilter}.
     */
    private static final System.Logger LOGGER = Log.get(MlowDecodePostfilter.class);

    /**
     * The linear prediction order {@code SMPL_LPC_ORDER}.
     */
    private static final int LPC_ORDER = 16;

    /**
     * The internal 20 ms frame length in samples, {@code SMPL_FRAME_LEN} = {@code 16 * 20}.
     */
    private static final int FRAME_LEN = 16 * 20;

    /**
     * The harmonic postfilter comb feedback delay {@code SMPL_HARM_POSTF_FB_DELAY}.
     */
    private static final int HARM_FB_DELAY = HarmonicPostfilterTables.FB_DELAY;

    /**
     * The pitch subframe count over a frame {@code SMPL_PITCH_NUM_SUBFRAMES}.
     */
    private static final int PITCH_NUM_SUBFRAMES = 8;

    /**
     * The harmonic postfilter lag subframe length {@code SMPL_HARM_POSTF_LAG_SUBFR_LEN} =
     * {@code SMPL_FRAME_LEN / SMPL_PITCH_NUM_SUBFRAMES} = {@code 40}.
     */
    private static final int HARM_LAG_SUBFR_LEN = FRAME_LEN / PITCH_NUM_SUBFRAMES;

    /**
     * The harmonic postfilter buffered delay {@code SMPL_HARM_POSTF_DELAY} =
     * {@code SMPL_HARM_POSTF_LAG_SUBFR_LEN} = {@code 40}.
     */
    private static final int HARM_DELAY = HARM_LAG_SUBFR_LEN;

    /**
     * The maximum pitch lag in samples {@code SMPL_MAX_PITCH_LAG} = {@code 320}, equal to
     * {@code SMPL_MAXPITCH_LEN}.
     */
    private static final int MAX_PITCH_LAG = HarmonicPostfilterTables.MAX_PITCH_LAG;

    /**
     * The minimum pitch lag in samples {@code SMPL_MIN_PITCH_LAG} = {@code 32}.
     */
    private static final int MIN_PITCH_LAG = HarmonicPostfilterTables.MIN_PITCH_LAG;

    /**
     * The maximum number of internal frames per packet {@code SMPL_MAX_FRAMES_PER_PACKET}.
     */
    private static final int MAX_FRAMES_PER_PACKET = 6;

    /**
     * The harmonic postfilter feedback strength weight {@code SMPL_HARM_POSTF_FB_STRENGTH}.
     */
    private static final float HARM_FB_STRENGTH = 0.4734f;

    /**
     * The harmonic postfilter overall strength {@code SMPL_HARM_POSTF_STRENGTH}.
     */
    private static final float HARM_STRENGTH = 0.6438f;

    /**
     * The harmonic postfilter high lag reduction factor {@code SMPL_HARM_POSTF_REDUCTION_FAC}.
     */
    private static final float HARM_REDUCTION_FAC = 0.0579f;

    /**
     * The LPC postfilter impulse response length and FFT size {@code SMPL_LPC_POST_IMPZ_LEN}.
     */
    private static final int LPC_POST_IMPZ_LEN = 32;

    /**
     * The LPC postfilter bandwidth expansion gamma table, indexed {@code [lowRate][voiced][stage]}; stage 0 is
     * the numerator {@code A(z/g1)} expansion and stage 1 the denominator {@code A(z/g2)} expansion.
     */
    private static final float[][][] LPC_POSTFILT_GAMMA = {
            {{0.9705f, 0.9833f}, {0.9086f, 0.9900f}},
            {{0.9727f, 0.9691f}, {0.9359f, 0.9025f}}
    };

    /**
     * The high pass postfilter low emphasis coefficients {@code {1.0f, -0.995f}}.
     */
    private static final float[] LO_EMPH_COEF = {1.0f, -0.995f};

    /**
     * The high pass postfilter lag change threshold; a lag ratio beyond it triggers a coefficient overlap add
     * transition.
     */
    private static final float LAG_CHANGE_THRESHOLD = 1.25f;

    /**
     * The high pass postfilter fallback corner frequency in hertz for unvoiced frames
     * {@code SMPL_HP_POSTF_FCORNER_3DB_HZ} = {@code 50}.
     */
    private static final float HP_FCORNER_3DB_HZ = 50.0f;

    /**
     * The high pass postfilter coefficient transition shaping speed {@code SMPL_HP_POSTF_TRANSITION_SPEED} =
     * {@code 2}, the down ramp exponent.
     */
    private static final float HP_TRANSITION_SPEED = 2.0f;

    /**
     * Truncated single precision pi {@code SMPL_PI}.
     */
    private static final float PI = 3.1415926535897f;

    /**
     * The pitch resonance high pass postfilter moving average shape factor for the 1.2 dB peak fit.
     */
    private static final float HP_PITCH_MAF = 0.100f;

    /**
     * The pitch resonance high pass postfilter forward (numerator) fit for the 1.2 dB peak.
     */
    private static final float[] HP_PITCH_ARF = {0.608057355f, 0.070939485f};

    /**
     * The pitch resonance high pass postfilter resonance (denominator) fit for the 1.2 dB peak.
     */
    private static final float[] HP_PITCH_ARR = {-2.187380512f, 2.291030664f};

    /**
     * The fallback (no pitch) high pass postfilter moving average shape factor.
     */
    private static final float HP_FALLBACK_MAF = 0.1f;

    /**
     * The fallback high pass postfilter forward fit.
     */
    private static final float[] HP_FALLBACK_ARF = {0.728508218f, 0.476039848f};

    /**
     * The fallback high pass postfilter resonance fit.
     */
    private static final float[] HP_FALLBACK_ARR = {-4.363803713f, 8.441854006f};

    /**
     * The LPC postfilter moving average memory of {@code SMPL_LPC_ORDER} taps.
     */
    private final float[] lpcStateMa;

    /**
     * The LPC postfilter auto regressive memory of {@code SMPL_LPC_ORDER} taps.
     */
    private final float[] lpcStateAr;

    /**
     * The {@value #LPC_POST_IMPZ_LEN}-point real FFT used to compute the LPC postfilter nyquist tilt gain.
     */
    private final Pffft lpcFft;

    /**
     * The high pass postfilter pre low emphasis single tap auto regressive memory.
     */
    private final float[] hpStateLoEmph1;

    /**
     * The high pass postfilter post low emphasis single tap moving average memory.
     */
    private final float[] hpStateLoEmph2;

    /**
     * The high pass postfilter second order ARMA memory of four taps.
     */
    private final float[] hpStateHp;

    /**
     * The high pass postfilter previous frame input, used by the lag change overlap add transition.
     */
    private final float[] hpXOld;

    /**
     * The high pass postfilter current moving average coefficients.
     */
    private final float[] hpCoefMa;

    /**
     * The high pass postfilter current auto regressive coefficients.
     */
    private final float[] hpCoefAr;

    /**
     * The high pass postfilter previous pitch lag; the sentinel {@code -1.0f} marks the uninitialized first
     * call.
     */
    private float hpLagOld;

    /**
     * The high pass postfilter coefficient transition down ramp for a 20 ms frame.
     */
    private final float[] hpRampDn20;

    /**
     * The high pass postfilter coefficient transition down ramp for a 10 ms frame.
     */
    private final float[] hpRampDn10;

    /**
     * The harmonic postfilter comb delay line of
     * {@code SMPL_MAXPITCH_LEN + SMPL_FRAME_LEN * SMPL_MAX_FRAMES_PER_PACKET + SMPL_HARM_POSTF_DELAY} samples.
     */
    private final float[] harmStateComb;

    /**
     * The harmonic postfilter cross block diff carry of {@code 2 * SMPL_HARM_POSTF_FB_DELAY} samples.
     */
    private final float[] harmState1;

    /**
     * The harmonic postfilter running low pass comb coefficients of {@code 2 * SMPL_HARM_POSTF_FB_DELAY + 1}
     * taps.
     */
    private final float[] harmLpCoefs;

    /**
     * The harmonic postfilter previous pitch lag.
     */
    private int harmPrevLag;

    /**
     * The harmonic postfilter previous filtered flag.
     */
    private int harmPrevDidFilter;

    /**
     * The shared pitch indexed low pass comb window table.
     */
    private final HarmonicPostfilterTables harmTables;

    /**
     * Constructs a decode postfilter chain with zeroed state.
     *
     * <p>All filter memories start silent, the high pass previous lag starts at the {@code -1.0f} uninitialized
     * sentinel, and the FFT and the shared comb window table are built once. The chain is ready to process the
     * first packet of a stream.
     */
    public MlowDecodePostfilter() {
        this.lpcStateMa = new float[LPC_ORDER];
        this.lpcStateAr = new float[LPC_ORDER];
        this.lpcFft = new Pffft(LPC_POST_IMPZ_LEN, Pffft.REAL);
        this.hpStateLoEmph1 = new float[1];
        this.hpStateLoEmph2 = new float[1];
        this.hpStateHp = new float[4];
        this.hpXOld = new float[FRAME_LEN];
        this.hpCoefMa = new float[3];
        this.hpCoefAr = new float[3];
        this.hpLagOld = -1.0f;
        this.hpRampDn20 = buildRampDn(FRAME_LEN);
        this.hpRampDn10 = buildRampDn(FRAME_LEN / 2);
        this.harmStateComb = new float[MAX_PITCH_LAG + FRAME_LEN * MAX_FRAMES_PER_PACKET + HARM_DELAY];
        this.harmState1 = new float[2 * HARM_FB_DELAY];
        this.harmLpCoefs = new float[2 * HARM_FB_DELAY + 1];
        this.harmPrevLag = 0;
        this.harmPrevDidFilter = 0;
        this.harmTables = HarmonicPostfilterTables.INSTANCE;
    }

    /**
     * Returns the chain to its freshly constructed state.
     *
     * <p>Zeroes every filter memory and the harmonic comb delay line, and resets the high pass previous lag to
     * the {@code -1.0f} uninitialized sentinel. Call this between independent decode sessions; do not call it
     * between the packets of one continuous stream, which must thread state.
     */
    public void reset() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "postfilter chain reset");
        }
        Arrays.fill(lpcStateMa, 0.0f);
        Arrays.fill(lpcStateAr, 0.0f);
        Arrays.fill(hpStateLoEmph1, 0.0f);
        Arrays.fill(hpStateLoEmph2, 0.0f);
        Arrays.fill(hpStateHp, 0.0f);
        Arrays.fill(hpXOld, 0.0f);
        Arrays.fill(hpCoefMa, 0.0f);
        Arrays.fill(hpCoefAr, 0.0f);
        this.hpLagOld = -1.0f;
        Arrays.fill(harmStateComb, 0.0f);
        Arrays.fill(harmState1, 0.0f);
        Arrays.fill(harmLpCoefs, 0.0f);
        this.harmPrevLag = 0;
        this.harmPrevDidFilter = 0;
    }

    /**
     * Applies the LPC, high pass, and harmonic postfilters to one packet's pre postfilter synthesis in place.
     *
     * <p>Runs, per internal frame in {@code synthesis} (each {@code frameLength} samples): the optional LPC
     * postfilter ({@link #lpcPostfilter}) over the frame's subframes, then the pitch adaptive high pass
     * postfilter ({@link #hpPostfilter}) over the whole frame. After every frame the harmonic postfilter
     * ({@link #harmPostfilter}) runs once over the whole packet. The packet's lag subframes are the
     * concatenation of every frame's lags. The synthesis buffer is rewritten with the postfiltered signal.
     *
     * @param synthesis            the packet's pre postfilter synthesis (every frame's post {@code ar16},
     *                             post tilt samples concatenated), the {@code yBuf}; rewritten in place
     * @param numFrames            the number of internal frames in the packet
     * @param lpc                  the per frame per subframe LPC coefficient sets, indexed
     *                             {@code [frame][subframe][0..LPC_ORDER]} with index zero the monic
     *                             {@code 1.0f}
     * @param numSubframes         the number of subframes per frame
     * @param subframeLength       the subframe length in samples
     * @param lagsPerPacket        the packet's pitch lags, one per lag subframe, frames concatenated; each
     *                             {@code HARM_LAG_SUBFR_LEN}-sample lag subframe spans the harmonic comb
     * @param normalizedBitratePerFrame the per frame normalized bitrate the harmonic feedback strength reads;
     *                             the harmonic postfilter uses the packet average
     * @param voiced               {@code true} for a voiced packet (the postfilter gamma and tilt selection)
     * @param lowRate              {@code true} for the low rate mode
     * @param lpcPostfilterEnabled {@code true} to run the LPC postfilter; when {@code false} only its state is
     *                             advanced and the low rate tilt (applied by the kernel) is the active
     *                             short term shaper
     */
    public void process(float[] synthesis, int numFrames, float[][][] lpc, int numSubframes, int subframeLength,
                        float[] lagsPerPacket, float[] normalizedBitratePerFrame, boolean voiced, boolean lowRate,
                        boolean lpcPostfilterEnabled) {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE,
                    "postfilter packet: frames={0} subframes={1} voiced={2} lowRate={3} lpcPostfilter={4}",
                    numFrames, numSubframes, voiced, lowRate, lpcPostfilterEnabled);
        }
        var frameLength = numSubframes * subframeLength;
        var lagsPerFrame = frameLength / HARM_LAG_SUBFR_LEN;
        var packetLen = numFrames * frameLength;

        // The work buffer carries LPC_POST_IMPZ_LEN of leading headroom so the LPC postfilter's negative reads
        // at x[-LPC_ORDER - 1 ..] stay in bounds. The leading samples are scratch the LPC postfilter saves and
        // restores.
        var work = new float[LPC_POST_IMPZ_LEN + packetLen];
        System.arraycopy(synthesis, 0, work, LPC_POST_IMPZ_LEN, packetLen);

        var avgNormalizedBitrate = 0.0f;
        for (var frame = 0; frame < numFrames; frame++) {
            avgNormalizedBitrate += normalizedBitratePerFrame[frame];
            var frameOff = LPC_POST_IMPZ_LEN + frame * frameLength;
            if (lpcPostfilterEnabled) {
                for (var sf = 0; sf < numSubframes; sf++) {
                    lpcPostfilter(work, frameOff + sf * subframeLength, subframeLength, lpc[frame][sf],
                            voiced, lowRate);
                }
            } else {
                lpcPostfilterStateUpdate(work, frameOff, frameLength);
            }
            hpPostfilter(work, frameOff, frameLength, lagsPerPacket, frame * lagsPerFrame, lagsPerFrame);
        }
        var nLags = lagsPerPacket.length;
        harmPostfilter(work, LPC_POST_IMPZ_LEN, packetLen, lagsPerPacket, nLags, avgNormalizedBitrate / numFrames);

        System.arraycopy(work, LPC_POST_IMPZ_LEN, synthesis, 0, packetLen);
    }

    /**
     * Applies the LPC postfilter to one subframe in place and returns its nyquist tilt gain.
     *
     * <p>Bandwidth expands the subframe LPC into a numerator {@code A1 = A(z/g1)} and a denominator
     * {@code A2 = A(z/g2)} by the rate and voicing dependent gammas, forms the impulse response of
     * {@code A1/A2}, computes its peak spectral magnitude through an {@value #LPC_POST_IMPZ_LEN}-point FFT, and
     * scales {@code A1} by {@code g = min(1, 1/sqrt(maxHsq))} so the postfilter never amplifies. The subframe
     * is then filtered through the scaled moving average numerator and the auto regressive denominator, with
     * the moving average and auto regressive memories threaded from the previous subframe. The returned
     * nyquist gain feeds the high band path (out of this chain's scope) and is discarded here.
     *
     * @param x        the synthesis buffer
     * @param xOff     the offset of the subframe's first sample in {@code x}
     * @param len      the subframe length
     * @param predcoef the subframe LPC coefficients, {@code LPC_ORDER + 1} with the monic {@code 1.0f} lead
     * @param voiced   {@code true} for a voiced frame
     * @param lowRate  {@code true} for the low rate mode
     * @return the nyquist tilt gain of this subframe
     */
    private float lpcPostfilter(float[] x, int xOff, int len, float[] predcoef, boolean voiced, boolean lowRate) {
        var rateIx = lowRate ? 1 : 0;
        var voicedIx = voiced ? 1 : 0;
        var a1 = new float[LPC_ORDER + 1];
        var a2 = new float[LPC_ORDER + 1];
        System.arraycopy(predcoef, 0, a1, 0, LPC_ORDER + 1);
        System.arraycopy(predcoef, 0, a2, 0, LPC_ORDER + 1);
        bweExpand(a1, LPC_ORDER, LPC_POSTFILT_GAMMA[rateIx][voicedIx][0]);
        bweExpand(a2, LPC_ORDER, LPC_POSTFILT_GAMMA[rateIx][voicedIx][1]);

        // Impulse response of A1/A2: filter [A1, 0...] through AR(A2).
        var impz = new float[LPC_POST_IMPZ_LEN + LPC_ORDER];
        System.arraycopy(a1, 0, impz, LPC_ORDER, LPC_ORDER + 1);
        // impz[LPC_ORDER + (LPC_ORDER+1) ..] already zero
        Filters.ar16(impz, LPC_ORDER, LPC_POST_IMPZ_LEN, a2, impz, LPC_ORDER);

        var h = new float[LPC_POST_IMPZ_LEN];
        var impzFft = new float[LPC_POST_IMPZ_LEN];
        System.arraycopy(impz, LPC_ORDER, impzFft, 0, LPC_POST_IMPZ_LEN);
        lpcFft.transformOrdered(impzFft, h, null, true);
        var maxHsq = Math.max(h[0] * h[0], h[1] * h[1]);
        for (var i = 2; i < LPC_POST_IMPZ_LEN; i += 2) {
            maxHsq = Math.max(h[i] * h[i] + h[i + 1] * h[i + 1], maxHsq);
        }
        var g = Math.min(1.0f, 1.0f / (float) Math.sqrt(maxHsq + 1e-30f));
        var nyquistGain = g * (float) Math.sqrt(h[1] * h[1] + 1e-30f);

        // x[-LPC_ORDER - 1 .. -1] hold the prior frame history; save and restore them around the swap in of
        // the postfilter's own moving average and auto regressive memories.
        var stateTmp = new float[LPC_ORDER + 1];
        System.arraycopy(x, xOff - LPC_ORDER - 1, stateTmp, 0, LPC_ORDER + 1);

        var temp = new float[len];
        System.arraycopy(lpcStateMa, 0, x, xOff - LPC_ORDER, LPC_ORDER);
        for (var i = 0; i < LPC_ORDER + 1; i++) {
            a1[i] *= g;
        }
        Filters.ma(x, xOff, len, a1, LPC_ORDER + 1, temp, 0);
        System.arraycopy(x, xOff + len - LPC_ORDER, lpcStateMa, 0, LPC_ORDER);

        System.arraycopy(lpcStateAr, 0, x, xOff - LPC_ORDER, LPC_ORDER);
        Filters.ar16(temp, 0, len, a2, x, xOff);
        System.arraycopy(x, xOff + len - LPC_ORDER, lpcStateAr, 0, LPC_ORDER);

        System.arraycopy(stateTmp, 0, x, xOff - LPC_ORDER - 1, LPC_ORDER + 1);
        return nyquistGain;
    }

    /**
     * Advances the LPC postfilter state without filtering.
     *
     * <p>Copies the frame's trailing {@code LPC_ORDER} samples into both the moving average and auto regressive
     * memories so the next frame's LPC postfilter (should it be enabled) sees a continuous history. Called for
     * every frame when the LPC postfilter is disabled.
     *
     * @param x    the synthesis buffer
     * @param xOff the offset of the frame's first sample in {@code x}
     * @param len  the frame length
     */
    private void lpcPostfilterStateUpdate(float[] x, int xOff, int len) {
        System.arraycopy(x, xOff + len - LPC_ORDER, lpcStateMa, 0, LPC_ORDER);
        System.arraycopy(x, xOff + len - LPC_ORDER, lpcStateAr, 0, LPC_ORDER);
    }

    /**
     * Applies the pitch adaptive high pass postfilter to one frame in place.
     *
     * <p>Pre emphasizes the frame through a fixed first order auto regressive low emphasis, computes the energy
     * weighted mean pitch lag of the frame's lag subframes, selects the resonance coefficients from that lag
     * (or a fixed corner when the frame is unvoiced), filters the frame through the resulting second order
     * ARMA, and post emphasizes through a first order moving average low emphasis. A pitch lag that changes by
     * more than the lag change threshold versus the previous frame triggers an overlap add transition that
     * crossfades the old and new coefficient responses through the precomputed down ramp.
     *
     * @param x           the synthesis buffer
     * @param xOff        the offset of the frame's first sample in {@code x}
     * @param l           the frame length, {@code SMPL_FRAME_LEN} or {@code SMPL_FRAME_LEN / 2}
     * @param lags        the packet's lag array
     * @param lagOff      the offset of the frame's first lag in {@code lags}
     * @param nLags       the number of lags spanning the frame
     */
    private void hpPostfilter(float[] x, int xOff, int l, float[] lags, int lagOff, int nLags) {
        // Pre low emphasis AR1, in place.
        Filters.ar1(x, xOff, l, LO_EMPH_COEF, hpStateLoEmph1, 0, x, xOff);

        var lag = 0.0f;
        if (lags[lagOff] > 0) {
            var sumWghts = 0.0f;
            var sumWlags = 0.0f;
            for (var i = 0; i < nLags; i++) {
                sumWghts += lags[lagOff + i];
                sumWlags += lags[lagOff + i] * lags[lagOff + i];
            }
            lag = sumWlags / sumWghts;
        }

        var overlapAdd = false;
        var yOld = new float[l];
        var yTmp = new float[l];
        if (hpLagOld < 0.0f) {
            newCoefs(lag);
            hpLagOld = lag;
        } else if (lag > LAG_CHANGE_THRESHOLD * hpLagOld || LAG_CHANGE_THRESHOLD * lag < hpLagOld) {
            overlapAdd = true;
            Filters.arma2(x, xOff, l, hpCoefMa, hpCoefAr, hpStateHp, 0, yOld, 0);
            newCoefs(lag);
            hpLagOld = lag;
            var dummy = new float[l];
            Filters.arma2(hpXOld, 0, l, hpCoefMa, hpCoefAr, hpStateHp, 0, dummy, 0);
        } else if (lag != hpLagOld) {
            newCoefs(lag);
            hpLagOld = lag;
        }
        System.arraycopy(x, xOff, hpXOld, 0, l);

        Filters.arma2(x, xOff, l, hpCoefMa, hpCoefAr, hpStateHp, 0, yTmp, 0);

        if (overlapAdd) {
            var ramp = (l == FRAME_LEN) ? hpRampDn20 : hpRampDn10;
            for (var i = 0; i < l; i++) {
                yTmp[i] += (yOld[i] - yTmp[i]) * ramp[i];
            }
        }

        Filters.ma1(yTmp, 0, l, LO_EMPH_COEF, hpStateLoEmph2, 0, x, xOff);
    }

    /**
     * Recomputes the high pass postfilter resonance coefficients for a pitch lag.
     *
     * <p>A positive lag selects the pitch resonance fit (the 1.2 dB peak coefficient set); a nonpositive lag
     * selects the fixed corner fit at the {@value #HP_FCORNER_3DB_HZ} Hz corner. Both feed {@link #calcHpCoefs}.
     *
     * @param lag the energy weighted mean pitch lag, or {@code 0} for an unvoiced frame
     */
    private void newCoefs(float lag) {
        if (lag > 0.0f) {
            var f = 1.0f / lag;
            calcHpCoefs(HP_PITCH_MAF, HP_PITCH_ARF, HP_PITCH_ARR, f, hpCoefMa, hpCoefAr);
        } else {
            var fcorner = Math.min(Math.max(HP_FCORNER_3DB_HZ, 5.0f), 1500.0f);
            var f = fcorner / 16000.0f;
            calcHpCoefs(HP_FALLBACK_MAF, HP_FALLBACK_ARF, HP_FALLBACK_ARR, f, hpCoefMa, hpCoefAr);
        }
    }

    /**
     * Computes the second order ARMA coefficients of a resonant high pass from a frequency fit.
     *
     * <p>Builds a notch like moving average numerator and a resonant auto regressive denominator from the small
     * angle cosine approximation {@code cos(x) ~= 1 - 0.5 x^2}, then scales the numerator so the cascade has
     * unit gain at the nyquist frequency.
     *
     * @param maf    the moving average shape factor
     * @param arf    the forward (numerator) frequency fit pair
     * @param arr    the resonance (denominator) frequency fit pair
     * @param f      the normalized frequency (lag reciprocal, or corner over sample rate)
     * @param coefMa the output moving average coefficients, three taps
     * @param coefAr the output auto regressive coefficients, three taps
     */
    private static void calcHpCoefs(float maf, float[] arf, float[] arr, float f, float[] coefMa, float[] coefAr) {
        coefMa[0] = 1.0f;
        coefMa[1] = -2.0f * cosApprox(2.0f * PI * maf * f);
        coefMa[2] = 1.0f;
        var far = arf[0] * f + arf[1] * f * f;
        var rar = arr[0] * f + arr[1] * f * f;
        coefAr[0] = 1.0f;
        coefAr[1] = -2.0f * cosApprox(2.0f * PI * far) * (1.0f + rar);
        coefAr[2] = 1.0f + (2.0f * rar + rar * rar);
        var sc = (1.0f - coefAr[1] + coefAr[2]) / (1.0f - coefMa[1] + coefMa[2]);
        for (var i = 0; i < 3; i++) {
            coefMa[i] *= sc;
        }
    }

    /**
     * Returns the small angle cosine approximation {@code 1 - 0.5 * x * x}.
     *
     * @param x the angle in radians
     * @return {@code 1 - 0.5 * x * x}
     */
    private static float cosApprox(float x) {
        return 1.0f - 0.5f * x * x;
    }

    /**
     * Builds the high pass postfilter coefficient transition down ramp.
     *
     * <p>Forms {@code ramp[i] = cos(omega)^TRANSITION_SPEED} with {@code omega} stepping by
     * {@code pi / (2 * (length + 1))}, the crossfade weight the overlap add transition applies.
     *
     * @param length the ramp length ({@code SMPL_FRAME_LEN} or {@code SMPL_FRAME_LEN / 2})
     * @return the down ramp
     */
    private static float[] buildRampDn(int length) {
        var ramp = new float[length];
        var dOmega = PI / (2.0f * (length + 1.0f));
        var omega = dOmega;
        for (var i = 0; i < length; i++) {
            ramp[i] = (float) Math.pow((float) Math.cos(omega), HP_TRANSITION_SPEED);
            omega += dOmega;
        }
        return ramp;
    }

    /**
     * Applies the harmonic postfilter to one packet's signal in place.
     *
     * <p>Copies the packet into the comb delay line behind the carried history, then processes the packet one
     * pitch subframe block at a time through {@link #harmPostfilterCore}, advancing the comb pointer and the
     * cross block diff carry, applying each lag with the one subframe delay. The postfiltered packet is written
     * back into {@code x}, and the comb delay line is shifted to retain the trailing history for the next
     * packet.
     *
     * @param x                 the packet signal, post LPC and high pass postfilters; rewritten in place
     * @param xOff              the offset of the packet's first sample in {@code x}
     * @param xLen              the packet length {@code packetlen_16}
     * @param lags              the packet's lag array, one entry per lag subframe
     * @param nLags             the lag count {@code lags_per_subframe * num_subframes * num_frames}
     * @param normalizedBitrate the packet average normalized bitrate the feedback strength reads
     */
    private void harmPostfilter(float[] x, int xOff, int xLen, float[] lags, int nLags, float normalizedBitrate) {
        // diffBuf carries 2*FB_DELAY of leading history; the diff window starts at diffBase.
        var diffBuf = new float[FRAME_LEN * MAX_FRAMES_PER_PACKET + 2 * HARM_FB_DELAY];
        var diffBase = 2 * HARM_FB_DELAY;

        var lag = harmPrevLag;
        System.arraycopy(x, xOff, harmStateComb, MAX_PITCH_LAG + HARM_DELAY, xLen);

        var fbStrength = 1.0f - HARM_FB_STRENGTH * normalizedBitrate;
        var offset1 = 0;

        var lagCtr = 0;
        while (lagCtr < nLags) {
            var offset2 = 0;
            // carry the cross block diff history into the 16 samples before the diff window
            System.arraycopy(harmState1, 0, diffBuf, diffBase - 16, 16);
            var lagCtrEnd = Math.min(lagCtr + PITCH_NUM_SUBFRAMES, nLags);
            for (; lagCtr < lagCtrEnd; lagCtr++) {
                harmPostfilterCore(MAX_PITCH_LAG + offset1, HARM_DELAY + xLen - offset1, lag,
                        diffBuf, diffBase + offset2, x, xOff + offset1, HARM_LAG_SUBFR_LEN, fbStrength);
                offset1 += HARM_LAG_SUBFR_LEN;
                offset2 += HARM_LAG_SUBFR_LEN;
                lag = Math.round(lags[lagCtr]);
            }
            System.arraycopy(diffBuf, diffBase + offset2 - 16, harmState1, 0, 16);
        }

        harmPrevLag = lag;
        // shift the comb delay line down by xLen to retain the trailing history for the next packet
        System.arraycopy(harmStateComb, xLen, harmStateComb, 0, MAX_PITCH_LAG + HARM_DELAY);
    }

    /**
     * Processes one harmonic postfilter lag subframe.
     *
     * <p>When the lag is positive and the synthesis correlates positively with its pitch delayed average, forms
     * the periodicity difference {@code diff = strength * (avg - x)}, low pass filters it through the lag
     * indexed comb window scaled by the feedback strength, and adds the result back into the comb domain signal
     * with the {@value #HARM_FB_DELAY}-sample group shift; the postfiltered samples land in the output window.
     * When the lag is nonpositive or the correlation is nonpositive, the subframe is passed through, with a
     * zero input tail filtered out when the previous subframe did filter (so the comb's ringing decays
     * cleanly). The comb domain pointer {@code combOff} indexes {@link #harmStateComb}; the output pointer
     * {@code outOff} indexes {@code x}.
     *
     * @param combOff       the comb domain offset in {@link #harmStateComb}
     * @param futureSamples the count of available future comb samples {@code future_samples}
     * @param lag           the pitch lag in samples for this subframe (already one subframe delayed)
     * @param diff          the diff scratch buffer with {@code 2 * FB_DELAY} leading history
     * @param diffOff       the offset of this subframe's diff window in {@code diff}
     * @param out           the output buffer, the packet signal {@code x}
     * @param outOff        the offset of this subframe's output window in {@code out}
     * @param l             the lag subframe length {@value #HARM_LAG_SUBFR_LEN}
     * @param fbStrength    the feedback strength scaling the comb window
     */
    private void harmPostfilterCore(int combOff, int futureSamples, int lag, float[] diff, int diffOff,
                                    float[] out, int outOff, int l, float fbStrength) {
        var comb = harmStateComb;
        var yHarm = new float[l + 2 * HARM_FB_DELAY];

        var xy = 0.0f;
        if (lag > 0) {
            var lookforward = l + lag - futureSamples;
            if (lookforward > 0) {
                var l2nd = Math.max(l - lookforward, 0);
                for (var i = 0; i < l2nd; i++) {
                    yHarm[i] = comb[combOff - lag + i] + comb[combOff + lag + i];
                }
                for (var i = 0; i < l - l2nd; i++) {
                    yHarm[l2nd + i] = comb[combOff + l2nd - lag + i] + comb[combOff + l2nd + i];
                }
            } else {
                for (var i = 0; i < l; i++) {
                    yHarm[i] = comb[combOff - lag + i] + comb[combOff + lag + i];
                }
            }
            for (var i = 0; i < l; i++) {
                xy += comb[combOff + i] * yHarm[i];
            }
        }

        if (lag > 0 && xy > 0.0f) {
            var xx = 0.0f;
            for (var i = 0; i < l; i++) {
                xx += comb[combOff + i] * comb[combOff + i];
            }
            var yy = 0.0f;
            for (var i = 0; i < l; i++) {
                yy += yHarm[i] * yHarm[i];
            }
            yy *= 0.25f;
            var strength = 0.5f * xy / Math.max(yy, xx);
            var highLagReduction = 1.0f - HARM_REDUCTION_FAC
                                          * ((float) (lag - MIN_PITCH_LAG) / (MAX_PITCH_LAG - MIN_PITCH_LAG));
            strength *= highLagReduction * HARM_STRENGTH;
            var half = 0.5f * strength;
            for (var i = 0; i < l; i++) {
                yHarm[i] *= half;
            }
            for (var i = 0; i < l; i++) {
                diff[diffOff + i] = yHarm[i] - strength * comb[combOff + i];
            }
            var lp = harmTables.filter(HarmonicPostfilterTables.lagToFiltIx(lag));
            for (var i = 0; i < harmLpCoefs.length; i++) {
                harmLpCoefs[i] = lp[i] * fbStrength;
            }
            // symmetric moving average over the diff window; its filter state is the 16 samples before diffOff
            Filters.ma16Sym(diff, diffOff, l, harmLpCoefs, yHarm, 0);
            // add the base signal shifted by FB_DELAY onto the correction
            for (var i = 0; i < l; i++) {
                yHarm[i] += comb[combOff - HARM_FB_DELAY + i];
            }
            harmPrevDidFilter = 1;
        } else {
            for (var i = 0; i < HARM_LAG_SUBFR_LEN; i++) {
                diff[diffOff + i] = 0.0f;
            }
            if (harmPrevDidFilter != 0) {
                // zero input response over 2*FB_DELAY samples, added onto the base signal shifted by FB_DELAY
                Filters.ma16Sym(diff, diffOff, 2 * HARM_FB_DELAY, harmLpCoefs, yHarm, 0);
                for (var i = 0; i < 2 * HARM_FB_DELAY; i++) {
                    yHarm[i] += comb[combOff - HARM_FB_DELAY + i];
                }
                for (var i = 0; i < l - 2 * HARM_FB_DELAY; i++) {
                    yHarm[2 * HARM_FB_DELAY + i] = comb[combOff + HARM_FB_DELAY + i];
                }
            } else {
                for (var i = 0; i < l; i++) {
                    yHarm[i] = comb[combOff - HARM_FB_DELAY + i];
                }
            }
            harmPrevDidFilter = 0;
        }
        System.arraycopy(yHarm, 0, out, outOff, l);
    }

    /**
     * Applies a bandwidth expansion to an LPC coefficient set in place.
     *
     * <p>Multiplies the {@code i}-th coefficient by {@code bwe^i}, contracting the LPC poles radially toward
     * the origin; a nonpositive {@code bwe} zeroes every coefficient after the monic lead.
     *
     * @param a        the coefficient set, {@code lpcOrder + 1} entries with the monic {@code 1.0f} lead
     * @param lpcOrder the LPC order
     * @param bwe      the bandwidth expansion factor
     */
    private static void bweExpand(float[] a, int lpcOrder, float bwe) {
        if (bwe <= 0.0f) {
            for (var i = 1; i < lpcOrder + 1; i++) {
                a[i] = 0.0f;
            }
            return;
        }
        var c = bwe;
        for (var i = 1; i < lpcOrder + 1; i++) {
            a[i] *= c;
            c *= bwe;
        }
    }
}
