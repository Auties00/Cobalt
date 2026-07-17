package com.github.auties00.cobalt.calls.media.audio.neteq;

import java.util.Arrays;
import java.util.Random;
import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.ComfortNoiseDecoder;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Generates RFC 3389 comfort noise from a silence insertion descriptor payload, the shaped background
 * noise the jitter buffer renders during a discontinuous transmission gap.
 *
 * <p>A silence insertion descriptor carries a noise level byte and optionally a short set of reflection
 * coefficients describing the spectral shape of the background noise the sender measured. The generator
 * keeps the most recent descriptor through {@link #update(byte[])} and synthesizes one frame per
 * {@link #generate(int)} call: white Gaussian noise scaled to the descriptor energy and passed through the
 * all pole synthesis filter the reflection coefficients define so the noise carries the sender's spectral
 * tilt rather than sounding flat. A gap that begins before any descriptor has been seen produces silence
 * until the first descriptor arrives, since the energy and spectrum are unknown.
 *
 * <p>The generator is single writer: the jitter buffer drives it from the pull thread. It holds the filter
 * memory and the interpolated energy and reflection coefficients across frames so the synthesized noise is
 * continuous and a level or spectrum change transitions smoothly, and {@link #reset()} clears that state
 * when the stream is reconfigured.
 *
 * @implNote This implementation draws the excitation from a deterministically seeded {@link Random} Gaussian
 * rather than a bit exact random table. Comfort noise is locally synthesized filler that is never
 * transmitted, so the energy, spectrum, and interpolation reproduced here are the audible behaviour while
 * the exact noise samples carry no interoperability meaning. The reflection coefficient to polynomial
 * conversion and the per frame excitation scale are computed in the same fixed point arithmetic
 * ({@link #REFL_BETA_STD} and {@link #REFL_BETA_COMP_STD} steady state weights, {@link #REFL_BETA_NEW_P} and
 * {@link #REFL_BETA_COMP_NEW_P} first frame weights, the {@link #DBOV_ENERGY} dBov table) so the level and
 * spectral tilt match what the sender described.
 */
public final class ComfortNoiseGenerator {
    /**
     * The fixed linear prediction order the synthesis filter and the reflection coefficient interpolation
     * run at.
     *
     * <p>A descriptor carrying fewer reflection coefficients leaves the remaining ones at zero, and the
     * synthesis still runs at this full order.
     */
    private static final int LPC_ORDER = 12;

    /**
     * The dBov energy for each silence insertion descriptor level byte, indexed by the level clamped to
     * {@code 0..93}.
     *
     * <p>The descriptor's first byte selects the energy the comfort noise is synthesized at, decreasing as
     * the level byte rises.
     */
    private static final int[] DBOV_ENERGY = {
            1081109975, 858756178, 682134279, 541838517, 430397633, 341876992, 271562548, 215709799, 171344384, 136103682,
            108110997, 85875618, 68213428, 54183852, 43039763, 34187699, 27156255, 21570980, 17134438, 13610368,
            10811100, 8587562, 6821343, 5418385, 4303976, 3418770, 2715625, 2157098, 1713444, 1361037,
            1081110, 858756, 682134, 541839, 430398, 341877, 271563, 215710, 171344, 136104,
            108111, 85876, 68213, 54184, 43040, 34188, 27156, 21571, 17134, 13610,
            10811, 8588, 6821, 5418, 4304, 3419, 2716, 2157, 1713, 1361,
            1081, 859, 682, 542, 430, 342, 272, 216, 171, 136,
            108, 86, 68, 54, 43, 34, 27, 22, 17, 14,
            11, 9, 7, 5, 4, 3, 3, 2, 2, 1,
            1, 1, 1, 1
    };

    /**
     * The maximum silence insertion descriptor level byte the {@link #DBOV_ENERGY} table is indexed at.
     *
     * <p>A level byte above this is clamped to it.
     */
    private static final int MAX_LEVEL = 93;

    /**
     * The Q15 weight of the previous reflection coefficients in the steady state interpolation, {@code 0.8}.
     */
    private static final int REFL_BETA_STD = 26214;

    /**
     * The Q15 weight of the target reflection coefficients in the steady state interpolation, {@code 0.2}.
     */
    private static final int REFL_BETA_COMP_STD = 6553;

    /**
     * The Q15 weight of the previous reflection coefficients on the first frame of a noise period,
     * {@code 0.6}.
     */
    private static final int REFL_BETA_NEW_P = 19661;

    /**
     * The Q15 weight of the target reflection coefficients on the first frame of a noise period,
     * {@code 0.4}.
     */
    private static final int REFL_BETA_COMP_NEW_P = 13107;

    /**
     * The logger for {@link ComfortNoiseGenerator}.
     */
    private static final System.Logger LOGGER = Log.get(ComfortNoiseGenerator.class);

    /**
     * The white noise excitation source, deterministically seeded for reproducibility.
     */
    private final Random noise;

    /**
     * The target reflection coefficients from the most recent descriptor, in Q15.
     *
     * <p>The {@link #usedReflectionQ15} coefficients are interpolated toward these one step per frame.
     */
    private final int[] targetReflectionQ15;

    /**
     * The reflection coefficients currently driving the synthesis filter, in Q15.
     *
     * <p>Carried across frames and blended toward {@link #targetReflectionQ15} so a spectrum change
     * transitions smoothly.
     */
    private final int[] usedReflectionQ15;

    /**
     * The Q12 linear prediction polynomial for the synthesis filter, length {@link #LPC_ORDER} plus one.
     *
     * <p>Recomputed each frame from {@link #usedReflectionQ15} by the step up recursion; index zero is the
     * leading {@code 1.0} term in Q12.
     */
    private final int[] lpcQ12;

    /**
     * The per frame synthesis coefficients, {@link #lpcQ12} converted to {@code double} in Q0.
     *
     * <p>Recomputed once per frame in {@link #updateParametersAndScale()} as
     * {@code lpcQ12[i + 1] * (1.0 / 4096.0)} and read by {@link #synthesize(double)} so the Q12 to double
     * reciprocal is hoisted out of the per sample loop; sized to {@link #LPC_ORDER}.
     */
    private final double[] lpcCoeff;

    /**
     * The synthesis filter memory, the recent filtered output samples.
     *
     * <p>Carried across frames so the noise is continuous; sized to {@link #LPC_ORDER} and cleared on
     * {@link #reset()}.
     */
    private final double[] filterMemory;

    /**
     * The target energy from the most recent descriptor.
     *
     * <p>The {@link #usedEnergy} is interpolated toward this one step per frame.
     */
    private int targetEnergy;

    /**
     * The energy currently driving the excitation scale, blended toward {@link #targetEnergy} each frame.
     */
    private int usedEnergy;

    /**
     * The number of reflection coefficients the most recent descriptor carried.
     *
     * <p>Coefficients beyond this are held at zero; the synthesis still runs at the full {@link #LPC_ORDER}.
     */
    private int order;

    /**
     * Whether a descriptor has been seen, so the energy and spectrum are known.
     *
     * <p>Before the first descriptor the generator produces silence rather than guessing a level.
     */
    private boolean hasDescriptor;

    /**
     * Whether the next {@link #generate(int)} is the first of a noise period, selecting the faster
     * interpolation weights.
     */
    private boolean firstFrame;

    /**
     * Constructs a comfort noise generator that produces silence until the first descriptor.
     */
    public ComfortNoiseGenerator() {
        this.noise = new Random(0x434E47L);
        this.targetReflectionQ15 = new int[LPC_ORDER];
        this.usedReflectionQ15 = new int[LPC_ORDER];
        this.lpcQ12 = new int[LPC_ORDER + 1];
        this.lpcCoeff = new double[LPC_ORDER];
        this.filterMemory = new double[LPC_ORDER];
        this.targetEnergy = 0;
        this.usedEnergy = 0;
        this.order = LPC_ORDER;
        this.hasDescriptor = false;
        this.firstFrame = true;
    }

    /**
     * Updates the generator from a silence insertion descriptor payload.
     *
     * <p>Reads the level byte into the target energy through the dBov table and the following reflection
     * coefficients into the target spectrum; the interpolated state then transitions toward these over the
     * next frames. A payload carrying only a level byte yields a flat target spectrum. An empty payload
     * leaves the current descriptor in force, the RFC 3389 convention that a continuation without a
     * descriptor reuses the last spectral estimate.
     *
     * @param sidPayload the silence insertion descriptor bytes; an empty array reuses the last descriptor
     */
    public void update(byte[] sidPayload) {
        if (sidPayload == null || sidPayload.length == 0) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls cng update: empty payload, reusing last descriptor");
            return;
        }
        var length = Math.min(sidPayload.length, LPC_ORDER + 1);
        order = length - 1;

        var level = Math.min(sidPayload[0] & 0xFF, MAX_LEVEL);
        var energy = DBOV_ENERGY[level];
        energy = energy >> 1;
        energy += energy >> 2;
        targetEnergy = energy;

        for (var i = 0; i < order; i++) {
            targetReflectionQ15[i] = ((sidPayload[i + 1] & 0xFF) - 127) << 8;
        }
        for (var i = order; i < LPC_ORDER; i++) {
            targetReflectionQ15[i] = 0;
        }
        hasDescriptor = true;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls cng descriptor updated: level={0} order={1}", level, order);
    }

    /**
     * Synthesizes one frame of comfort noise of the given sample count.
     *
     * <p>Interpolates the energy and reflection coefficients one step toward the current descriptor,
     * rebuilds the synthesis filter and the excitation scale from them, draws white Gaussian excitation, and
     * runs it through the all pole filter. The interpolated state and filter memory persist across calls so
     * the level, spectrum, and waveform are continuous. Before any descriptor has been seen the frame is
     * silence.
     *
     * @param frameSamples the number of samples to produce
     * @return a fresh array of {@code frameSamples} signed 16 bit comfort noise samples
     */
    public short[] generate(int frameSamples) {
        var out = new short[Math.max(frameSamples, 0)];
        if (!hasDescriptor) {
            firstFrame = false;
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "calls cng generate: no descriptor yet, emitting silence, samples={0}", out.length);
            }
            return out;
        }
        var scale = updateParametersAndScale();
        for (var i = 0; i < out.length; i++) {
            var excitation = noise.nextGaussian() * scale;
            out[i] = clampToShort(synthesize(excitation));
        }
        firstFrame = false;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "calls cng generate: samples={0}", out.length);
        return out;
    }

    /**
     * Clears the interpolated state and synthesis filter memory so the next gap starts fresh.
     *
     * <p>Used when the stream is reconfigured; the next descriptor seeds the energy and spectrum anew and
     * the first frame after a reset uses the faster interpolation weights.
     */
    public void reset() {
        Arrays.fill(filterMemory, 0.0);
        Arrays.fill(usedReflectionQ15, 0);
        usedEnergy = 0;
        firstFrame = true;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls cng reset");
    }

    /**
     * Interpolates the energy and reflection coefficients toward the descriptor and returns the per sample
     * excitation scale for the frame.
     *
     * <p>Blends the used energy halfway to the target, blends each used reflection coefficient toward the
     * target with the period dependent Q15 weights, rebuilds the Q12 synthesis polynomial from the blended
     * coefficients, then computes the scale factor chain: the all pole gain correction
     * {@code En = 1.5 * sqrt(prod(1 - k^2)) << 6} times {@code sqrt(usedEnergy)}, right shifted by twelve.
     * The returned multiplier is that Q13 scale divided by the Q13 unit so a unit variance Gaussian sample
     * yields the level the descriptor energy and spectrum imply.
     *
     * @return the per sample excitation scale applied to a unit variance Gaussian
     */
    private double updateParametersAndScale() {
        var beta = firstFrame ? REFL_BETA_NEW_P : REFL_BETA_STD;
        var betaComp = firstFrame ? REFL_BETA_COMP_NEW_P : REFL_BETA_COMP_STD;

        usedEnergy = (usedEnergy >> 1) + (targetEnergy >> 1);

        for (var i = 0; i < LPC_ORDER; i++) {
            var blended = mul16Rsft(usedReflectionQ15[i], beta, 15)
                    + mul16Rsft(targetReflectionQ15[i], betaComp, 15);
            usedReflectionQ15[i] = (short) blended;
        }

        reflectionToLpcQ12();
        for (var i = 0; i < LPC_ORDER; i++) {
            lpcCoeff[i] = lpcQ12[i + 1] * (1.0 / 4096.0);
        }

        var enQ13 = 8192;
        for (var i = 0; i < LPC_ORDER; i++) {
            var refl = (short) usedReflectionQ15[i];
            var temp16 = 0x7fff - (refl * refl >> 15);
            enQ13 = enQ13 * temp16 >> 15;
        }
        var en = (int) Math.sqrt(Math.max(enQ13, 0)) << 6;
        en = en * 3 >> 1;
        var energyRoot = (int) Math.sqrt((double) Math.max(usedEnergy, 0));
        var scaleFactorQ13 = (long) en * energyRoot >> 12;
        return scaleFactorQ13 / 8192.0;
    }

    /**
     * Rebuilds {@link #lpcQ12} from {@link #usedReflectionQ15} with the step up recursion.
     *
     * <p>Starts the polynomial at the Q12 leading term {@code 4096}, then for each reflection coefficient
     * extends the polynomial and folds it back into the lower order terms, yielding the all pole synthesis
     * filter polynomial in Q12.
     */
    private void reflectionToLpcQ12() {
        var any = new int[LPC_ORDER + 1];
        lpcQ12[0] = 4096;
        any[0] = 4096;
        lpcQ12[1] = (short) ((usedReflectionQ15[0] + 4) >> 3);
        for (var m = 1; m < LPC_ORDER; m++) {
            var k = usedReflectionQ15[m];
            any[m + 1] = (short) ((k + 4) >> 3);
            for (var i = 0; i < m; i++) {
                any[i + 1] = (short) (lpcQ12[i + 1]
                        + (short) (((long) lpcQ12[m - i] * k + 16384) >> 15));
            }
            System.arraycopy(any, 0, lpcQ12, 0, m + 2);
        }
    }

    /**
     * Passes one excitation sample through the all pole synthesis filter and updates the filter memory.
     *
     * <p>The output is the excitation minus the linear combination of the recent outputs weighted by the
     * Q12 linear prediction coefficients, the direct form two all pole recursion; the memory shifts to hold
     * the new output.
     *
     * @param excitation the white noise excitation sample
     * @return the filtered output sample
     */
    private double synthesize(double excitation) {
        var output = excitation;
        for (var i = 0; i < LPC_ORDER; i++) {
            output -= lpcCoeff[i] * filterMemory[i];
        }
        for (var i = filterMemory.length - 1; i > 0; i--) {
            filterMemory[i] = filterMemory[i - 1];
        }
        filterMemory[0] = output;
        return output;
    }

    /**
     * Computes the fixed point product of two 16 bit operands with a right shift.
     *
     * <p>Multiplies the two operands as signed 16 bit values and arithmetic right shifts the 32 bit product
     * by the given amount.
     *
     * @param a     the first operand, treated as a signed 16 bit value
     * @param b     the second operand, treated as a signed 16 bit value
     * @param shift the right shift applied to the product
     * @return the shifted product
     */
    private static int mul16Rsft(int a, int b, int shift) {
        return (short) a * (short) b >> shift;
    }

    /**
     * Clamps a synthesized sample to the signed 16 bit range and rounds it to an integer sample.
     *
     * @param value the synthesized sample value
     * @return the clamped 16 bit sample
     */
    private static short clampToShort(double value) {
        var rounded = Math.round(value);
        if (rounded > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (rounded < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) rounded;
    }
}
