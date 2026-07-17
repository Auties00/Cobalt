package com.github.auties00.cobalt.calls.media.audio.codec.mlow.lsf;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.MlowTocByte;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.param.ParamDecoder;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Per frame line spectral frequency to linear prediction (LSF to LPC) driver for the MLow speech codec.
 *
 * <p>The MLow decoder codes one line spectral frequency (LSF) vector per 20 ms frame, but the short term
 * synthesis filter is applied per subframe. The parameter decoder ({@link ParamDecoder}) reconstructs the
 * per frame dequantized LSF vector and the frame's interpolation index; this class is the stage that turns
 * that pair, frame by frame, into the per subframe stabilized linear prediction filters the synthesis filter
 * consumes. It selects the per subframe interpolation factor row from the frame's interpolation index, hands
 * the dequantized LSF vector and that row to {@link LpcInterpolator}, and threads the previous frame LSF
 * state across frames.
 *
 * <p>For a regular (non SID) frame the interpolation factor row is the row at the frame's interpolation index
 * of the factor table for the frame's subframe count. For a silence insertion descriptor (SID) frame the
 * single comfort noise (DTX) factor row is used instead, regardless of the index.
 *
 * <p>The interpolation itself carries state: the previous frame's LSF vector is held inside the wrapped
 * {@link LpcInterpolator} and updated after every frame. This driver therefore must be fed every frame of one
 * continuous decode session in order; construct one driver per logical stream. {@link #reset()} returns both
 * this driver and its interpolator to the freshly constructed state. It does not reset {@link ParamDecoder}; a
 * caller threading a whole decode resets both in lock step.
 *
 * <p>The integer NLSF to LPC conversion inside the interpolation is bit exact against the native decoder (it
 * runs the integer fixed point conversion via {@link NlsfBridge}); the float interpolation and the bandwidth
 * expansion stabilization round identically to the native single precision arithmetic, so the produced per
 * subframe LPC filters and interpolated LSF vectors match the native decoder to within IEEE 754 rounding.
 *
 * <p>Scope is the SMPL 16 kHz, 60 ms, mono low band decode path with {@code SMPL_LPC_ORDER == 16}. The
 * packet loss concealment adaptations the decoder performs around this stage are inactive on a continuous
 * loss free decode and are not part of this driver; the high band reuse of the interpolation is out of scope.
 * This type is stateful per stream and is not thread safe.
 *
 * @implNote This implementation factors the LSF interpolation stage out of the surrounding excitation and
 * synthesis loop. The subframe count selects among the one, two, and four subframe factor tables, and the
 * interpolation index selects the row within the chosen table. The actual blend, NLSF to LPC conversion, and
 * stabilization are delegated to {@link LpcInterpolator#interpolate(float[], float[])}, which holds the
 * carried previous frame LSF state.
 */
public final class SubframeLpc {
    /**
     * The logger for {@link SubframeLpc}.
     */
    private static final System.Logger LOGGER = Log.get(SubframeLpc.class);

    /**
     * Per subframe interpolation factors for a single subframe frame.
     *
     * <p>A 10 ms low band frame has one subframe and no coded interpolation index (the parameter decoder
     * never codes one for a single subframe frame, so the index is always {@code 0}), so the single factor
     * {@code 0.95f} is always used. Held as a flat one row array, like its {@link #INTERPOL_DTX_1} comfort
     * noise counterpart.
     */
    private static final float[] INTERPOL_1 = {0.95f};

    /**
     * Per subframe interpolation factors for a two subframe frame.
     *
     * <p>Indexed by the coded interpolation index: row {@code 0} is {@code {0.75f, 1.0f}} and row {@code 1}
     * is {@code {0.4f, 0.95f}}. The two subframe layout is the low rate 60 ms path.
     */
    private static final float[][] INTERPOL_2 = {
            {0.75f, 1.0f},
            {0.4f, 0.95f}
    };

    /**
     * Per subframe interpolation factors for a four subframe frame.
     *
     * <p>Indexed by the coded interpolation index: row {@code 0} is {@code {0.55f, 0.88f, 1.0f, 1.0f}} and
     * row {@code 1} is {@code {0.3f, 0.65f, 0.95f, 1.0f}}. The four subframe layout is the high rate 60 ms
     * path, the common case of the SMPL scope. A trailing {@code 1.0f} that repeats the prior factor triggers
     * the filter reuse fast path in {@link LpcInterpolator}.
     */
    private static final float[][] INTERPOL_4 = {
            {0.55f, 0.88f, 1.0f, 1.0f},
            {0.3f, 0.65f, 0.95f, 1.0f}
    };

    /**
     * Comfort noise (DTX) per subframe interpolation factors for a single subframe frame.
     *
     * <p>Used for a single subframe SID frame in place of {@link #INTERPOL_1}; the comfort noise model
     * interpolates more slowly toward the new spectrum, hence the smaller {@code 0.25f} factor.
     */
    private static final float[] INTERPOL_DTX_1 = {0.25f};

    /**
     * Comfort noise (DTX) per subframe interpolation factors for a two subframe frame.
     *
     * <p>A single row {@code {0.15f, 0.3f}} used for a two subframe SID frame regardless of the coded
     * interpolation index.
     */
    private static final float[] INTERPOL_DTX_2 = {0.15f, 0.3f};

    /**
     * Comfort noise (DTX) per subframe interpolation factors for a four subframe frame.
     *
     * <p>A single row {@code {0.1f, 0.157f, 0.2f, 0.3f}} used for a four subframe SID frame regardless of the
     * coded interpolation index.
     */
    private static final float[] INTERPOL_DTX_4 = {0.1f, 0.157f, 0.2f, 0.3f};

    /**
     * The wrapped per subframe interpolator and LPC stabilizer, holding the carried previous frame LSF state.
     */
    private final LpcInterpolator interpolator;

    /**
     * Constructs an LSF to LPC driver with a freshly constructed interpolator.
     *
     * <p>The wrapped {@link LpcInterpolator} starts with a zeroed previous frame LSF vector, so the first
     * frame fed to {@link #process(ParamDecoder.DecodedFrame, MlowTocByte)} is treated as the reset frame and
     * interpolates against itself.
     */
    public SubframeLpc() {
        this.interpolator = new LpcInterpolator();
    }

    /**
     * Returns this driver to its freshly constructed state.
     *
     * <p>Resets the wrapped {@link LpcInterpolator}, zeroing its carried previous frame LSF vector so the next
     * frame is treated as the reset frame. Call this between independent decode sessions; do not call it
     * between the frames of one continuous stream, which must thread the previous frame vector.
     */
    public void reset() {
        interpolator.reset();
    }

    /**
     * Returns the wrapped interpolator's carried previous frame LSF vector, delegating to
     * {@link LpcInterpolator#previousLsf()}.
     *
     * <p>The returned vector is live and is consulted by the packet loss concealment to adapt after a loss.
     *
     * @return the live previous frame LSF vector
     */
    public float[] previousLsf() {
        return interpolator.previousLsf();
    }

    /**
     * Converts one decoded frame's dequantized LSF vector into per subframe stabilized LPC filters.
     *
     * <p>Selects the per subframe interpolation factor row from the frame's
     * {@link ParamDecoder.DecodedFrame#lsfInterpolIdx()} (or the comfort noise row when {@code toc} announces
     * a SID frame), then drives {@link LpcInterpolator#interpolate(float[], float[])} with the frame's
     * dequantized LSF vector and that row, advancing the carried previous frame state. The subframe count is
     * taken from {@code toc}.
     *
     * @param frame the decoded frame whose dequantized LSF vector ({@link ParamDecoder.DecodedFrame#lsf()})
     *              and interpolation index ({@link ParamDecoder.DecodedFrame#lsfInterpolIdx()}) drive the
     *              interpolation
     * @param toc   the decoded TOC of the packet, supplying the subframe count, low rate flag, and SID flag
     * @return the per subframe stabilized LPC filters and interpolated LSF vectors
     */
    public LpcInterpolator.InterpolatedFrame process(ParamDecoder.DecodedFrame frame, MlowTocByte toc) {
        var numSubframes = toc.numSubframes();
        var interpol = interpolRow(numSubframes, frame.lsfInterpolIdx(), toc.sid());
        return interpolator.interpolate(frame.lsf(), interpol);
    }

    /**
     * Selects the per subframe interpolation factor row for one frame.
     *
     * <p>For a regular frame the row is the row at {@code lsfInterpolIdx} of the factor table chosen by
     * {@code numSubframes}; for a SID frame the single comfort noise row for that subframe count is returned
     * regardless of the index. The returned array's length is {@code numSubframes}.
     *
     * @param numSubframes   the subframe count of the frame; 1, 2, or 4
     * @param lsfInterpolIdx the coded interpolation index; ignored for a SID frame
     * @param sid            {@code true} for a silence insertion descriptor frame, selecting the comfort noise
     *                       factor row
     * @return the per subframe interpolation factor row of length {@code numSubframes}
     * @throws IllegalArgumentException if {@code numSubframes} is not 1, 2, or 4
     */
    private static float[] interpolRow(int numSubframes, int lsfInterpolIdx, boolean sid) {
        return switch (numSubframes) {
            case 1 -> sid ? INTERPOL_DTX_1 : INTERPOL_1;
            case 2 -> sid ? INTERPOL_DTX_2 : INTERPOL_2[lsfInterpolIdx];
            case 4 -> sid ? INTERPOL_DTX_4 : INTERPOL_4[lsfInterpolIdx];
            default -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "mlow subframe lpc: unsupported subframe count={0}", numSubframes);
                }
                throw new IllegalArgumentException("unsupported subframe count " + numSubframes);
            }
        };
    }
}
