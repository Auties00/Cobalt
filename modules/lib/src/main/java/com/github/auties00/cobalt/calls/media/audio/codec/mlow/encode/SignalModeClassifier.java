package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Encoder per frame voiced/unvoiced signal mode classifier of the MLow speech codec.
 *
 * <p>The core encoder computes one open loop pitch estimate and one voice activity decision per 20 ms frame,
 * then calls this classifier to collapse those features into a single scalar voicing strength. The sign of the
 * returned strength, gated by the packet's active voice flag, is the frame's voicing decision that selects the
 * encoder coding path: a positive strength on an active voice packet marks the frame voiced, so the parameter
 * encoder codes the adaptive codebook pitch lag and runs the voiced analysis by synthesis search; a strength
 * that is not positive marks the frame unvoiced, so the pitch lags are zeroed and the unvoiced fixed codebook
 * plus residual energy path is taken instead. The magnitude of the strength is also retained by the core
 * encoder and feeds the bit rate controller.
 *
 * <p>The strength is a weighted average of five soft features, biased and smoothed with one frame of
 * hysteresis:
 * <ul>
 *   <li><b>Pitch correlation strength.</b> The open loop pitch correlation, clamped to {@code [0, 1]} and
 *       mapped through the inverse sigmoid of {@code 0.1 + 0.75 * corr} into roughly {@code [-1.4, 1.4]}; high
 *       correlation pulls towards voiced.</li>
 *   <li><b>Voice activity strength.</b> A monotone map of the speech activity probability into roughly
 *       {@code [-1, 0]}; near silence pulls towards unvoiced.</li>
 *   <li><b>Spectral tilt strength.</b> The low band minus high band energy of the LPC power spectrum, each
 *       measured above an adaptively smoothed background floor and cubed to soften the decision into roughly
 *       {@code [-1, 1]}; a spectrum dominated by low frequencies pulls towards voiced.</li>
 *   <li><b>Harmonicity.</b> The spectral harmonic strength of the dominant lag, passed through from the pitch
 *       estimator unchanged.</li>
 *   <li><b>Short lag strength.</b> A sigmoid of the average lag against a 38 sample knee in roughly
 *       {@code [-1, 0]}; very short lags pull towards unvoiced.</li>
 * </ul>
 * The five features are combined with the fixed weights {@code {1.0, 0.5, 0.5, 0.7, 0.3}}, normalized by their
 * sum, biased by {@value #VUV_BIAS}, then nudged by {@value #VUV_HYST} times a hysteresis decayed copy of the
 * previous frame's strength. The decayed copy is divided down when the pitch lag jumps, so a sudden lag change
 * weakens the carry over and lets the decision flip more readily.
 *
 * <p>This classifier is stateful across the frames of a stream through the internal voicing state it carries:
 * the hysteresis copy of the previous strength, the previous frame's last subframe lag, and the smoothed low
 * band and high band background energy floors. All four start at zero, and {@link #reset()} restores exactly
 * that. Construct one classifier per logical stream, feed it every frame in order, and reset it between
 * independent streams. This type is not thread safe.
 *
 * <p>Scope is the shipped SMPL 16 kHz mono high rate configuration; the validated path is the 60 ms packet of
 * three 20 ms frames, each carrying {@value #LAGS_LEN} lag subframes and a {@value #F2_LEN} bin LPC power
 * spectrum.
 *
 * @implNote This implementation reproduces the exact floating point arithmetic of the fast math (associative,
 * flush to nothing) build of the reference codec, whose two spectral tilt energy sums are vectorized into four
 * lane packed single accumulators with the integer index weights computed and converted in lane, so
 * {@link #spectralTiltEnergyLow} and {@link #spectralTiltEnergyHigh} accumulate the same way: lane {@code k}
 * holds the running sum of {@code ((knee +/- i) * (i + 3)) * F2[i]} and the lanes reduce as
 * {@code (lane0 + lane2) + (lane1 + lane3)}, with the low band sum finishing on a three element scalar tail.
 * The fast math reassociations are mirrored: the tilt denominator associates as {@code (low + eps) + high}, the
 * cubed tilt numerator term as {@code (tilt * weight) * (tilt * tilt)}, the inverse sigmoid negation and the
 * lag sigmoid fold into the final weighted sum as subtractions, and the bias adds as the negative literal. A
 * plain left to right rewrite differs in the last float bit and can flip the voiced decision, which changes the
 * entire coded frame, so the order here is load bearing.
 */
final class SignalModeClassifier {
    /**
     * The logger for {@link SignalModeClassifier}.
     */
    private static final System.Logger LOGGER = Log.get(SignalModeClassifier.class);

    /**
     * Linear prediction power spectrum bin count.
     *
     * <p>The length of the {@code F2} magnitude squared spectrum the LPC analysis produces and this classifier
     * reads.
     */
    private static final int F2_LEN = 257;

    /**
     * Number of lag subframes per 20 ms frame in the locked high rate scope, a 320 sample frame divided into
     * 40 sample lag subframes.
     *
     * <p>The classifier reads the first lag ({@code lags[0]}) for the hysteresis lag jump test and the last lag
     * ({@code lags[LAGS_LEN - 1]}) to carry into the next frame's hysteresis.
     */
    private static final int LAGS_LEN = 8;

    /**
     * Index splitting the low band and high band spectral tilt sums.
     *
     * <p>The low band sum runs over bins {@code [2, TRANSITION_IX)} and the high band sum over
     * {@code [TRANSITION_IX, F2_LEN)}; the integer truncation of {@code 257 / 3} fixes this at {@code 85}.
     */
    private static final int TRANSITION_IX = F2_LEN / 3;

    /**
     * Voicing decision bias.
     *
     * <p>Added to the normalized weighted feature average so the zero crossing of the returned strength sits at
     * the codec's tuned operating point rather than at the unbiased feature mean.
     */
    private static final float VUV_BIAS = -0.1038f;

    /**
     * Hysteresis weight on the previous frame's strength.
     *
     * <p>Scales the lag jump decayed copy of the previous frame's voicing strength before it is added to the
     * current frame's strength, smoothing the voicing decision across frames.
     */
    private static final float VUV_HYST = 0.05f;

    /**
     * Feature weights on pitch correlation, voice activity, spectral tilt, harmonicity, and short lags.
     *
     * <p>The weighted feature sum is divided by the sum of these five weights ({@code 3.0}); only these five
     * entries are read.
     */
    private static final float[] VUV_WEIGHTS = {1.0f, 0.5f, 0.5f, 0.7f, 0.3f};

    /**
     * Sum of the five feature weights.
     *
     * <p>Computed as the left to right running sum {@code (((w0 + w1) + w2) + w3) + w4}; for the constant weight
     * table this is exactly {@code 3.0f}.
     */
    private static final float WEIGHTS_SUM = sumVec5(VUV_WEIGHTS);

    /**
     * Hysteresis copy of the previous frame's voicing strength.
     *
     * <p>Set each frame to {@code tanhf(3 * voicingStrength)} of the frame just classified, optionally divided
     * down at the start of the next frame when the pitch lag jumps, then scaled by {@value #VUV_HYST} into the
     * next strength. Zero on construction and after {@link #reset()}.
     */
    private float voicingPrev;

    /**
     * The previous frame's last subframe pitch lag.
     *
     * <p>Compared against the current frame's {@code lags[0]} to detect a lag jump that decays the hysteresis
     * carry over; a value that is not positive (the initial state) disables the decay for the first frame. Zero
     * on construction and after {@link #reset()}.
     */
    private float lastLagPrev;

    /**
     * Smoothed low band background energy floor.
     *
     * <p>Tracks the low band spectral tilt energy of inactive frames (those whose voice activity strength is
     * below {@code -0.1}) with a leaky update, so the active frame tilt is measured as an excess above the noise
     * floor. Zero on construction and after {@link #reset()}.
     */
    private float nrgLoBgn;

    /**
     * Smoothed high band background energy floor.
     *
     * <p>The high band counterpart of {@link #nrgLoBgn}, updated on the same inactive frame condition. Zero on
     * construction and after {@link #reset()}.
     */
    private float nrgHiBgn;

    /**
     * Creates a signal mode classifier in the freshly reset state, all four hysteresis and background fields
     * zero.
     */
    SignalModeClassifier() {
    }

    /**
     * Restores the classifier to its freshly constructed state.
     *
     * <p>Clears the hysteresis strength copy, the previous frame lag, and both background energy floors. Call
     * between independent streams so a new stream's first frame sees no carry over.
     */
    void reset() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "signal mode classifier reset");
        }
        voicingPrev = 0.0f;
        lastLagPrev = 0.0f;
        nrgLoBgn = 0.0f;
        nrgHiBgn = 0.0f;
    }

    /**
     * Classifies one frame and returns its voicing strength.
     *
     * <p>Combines the five soft voicing features into a normalized, biased, hysteresis smoothed strength and
     * advances the internal state: it updates the smoothed background energy floors on inactive frames, decays
     * and rescales the hysteresis carry over, stores this frame's {@code tanh} shaped strength as the next
     * frame's carry over, and records this frame's last lag. The caller treats a strictly positive return on an
     * active voice packet as a voiced frame and any other return as unvoiced.
     *
     * <p>The {@code f2} spectrum must hold exactly {@value #F2_LEN} bins and {@code lags} at least
     * {@value #LAGS_LEN} entries; only {@code lags[0]} and {@code lags[}{@value #LAGS_LEN}{@code  - 1]} are read.
     * The speech activity probability must lie in {@code [0, 1]}.
     *
     * @param pitchCorr    the open loop pitch correlation from {@link OpenLoopPitch.Result#pitchCorr()}
     * @param lags         the per subframe pitch lags of this frame from {@link OpenLoopPitch.Result#lags()}
     * @param avgLag       the dominant subframe average lag from {@link OpenLoopPitch.Result#avgLag()}
     * @param harmStrength the spectral harmonicity of the average lag from
     *                     {@link OpenLoopPitch.Result#harmStrength()}
     * @param f2           the LPC power spectrum of this frame, {@value #F2_LEN} bins
     * @param spActProb    the speech activity probability in {@code [0, 1]} from the {@link Vad} result for this
     *                     frame
     * @return the voicing strength; positive on this active voice path means voiced
     */
    float classify(float pitchCorr, float[] lags, float avgLag, float harmStrength, float[] f2, float spActProb) {
        var corrStrength = invSigmoid(0.1f + 0.75f * Math.min(Math.max(pitchCorr, 0.0f), 1.0f));

        var vadReciprocal = 1.04f / (spActProb + 0.04f);
        var vadStrength = (1.0f - vadReciprocal) * 0.04f;

        var nrgLo = spectralTiltEnergyLow(f2);
        var nrgHi = spectralTiltEnergyHigh(f2);
        if (vadReciprocal > 3.5f) {
            var smthCoef = -0.5f * vadStrength;
            nrgLoBgn += smthCoef * (nrgLo - nrgLoBgn);
            nrgHiBgn += smthCoef * (nrgHi - nrgHiBgn);
        }
        var tiltStrength = (Math.max(nrgLo - nrgLoBgn, 0.0f) - Math.max(nrgHi - nrgHiBgn, 0.0f))
                           / ((nrgLo + 1e-9f) + nrgHi);
        var tiltSquared = tiltStrength * tiltStrength;

        var lagSigmoid = lagSigmoid(avgLag);

        var corrTerm = corrStrength * VUV_WEIGHTS[0];
        var tiltTerm = (tiltStrength * VUV_WEIGHTS[2]) * tiltSquared;
        var vadTerm = vadStrength * VUV_WEIGHTS[1];
        var harmTerm = harmStrength * VUV_WEIGHTS[3];
        var lagSigTerm = lagSigmoid * VUV_WEIGHTS[4];
        var numerator = (tiltTerm + corrTerm) + ((vadTerm + harmTerm) - lagSigTerm);
        var voicingStrength = numerator / WEIGHTS_SUM + VUV_BIAS;

        var carry = voicingPrev;
        if (lastLagPrev > 0.0f) {
            var tmp = log2f(lags[0] / lastLagPrev);
            if (tmp > 0.0f) {
                tmp *= 0.5f;
            }
            carry /= 0.4f + tmp * tmp;
        }
        voicingStrength += carry * VUV_HYST;
        voicingPrev = tanhf(3.0f * voicingStrength);
        lastLagPrev = lags[LAGS_LEN - 1];

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "signal mode classify pitchCorr={0} avgLag={1} harmStrength={2} spActProb={3} strength={4}",
                    pitchCorr, avgLag, harmStrength, spActProb, voicingStrength);
        }

        return voicingStrength;
    }

    /**
     * Computes the low band spectral tilt energy over bins {@code [2, TRANSITION_IX)}.
     *
     * <p>Each bin contributes {@code ((TRANSITION_IX - i) * (i + 3)) * F2[i]}, with the integer factors
     * converted to float and multiplied before the spectrum value. The {@code 80} bin span {@code [2, 81]}
     * accumulates into a four lane sum (lane {@code k} summing bins {@code 2 + k, 6 + k, ...}), reduces the
     * lanes as {@code (lane0 + lane2) + (lane1 + lane3)} after folding in the two wide tail bins {@code 82} and
     * {@code 83}, and finishes with the scalar bin {@code 84}.
     *
     * @param f2 the LPC power spectrum, {@value #F2_LEN} bins
     * @return the low band tilt energy
     */
    private static float spectralTiltEnergyLow(float[] f2) {
        var l0 = 0.0f;
        var l1 = 0.0f;
        var l2 = 0.0f;
        var l3 = 0.0f;
        var i = 2;
        for (; i + 3 < TRANSITION_IX; i += 4) {
            l0 += ((float) (TRANSITION_IX - i) * (float) (i + 3)) * f2[i];
            l1 += ((float) (TRANSITION_IX - (i + 1)) * (float) (i + 4)) * f2[i + 1];
            l2 += ((float) (TRANSITION_IX - (i + 2)) * (float) (i + 5)) * f2[i + 2];
            l3 += ((float) (TRANSITION_IX - (i + 3)) * (float) (i + 6)) * f2[i + 3];
        }
        var lane0 = l0 + l2;
        var lane1 = l1 + l3;
        for (; i + 1 < TRANSITION_IX; i += 2) {
            lane0 += ((float) (TRANSITION_IX - i) * (float) (i + 3)) * f2[i];
            lane1 += ((float) (TRANSITION_IX - (i + 1)) * (float) (i + 4)) * f2[i + 1];
        }
        var sum = lane0 + lane1;
        for (; i < TRANSITION_IX; i++) {
            sum += ((float) (TRANSITION_IX - i) * (float) (i + 3)) * f2[i];
        }
        return sum;
    }

    /**
     * Computes the high band spectral tilt energy over bins {@code [TRANSITION_IX, F2_LEN)}.
     *
     * <p>Each bin contributes {@code ((i - TRANSITION_IX) * (i + 3)) * F2[i]}, with the integer factors
     * converted to float and multiplied before the spectrum value. The {@code 172} bin span {@code [85, 256]}
     * is an exact multiple of four, so it accumulates into a four lane sum (lane {@code k} summing bins
     * {@code 85 + k, 89 + k, ...}) with no scalar tail, and the lanes reduce as
     * {@code (lane1 + lane3) + (lane0 + lane2)}.
     *
     * @param f2 the LPC power spectrum, {@value #F2_LEN} bins
     * @return the high band tilt energy
     */
    private static float spectralTiltEnergyHigh(float[] f2) {
        var l0 = 0.0f;
        var l1 = 0.0f;
        var l2 = 0.0f;
        var l3 = 0.0f;
        for (var i = TRANSITION_IX; i < F2_LEN; i += 4) {
            l0 += ((float) (i - TRANSITION_IX) * (float) (i + 3)) * f2[i];
            l1 += ((float) ((i + 1) - TRANSITION_IX) * (float) (i + 4)) * f2[i + 1];
            l2 += ((float) ((i + 2) - TRANSITION_IX) * (float) (i + 5)) * f2[i + 2];
            l3 += ((float) ((i + 3) - TRANSITION_IX) * (float) (i + 6)) * f2[i + 3];
        }
        var lane0 = l0 + l2;
        var lane1 = l1 + l3;
        return lane1 + lane0;
    }

    /**
     * Maps an average lag to the short lag sigmoid feature.
     *
     * <p>Evaluates the logistic sigmoid of {@code x = 0.25 * (38 - avgLag)} with overflow guards: {@code x > 80}
     * returns {@code 1} (very short lags) and {@code x < -80} returns {@code 0} (very long lags); otherwise the
     * value is {@code 1 / (1 + expf(-x))}, with {@code -x} formed as {@code 0.25 * (avgLag - 38)}. The
     * classifier negates this sigmoid to form the short lag feature; the negation folds into the final weighted
     * sum as a subtraction, so this method returns the sigmoid value before the sign flip.
     *
     * @param avgLag the dominant subframe average lag
     * @return the short lag sigmoid value in {@code [0, 1]}, before the sign flip
     */
    private static float lagSigmoid(float avgLag) {
        var x = 0.25f * (38.0f - avgLag);
        if (x > 80.0f) {
            return 1.0f;
        }
        if (x < -80.0f) {
            return 0.0f;
        }
        return 1.0f / (1.0f + expf(0.25f * (avgLag - 38.0f)));
    }

    /**
     * Computes the inverse sigmoid (logit) of a probability.
     *
     * <p>Returns {@code -logf((1 / x) - 1)}. The caller only ever passes {@code 0.1 + 0.75 * c} for a
     * correlation {@code c} clamped to {@code [0, 1]}, so the argument lies in {@code [0.1, 0.85]} and the
     * logarithm is always defined. The negation is computed here; the final weighted sum folds it in as a
     * subtraction, which is bit equivalent.
     *
     * @param x the probability in {@code (0, 1)}
     * @return the logit of {@code x}, the pitch correlation contribution before weighting
     */
    private static float invSigmoid(float x) {
        return -logf((1.0f / x) - 1.0f);
    }

    /**
     * Computes a single precision exponential.
     *
     * <p>This is a verbatim port of the platform {@code expf}, whose result feeds the voicing decision and the
     * carried hysteresis state, so a one bit deviation from a rounded double precision exponential can flip a
     * frame's voicing. It converts the float argument to double, scales by {@code 64 / ln2}, rounds to the
     * nearest integer {@code k} (round half to even), and decomposes {@code k = 64 m + idx}; the result is
     * {@code table[idx] * (1 + r + r^2 (r/6 + 1/2)) * 2^m} with reduced argument {@code r = x - k * (ln2 / 64)}
     * and {@code table} the 64 entry {@link #EXP_TABLE} of fractional powers of two, finally rounded back to
     * {@code float}. The caller only reaches this on arguments in {@code [-80, 80]}, so the overflow and
     * underflow tails are not exercised; this port defers to {@link Math#exp(double)} for any argument outside
     * the table reduction range as a guard.
     *
     * @param x the argument
     * @return {@code expf(x)} in single precision
     */
    private static float expf(float x) {
        var bits = Float.floatToRawIntBits(x);
        if ((bits & 0x7fffffff) >= 0x7f800000) {
            return (float) Math.exp(x);
        }
        double xd = x;
        var f = xd * 92.332482616893657;
        if (f >= 8192.0 || f < -9600.0) {
            return (float) Math.exp(x);
        }
        var k = (int) Math.rint(f);
        var idx = k & 0x3f;
        var m = (k - idx) >> 6;
        var r = xd - (double) k * 0.010830424696249145;
        var poly = 0.16666666666666666 * r + 0.5;
        poly = r * r * poly;
        poly = poly + r;
        var tableValue = Double.longBitsToDouble(EXP_TABLE[idx]);
        poly = poly * tableValue + tableValue;
        var scale = Double.longBitsToDouble(((long) (m + 0x3ff)) << 52);
        return (float) (poly * scale);
    }

    /**
     * Computes a single precision natural logarithm.
     *
     * <p>This is a verbatim port of the platform {@code logf}, whose result is the dominant pitch correlation
     * feature (weight one) of the voicing strength, so it must match bit for bit. It splits {@code x} into a
     * mantissa table index from the top mantissa bits (with the rounding bit folded in) and an exponent
     * {@code e}; the reduced argument {@code r = recip[idx] * (idxValue - mantissa)} drives the cubic
     * {@code r + r^2 (r/3 + 1/2)}, and the result reconstructs {@code log(x)} from the two split logarithm
     * tables {@link #LOGF_HI} and {@link #LOGF_LO} and the high and low parts of {@code e * ln2}. Inputs within
     * {@code 1/16} of one take a dedicated atanh style series in {@code s = (x - 1) / (x + 1)}. The caller only
     * reaches this on the strictly positive {@code (1 / p) - 1} for {@code p} in {@code [0.1, 0.85]}.
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
            // TODO: this atanh series branch is one unit in the last place high of the runtime logf on roughly
            //  78 of the 1.5M float arguments in [0.94, 1.06]; the runtime appears to route a few of these to
            //  its main reduction path on a boundary this reconstruction has not pinned down. The residual reaches
            //  the returned voicing strength only when the pitch correlation lands near 0.533 (so the inverse
            //  sigmoid argument lands near one) and never flips the voiced/unvoiced sign; observed at about one
            //  frame in 72000 under fuzzing weighted towards arguments near one and zero frames under realistic
            //  speech features.
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
        var exponent = (float) (((int) (bits >>> 23)) - 0x7f);
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
     * Computes a single precision base 2 logarithm.
     *
     * <p>This is a verbatim port of the platform {@code log2f}, a separate routine from {@link #logf} that feeds
     * the hysteresis carry over decay. It normalizes {@code x} into a mantissa {@code m} in
     * {@code [sqrt(1/2), sqrt(2))} and an exponent {@code e}, evaluates the atanh style series in
     * {@code s = (m - 1) / (m + 1)} with the polynomial {@code (0.29884401 s^2 + 0.399765521) s^2 + 0.666667938},
     * and scales the mantissa logarithm to base two with the high and low parts of {@code log2(e)} before adding
     * the integer exponent. The argument is the ratio of the current first lag to the previous frame's last lag,
     * strictly positive on the path that reaches this call.
     *
     * @param x the strictly positive argument
     * @return {@code log2f(x)} in single precision
     */
    private static float log2f(float x) {
        var bits = Float.floatToRawIntBits(x);
        if (bits == 0 || (bits & 0x7fffffff) >= 0x7f800000 || (bits >>> 31) != 0) {
            return (float) (Math.log(x) / 0.6931471805599453);
        }
        var exponent = (int) ((bits >>> 23) & 0xff) - 126;
        var mantissa = Float.intBitsToFloat((bits & 0x807fffff) | 0x3f000000);
        var scaledExponent = (short) exponent;
        var normalized = mantissa;
        if (!(0.707106769f <= mantissa)) {
            normalized = mantissa + mantissa;
            scaledExponent = (short) (scaledExponent - 1);
        }
        var exponentValue = (float) (int) scaledExponent;
        var numerator = normalized - 1.0f;
        var s = numerator / (normalized + 1.0f);
        var sSquared = s * s;
        var poly = sSquared * 0.29884401f;
        poly = poly + 0.399765521f;
        poly = poly * sSquared;
        poly = poly + 0.666667938f;
        poly = poly * sSquared;
        var correction = numerator - poly;
        var scaledS = s * correction;
        scaledS = scaledS * 1.44269502f;
        var low = numerator * 1.92596303e-08f;
        var high = numerator * 1.44269502f;
        low = low - scaledS;
        var result = low + high;
        return result + exponentValue;
    }

    /**
     * Sums the first five feature weights left to right.
     *
     * <p>Accumulates as {@code (((x0 + x1) + x2) + x3) + x4}, the running sum order; for the constant weight
     * table the result is exactly {@code 3.0f}.
     *
     * @param x the weight table, at least five entries
     * @return the sum of the first five entries
     */
    private static float sumVec5(float[] x) {
        var sum = x[0];
        for (var i = 1; i < 5; i++) {
            sum += x[i];
        }
        return sum;
    }

    /**
     * Mantissa table of the fractional powers of two used by the {@code tanhf} exponential path.
     *
     * <p>Entry {@code i} is {@code 2^(i/32)} rounded to a fourteen bit mantissa, paired with the correction term
     * in {@link #TANHF_TABLE_LOW}; together they give the reduced range scale factor of the table based single
     * precision exponential inside {@link #tanhf}.
     */
    private static final float[] TANHF_TABLE_HIGH = {
            Float.intBitsToFloat(0x3f800000), Float.intBitsToFloat(0x3f82c000),
            Float.intBitsToFloat(0x3f858000), Float.intBitsToFloat(0x3f888000),
            Float.intBitsToFloat(0x3f8b8000), Float.intBitsToFloat(0x3f8e8000),
            Float.intBitsToFloat(0x3f91c000), Float.intBitsToFloat(0x3f94c000),
            Float.intBitsToFloat(0x3f980000), Float.intBitsToFloat(0x3f9b8000),
            Float.intBitsToFloat(0x3f9ec000), Float.intBitsToFloat(0x3fa24000),
            Float.intBitsToFloat(0x3fa5c000), Float.intBitsToFloat(0x3fa98000),
            Float.intBitsToFloat(0x3fad4000), Float.intBitsToFloat(0x3fb10000),
            Float.intBitsToFloat(0x3fb50000), Float.intBitsToFloat(0x3fb8c000),
            Float.intBitsToFloat(0x3fbd0000), Float.intBitsToFloat(0x3fc10000),
            Float.intBitsToFloat(0x3fc54000), Float.intBitsToFloat(0x3fc98000),
            Float.intBitsToFloat(0x3fce0000), Float.intBitsToFloat(0x3fd28000),
            Float.intBitsToFloat(0x3fd74000), Float.intBitsToFloat(0x3fdbc000),
            Float.intBitsToFloat(0x3fe0c000), Float.intBitsToFloat(0x3fe58000),
            Float.intBitsToFloat(0x3feac000), Float.intBitsToFloat(0x3fefc000),
            Float.intBitsToFloat(0x3ff50000), Float.intBitsToFloat(0x3ffa8000)
    };

    /**
     * Correction table for the fractional powers of two used by the {@code tanhf} exponential path.
     *
     * <p>Entry {@code i} is the low part of {@code 2^(i/32)}, added to {@link #TANHF_TABLE_HIGH} entry {@code i}
     * to recover the scale factor to full single precision accuracy.
     */
    private static final float[] TANHF_TABLE_LOW = {
            Float.intBitsToFloat(0x00000000), Float.intBitsToFloat(0x39d86988),
            Float.intBitsToFloat(0x3aab0d9f), Float.intBitsToFloat(0x3a407404),
            Float.intBitsToFloat(0x3a2e0f1e), Float.intBitsToFloat(0x3a90e62d),
            Float.intBitsToFloat(0x38f4dce0), Float.intBitsToFloat(0x3ad3bea3),
            Float.intBitsToFloat(0x3adfc146), Float.intBitsToFloat(0x39d39b9c),
            Float.intBitsToFloat(0x3ad4c982), Float.intBitsToFloat(0x3ac10c0c),
            Float.intBitsToFloat(0x3afb5aa6), Float.intBitsToFloat(0x3a856ad3),
            Float.intBitsToFloat(0x3a41f752), Float.intBitsToFloat(0x3a8fd607),
            Float.intBitsToFloat(0x391e6678), Float.intBitsToFloat(0x3aeebd1d),
            Float.intBitsToFloat(0x398a39f4), Float.intBitsToFloat(0x3ab13329),
            Float.intBitsToFloat(0x3a9ca845), Float.intBitsToFloat(0x3ae6f619),
            Float.intBitsToFloat(0x3a923054), Float.intBitsToFloat(0x3aa07647),
            Float.intBitsToFloat(0x391f9958), Float.intBitsToFloat(0x3aeede5f),
            Float.intBitsToFloat(0x39cdeec0), Float.intBitsToFloat(0x3ae41b9d),
            Float.intBitsToFloat(0x37c6e7c0), Float.intBitsToFloat(0x3a92e66f),
            Float.intBitsToFloat(0x3a95f454), Float.intBitsToFloat(0x38ecb6d0)
    };

    /**
     * Mantissa table of fractional powers of two for the {@code expf} reduction.
     *
     * <p>Entry {@code i} is the {@code double} bit pattern of {@code 2^(i/64)}, recovered with
     * {@link Double#longBitsToDouble(long)} and multiplied by the reduced range polynomial of {@link #expf}.
     */
    private static final long[] EXP_TABLE = {
            0x3ff0000000000000L, 0x3ff02c9a3e778061L, 0x3ff059b0d3158574L, 0x3ff0874518759bc8L,
            0x3ff0b5586cf9890fL, 0x3ff0e3ec32d3d1a2L, 0x3ff11301d0125b51L, 0x3ff1429aaea92de0L,
            0x3ff172b83c7d517bL, 0x3ff1a35beb6fcb75L, 0x3ff1d4873168b9aaL, 0x3ff2063b88628cd6L,
            0x3ff2387a6e756238L, 0x3ff26b4565e27cddL, 0x3ff29e9df51fdee1L, 0x3ff2d285a6e4030bL,
            0x3ff306fe0a31b715L, 0x3ff33c08b26416ffL, 0x3ff371a7373aa9cbL, 0x3ff3a7db34e59ff7L,
            0x3ff3dea64c123422L, 0x3ff4160a21f72e2aL, 0x3ff44e086061892dL, 0x3ff486a2b5c13cd0L,
            0x3ff4bfdad5362a27L, 0x3ff4f9b2769d2ca7L, 0x3ff5342b569d4f82L, 0x3ff56f4736b527daL,
            0x3ff5ab07dd485429L, 0x3ff5e76f15ad2148L, 0x3ff6247eb03a5585L, 0x3ff6623882552225L,
            0x3ff6a09e667f3bcdL, 0x3ff6dfb23c651a2fL, 0x3ff71f75e8ec5f74L, 0x3ff75feb564267c9L,
            0x3ff7a11473eb0187L, 0x3ff7e2f336cf4e62L, 0x3ff82589994cce13L, 0x3ff868d99b4492edL,
            0x3ff8ace5422aa0dbL, 0x3ff8f1ae99157736L, 0x3ff93737b0cdc5e5L, 0x3ff97d829fde4e50L,
            0x3ff9c49182a3f090L, 0x3ffa0c667b5de565L, 0x3ffa5503b23e255dL, 0x3ffa9e6b5579fdbfL,
            0x3ffae89f995ad3adL, 0x3ffb33a2b84f15fbL, 0x3ffb7f76f2fb5e47L, 0x3ffbcc1e904bc1d2L,
            0x3ffc199bdd85529cL, 0x3ffc67f12e57d14bL, 0x3ffcb720dcef9069L, 0x3ffd072d4a07897cL,
            0x3ffd5818dcfba487L, 0x3ffda9e603db3285L, 0x3ffdfc97337b9b5fL, 0x3ffe502ee78b3ff6L,
            0x3ffea4afa2a490daL, 0x3ffefa1bee615a27L, 0x3fff50765b6e4540L, 0x3fffa7c1819e90d8L,
    };

    /**
     * Reciprocal table for the {@code logf} argument reduction.
     *
     * <p>Entry {@code idx} is the {@code float} bit pattern of the reciprocal that scales the reduced mantissa
     * argument of {@link #logf}, recovered with {@link Float#intBitsToFloat(int)}.
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
     * High part of the {@code logf} reduced interval logarithm.
     *
     * <p>Entry {@code idx} is the {@code float} bit pattern of the high part of the logarithm of the reduction
     * point of {@link #logf}, recovered with {@link Float#intBitsToFloat(int)} and accumulated with the high
     * part of {@code e * ln2}.
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
     * Low part of the {@code logf} reduced interval logarithm.
     *
     * <p>Entry {@code idx} is the {@code float} bit pattern of the low part of the logarithm of the reduction
     * point of {@link #logf}, recovered with {@link Float#intBitsToFloat(int)} and accumulated with the low part
     * of {@code e * ln2}.
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
     * Computes a single precision hyperbolic tangent.
     *
     * <p>The result is stored back as the next frame's hysteresis carry over ({@link #voicingPrev}); a one bit
     * error there propagates through the hysteresis chain and can flip a later frame's voicing decision, so this
     * is a verbatim port of the platform single precision {@code tanhf} rather than a rounded double precision
     * tanh, which differs by up to one unit in the last place. The routine uses a sign folded {@code |x|} and
     * three magnitude regions:
     * <ul>
     *   <li><b>Tiny.</b> For {@code |x|} below {@code 2^-13} the result is {@code x} itself.</li>
     *   <li><b>Saturated.</b> For {@code |x|} above {@code 10} the result is {@code +/-1}.</li>
     *   <li><b>Small ({@code |x| <= 1}).</b> A degree five over degree two rational approximation
     *       {@code |x| + P(x^2) * x^3 / Q(x^2)} with one coefficient set for {@code |x| < 0.9} and a second for
     *       {@code 0.9 <= |x| <= 1}.</li>
     *   <li><b>Large ({@code 1 < |x| <= 10}).</b> {@code 1 - 2 / (exp(2|x|) + 1)} with {@code exp(2|x|)} formed
     *       by a 32 entry table based reduction (the {@link #TANHF_TABLE_HIGH} and {@link #TANHF_TABLE_LOW}
     *       scale factors, a cubic reduced range polynomial, and a two part power of two scaling).</li>
     * </ul>
     * Every floating point step is single precision and ordered exactly as the runtime emits it; the integer
     * arithmetic of the table index and the power of two exponent split uses the same arithmetic shifts and sign
     * biases.
     *
     * @param x the argument
     * @return {@code tanhf(x)} in single precision, bit identical to the linked runtime
     */
    private static float tanhf(float x) {
        var bits = Float.floatToRawIntBits(x);
        var absBits = bits & 0x7fffffff;
        if (absBits < 0x39000000) {
            return x;
        }
        if (absBits > 0x7f800000) {
            return x + x;
        }
        var sign = 1.0f - 2.0f * (absBits != bits ? 1 : 0);
        var ax = sign * x;
        if (ax > 10.0f) {
            return sign;
        }
        if (1.0f < ax) {
            var twoAx = ax + ax;
            var scaled = twoAx * 46.1662407f;
            var rounded = scaled <= 0.0f ? scaled - 0.5f : scaled + 0.5f;
            var n = (int) rounded;
            var idx = n & 31;
            var tableHigh = TANHF_TABLE_HIGH[idx];
            var tableLow = TANHF_TABLE_LOW[idx];
            var reduced = twoAx - (float) n * 0.0216598511f;
            var reducedLow = (float) (-n) * 9.98318228e-07f;

            var q1 = (n + (n >> 31 & 31)) >> 5;
            var q2 = (q1 - (q1 >> 31)) >> 1;
            var scaleHighExp = q1 - q2;
            var scaleLow = Float.intBitsToFloat((q2 + 0x7f) << 23);
            var scaleHigh = Float.intBitsToFloat((scaleHighExp + 0x7f) << 23);

            var r = reducedLow + reduced;
            var poly = r;
            var rSquared = r * r;
            poly = poly * 0.166666672f;
            poly = poly + 0.5f;
            poly = poly * rSquared;
            var tableSum = tableLow + tableHigh;
            poly = poly + reducedLow;
            poly = poly + reduced;
            poly = poly * tableSum;
            poly = poly + tableLow;
            poly = poly + tableHigh;
            poly = poly * scaleLow;
            poly = poly * scaleHigh;
            poly = poly + 1.0f;
            var quotient = 2.0f / poly;
            return (1.0f - quotient) * sign;
        }

        var x2 = ax * ax;
        var x3 = x2 * ax;
        float p;
        float q;
        if (0.9f <= ax) {
            p = 3.82753497e-05f * x2;
            p = p - 0.00123256445f;
            p = p * x2;
            p = p - 0.240698591f;
            q = 0.292529076f * x2;
            q = q + 0.722097397f;
        } else {
            p = 4.89163103e-05f * x2;
            p = p - 0.00146283559f;
            p = p * x2;
            p = p - 0.281928062f;
            q = 0.342701793f * x2;
            q = q + 0.845784187f;
        }
        p = p * x3;
        return (ax + p / q) * sign;
    }
}
