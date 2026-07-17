package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.EncoderTables;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.MiscTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Per subframe target bitrate controller of the MLow speech encoder.
 *
 * <p>This controller is the rate allocation stage that sits between the rate state set by the encoder API
 * ({@code mainBitRate} and {@code fecBitRate}, produced by the in band FEC rate split {@link #controlLbrr}) and
 * the analysis by synthesis CELP search. For every subframe of every frame the core encoder asks this object two
 * things: how many fixed codebook pulses the subframe may spend and how perceptually important the subframe is
 * relative to its neighbours. The pulse budget caps the CELP pulse search; the importance scales the rate
 * distortion weight the search trades bits against. Together they steer the instantaneous bitrate toward the
 * configured target without a hard per frame bit cap.
 *
 * <p>The allocation is a model plus a feedback loop:
 * <ul>
 * <li>{@link #control} evaluates the open loop pulse target model (a degree four polynomial plus an exponential
 * tail, with coefficients {@link EncoderTables#RATE_CONTROL_MODEL_COMP5}) to turn the kbit/s target into a pulses
 * per 20 ms ceiling, converts that to a per subframe pulse cap, then forms the subframe importance from the
 * smoothed weighted energy, the voicing and nonflatness features, the speech activity probability, and the
 * running feedback scale. It also seeds the one shot per rate bitrate scale the first time a target bitrate is
 * seen.</li>
 * <li>{@link #updateScale} closes the loop after the frame is coded: it compares the bits actually spent against
 * the target, integrates the relative error into a clamped smoothed delta, and turns that into the multiplicative
 * adjustment factor the next {@link #control} call applies. Inactive frames decay the smoothed delta back toward
 * neutral instead.</li>
 * </ul>
 * The two nonflatness helpers {@link #nonflatness} and {@link #hrNonflatThres} belong to the same computation:
 * the encoder uses them to decide the unvoiced flatness threshold that, with the controller's pulse caps, gates
 * whether an unvoiced subframe is coded with pulses at all.
 *
 * <p>The high rate versus low rate path is selected upstream by the encoder (when {@code mainBitRate} is at or
 * below the low rate threshold); the controller receives the decision as the {@code lowRate} flag and indexes the
 * model and threshold tables by it. The supported scope is the SMPL 16 kHz, 60 ms, mono high rate path (9600 bps,
 * payload bucket {@value #FRAMELEN_IDX_60MS}); a 60 ms packet is three 20 ms frames of four 80 sample subframes
 * each. There is no in band FEC on this path, so {@code fecBitRate} is zero and the per rate loops collapse to the
 * single main rate point ({@value #IDX_MAIN}); the FEC point ({@value #IDX_FEC}) is carried for structural
 * fidelity and is never populated on this scope.
 *
 * <p>One controller instance carries the feedback state of a single logical stream; construct one per base
 * encoder, call {@link #control} for every subframe in order, and call {@link #updateScale} once per coded frame.
 * The pure model helpers ({@link #hrNonflatThres}, {@link #nonflatness}, {@link #controlLbrr}) are static and
 * stateless. This type is stateful and is not thread safe.
 *
 * @implNote This implementation reproduces every {@code float} accumulation in source order so the output matches
 * the reference MLow codec bit for bit. The reduction loops in this controller are strict left to right single
 * precision; only the two energy helpers it calls into, {@link #nrg} and the sum reduction, follow the fast math
 * grouping of the reference codec's utility unit. The model polynomial evaluates its cubic, quartic, and
 * exponential terms with {@code (float) Math.pow} to match the reference {@code powf} calls. The model
 * coefficients are stored as {@code double} in {@link EncoderTables#RATE_CONTROL_MODEL_COMP5} but are narrowed to
 * {@code float} before use to match the reference single precision reads. The rate class loop start index, in
 * {@link #startRate}, preserves the reference C operator precedence where {@code +} binds tighter than
 * {@code ||}, so the whole expression is a boolean that is {@code 1} whenever {@code fecBitRate} is zero, making
 * the loop run the single main rate point.
 */
public final class BitrateController {
    /**
     * Number of rate points the controller allocates over.
     */
    private static final int MAX_RATES = 2;

    /**
     * Index of the in band FEC rate point.
     */
    private static final int IDX_FEC = 0;

    /**
     * Index of the main rate point.
     */
    private static final int IDX_MAIN = 1;

    /**
     * Linear prediction order of the MLow short term filter; the length of the weighted LSF vector the
     * nonflatness measure folds in.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Number of cross frame nonflatness energy state slots.
     */
    private static final int NON_FLAT_STATE_LEN = 5;

    /**
     * Sub block length of the nonflatness energy partition.
     */
    private static final int NON_FLAT_SUBFR_LEN = 16;

    /**
     * Length of the nonflatness energy scratch, the maximum frame length of 320 samples divided by
     * {@value #NON_FLAT_SUBFR_LEN} plus the state length {@value #NON_FLAT_STATE_LEN}.
     */
    private static final int NON_FLAT_NRGS_LEN = (320 / NON_FLAT_SUBFR_LEN) + NON_FLAT_STATE_LEN;

    /**
     * Ceiling on the unvoiced nonflatness threshold.
     */
    private static final float UV_NONFLATNESS_THR = 0.5f;

    /**
     * Per subframe pulse budget ceiling.
     */
    private static final int MAX_PULSES_PER_SF = 40;

    /**
     * Euler's number as a single precision literal, used by the model exponential tail.
     */
    private static final float E = 2.7182818284590f;

    /**
     * Payload length bucket index for a 60 ms packet.
     */
    private static final int FRAMELEN_IDX_60MS = 2;

    /**
     * One shot bitrate scale gain seeding the rate control loop.
     */
    private static final float RATE_CONT_SCALE = 26.0f;

    /**
     * Maximum smoothed bitrate delta.
     */
    private static final float RATE_CONT_CLAMP_MAX = 0.9f;

    /**
     * Minimum smoothed bitrate delta.
     */
    private static final float RATE_CONT_CLAMP_MIN = -0.3f;

    /**
     * Integration gain on the feedback loop.
     */
    private static final float RATE_CONT_GAIN = 0.05f;

    /**
     * Rate control compensation knee in kbit/s for the FEC pulse target model.
     */
    private static final float RATE_THRES_KBPS = 9.0f;

    /**
     * The two bitrate anchors, in bits per second, of the high rate unvoiced nonflatness threshold line that
     * {@link #hrNonflatThres} interpolates between.
     */
    private static final float[] HR_NONFLAT_BITRATES = {10000.0f, 18000.0f};

    /**
     * The two threshold anchors of the high rate unvoiced nonflatness threshold line that {@link #hrNonflatThres}
     * interpolates between.
     */
    private static final float[] HR_NONFLAT_THRESHOLDS = {0.5f, 0.0f};

    /**
     * The logger for {@link BitrateController}.
     */
    private static final System.Logger LOGGER = Log.get(BitrateController.class);

    /**
     * The previous frame's voicing decision.
     *
     * <p>Read to bump the importance on a voicing transition and overwritten at the end of each rate point loop
     * iteration; starts zeroed.
     */
    private int prevVoiced;

    /**
     * The smoothed weighted energy running mean.
     *
     * <p>A first order leaky integrator of the per subframe weighted energy; the importance denominator. Starts
     * zeroed.
     */
    private float rateContWnrgSmth;

    /**
     * The per rate one shot bitrate scale.
     *
     * <p>Seeded the first time each rate point's target bitrate is seen (when {@link #rateContBitrate} differs)
     * and held thereafter; the final multiplicative factor on the importance. Starts zeroed.
     */
    private final float[] rateContBitrateScale;

    /**
     * The per rate smoothed bitrate delta.
     *
     * <p>The clamped integral of the relative bits spent error that {@link #updateScale} maintains and that the
     * adjustment factor is derived from. Starts zeroed.
     */
    private final float[] bitrateDeltaSmth;

    /**
     * The per rate target bitrate last seen.
     *
     * <p>Guards the one shot {@link #rateContBitrateScale} seeding; reseeds the scale whenever the target bitrate
     * changes. Starts zeroed.
     */
    private final float[] rateContBitrate;

    /**
     * The per rate feedback adjustment factor.
     *
     * <p>The multiplicative correction {@code max(1 - bitrateDeltaSmth, 0)} that the next {@link #control} folds
     * into the importance. Initialized to {@code 1.0} for every rate point by {@link #init}.
     */
    private final float[] adjustmentFactor;

    /**
     * The per subframe rate allocation result of one {@link #control} call.
     *
     * <p>{@code maxPulsesPerSubfr[r]} is the fixed codebook pulse budget for rate point {@code r};
     * {@code subfrImportance[r]} is the perceptual importance weight. Both arrays have {@value #MAX_RATES} entries
     * indexed by rate point; on the 9600 bps scope only the main point {@value #IDX_MAIN} is populated and the FEC
     * point {@value #IDX_FEC} stays zero.
     *
     * @param maxPulsesPerSubfr the per rate fixed codebook pulse budget
     * @param subfrImportance   the per rate subframe importance weight
     */
    public record Allocation(short[] maxPulsesPerSubfr, float[] subfrImportance) {
    }

    /**
     * The {@code mainBitRate} and {@code fecBitRate} rate split produced by {@link #controlLbrr}.
     *
     * @param mainBitRate the main payload bitrate in bits per second
     * @param fecBitRate  the in band FEC bitrate in bits per second
     */
    public record RateSplit(int mainBitRate, int fecBitRate) {
    }

    /**
     * Constructs a controller in the neutral initial state.
     *
     * <p>Allocates the per rate feedback arrays and delegates to {@link #init}, which zeroes the feedback state
     * and sets every rate point's adjustment factor to {@code 1.0}, the neutral correction.
     */
    public BitrateController() {
        this.rateContBitrateScale = new float[MAX_RATES];
        this.bitrateDeltaSmth = new float[MAX_RATES];
        this.rateContBitrate = new float[MAX_RATES];
        this.adjustmentFactor = new float[MAX_RATES];
        init();
    }

    /**
     * Resets the controller to its neutral initial state.
     *
     * <p>Zeroes every feedback field and sets each rate point's adjustment factor to {@code 1.0}. Call between
     * independent streams to clear the carried feedback history.
     */
    public void init() {
        prevVoiced = 0;
        rateContWnrgSmth = 0.0f;
        for (var r = 0; r < MAX_RATES; r++) {
            rateContBitrateScale[r] = 0.0f;
            bitrateDeltaSmth[r] = 0.0f;
            rateContBitrate[r] = 0.0f;
            adjustmentFactor[r] = 1.0f;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "bitrate controller reset");
        }
    }

    /**
     * Computes the per subframe pulse budget and perceptual importance for one subframe.
     *
     * <p>Updates the smoothed weighted energy mean, then for each active rate point evaluates the pulse target
     * model, derives the per subframe pulse cap (scaled by speech activity and bounded by the per frame pulse
     * distribution), and forms the importance from the weighted energies, the voicing and nonflatness features,
     * the speech activity probability, the voicing transition bump, the voicing strength damping, the speech
     * activity importance factor, and the running feedback scale. On this scope the loop runs only the main rate
     * point.
     *
     * @param dtxSidFrame              {@code true} when the current frame is a discontinuous transmission SID
     *                                 frame
     * @param codedAsActiveVoice       {@code true} when the frame is coded as active voice
     * @param spActProb                the speech activity probability in {@code [0, 1]}
     * @param nonflatness              the subframe spectral nonflatness
     * @param voicingStrength          the subframe voicing strength
     * @param voiced                   {@code 1} for a voiced frame, {@code 0} otherwise
     * @param wnrg                     the subframe weighted energy
     * @param wnrgNext                 the next subframe's weighted energy
     * @param lowRate                  {@code true} on the low rate path, {@code false} on high rate
     * @param framelen                 the frame length in samples
     * @param subfrlen                 the subframe length in samples
     * @param internalSampleRate       the internal sample rate in Hertz
     * @param payloadSizeMs            the packet payload size in milliseconds
     * @param fecBitRate               the in band FEC bitrate in bits per second
     * @param mainBitRate              the main bitrate in bits per second
     * @param complexity               the encoder complexity setting
     * @param useDtx                   {@code true} when discontinuous transmission is enabled
     * @param useFecRateCompensation   {@code true} when the FEC rate compensation gate is enabled
     * @param subFrameImportanceFactor the speech activity importance shaping factor
     * @return the per rate pulse budget and importance for the subframe
     */
    public Allocation control(boolean dtxSidFrame, boolean codedAsActiveVoice, float spActProb, float nonflatness,
                              float voicingStrength, int voiced, float wnrg, float wnrgNext, boolean lowRate,
                              int framelen, int subfrlen, int internalSampleRate, int payloadSizeMs,
                              int fecBitRate, int mainBitRate, int complexity, boolean useDtx,
                              boolean useFecRateCompensation, float subFrameImportanceFactor) {
        var lowRateIdx = lowRate ? 1 : 0;
        // The model and threshold tables index the rate class as (lowRate ? 0 : 1), the inverse of the plain
        // low rate flag; only the pulses per frame table lookup uses the flag directly.
        var modelIdx = lowRate ? 0 : 1;

        var bweBitrate = 0;
        if (internalSampleRate > 16000) {
            bweBitrate += lowRate ? 450 : 750;
            bweBitrate += payloadSizeMs == 10 ? 450 : 0;
        }

        rateContWnrgSmth += 0.6f * (wnrg - rateContWnrgSmth);

        var framelenIdx = (payloadSizeMs == 10) ? 0
                : payloadSizeMs == 20 ? 1
                : payloadSizeMs == 60 ? 2 : 3;

        var maxPulsesPerSubfr = new short[MAX_RATES];
        var subfrImportance = new float[MAX_RATES];

        var startR = startRate(fecBitRate, mainBitRate);
        for (var r = startR; r <= IDX_MAIN; r++) {
            var bitRate = (r == IDX_FEC) ? (float) fecBitRate : (float) mainBitRate;
            bitRate = Math.min(bitRate, 30000.0f);
            var rateKbps = (bitRate - bweBitrate) / 1000.0f;
            if (!lowRate) {
                rateKbps *= complexity == 1 ? 0.9900990f
                        : complexity == 2 ? 0.9900990f
                        : complexity == 3 ? 1.0101010f
                        : complexity == 4 ? 1.0101010f
                        : 1.0f;
            }
            float pulsesPer20msTargetMax;
            float rateControlThrs = EncoderTables.RATE_CONTROL_THRS_COMP5[framelenIdx][modelIdx];
            if (bitRate - bweBitrate < rateControlThrs) {
                pulsesPer20msTargetMax = 1.0f;
            } else {
                var coeff = EncoderTables.RATE_CONTROL_MODEL_COMP5[framelenIdx][modelIdx];
                if ((r == IDX_FEC) && !lowRate && useFecRateCompensation) {
                    pulsesPer20msTargetMax = Math.max(bitrate2pulsesHrFec(rateKbps, coeff, rateControlThrs), 1.0f);
                } else {
                    pulsesPer20msTargetMax = Math.max(bitrate2pulses(rateKbps, coeff), 1.0f);
                }
            }

            var relPulserate = pulsesPer20msTargetMax / 16.0f * (320.0f / framelen);
            var relPulserateLog = (float) Math.log(relPulserate);
            if (rateContBitrate[r] != bitRate) {
                var bitrateScale = RATE_CONT_SCALE * relPulserate * (1 + 0.4f * relPulserateLog * relPulserateLog);
                rateContBitrateScale[r] = bitrateScale;
                rateContBitrate[r] = bitRate;
            }

            var numsubfrs = framelen / subfrlen;
            maxPulsesPerSubfr[r] = (short) (1 + (int) rint(pulsesPer20msTargetMax * (1 + 0.5f) / numsubfrs));
            if (useDtx && dtxSidFrame) {
                maxPulsesPerSubfr[r] = 0;
            } else {
                maxPulsesPerSubfr[r] = (short) rint(maxPulsesPerSubfr[r] * (0.5f + 0.5f * (float) Math.sqrt(spActProb + 1e-12f)));
                var frameType = !codedAsActiveVoice ? 0 : (voiced == 1) ? 2 : 1;
                var maxPulses = MiscTables.MAX_PULSES_PER_FRAME[lowRateIdx][frameType] * framelen / 320;
                maxPulsesPerSubfr[r] = (short) Math.min(maxPulsesPerSubfr[r], maxPulses / numsubfrs);
            }

            var importance = (wnrg + 0.01f * wnrgNext) / (rateContWnrgSmth + 0.02f * wnrgNext + 1e-12f);
            if (voiced != 0) {
                if (bitRate <= 9000) {
                    importance = (float) Math.sqrt(importance + 1e-12f);
                }
            } else {
                importance *= 0.9f + 0.3f * sigmoid(nonflatness - 2.0f);
                importance *= 0.8f;
            }
            if (voiced != prevVoiced) {
                importance *= 1.1f;
            }
            importance *= 0.9f + 0.3f * 1.0f / (1.0f + 25.0f * voicingStrength * voicingStrength);

            var impFactor = subFrameImportanceFactor;
            if (impFactor <= 1.0f) {
                importance *= (1 - impFactor) + impFactor * (float) Math.sqrt(spActProb + 1e-12f);
            } else if (impFactor <= 2.0f) {
                impFactor -= 1;
                importance *= (1 - impFactor) + impFactor * spActProb;
            } else {
                impFactor -= 2;
                importance *= (1 - impFactor) + impFactor * spActProb * spActProb;
            }
            importance *= adjustmentFactor[r] * rateContBitrateScale[r];
            subfrImportance[r] = importance;
            prevVoiced = voiced;
        }

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "rate control: lowRate={0} mainBitRate={1} voiced={2} maxPulses={3} importance={4}",
                    lowRate, mainBitRate, voiced, maxPulsesPerSubfr[IDX_MAIN], subfrImportance[IDX_MAIN]);
        }
        return new Allocation(maxPulsesPerSubfr, subfrImportance);
    }

    /**
     * Closes the feedback loop after a frame is coded.
     *
     * <p>For each active rate point either decays the smoothed bitrate delta toward neutral (inactive frames) or
     * integrates the relative error between the measured and target bitrate into the clamped smoothed delta and
     * recomputes the adjustment factor {@code max(1 - bitrateDeltaSmth, 0)}. The measured bitrate splits the per
     * packet TOC and byte rounding overhead evenly across the active rate points.
     *
     * @param frameMs            the frame length in milliseconds
     * @param framesPerPacket    the number of frames per packet
     * @param bitsUsed           the bits spent per rate point this frame, indexed by rate point with
     *                           {@value #MAX_RATES} entries
     * @param fecBitRate         the in band FEC bitrate in bits per second
     * @param mainBitRate        the main bitrate in bits per second
     * @param codedAsActiveVoice {@code true} when the frame was coded as active voice
     */
    public void updateScale(int frameMs, int framesPerPacket, float[] bitsUsed, int fecBitRate, int mainBitRate,
                            boolean codedAsActiveVoice) {
        var startR = startRate(fecBitRate, mainBitRate);
        var externalBits = 8.0f / (float) framesPerPacket / (MAX_RATES - startR);
        externalBits += 4.5f / (float) framesPerPacket / (MAX_RATES - startR);
        for (var r = startR; r <= IDX_MAIN; r++) {
            if (!codedAsActiveVoice) {
                var smthCoef = 1.0f - (float) frameMs * 0.00125f;
                bitrateDeltaSmth[r] *= smthCoef;
            } else {
                var bitRate = (r == IDX_FEC) ? (float) fecBitRate : (float) mainBitRate;
                var measuredBitrate = (bitsUsed[r] + externalBits) * (1000.0f / (float) frameMs);
                var measuredBitrateDelta = (measuredBitrate - bitRate) / bitRate;
                bitrateDeltaSmth[r] += measuredBitrateDelta * RATE_CONT_GAIN * frameMs / 20.0f;
                bitrateDeltaSmth[r] = Math.max(Math.min(bitrateDeltaSmth[r], RATE_CONT_CLAMP_MAX), RATE_CONT_CLAMP_MIN);
                adjustmentFactor[r] = Math.max(1.0f - bitrateDeltaSmth[r], 0.0f);
            }
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "rate control feedback: mainBitRate={0} adjustmentFactor={1} deltaSmth={2}",
                    mainBitRate, adjustmentFactor[IDX_MAIN], bitrateDeltaSmth[IDX_MAIN]);
        }
    }

    /**
     * Returns the rate point loop start index.
     *
     * <p>Reproduces the C operator precedence exactly: {@code +} binds tighter than {@code ||}, so the expression
     * evaluates {@code (IDX_FEC + (fecBitRate == 0)) || (fecBitRate == mainBitRate)} and yields a boolean
     * {@code 0} or {@code 1}, not an arithmetic sum. It is {@code 1} (the main rate point) whenever
     * {@code fecBitRate} is zero or equals {@code mainBitRate}, which is always the case on the no FEC scope.
     *
     * @param fecBitRate  the in band FEC bitrate in bits per second
     * @param mainBitRate the main bitrate in bits per second
     * @return {@value #IDX_FEC} when the FEC point is active, {@value #IDX_MAIN} otherwise
     */
    private static int startRate(int fecBitRate, int mainBitRate) {
        var expr = (IDX_FEC + (fecBitRate == 0 ? 1 : 0)) != 0 || (fecBitRate == mainBitRate);
        return expr ? 1 : 0;
    }

    /**
     * Evaluates the pulse target model.
     *
     * <p>A degree four polynomial in the kbit/s target plus a single exponential tail. The coefficients are read
     * as {@code float} (narrowed from the {@code double} table) to match the reference single precision reads,
     * and the cubic, quartic, and exponential terms use {@code (float) Math.pow} to match the reference
     * {@code powf}.
     *
     * @param rateKbps the target bitrate in kbit/s
     * @param coeff    the eight model coefficients for the payload bucket and rate class
     * @return the modelled pulses per 20 ms target
     */
    private static float bitrate2pulses(float rateKbps, double[] coeff) {
        var c0 = (float) coeff[0];
        var c1 = (float) coeff[1];
        var c2 = (float) coeff[2];
        var c3 = (float) coeff[3];
        var c4 = (float) coeff[4];
        var c5 = (float) coeff[5];
        var c6 = (float) coeff[6];
        var c7 = (float) coeff[7];
        return c0
                + c1 * rateKbps
                + c2 * rateKbps * rateKbps
                + c3 * (float) Math.pow(rateKbps, 3.0f)
                + c4 * (float) Math.pow(rateKbps, 4.0f)
                + c5 * (float) Math.pow(E, (rateKbps - c6) * c7);
    }

    /**
     * Evaluates the FEC pulse target model with low rate compensation.
     *
     * <p>Above {@value #RATE_THRES_KBPS} kbit/s this is the plain model; below it the target is interpolated
     * between the one pulse rate and the model value at the knee so the FEC stream does not undershoot to a single
     * pulse too early. The {@code onePulseRateBps} argument is the rate control threshold for the payload bucket
     * and rate class.
     *
     * @param rateKbps        the target bitrate in kbit/s
     * @param coeff           the eight model coefficients for the payload bucket and rate class
     * @param onePulseRateBps the one pulse rate threshold in bits per second
     * @return the compensated pulses per 20 ms target
     */
    private static float bitrate2pulsesHrFec(float rateKbps, double[] coeff, float onePulseRateBps) {
        if (rateKbps >= RATE_THRES_KBPS) {
            return bitrate2pulses(rateKbps, coeff);
        } else if (onePulseRateBps >= RATE_THRES_KBPS * 1000.0f) {
            return 1.0f;
        } else {
            var pulsesThres = bitrate2pulses(RATE_THRES_KBPS, coeff);
            var sc = (RATE_THRES_KBPS - rateKbps) / (RATE_THRES_KBPS - onePulseRateBps / 1000.0f);
            return pulsesThres - sc * (pulsesThres - 1.0f);
        }
    }

    /**
     * Computes the high rate unvoiced nonflatness threshold.
     *
     * <p>A line through {@code (10000 bps, 0.5)} and {@code (18000 bps, 0.0)} evaluated at the target bitrate,
     * with the bitrate first scaled by the square root of the speech activity probability, then clamped to the
     * range from {@code 0} to {@value #UV_NONFLATNESS_THR}. The scaling truncates back to {@code int} because the
     * {@code bitrate} parameter is an {@code int}, so the line is evaluated at the truncated bitrate.
     *
     * @param bitrate   the target bitrate in bits per second
     * @param spActProb the speech activity probability; must not be negative
     * @return the clamped nonflatness threshold
     */
    public static float hrNonflatThres(int bitrate, float spActProb) {
        var bitrates = HR_NONFLAT_BITRATES;
        var thresholds = HR_NONFLAT_THRESHOLDS;
        var a = (thresholds[1] - thresholds[0]) / (bitrates[1] - bitrates[0]);
        var b = thresholds[0] - a * bitrates[0];
        var scaledBitrate = (int) (bitrate * (float) Math.sqrt(spActProb + 1e-12f));
        return Math.min(Math.max(a * scaledBitrate + b, 0.0f), UV_NONFLATNESS_THR);
    }

    /**
     * Computes the spectral nonflatness of an LPC residual frame.
     *
     * <p>Partitions the residual into {@value #NON_FLAT_SUBFR_LEN} sample sub blocks, measures each sub block's
     * energy, prepends the carried cross frame energy state when the new energies dominate it, updates the state
     * with the trailing sub block energies, and returns the nonflatness ratio of the selected energy run plus a
     * small weighted LSF nonflatness term. Mutates {@code state} in place.
     *
     * @param resLpc    the LPC residual, read from {@code offset} for {@code length} samples
     * @param offset    the first residual sample offset within {@code resLpc}
     * @param length    the residual length in samples
     * @param wlsf      the weighted LSF vector, {@value #LPC_ORDER} entries
     * @param state     the cross frame energy state, {@value #NON_FLAT_STATE_LEN} entries, mutated in place
     * @return the combined residual and weighted LSF nonflatness measure
     */
    public static float nonflatness(float[] resLpc, int offset, int length, float[] wlsf, float[] state) {
        var nrgs = new float[NON_FLAT_NRGS_LEN];
        var n = length / NON_FLAT_SUBFR_LEN;
        for (var i = 0; i < n; i++) {
            nrgs[i + NON_FLAT_STATE_LEN] = nrg(resLpc, offset + i * NON_FLAT_SUBFR_LEN, NON_FLAT_SUBFR_LEN)
                    + NON_FLAT_SUBFR_LEN * 2e-10f;
        }
        var sumState = 0.0f;
        var sumNrgs = 0.0f;
        for (var i = 0; i < NON_FLAT_STATE_LEN; i++) {
            sumState += state[i];
            sumNrgs += nrgs[i + NON_FLAT_STATE_LEN];
        }

        var run = n;
        if (sumState < sumNrgs) {
            System.arraycopy(state, 0, nrgs, 0, NON_FLAT_STATE_LEN);
            run += NON_FLAT_STATE_LEN;
        }
        System.arraycopy(nrgs, length / NON_FLAT_SUBFR_LEN, state, 0, NON_FLAT_STATE_LEN);

        var base = (length / NON_FLAT_SUBFR_LEN) + NON_FLAT_STATE_LEN - run;
        return nonflat(nrgs, base, run) + 0.05f * nonflat(wlsf, 0, LPC_ORDER);
    }

    /**
     * Computes the nonflatness ratio of an energy run.
     *
     * <p>Returns {@code L * sumOfSquares / sum^2 - 1}, the excess of the mean square over the squared mean, or
     * {@code -1} when the squared sum is not positive. The sum is a strict left to right single precision
     * reduction; the sum of squares comes from {@link #nrg}.
     *
     * @param x      the energy values, read from {@code offset}
     * @param offset the first value offset within {@code x}
     * @param length the run length
     * @return the nonflatness ratio, or {@code -1.0} when the squared sum is not positive
     */
    private static float nonflat(float[] x, int offset, int length) {
        var sumx = 0.0f;
        for (var n = 0; n < length; n++) {
            sumx += x[offset + n];
        }
        var sumxSq = sumx * sumx;
        if (sumxSq <= 0.0f) {
            return -1.0f;
        }
        return (length * nrg(x, offset, length) / sumxSq) - 1.0f;
    }

    /**
     * Derives the {@code mainBitRate} and {@code fecBitRate} rate split from the requested total bitrate.
     *
     * <p>When in band FEC is enabled and the packet loss rate is at least one percent, splits the total bitrate
     * into an FEC portion (a loss dependent fraction floored at 4500 bps) and a main portion (floored at a loss
     * dependent minimum), with corrective rebalancing when the main rate would fall below its minimum, the FEC
     * rate below its floor, or the two rates collapse together; otherwise the whole bitrate is the main rate with
     * no FEC. Both rates are finally clamped to the codec rate range. On the 9600 bps no FEC scope this always
     * returns {@code (mainBitRate = bitRate, fecBitRate = 0)}.
     *
     * @param bitRate              the requested total bitrate in bits per second
     * @param useInBandFEC         {@code true} when in band FEC is enabled
     * @param packetLossPercentage the uplink packet loss percentage
     * @return the derived main and FEC bitrate split
     */
    public static RateSplit controlLbrr(int bitRate, boolean useInBandFEC, int packetLossPercentage) {
        int mainBitRate;
        int fecBitRate;
        if (useInBandFEC && packetLossPercentage >= 1) {
            var ratio = (packetLossPercentage - 2.0f) / (20.0f - 2.0f);
            ratio = Math.max(Math.min(ratio, 1.0f), 0.0f);
            var split = 0.25f + ratio * (0.5f - 0.25f);
            var minMainBitRate = (int) (12000.0f + ratio * (4500.0f - 12000.0f));
            fecBitRate = Math.max((int) rint(bitRate * split), 4500);
            mainBitRate = bitRate - fecBitRate;
            if (mainBitRate < minMainBitRate) {
                mainBitRate = minMainBitRate;
                fecBitRate = bitRate - mainBitRate;
            }
            if (fecBitRate < 4500) {
                fecBitRate = 0;
                mainBitRate = bitRate;
            }
            if ((mainBitRate - fecBitRate) <= 1000) {
                fecBitRate = bitRate / 2;
                mainBitRate = bitRate - fecBitRate;
            }
        } else {
            mainBitRate = bitRate;
            fecBitRate = 0;
        }
        mainBitRate = Math.max(Math.min(mainBitRate, 30000), 3000);
        fecBitRate = Math.max(Math.min(fecBitRate, 30000), 0);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "rate split: requested={0} main={1} fec={2}", bitRate, mainBitRate, fecBitRate);
        }
        return new RateSplit(mainBitRate, fecBitRate);
    }

    /**
     * Computes the sum of squares of a value run.
     *
     * <p>Reproduces the four wide reduction the reference codec's fast math build compiles to: four lane
     * accumulators each sum {@code x[4k + lane]^2} left to right, then a horizontal reduction folds them as
     * {@code (lane0 + lane2) + (lane1 + lane3)}, and a scalar tail adds the final {@code length % 4} squares left
     * to right. A naive left to right sum disagrees by tens of thousands of ULP on long runs.
     *
     * @param x      the values, read from {@code offset}
     * @param offset the first value offset within {@code x}
     * @param length the run length
     * @return the single precision sum of squares
     */
    private static float nrg(float[] x, int offset, int length) {
        var lane0 = 0.0f;
        var lane1 = 0.0f;
        var lane2 = 0.0f;
        var lane3 = 0.0f;
        var vecEnd = length & ~3;
        for (var n = 0; n < vecEnd; n += 4) {
            var x0 = x[offset + n];
            var x1 = x[offset + n + 1];
            var x2 = x[offset + n + 2];
            var x3 = x[offset + n + 3];
            lane0 += x0 * x0;
            lane1 += x1 * x1;
            lane2 += x2 * x2;
            lane3 += x3 * x3;
        }
        var nrg = (lane0 + lane2) + (lane1 + lane3);
        for (var n = vecEnd; n < length; n++) {
            nrg += x[offset + n] * x[offset + n];
        }
        return nrg;
    }

    /**
     * Evaluates the numerically guarded logistic sigmoid.
     *
     * <p>Returns {@code 1 / (1 + exp(-x))}, saturating to {@code 1.0} above {@code 80} and {@code 0.0} below
     * {@code -80} to keep the exponential out of the overflow and denormal ranges.
     *
     * @param x the logit
     * @return the sigmoid in {@code [0, 1]}
     */
    private static float sigmoid(float x) {
        if (x > 80.0f) {
            return 1.0f;
        }
        if (x < -80.0f) {
            return 0.0f;
        }
        return 1.0f / (1.0f + (float) Math.exp(-x));
    }

    /**
     * Rounds a single precision value to the nearest integer with halves away from zero.
     *
     * <p>The rounding matches C {@code roundf}: half way cases round away from zero, unlike
     * {@link Math#round(float)}, which rounds half up. It applies the away from zero rule on the single precision
     * input.
     *
     * @param x the value to round, evaluated in single precision
     * @return the nearest integer with halves away from zero
     */
    private static int rint(float x) {
        return (int) (x < 0.0f ? Math.ceil(x - 0.5f) : Math.floor(x + 0.5f));
    }
}
