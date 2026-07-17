package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables.NrgResTables;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Quantizes the per subframe residual energy of one MLow unvoiced low band frame into the frame gain, shape,
 * and per subframe Q14 decibel indices the bitstream carries.
 *
 * <p>When the core encoder decides a frame is unvoiced it does not transmit an adaptive codebook gain;
 * instead it measures the linear prediction residual energy of each subframe and quantizes it as a decibel
 * quantity. This class is the exact inverse of the residual energy decode in
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.ResNrgDequantizer}: it consumes the
 * per subframe linear residual energies the caller measured and produces the integer indices that decoder
 * reads back, namely the frame index {@code nrgres_frame_qi}, the shape index {@code nrgres_shape_qi}, and
 * the reconstructed per subframe {@code nrgres_dbq_Q14}.
 *
 * <p>The quantization is:
 * <ul>
 * <li>each subframe energy is mapped to decibels by {@code min(10 * log10(nrgres[i] + bias), 0)} and the
 * decibel values are averaged into a single frame decibel level;</li>
 * <li>the frame level is uniformly quantized into {@code nrgres_frame_qi} against the step size for the
 * frame's subframe count ({@link NrgResTables#NRG_STEP_DB_Q14}), then dequantized back to the Q14 frame
 * level the decoder reconstructs;</li>
 * <li>for one subframe frames there is no shape; {@code nrgres_dbq_Q14[0]} is the frame level and
 * {@code nrgres_shape_qi} stays zero;</li>
 * <li>for two and four subframe frames the per subframe deviations from the frame level are vector quantized
 * against the matching shape codebook ({@link NrgResTables#SHAPE_CB_2_Q10} or
 * {@link NrgResTables#SHAPE_CB_4_Q10}) by minimum squared decibel error, selecting {@code nrgres_shape_qi},
 * and each {@code nrgres_dbq_Q14[i]} is the frame level plus the selected codebook entry scaled to Q14.</li>
 * </ul>
 * The fixed codebook gain offset index ({@code fcbg_idx}) the decoder also reads is not produced here: it is
 * chosen during the pulse search and committed by the core encoder before this routine runs, so it is
 * outside the residual energy quantization scope.
 *
 * <p>The produced {@code nrgres_frame_qi}, {@code nrgres_shape_qi}, and {@code nrgres_dbq_Q14} are integer
 * quantities and reconstruct bit for bit to the values
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.celp.ResNrgDequantizer} decodes from the
 * same bitstream. The intermediate decibel computation is single precision {@code float}; only the final
 * integer indices are contractually exact.
 *
 * <p>This quantizer is stateless: every value it produces depends only on the supplied energies and subframe
 * count. Instances are cheap and hold no mutable state; the {@link Result} record carries the outputs.
 *
 * <p>Scope is the SMPL 16 kHz / 60 ms / mono low band encode path. This type is internal to the MLow encode
 * implementation and is not exported from the module.
 *
 * @implNote This implementation reproduces the fixed point arithmetic of the reference quantizer exactly. The
 * frame index is {@code roundf((frame_db - RES_NRG_MIN_DB) / (step_db_Q14 / 2^14))}; because C {@code roundf}
 * rounds halves away from zero rather than half up like {@link Math#round}, the rounding is done by
 * {@link #roundfAwayFromZero(float)}. The Q14 frame level is the integer product
 * {@code nrgres_frame_qi * step_db_Q14} plus {@code RES_NRG_MIN_DB * 2^14}, computed in {@code int}. The
 * shape codebook search subtracts the dequantized Q14 frame level (converted to {@code float}) from each
 * decibel value, then minimizes the sum of squared differences against the codebook entries scaled by
 * {@code 1 / 2^10}; it keeps the first codebook vector whose distortion is strictly less than the running
 * best, and each {@code nrgres_dbq_Q14[i]} is reconstructed as the Q14 frame level plus the codebook entry
 * promoted from Q10 to Q14 by {@code 2^4}. The step values are read unsigned from the {@code short[]} table
 * by masking with {@code 0xFFFF}.
 */
public final class NrgResQuantizer {
    /**
     * Residual energy bias added before the logarithm.
     *
     * <p>This small floor keeps {@code log10} finite at zero energy. Kept as a {@code float} so the addition
     * is performed in single precision.
     */
    private static final float RES_NRG_BIAS = 3.1622776e-9f;

    /**
     * The Q14 scale {@code 1 / 2^14}, converting a Q14 decibel step to its real decibel value for the frame
     * index division and the shape search frame level subtraction.
     */
    private static final float Q14_SCALE = 1.0f / (1 << 14);

    /**
     * The Q10 scale {@code 1 / 2^10}, converting a Q10 shape codebook entry to its real decibel value for the
     * squared error search.
     */
    private static final float Q10_SCALE = 1.0f / (1 << 10);

    /**
     * The logger for {@link NrgResQuantizer}.
     */
    private static final System.Logger LOGGER = Log.get(NrgResQuantizer.class);

    /**
     * Constructs a residual energy quantizer.
     *
     * <p>The quantizer is stateless, so the instance exists only to group the encode entry point; it holds no
     * fields.
     */
    public NrgResQuantizer() {
    }

    /**
     * The quantized residual energy parameters of one unvoiced low band frame.
     *
     * @param nrgresFrameQi the frame level energy index
     * @param nrgresShapeQi the shape index for two and four subframe frames; {@code 0} for single subframe
     *                      frames where no shape is coded
     * @param nrgresDbqQ14  the per subframe reconstructed quantized energy in Q14 decibels, length equal to
     *                      the frame's subframe count
     */
    public record Result(int nrgresFrameQi, int nrgresShapeQi, int[] nrgresDbqQ14) {
    }

    /**
     * Quantizes the per subframe residual energies of one unvoiced low band frame.
     *
     * <p>Maps each subframe energy to clamped decibels, averages them into the frame level, uniformly
     * quantizes the frame level into the frame index, and for multi subframe frames vector quantizes the per
     * subframe deviations into the shape index. Reconstructs the per subframe Q14 decibel energies the
     * decoder reads back. Only the first {@code numSubfr} entries of {@code nrgres} are read.
     *
     * @param nrgres   the per subframe linear residual energies, at least {@code numSubfr} entries
     * @param numSubfr the number of subframes in the frame; must be 1, 2, or 4
     * @return the quantized frame index, shape index, and reconstructed Q14 energies
     * @throws IllegalArgumentException if {@code numSubfr} is not 1, 2, or 4
     */
    public Result quantize(float[] nrgres, int numSubfr) {
        var tableIx = numSubfrToIdx(numSubfr);

        var nrgresFrameDb = 0.0f;
        var nrgresDb = new float[numSubfr];
        for (var i = 0; i < numSubfr; i++) {
            nrgresDb[i] = Math.min(10.0f * log10f(nrgres[i] + RES_NRG_BIAS), NrgResTables.RES_NRG_MAX_DB);
            nrgresFrameDb += nrgresDb[i];
        }
        nrgresFrameDb /= numSubfr;

        var stepQ14 = NrgResTables.NRG_STEP_DB_Q14[tableIx] & 0xFFFF;
        var frameQi = roundfAwayFromZero(
                (nrgresFrameDb - NrgResTables.RES_NRG_MIN_DB) / (Q14_SCALE * stepQ14));

        var frameDbqQ14 = frameQi * stepQ14;
        frameDbqQ14 += NrgResTables.RES_NRG_MIN_DB << 14;

        for (var i = 0; i < numSubfr; i++) {
            nrgresDb[i] -= frameDbqQ14 * Q14_SCALE;
        }

        var dbqQ14 = new int[numSubfr];
        if (numSubfr == 1) {
            dbqQ14[0] = frameDbqQ14;
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "nrgres quant result: numSubfr=1 frameQi={0} shapeQi=0", frameQi);
            }
            return new Result(frameQi, 0, dbqQ14);
        }

        var cb = numSubfr == 4 ? NrgResTables.SHAPE_CB_4_Q10 : NrgResTables.SHAPE_CB_2_Q10;
        var nVecs = cb.length / numSubfr;
        var bestRd = 1e30f;
        var shapeQi = 0;
        for (var n = 0; n < nVecs; n++) {
            var rd = 0.0f;
            for (var i = 0; i < numSubfr; i++) {
                var d = nrgresDb[i] - cb[n * numSubfr + i] * Q10_SCALE;
                rd += d * d;
            }
            if (rd < bestRd) {
                shapeQi = n;
                bestRd = rd;
            }
        }

        for (var i = 0; i < numSubfr; i++) {
            dbqQ14[i] = frameDbqQ14 + cb[shapeQi * numSubfr + i] * 16;
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "nrgres quant result: numSubfr={0} frameQi={1} shapeQi={2}",
                    numSubfr, frameQi, shapeQi);
        }
        return new Result(frameQi, shapeQi, dbqQ14);
    }

    /**
     * Computes the single precision base ten logarithm.
     *
     * <p>Evaluates {@link Math#log10(double)} in {@code double} and narrows the result to {@code float}; the
     * subsequent clamp, average, and rounding absorb the fractional difference into the same integer frame
     * index.
     *
     * @param x the argument; positive at the residual energy magnitudes after the bias addition
     * @return the base ten logarithm of {@code x}, narrowed to {@code float}
     */
    private static float log10f(float x) {
        return (float) Math.log10(x);
    }

    /**
     * Rounds a {@code float} to the nearest integer with halves away from zero.
     *
     * <p>C {@code roundf} rounds a value exactly halfway between two integers to the one with the larger
     * magnitude, which differs from {@link Math#round(float)} (halves toward positive infinity) for negative
     * arguments. This adds {@code 0.5f} to the magnitude and truncates toward zero, reproducing that behavior
     * for the finite values the residual energy quantizer produces.
     *
     * @param x the value to round
     * @return the nearest integer to {@code x}, ties rounded away from zero
     */
    private static int roundfAwayFromZero(float x) {
        return x < 0.0f ? (int) (x - 0.5f) : (int) (x + 0.5f);
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
