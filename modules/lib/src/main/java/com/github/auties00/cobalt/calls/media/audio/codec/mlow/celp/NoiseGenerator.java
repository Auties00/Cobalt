package com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp;

import java.util.Arrays;

/**
 * Generates the noise excitation that MLow adds to the deterministic pulse and pitch excitation.
 *
 * <p>After {@link CelpSynthesizer} has produced a subframe's fixed codebook and adaptive codebook
 * excitation, the decoder fills the gaps between the sparse pulses with a shaped noise floor so the
 * synthesized speech does not sound buzzy. This class produces that per subframe noise vector, which the
 * caller adds to the excitation before the short term synthesis filter. The shaping differs sharply by
 * voicing:
 * <ul>
 * <li><b>Voiced.</b> The noise is spectrally matched to the current excitation: a smoothed short term
 * autocorrelation of the excitation is flattened through a discrete cosine transform, turned into a
 * second order shaping filter, and applied to an envelope modulated white noise burst. A voiced onset
 * crossfade injects a decaying unvoiced burst for the first samples after silence.</li>
 * <li><b>Unvoiced.</b> The noise fills the excitation to a target residual energy, with a floor plus gain
 * envelope solved from the smoothed energy envelope and the previous subframe's tail so the energy
 * transitions smoothly. When pulses are present the noise is written only into the zero gaps between them
 * and capped at half the unvoiced fixed codebook gain; otherwise it modulates the whole subframe.</li>
 * </ul>
 * The two branches share a final two stage output filter: a fixed second order moving average smoother for
 * the voiced and voiced tail noise and a voicing dependent high pass auto regressive moving average stage
 * for the unvoiced noise, whose corner frequency rises when the first two line spectral frequencies crowd
 * together (a sign of a sharp formant).
 *
 * <p>This generator is stateful: it owns the smoothing states (the envelope smoother, the previous envelope
 * tail, the two output filter states, the smoothed autocorrelation, the shaping filter state, the previous
 * voicing and since unvoiced counters, and the pseudo random seed). Construct one per logical decode stream
 * and call {@link #genNoise} once per subframe in order; {@link #reset()} returns every state to the freshly
 * constructed (zeroed) value.
 *
 * <p>Scope is the SMPL 16 kHz, 60 ms, mono low band path with a linear prediction order of 16. This type is
 * not thread safe.
 *
 * @implNote This implementation reproduces the decoder's single precision arithmetic. The pseudo random
 * pulse generator is a linear congruential generator whose seed advances as
 * {@code seed = 907633515 + seed * 196314165} with 32 bit overflow, emitting four outputs per step taken
 * from the top, second, third, and fourth bytes of the new seed scaled by {@code 8.1e-10f}; because it is
 * pure 32 bit integer arithmetic, the pulse sequence is bit exact. The transcendental calls map to
 * {@code Math.exp}, {@code Math.log}, and {@code Math.sqrt} narrowed to {@code float}. The float path is not
 * bit exact against a double precision reference (double precision intermediate promotion under aggressive
 * optimization versus Java strict single precision), but it tracks the reference decoder's own noise within
 * the carried float envelope.
 */
public final class NoiseGenerator {
    /**
     * Linear prediction order of the MLow short term filter; the length of the line spectral frequency
     * vector the unvoiced high pass corner reads.
     */
    private static final int LPC_ORDER = 16;

    /**
     * The order of the short term autocorrelation used to spectrally match the voiced noise.
     */
    private static final int NOISE_CORR_ORDER = 2;

    /**
     * The number of discrete cosine transform bands the voiced autocorrelation is flattened over.
     */
    private static final int NOISE_DCT_ORDER = 16;

    /**
     * Pi as a single precision constant.
     */
    private static final float PI = 3.1415926535897f;

    /**
     * The voiced noise output gain.
     */
    private static final float DEC_NOISE_V_NOISE_GAIN = 0.35f;

    /**
     * The unvoiced noise output gain.
     */
    private static final float DEC_NOISE_UV_NOISE_GAIN = 0.8f;

    /**
     * The base unvoiced high pass corner frequency in Hertz.
     */
    private static final float DEC_NOISE_UV_FCORNER_HZ = 800.0f;

    /**
     * The voiced envelope smoothing coefficient.
     */
    private static final float ENV_SMTH_COEF_V = 0.95f;

    /**
     * The unvoiced envelope smoothing coefficient.
     */
    private static final float ENV_SMTH_COEF_UV = 0.995f;

    /**
     * The voiced onset crossfade decay coefficient.
     */
    private static final float ENV_SMTH_COEF_UV_V = 0.99f;

    /**
     * The fixed second order moving average smoother applied to the voiced (and voiced tail) noise.
     */
    private static final float[] COEF_MA_V = {0.25f, -0.496f, 0.25f};

    /**
     * The pseudo random generator multiplier.
     */
    private static final int RAND_MULTIPLIER = 196314165;

    /**
     * The pseudo random generator increment.
     */
    private static final int RAND_INCREMENT = 907633515;

    /**
     * The pseudo random pulse output scale applied to each extracted seed byte.
     */
    private static final float RAND_SCALE = 8.1e-10f;

    /**
     * The shared discrete cosine transform matrix, row major
     * {@code [NOISE_CORR_ORDER + 1][NOISE_DCT_ORDER]}, computed once at construction.
     */
    private final float[][] dctMatT;

    /**
     * The voiced envelope smoother state carried across subframes.
     */
    private float envSmth;

    /**
     * The previous subframe's last envelope value.
     */
    private float envLast;

    /**
     * The unvoiced output high pass filter state, two taps.
     */
    private final float[] outStateUv;

    /**
     * The voiced output moving average smoother state, two taps.
     */
    private final float[] outStateV;

    /**
     * The smoothed short term autocorrelation of the voiced excitation, {@code NOISE_CORR_ORDER + 1} taps.
     */
    private final float[] corrSmth;

    /**
     * The voiced shaping filter state, {@code NOISE_CORR_ORDER} taps.
     */
    private final float[] shapeState;

    /**
     * The previous subframe's voicing flag.
     */
    private int prevVoiced;

    /**
     * The count of consecutive voiced subframes since the last unvoiced one; gates the voiced tail unvoiced
     * injection.
     */
    private int sinceUnvoiced;

    /**
     * The pseudo random generator seed.
     */
    private int randSeed;

    /**
     * Constructs a noise generator with a freshly computed discrete cosine transform matrix and zeroed
     * smoothing state.
     *
     * <p>All filter states, the smoothed autocorrelation, the envelope smoother, the previous voicing and
     * since unvoiced counters, and the pseudo random seed start at zero.
     */
    public NoiseGenerator() {
        this.dctMatT = buildDctMatrix();
        this.outStateUv = new float[2];
        this.outStateV = new float[2];
        this.corrSmth = new float[NOISE_CORR_ORDER + 1];
        this.shapeState = new float[NOISE_CORR_ORDER];
    }

    /**
     * Returns this generator to its freshly constructed state.
     *
     * <p>Zeroes every smoothing state, the smoothed autocorrelation, the envelope tracker, the voicing
     * counters, and the pseudo random seed. Call this between independent decode sessions; do not call it
     * between the subframes of one continuous stream, which must thread the smoothing state.
     */
    public void reset() {
        this.envSmth = 0.0f;
        this.envLast = 0.0f;
        Arrays.fill(outStateUv, 0.0f);
        Arrays.fill(outStateV, 0.0f);
        Arrays.fill(corrSmth, 0.0f);
        Arrays.fill(shapeState, 0.0f);
        this.prevVoiced = 0;
        this.sinceUnvoiced = 0;
        this.randSeed = 0;
    }

    /**
     * Produces one subframe's noise excitation.
     *
     * <p>Runs the voiced or unvoiced shaping branch selected by {@code voiced}, then the shared two stage
     * output filter, advancing every smoothing state and the voicing counters. The {@code excLpc} input is
     * the excitation already carrying the fixed codebook and (for voiced frames) adaptive codebook layers;
     * the voiced branch matches the noise spectrum to it, and the unvoiced branch sizes the noise relative to
     * it and writes only into its zero gaps when pulses are present.
     *
     * @param excLpc            the subframe excitation so far (fixed plus adaptive codebook), {@code l}
     *                          entries
     * @param l                 the subframe length in samples
     * @param voiced            {@code true} for the voiced shaping branch, {@code false} for the unvoiced
     *                          branch
     * @param numPulses         the subframe's pulse count; zero selects the flat envelope unvoiced sub branch
     * @param nrgres            the target linear residual energy
     * @param fcbgIdx           the subframe's unvoiced fixed codebook gain index, used to cap the gapped
     *                          unvoiced noise
     * @param lsf               the subframe's line spectral frequencies, {@code LPC_ORDER} entries; set the
     *                          unvoiced high pass corner
     * @param normalizedBitrate the frame's normalized bitrate
     * @param noise             the noise output, {@code l} entries written
     */
    public void genNoise(float[] excLpc, int l, boolean voiced, int numPulses, float nrgres,
                         int fcbgIdx, float[] lsf, float normalizedBitrate, float[] noise) {
        var v = voiced;
        var nrgRatio = 1.0f;
        var noiseUv = new float[l];
        var noiseV2 = new float[l];
        var env = new float[l];

        if (v) {
            var corrs = new float[NOISE_CORR_ORDER + 1];
            for (var i = 0; i < NOISE_CORR_ORDER + 1; i++) {
                corrs[i] = dotProd(excLpc, 0, excLpc, i, l - i);
            }
            corrs[0] += 1e-12f;
            var corrSmthCoef = l == 160 ? 0.4f : 0.16f;
            for (var i = 0; i < NOISE_CORR_ORDER + 1; i++) {
                corrSmth[i] += corrSmthCoef * (corrs[i] - corrSmth[i]);
            }
            var scale = DEC_NOISE_V_NOISE_GAIN * DEC_NOISE_V_NOISE_GAIN * corrs[0] / corrSmth[0];
            var c = new float[NOISE_CORR_ORDER + 1];
            for (var i = 0; i < NOISE_CORR_ORDER + 1; i++) {
                c[i] = corrSmth[i] * scale;
            }
            c[1] *= 2.0f;
            c[2] *= 2.0f;

            var f2 = new float[NOISE_DCT_ORDER];
            matrixMultTransp16(dctMatT, c, f2, NOISE_CORR_ORDER + 1);
            var m = maximum(f2, NOISE_DCT_ORDER) * 1.5f;
            var f2Tgt = new float[NOISE_DCT_ORDER];
            for (var i = 0; i < NOISE_DCT_ORDER; i++) {
                f2Tgt[i] = m - f2[i];
            }
            var ctgt = new float[NOISE_CORR_ORDER + 1];
            matrixMult(dctMatT, f2Tgt, ctgt, NOISE_CORR_ORDER + 1, NOISE_DCT_ORDER);
            var noiseV = new float[l];
            genRandPulses(noiseV, l);
            if (prevVoiced == 0) {
                envSmth = envLast;
            }
            getEnv(excLpc, l, ENV_SMTH_COEF_V, env);
            for (var i = 0; i < l; i++) {
                noiseV[i] *= env[i];
            }
            var nrgNoise = nrg(noiseV, l);
            var inv = 1.0f / (nrgNoise + 1e-12f);
            for (var i = 0; i < NOISE_CORR_ORDER + 1; i++) {
                ctgt[i] *= inv;
            }
            var coefMa = new float[NOISE_CORR_ORDER + 1];
            specFact2(ctgt, coefMa);

            filtMa2(noiseV, l, coefMa, shapeState, noiseV2);

            if (prevVoiced == 0) {
                genRandPulses(noiseUv, l);
                var envVal = envLast * ENV_SMTH_COEF_UV_V;
                for (var i = 0; i < l; i += 2) {
                    noiseUv[i] *= envVal;
                    noiseUv[i + 1] *= envVal * ENV_SMTH_COEF_UV_V;
                    envVal *= ENV_SMTH_COEF_UV_V * ENV_SMTH_COEF_UV_V;
                }
            } else if (sinceUnvoiced < 2) {
                Arrays.fill(noiseUv, 0, l, 0.0f);
            }
            envLast = env[l - 1];
        } else {
            Arrays.fill(corrSmth, 0.0f);
            Arrays.fill(shapeState, 0.0f);
            Arrays.fill(noiseV2, 0, l, 0.0f);

            float nrgTgt;
            if (numPulses > 0) {
                nrgRatio = nrg(excLpc, l) / (nrgres + 1e-20f);
                var hardness = 10.0f + 20.0f * normalizedBitrate;
                nrgTgt = nrgres * (float) (Math.log(Math.exp(hardness * (1.0f - nrgRatio)) + 1) / hardness);
                getEnv(excLpc, l, ENV_SMTH_COEF_UV, env);
            } else {
                nrgRatio = 0.0f;
                nrgTgt = nrgres;
                getEnv0(l, ENV_SMTH_COEF_UV, env);
            }

            var scale = 1.0f / l;
            nrgTgt = nrgTgt * scale + 1e-30f;
            var nrgEnv = nrg(env, l) * scale;
            var f = (float) Math.sqrt(nrgTgt);
            var g = (float) Math.sqrt(nrgTgt / nrgEnv);
            var ge = g * env[0];
            var envLastLocal = envLast;
            if (envLastLocal < Math.min(f, ge)) {
                if (f < ge) {
                    g = 0.0f;
                } else {
                    f = 0.0f;
                }
            } else if (envLastLocal > Math.max(f, ge)) {
                if (f > ge) {
                    g = 0.0f;
                } else {
                    f = 0.0f;
                }
            } else {
                var sumEnv = sum(env, l) * scale;
                var a = nrgEnv + env[0] * env[0] - 2.0f * sumEnv * env[0];
                var b = 2.0f * envLastLocal * (sumEnv - env[0]);
                var cc = envLastLocal * envLastLocal - nrgTgt;
                var tmp = b * b - 4.0f * a * cc;
                if ((tmp < 1e-35f) || (a < 1e-25f)) {
                    f = 0.0f;
                    g = 0.0f;
                } else {
                    tmp = (float) Math.sqrt(tmp);
                    scale = 0.5f / a;
                    g = (-b + tmp) * scale;
                    f = envLastLocal - env[0] * g;
                    if (f < 0) {
                        g = (-b - tmp) * scale;
                        f = envLastLocal - env[0] * g;
                    }
                }
            }

            genRandPulses(noiseUv, l);
            if (numPulses > 0) {
                var maxVal = fcbgainsUvCap(fcbgIdx);
                for (var i = 0; i < l; i++) {
                    if (excLpc[i] == 0.0f) {
                        noiseUv[i] *= Math.min(f + g * env[i], maxVal);
                    } else {
                        noiseUv[i] = 0.0f;
                    }
                }
                envLast = Math.min(f + g * env[l - 1], maxVal);
            } else {
                for (var i = 0; i < l; i++) {
                    noiseUv[i] *= f + g * env[i];
                }
                envLast = f + g * env[l - 1];
            }
        }

        if (prevVoiced != 0 || v) {
            filtMa2(noiseV2, l, COEF_MA_V, outStateV, noise);
        } else {
            Arrays.fill(noise, 0, l, 0.0f);
        }
        if (sinceUnvoiced < 2 || !v) {
            addNoiseUv(noiseUv, l, lsf, nrgRatio, noise);
        } else {
            outStateUv[0] = 0.0f;
            outStateUv[1] = 0.0f;
        }
        prevVoiced = v ? 1 : 0;
        if (v) {
            sinceUnvoiced++;
        } else {
            sinceUnvoiced = 0;
        }
    }

    /**
     * Filters and adds the unvoiced noise burst into the output.
     *
     * <p>Sets the high pass corner from the spread of the first two line spectral frequencies and the
     * residual energy ratio, builds a first order auto regressive moving average high pass filter, runs the
     * unvoiced noise through it, and adds the result into {@code noise}.
     *
     * @param excNoiseUv the unvoiced noise burst, filtered in place, {@code l} entries
     * @param l          the subframe length
     * @param lsf        the line spectral frequencies, {@code LPC_ORDER} entries
     * @param nrgRatio   the excitation to residual energy ratio
     * @param noise      the output the filtered burst is added into, {@code l} entries
     */
    private void addNoiseUv(float[] excNoiseUv, int l, float[] lsf, float nrgRatio, float[] noise) {
        var lsfHz = 16000.0f * (lsf[0] + lsf[1]) / (4.0f * PI);
        var minUvFcornerHz = lsfHz * 3.0f * sigmoid(0.2f / (lsf[1] - lsf[0] + 1e-30f) - 3.0f);
        var uvFcornerHz = DEC_NOISE_UV_FCORNER_HZ * Math.min(1.0f, 0.6f + 0.4f * nrgRatio);
        uvFcornerHz = Math.max(uvFcornerHz, minUvFcornerHz);
        uvFcornerHz = Math.min(uvFcornerHz, 1500.0f);
        var coefTmp = 6.0f * uvFcornerHz / 16000.0f;
        var coefMaUv = new float[2];
        var coefArUv = new float[2];
        coefMaUv[0] = (1.0f - 0.5f * coefTmp) * DEC_NOISE_UV_NOISE_GAIN;
        coefMaUv[1] = -coefMaUv[0];
        coefArUv[0] = 1.0f;
        coefArUv[1] = -1.0f + coefTmp;
        filtArma1(excNoiseUv, l, coefMaUv, coefArUv, outStateUv, excNoiseUv);
        for (var i = 0; i < l; i++) {
            noise[i] += excNoiseUv[i];
        }
    }

    /**
     * Generates a pseudo random bipolar pulse train.
     *
     * <p>Advances the linear congruential seed and emits four samples per step, each the seed shifted left by
     * 0, 8, 16, or 24 bits, reinterpreted as a signed 32 bit integer, and scaled by {@code 8.1e-10f}; the
     * tail handles a length not divisible by four with one sample (top bits) per step. The seed update and
     * the byte extraction are pure 32 bit integer arithmetic, so the sequence is bit exact against a
     * reference generator.
     *
     * @param noise the output noise array, {@code l} entries written
     * @param l     the number of samples to generate
     */
    private void genRandPulses(float[] noise, int l) {
        var i = 0;
        for (; i < l - 3; i += 4) {
            randSeed = RAND_INCREMENT + randSeed * RAND_MULTIPLIER;
            noise[i] = RAND_SCALE * randSeed;
            noise[i + 1] = RAND_SCALE * (randSeed << 8);
            noise[i + 2] = RAND_SCALE * (randSeed << 16);
            noise[i + 3] = RAND_SCALE * (randSeed << 24);
        }
        for (; i < l; i++) {
            randSeed = RAND_INCREMENT + randSeed * RAND_MULTIPLIER;
            noise[i] = RAND_SCALE * randSeed;
        }
    }

    /**
     * Computes the smoothed energy envelope of a signal.
     *
     * <p>Tracks a leaky integrated short term energy and writes its square root as the envelope, processing
     * four samples per step with a two rate update. The smoother state carries across subframes via
     * {@link #envSmth}.
     *
     * @param exc the input signal, {@code len} entries
     * @param len the signal length; a multiple of four
     * @param smthCoefIn the envelope smoothing coefficient (before squaring)
     * @param env the envelope output, {@code len} entries written
     */
    private void getEnv(float[] exc, int len, float smthCoefIn, float[] env) {
        var smthCoef = smthCoefIn * smthCoefIn;
        var state = envSmth + 1e-8f;
        state *= state;
        var gainCoef = 1.0f - smthCoef;
        var smthCoef2 = smthCoef * smthCoef;
        var gainSmthCoef = gainCoef * smthCoef;
        for (var i = 0; i < len - 3; i += 4) {
            var tmp0 = exc[i] * exc[i] + exc[i + 1] * exc[i + 1];
            var tmp1 = exc[i + 2] * exc[i + 2] + exc[i + 3] * exc[i + 3];
            var y1 = gainCoef * tmp1 + gainSmthCoef * tmp0 + smthCoef2 * state;
            var y0 = gainCoef * tmp0 + smthCoef * state;
            env[i] = env[i + 1] = (float) Math.sqrt(y0);
            env[i + 2] = env[i + 3] = (float) Math.sqrt(y1);
            state = y1;
        }
        envSmth = env[len - 1];
    }

    /**
     * Computes a decaying envelope from the smoother state alone.
     *
     * <p>Used when there is no excitation to track; the envelope decays geometrically from the smoother state
     * by the smoothing coefficient. The smoother state carries across subframes via {@link #envSmth}.
     *
     * @param len      the envelope length; a multiple of four and at least four
     * @param smthCoef the envelope smoothing coefficient
     * @param env      the envelope output, {@code len} entries written
     */
    private void getEnv0(int len, float smthCoef, float[] env) {
        var smthCoef2 = smthCoef * smthCoef;
        env[0] = env[1] = (envSmth + 1e-8f) * smthCoef;
        for (var i = 2; i < len - 2; i += 4) {
            env[i + 2] = env[i + 3] = env[i - 1] * smthCoef2;
            env[i] = env[i + 1] = env[i - 1] * smthCoef;
        }
        env[len - 2] = env[len - 1] = env[len - 3] * smthCoef;
        envSmth = env[len - 1];
    }

    /**
     * Applies a second order moving average filter with carried state.
     *
     * <p>Runs a monic aware first stage plus the second tap, with the two tap state supplying the filter
     * history at the subframe boundary. The input and output must not alias; this routine writes into a
     * separate output buffer, so the constraint is met.
     *
     * @param x     the input signal, {@code n} entries
     * @param n     the signal length; at least two
     * @param coef  the three filter taps
     * @param state the two tap filter state, updated in place
     * @param y     the output signal, {@code n} entries written; must differ from {@code x}
     */
    private static void filtMa2(float[] x, int n, float[] coef, float[] state, float[] y) {
        if (coef[0] == 1.0f) {
            y[0] = x[0];
            for (var i = 1; i < n; i++) {
                y[i] = x[i] + coef[1] * x[i - 1];
            }
        } else {
            for (var i = 0; i < n; i++) {
                y[i] = coef[0] * x[i];
            }
            for (var i = 1; i < n; i++) {
                y[i] += coef[1] * x[i - 1];
            }
        }
        for (var i = 2; i < n; i++) {
            y[i] += coef[2] * x[i - 2];
        }
        y[0] = coef[0] * x[0] + coef[1] * state[0] + coef[2] * state[1];
        y[1] += coef[2] * state[0];
        state[0] = x[n - 1];
        state[1] = x[n - 2];
    }

    /**
     * Applies a first order auto regressive moving average filter with carried state.
     *
     * <p>Runs the first order moving average stage then the first order auto regressive stage, with the two
     * tap state holding one tap for each stage. When the input and output alias, as the unvoiced caller does,
     * the moving average result is staged through a temporary buffer so the auto regressive recurrence reads
     * unmodified samples.
     *
     * @param x      the input signal, {@code n} entries
     * @param n      the signal length; at least one
     * @param coefMa the two moving average taps
     * @param coefAr the two auto regressive taps; {@code coefAr[0]} is one (monic)
     * @param state  the two tap state, updated in place: tap zero is the moving average state, tap one the
     *               auto regressive state
     * @param y      the output signal, {@code n} entries written; may alias {@code x}
     */
    private static void filtArma1(float[] x, int n, float[] coefMa, float[] coefAr, float[] state, float[] y) {
        var ma = new float[n];
        // First order moving average stage.
        if (coefMa[0] == 1.0f) {
            ma[0] = x[0];
            for (var i = 1; i < n; i++) {
                ma[i] = x[i] + coefMa[1] * x[i - 1];
            }
        } else {
            for (var i = 0; i < n; i++) {
                ma[i] = coefMa[0] * x[i];
            }
            for (var i = 1; i < n; i++) {
                ma[i] += coefMa[1] * x[i - 1];
            }
        }
        ma[0] = coefMa[0] * x[0] + coefMa[1] * state[0];
        state[0] = x[n - 1];
        // First order auto regressive stage: y[n] = x[n] - coefAr[1] * y[n-1].
        var ar1 = -coefAr[1];
        var ytmp = state[1];
        for (var i = 0; i < n; i++) {
            ytmp = ma[i] + ytmp * ar1;
            y[i] = ytmp;
        }
        state[1] = ytmp;
    }

    /**
     * Computes the symmetric two zero, two pole spectral factorization.
     *
     * <p>Solves for the second order minimum phase moving average filter whose autocorrelation matches the
     * three supplied target correlations, by two Newton iterations of the reflection coefficient system,
     * then scales the resulting monic filter to the target zero lag energy.
     *
     * @param c the three target correlations; {@code c[0]} is not negative and is biased in place
     * @param a the three filter taps written
     */
    private static void specFact2(float[] c, float[] a) {
        c[0] += 1e-30f;
        var invC0 = 1.0f / c[0];
        var r2 = c[2] * invC0;
        var r1 = c[1] / (c[0] * (1 + r2));
        for (var iter = 0; iter < 2; iter++) {
            var v0 = 1.0f + r1 * r1 + r2 * r2;
            var v1 = r1 + r1 * r2;
            var s = -2.0f / v0;
            var da0 = s * r1;
            var da1 = s * r2;
            s = v0 * invC0;
            var e1 = s * c[1] - v1;
            var e2 = s * c[2] - r2;
            var r0 = 2.0f * r1 + v0 * da0;
            var r3 = 2.0f * r2 + v0 * da1;
            var rr00 = r0 * r0;
            var rr01 = r0 * r3;
            var rr11 = r3 * r3;
            var rA = 1.0f + r2 + v1 * da0;
            var rB = r1 + v1 * da1;
            rr00 += rA * rA;
            rr01 += rA * rB;
            rr11 += rB * rB;
            var re0 = rA * e1;
            var re1 = rB * e1;
            var rC = r2 * da0;
            var rD = 1.0f + r2 * da1;
            rr00 += rC * rC;
            rr01 += rC * rD;
            rr11 += rD * rD;
            re0 += rC * e2;
            re1 += rD * e2;
            s = rr00 * rr11 - rr01 * rr01;
            if (s < 1e-4f) {
                break;
            }
            s = 1.0f / s;
            r1 += (rr11 * re0 - rr01 * re1) * s;
            r2 += (-rr01 * re0 + rr00 * re1) * s;
        }
        var sc = (float) Math.sqrt(c[0] / (1.0f + r1 * r1 + r2 * r2));
        a[0] = sc;
        a[1] = sc * r1;
        a[2] = sc * r2;
    }

    /**
     * Multiplies the transpose of the discrete cosine matrix by a vector.
     *
     * <p>Computes the sixteen entry product {@code y = C^T x} where {@code C} is the row major
     * {@code [lenX][16]} matrix; the result length is fixed at {@code NOISE_DCT_ORDER}.
     *
     * @param c    the matrix as {@code [NOISE_CORR_ORDER + 1][NOISE_DCT_ORDER]}
     * @param x    the input vector, {@code lenX} entries
     * @param y    the output vector, sixteen entries written
     * @param lenX the input length
     */
    private static void matrixMultTransp16(float[][] c, float[] x, float[] y, int lenX) {
        var tmp = new float[16];
        var xt = x[0];
        for (var i = 0; i < 16; i++) {
            tmp[i] = c[0][i] * xt;
        }
        for (var j = 1; j < lenX; j++) {
            xt = x[j];
            for (var i = 0; i < 16; i++) {
                tmp[i] += c[j][i] * xt;
            }
        }
        System.arraycopy(tmp, 0, y, 0, 16);
    }

    /**
     * Multiplies the discrete cosine matrix by a vector.
     *
     * <p>Computes {@code y[i] = sum_j C[i][j] x[j]} over the row major {@code [lenY][lenX]} matrix.
     *
     * @param c    the matrix as {@code [NOISE_CORR_ORDER + 1][NOISE_DCT_ORDER]}
     * @param x    the input vector, {@code lenX} entries
     * @param y    the output vector, {@code lenY} entries written
     * @param lenY the output length
     * @param lenX the input length
     */
    private static void matrixMult(float[][] c, float[] x, float[] y, int lenY, int lenX) {
        for (var i = 0; i < lenY; i++) {
            y[i] = dotProd(c[i], 0, x, 0, lenX);
        }
    }

    /**
     * Computes the dot product of two windowed signals.
     *
     * @param a    the first array
     * @param aOff the offset into the first array
     * @param b    the second array
     * @param bOff the offset into the second array
     * @param len  the number of terms
     * @return the accumulated single precision dot product
     */
    private static float dotProd(float[] a, int aOff, float[] b, int bOff, int len) {
        var ret = 0.0f;
        for (var i = 0; i < len; i++) {
            ret += a[aOff + i] * b[bOff + i];
        }
        return ret;
    }

    /**
     * Computes the energy (sum of squares) of a signal.
     *
     * @param x the signal
     * @param n the length
     * @return the single precision sum of squares
     */
    private static float nrg(float[] x, int n) {
        var r = 0.0f;
        for (var i = 0; i < n; i++) {
            r += x[i] * x[i];
        }
        return r;
    }

    /**
     * Computes the sum of a signal.
     *
     * @param x the signal
     * @param n the length
     * @return the single precision sum
     */
    private static float sum(float[] x, int n) {
        var r = 0.0f;
        for (var i = 0; i < n; i++) {
            r += x[i];
        }
        return r;
    }

    /**
     * Returns the maximum of a signal.
     *
     * @param x   the signal
     * @param len the length; at least one
     * @return the largest entry
     */
    private static float maximum(float[] x, int len) {
        var max = x[0];
        for (var i = 1; i < len; i++) {
            if (x[i] > max) {
                max = x[i];
            }
        }
        return max;
    }

    /**
     * Computes the numerically guarded logistic sigmoid.
     *
     * <p>Saturates to one above {@code 80} and to zero below {@code -80} to keep the exponential finite.
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
     * Computes one entry of the unvoiced fixed codebook gain table and halves it for the noise cap.
     *
     * <p>The table entry is {@code 10^(0.05 * (ix - 90))}; computing it on demand avoids holding the whole
     * table here, since only one entry is read per unvoiced subframe.
     *
     * @param fcbgIdx the unvoiced fixed codebook gain index
     * @return half the table entry, the unvoiced noise cap
     */
    private static float fcbgainsUvCap(int fcbgIdx) {
        var db = fcbgIdx * 1.0f + (-90.0f);
        return (float) Math.pow(10.0, 0.05 * db) * 0.5f;
    }

    /**
     * Computes the shared discrete cosine transform matrix.
     *
     * <p>Entry {@code [j][i]} is
     * {@code cos(j * (0.5 + i) * pi / NOISE_DCT_ORDER) / sqrt(NOISE_DCT_ORDER)}, the type III cosine basis
     * the voiced noise spectral flattening projects onto and back from.
     *
     * @return a freshly allocated {@code [NOISE_CORR_ORDER + 1][NOISE_DCT_ORDER]} matrix
     */
    private static float[][] buildDctMatrix() {
        var mat = new float[NOISE_CORR_ORDER + 1][NOISE_DCT_ORDER];
        var sc = (float) (1.0 / Math.sqrt(NOISE_DCT_ORDER));
        for (var i = 0; i < NOISE_DCT_ORDER; i++) {
            var dOmega = ((0.5f + i) * PI) / NOISE_DCT_ORDER;
            var omega = 0.0f;
            for (var j = 0; j < NOISE_CORR_ORDER + 1; j++) {
                mat[j][i] = (float) Math.cos(omega) * sc;
                omega += dOmega;
            }
        }
        return mat;
    }
}
