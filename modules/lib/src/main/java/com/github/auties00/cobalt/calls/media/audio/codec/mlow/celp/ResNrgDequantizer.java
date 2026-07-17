package com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeDecoder;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.NrgResTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Decodes and dequantizes the residual energy and fixed codebook gain parameters of an MLow unvoiced low
 * band frame.
 *
 * <p>When an MLow frame is coded as unvoiced (or as conditional background noise), its excitation gain is
 * not an adaptive codebook gain but a quantized residual energy in decibels. The decoder reads, against
 * the {@link NrgResTables} cumulative mass functions and in this order:
 * <ul>
 * <li>a frame level energy index, drawn against the gain cumulative mass function for the frame's subframe
 * count;</li>
 * <li>for two and four subframe frames, a shape index that selects a vector of per subframe Q10 decibel
 * deltas;</li>
 * <li>for each subframe that carries pulses, a fixed codebook gain offset index, range decoded against a
 * window of one of the fixed codebook gain offset cumulative mass functions.</li>
 * </ul>
 * From the frame index and shape deltas the decoder reconstructs the per subframe quantized energy in Q14
 * decibels; {@link #dequantizeResnrg(int, int)} then converts one such Q14 energy into the linear residual
 * energy the excitation synthesis scales by.
 *
 * <p>The decoded integer indices and the Q14 reconstruction are exact integer arithmetic. The final
 * {@link #dequantizeResnrg(int, int)} is a single precision power approximation, so it is near exact
 * (within a small relative epsilon) rather than exact.
 *
 * <p>This decoder is stateless: every value it reads depends only on the supplied range decoder, the
 * frame's subframe count, and the per subframe pulse counts. Instances are cheap and hold no mutable
 * state; the {@link DecodeResult} record carries the outputs.
 *
 * @implNote This implementation rounds the Q14 energy to whole decibels with the arithmetic right shift
 * {@code (dbqQ14 + (1 << 13)) >> 14}, clamps it to the residual energy decibel range
 * {@code [RES_NRG_MIN_DB, RES_NRG_MAX_DB]}, and windows the fixed codebook gain offset cumulative mass
 * function starting at {@code -nrgresDbq} for {@code maxOffset - minOffset + 2} entries. Shape codebook
 * deltas are stored in Q10 and added in Q14 by scaling them by {@code 16} (that is {@code 2^4}). The
 * {@link #dequantizeResnrg(int, int)} uses the fast bit hack power approximation
 * {@link #powfFast(float, float)} rather than an accurate power: the two disagree by up to several percent
 * at the residual energy magnitudes, so only the approximation reproduces the decoder output. All of
 * {@link #dequantizeResnrg(int, int)} is computed in single precision {@code float}.
 */
public final class ResNrgDequantizer {
    /**
     * The logger for {@link ResNrgDequantizer}.
     */
    private static final System.Logger LOGGER = Log.get(ResNrgDequantizer.class);

    /**
     * Residual energy bias added before the logarithm in the forward quantizer and subtracted back out
     * after the inverse power.
     *
     * <p>This small floor keeps the forward base 10 logarithm finite at zero energy. It is held as a
     * {@code float} so that the subtraction in {@link #dequantizeResnrg(int, int)} is performed in single
     * precision.
     */
    private static final float RES_NRG_BIAS = 3.1622776e-9f;

    /**
     * Bias constant of the fast power approximation, the integer {@code 1064866805}.
     *
     * <p>This is the raw {@code float} bit pattern the linear fit in {@link #powfFast(float, float)}
     * centers on; it is subtracted from and then added back to the input's raw integer bits.
     */
    private static final int POWF_FAST_BIAS = 1064866805;

    /**
     * Pulse count bin step; the per subframe pulse count is divided by this to pick the fixed codebook gain
     * offset cumulative mass function column.
     */
    private static final int N_PULSES_STEP = 10;

    /**
     * Number of fixed codebook gain offset cumulative mass function columns; the pulse count bin is clamped
     * to this minus one.
     */
    private static final int FCB_G_OFFSET_CMFS = 4;

    /**
     * Length in decibel steps of the unvoiced fixed codebook gain index range, {@code (0 - (-90)) / 1 = 90}.
     */
    private static final int UV_GAIN_IDX_LEN = 90;

    /**
     * Constructs a residual energy dequantizer.
     *
     * <p>The decoder is stateless, so the instance exists only to group the decode entry points; it holds
     * no fields.
     */
    public ResNrgDequantizer() {
    }

    /**
     * The decoded and reconstructed residual energy parameters of one unvoiced low band frame.
     *
     * @param nrgresFrameQi the frame level energy index
     * @param nrgresShapeQi the shape index for two and four subframe frames; {@code 0} for single subframe
     *                      frames where no shape is coded
     * @param nrgresDbqQ14  the per subframe reconstructed quantized energy in Q14 decibels, length equal to
     *                      the frame's subframe count
     * @param fcbgIdx       the per subframe fixed codebook gain offset index; entries for subframes without
     *                      pulses are left {@code 0}
     */
    public record DecodeResult(int nrgresFrameQi, int nrgresShapeQi, int[] nrgresDbqQ14, int[] fcbgIdx) {
    }

    /**
     * Decodes the residual energy and fixed codebook gain parameters of one unvoiced low band frame.
     *
     * <p>Reads the frame energy index, the shape index (for two and four subframe frames), reconstructs the
     * per subframe Q14 energy, then reads one fixed codebook gain offset index for each subframe with a
     * positive pulse count. The {@code sfPulses} array supplies the per subframe pulse counts the pulse
     * decoder produced earlier in the same frame; only its first {@code numSubfr} entries are read.
     *
     * @param decoder  the range decoder positioned at the residual energy parameters
     * @param numSubfr the number of subframes in the frame; must be 1, 2, or 4
     * @param sfPulses the per subframe pulse counts, at least {@code numSubfr} entries
     * @return the decoded indices and reconstructed Q14 energies
     * @throws IllegalArgumentException if {@code numSubfr} is not 1, 2, or 4
     */
    public DecodeResult decode(MlowRangeDecoder decoder, int numSubfr, int[] sfPulses) {
        var tableIx = numSubfrToIdx(numSubfr);
        int frameQi;
        var shapeQi = 0;
        if (numSubfr == 1) {
            frameQi = MlowEntropyWrapper.decodeUpdate(decoder, NrgResTables.gain1Cmf());
        } else if (numSubfr == 2) {
            frameQi = MlowEntropyWrapper.decodeUpdate(decoder, NrgResTables.gain2Cmf());
            shapeQi = MlowEntropyWrapper.decodeUpdate(decoder, NrgResTables.shapeCb2Cmf());
        } else {
            frameQi = MlowEntropyWrapper.decodeUpdate(decoder, NrgResTables.gain4Cmf());
            shapeQi = MlowEntropyWrapper.decodeUpdate(decoder, NrgResTables.shapeCb4Cmf());
        }

        var frameDbqQ14 = frameQi * (NrgResTables.NRG_STEP_DB_Q14[tableIx] & 0xFFFF);
        frameDbqQ14 += NrgResTables.RES_NRG_MIN_DB << 14;

        var dbqQ14 = new int[numSubfr];
        if (numSubfr == 1) {
            dbqQ14[0] = frameDbqQ14;
        } else {
            var cb = numSubfr == 4 ? NrgResTables.SHAPE_CB_4_Q10 : NrgResTables.SHAPE_CB_2_Q10;
            for (var i = 0; i < numSubfr; i++) {
                dbqQ14[i] = frameDbqQ14 + cb[shapeQi * numSubfr + i] * 16;
            }
        }

        var fcbgIdx = new int[numSubfr];
        for (var i = 0; i < numSubfr; i++) {
            if (sfPulses[i] > 0) {
                fcbgIdx[i] = decodeFcbgOffset(decoder, tableIx, dbqQ14[i], sfPulses[i]);
            }
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "resnrg decode: subframes={0} frameQi={1} shapeQi={2}", numSubfr, frameQi, shapeQi);
        }
        return new DecodeResult(frameQi, shapeQi, dbqQ14, fcbgIdx);
    }

    /**
     * Decodes one per subframe fixed codebook gain offset index.
     *
     * <p>Rounds the subframe's Q14 energy to whole decibels with the arithmetic rounding shift, clamps it
     * to the residual energy decibel range, then windows into the fixed codebook gain offset cumulative
     * mass function selected by subframe count bin and pulse count bin. The window starts at
     * {@code -nrgresDbq} and runs for {@code maxOffset - minOffset + 2} entries.
     *
     * @param decoder the range decoder positioned at the gain offset symbol
     * @param tableIx the subframe count bin: 0 for one, 1 for two, 2 for four subframes
     * @param dbqQ14  the subframe's reconstructed Q14 decibel energy
     * @param nPulses the subframe's pulse count, used to pick the pulse count cumulative mass function
     *                column
     * @return the decoded fixed codebook gain offset index, relative to the window start
     */
    private int decodeFcbgOffset(MlowRangeDecoder decoder, int tableIx, int dbqQ14, int nPulses) {
        var nrgresDbq = (dbqQ14 + (1 << 13)) >> 14;
        nrgresDbq = Math.min(Math.max(nrgresDbq, NrgResTables.RES_NRG_MIN_DB), NrgResTables.RES_NRG_MAX_DB);
        var minOffset = -nrgresDbq;
        var maxOffset = UV_GAIN_IDX_LEN - nrgresDbq;
        var cmfLen = maxOffset - minOffset + 2;
        var cmfIx = Math.min(nPulses / N_PULSES_STEP, FCB_G_OFFSET_CMFS - 1);
        var cmf = NrgResTables.fcbgOffsetCmf(tableIx, cmfIx);
        return MlowEntropyWrapper.decodeUpdate(decoder, cmf, minOffset, cmfLen);
    }

    /**
     * Converts one quantized residual energy in Q14 decibels into linear residual energy.
     *
     * <p>Computes {@code 10^(0.1 * dbq / 2^14) - RES_NRG_BIAS}, floors the result at zero, then scales by
     * the fixed codebook subframe length. The energy is in the same domain the excitation noise generator
     * scales the unvoiced fixed codebook pulses by.
     *
     * @param nrgresFrameDbqQ14 the quantized residual energy in Q14 decibels, one entry of
     *                          {@link DecodeResult#nrgresDbqQ14()}
     * @param fcbSubfrLen       the fixed codebook subframe length in samples
     * @return the linear residual energy, never negative, scaled by {@code fcbSubfrLen}
     */
    public static float dequantizeResnrg(int nrgresFrameDbqQ14, int fcbSubfrLen) {
        var exponent = 0.1f * (nrgresFrameDbqQ14 / (float) (1 << 14));
        var resnrg = powfFast(10.0f, exponent) - RES_NRG_BIAS;
        resnrg = Math.max(resnrg, 0.0f);
        resnrg *= fcbSubfrLen;
        return resnrg;
    }

    /**
     * Computes a fast single precision power approximation.
     *
     * <p>Reinterprets the base {@code a} as a 32 bit integer, fits {@code a} raised to {@code b} with the
     * linear expression {@code (int)(b * (bits - POWF_FAST_BIAS) + POWF_FAST_BIAS)} in {@code float}
     * arithmetic, and reinterprets the result back to a {@code float}. This is intentionally not the
     * accurate power; it is the approximation the decoder relies on for the residual energy inverse power.
     *
     * @param a the base
     * @param b the exponent
     * @return the approximate value of {@code a} raised to {@code b}
     */
    private static float powfFast(float a, float b) {
        var bits = Float.floatToRawIntBits(a);
        var result = (int) (b * (bits - POWF_FAST_BIAS) + (float) POWF_FAST_BIAS);
        return Float.intBitsToFloat(result);
    }

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
