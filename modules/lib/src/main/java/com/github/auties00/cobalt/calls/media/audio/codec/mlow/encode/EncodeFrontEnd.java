package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode.LsfQuantizer.QuantizedLsf;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;

/**
 * Short term analysis front end of the MLow speech encoder, chaining the linear prediction analysis and the
 * line spectral frequency vector quantizer into one call per frame.
 *
 * <p>Each high passed 16 kHz frame becomes a quantized short term spectrum in two steps that this class wires
 * together:
 * <ul>
 * <li>windowing, linear prediction, and bandwidth expansion build the bandwidth expanded prediction filter
 * from the frame's lookback buffer ({@link LpcAnalysis});</li>
 * <li>a two stage codebook search selects the stage 1 and stage 2 indices that quantize the line spectral
 * frequencies of that filter ({@link LsfQuantizer}).</li>
 * </ul>
 * The conditional branch is taken exactly when the conditional coding flag is set, which holds when the frame
 * is not the first of its packet and its voicing equals the previous frame's voicing; on that branch the
 * quantizer conditions on the previous frame's quantized line spectral frequencies, which this front end
 * threads on its {@link #previousLsf} field.
 *
 * <p>This class owns the cross frame analysis state the two stages do not: the previous frame's quantized LSF
 * vector that the conditional path reads. The caller supplies the already windowed and high passed lookback
 * buffer per frame; the windowing geometry and the high pass live upstream of the analysis and are not
 * reproduced here.
 *
 * <p>The selected indices ({@link QuantizedLsf#indices()}) are the exact output the decoder side dequantizer
 * reads back: the quantized LSF vector round trips through that dequantizer to exactly the vector the decoder
 * reconstructs.
 *
 * <p>Scope is the SMPL 16 kHz, 60 ms, mono low band encode path with prediction order {@value #LPC_ORDER}. One
 * front end instance carries the state of a single logical stream; construct one per stream, feed it every
 * frame in order, and call {@link #reset()} between independent streams. This type is stateful and is not
 * thread safe.
 *
 * @implNote This implementation holds one {@link LpcAnalysis} and one {@link LsfQuantizer}, both immutable and
 * shareable, plus the single mutable previous LSF carry and a reused windowing scratch buffer. The lookback
 * buffer is passed straight to {@link LpcAnalysis#window(float[], int, boolean, float[])} and then
 * {@link LpcAnalysis#analyze(float[])}, which already applies the bandwidth expansion, so this front end adds
 * no arithmetic of its own beyond the carry update. The carry is overwritten with each frame's quantized
 * reconstruction ({@link QuantizedLsf#lsf()}). Storing the quantized vector directly is the true previous
 * frame carry only on the high rate path, where the last subframe interpolation coefficient is {@code 1.0} so
 * the interpolated last subframe vector equals the quantized vector.
 */
public final class EncodeFrontEnd {
    /**
     * Linear prediction order of the MLow short term filter, and the length of the quantized LSF vector
     * carried across frames.
     */
    private static final int LPC_ORDER = 16;

    /**
     * Length of the analysis lookback buffer for one 20 ms frame within a 60 ms packet.
     *
     * <p>The window stage reads exactly this many samples from the supplied lookback buffer.
     */
    private static final int LPC_BUF_LEN_20MS = 448;

    /**
     * The logger for {@link EncodeFrontEnd}.
     */
    private static final System.Logger LOGGER = Log.get(EncodeFrontEnd.class);

    /**
     * The short term linear prediction analysis stage, shared and immutable.
     */
    private final LpcAnalysis analysis;

    /**
     * The two stage line spectral frequency vector quantizer, shared and immutable.
     */
    private final LsfQuantizer quantizer;

    /**
     * The previous frame's quantized LSF vector.
     *
     * <p>Read only on the conditional coding path and overwritten with each frame's quantized reconstruction;
     * starts zeroed, which the first frame of a stream (always non conditional) does not read.
     */
    private final float[] previousLsf;

    /**
     * The windowed lookback buffer scratch, reused across frames to avoid a per frame allocation.
     */
    private final float[] windowed;

    /**
     * Holds the result of one frame's short term analysis and quantization.
     *
     * <p>{@code analysis} is the full {@link LpcAnalysis} output (the raw and bandwidth expanded filters, the
     * analysis line spectral frequencies, and the autocorrelation); {@code quantized} is the
     * {@link LsfQuantizer} output (the selected indices, the quantized LSF vector, and the rate distortion
     * scalars). The quantized LSF vector is the one threaded back as the previous frame carry.
     *
     * @param analysis  the short term analysis result for the frame
     * @param quantized the LSF quantization result for the frame
     */
    public record FrameResult(LpcAnalysis.Result analysis, QuantizedLsf quantized) {
    }

    /**
     * Constructs a front end with a fresh analysis stage, quantizer, and zeroed cross frame carry.
     *
     * <p>The analysis allocates its own FFT setup and the quantizer loads the shared encoder tables; the
     * previous LSF carry starts zeroed, ready for the first non conditional frame of a stream.
     */
    public EncodeFrontEnd() {
        this.analysis = new LpcAnalysis();
        this.quantizer = new LsfQuantizer();
        this.previousLsf = new float[LPC_ORDER];
        this.windowed = new float[LPC_BUF_LEN_20MS];
    }

    /**
     * Returns this front end to its freshly constructed state.
     *
     * <p>Zeroes the previous LSF carry; the analysis and quantizer stages hold no state across frames, so no
     * other state is reset. Call this between independent streams; do not call it between the frames of one
     * stream, which must thread the carry.
     */
    public void reset() {
        Arrays.fill(previousLsf, 0.0f);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "encode front end reset");
        }
    }

    /**
     * Analyzes and quantizes one frame's short term spectrum.
     *
     * <p>Windows the lookback buffer, runs the linear prediction analysis and bandwidth expansion, and
     * searches the two stage codebook for the quantizer indices, taking the conditional path when
     * {@code condCoding} is set. The quantized LSF vector is stored as the previous frame carry for the next
     * conditionally coded frame.
     *
     * @param lookback   the frame's lookback buffer, at least {@value #LPC_BUF_LEN_20MS} samples from
     *                   {@code offset}
     * @param offset     the offset of the first lookback sample within {@code lookback}
     * @param longWindow {@code true} to use the long trailing analysis taper (more frames follow in the
     *                   packet), {@code false} for the short taper with trailing zeros
     * @param condCoding {@code true} to take the conditional quantization path
     * @param surv       the survivor count for the codebook search
     * @param rdwAdj      the rate distortion weighting adjustment
     * @param voiced     {@code 0} for the unvoiced class, {@code 1} for the voiced class
     * @param lowRate    {@code 0} for high rate, {@code 1} for low rate
     * @return the frame's analysis and quantization result
     */
    public FrameResult process(float[] lookback, int offset, boolean longWindow, boolean condCoding,
                               int surv, float rdwAdj, int voiced, int lowRate) {
        analysis.window(lookback, offset, longWindow, windowed);
        var lpc = analysis.analyze(windowed);

        QuantizedLsf quant;
        if (condCoding) {
            quant = quantizer.quantCond(surv, lpc.lpc(), previousLsf, rdwAdj, voiced, lowRate);
        } else {
            quant = quantizer.quant(surv, lpc.lpc(), rdwAdj, voiced, lowRate);
        }
        // This stores the quantized vector directly as the carry, which is the true previous frame carry only
        // on the high rate path, where the last subframe interpolation coefficient is 1.0 so the interpolated
        // last subframe vector equals the quantized vector. On the low rate alternative index path the last
        // coefficient is 0.95, so the true carry is the blended last subframe vector, which that path threads
        // through its own committed candidate rather than this field.
        System.arraycopy(quant.lsf(), 0, previousLsf, 0, LPC_ORDER);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "encode front end process: condCoding={0} voiced={1} lowRate={2}",
                    condCoding, voiced, lowRate);
        }
        return new FrameResult(lpc, quant);
    }
}
