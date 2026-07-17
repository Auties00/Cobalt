package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

/**
 * Holds the quantized low band parameters the MLow speech encoder produces for one internal 20 ms frame.
 *
 * <p>The MLow core encoder fills one instance per frame from the analysis by synthesis search stages: the
 * voicing decision, the two stage line spectral frequency indices and the interpolation index, the fixed
 * codebook excitation pulses, and then either the voiced gain and lag set (the per subframe adaptive codebook
 * and fixed codebook gain indices plus the pitch lag block segmentation and lag indices) or the unvoiced
 * residual energy set (the frame energy index, the shape index, the per subframe fixed codebook gain offset
 * indices, and the reconstructed per subframe Q14 decibel energies the gain offset window keys off). The
 * serializer reads exactly these fields, in this order, and emits the range coded bitstream that the decode
 * side {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.param.ParamDecoder} reconstructs them
 * from.
 *
 * <p>This is the carrier between the encode front end (the LPC and LSF, pitch, voicing, CELP, and residual
 * energy search blocks) and {@link ParamEncoder}. It is a plain immutable value: a frame's parameters are
 * gathered by the front end, packed into one instance, and handed to the serializer. The {@code pulses} array
 * is the stacked signed pulse layout indexed by absolute sample position within the frame, each entry the
 * signed pulse magnitude at that position with coincident pulses summed; {@link PulseEncoder} re derives the
 * per subframe pulse counts and positions from it, so {@code sfPulses} is an analysis convenience the
 * serializer recomputes rather than trusts.
 *
 * <p>Scope is the SMPL 16 kHz, 60 ms, mono low band path with prediction order {@value #LPC_ORDER}. The high
 * band parameters are out of scope at 16 kHz and are not carried here. This type is internal to the MLow
 * encode implementation and is intentionally not exported from the module.
 *
 * @param voiced         {@code true} when the frame is coded voiced
 * @param lsfIdx         the {@value #LPC_ORDER}{@code  + 1} LSF indices: the stage one centroid at index
 *                       {@code 0} then the {@value #LPC_ORDER} stage two level indices
 * @param lsfInterpolIdx the LSF interpolation index for a multi subframe frame coded as active voice;
 *                       {@code 0} when not coded
 * @param pulses         the stacked signed fixed codebook pulses indexed by absolute sample position,
 *                       {@code framelen} entries
 * @param sfPulses       the per subframe pulse count, one entry per subframe; an analysis convenience the
 *                       serializer recomputes from {@code pulses}
 * @param acbgIdx        the per subframe adaptive codebook gain index, valid only for a voiced frame
 * @param fcbgIdx        the per subframe fixed codebook gain index (voiced) or gain offset index (unvoiced);
 *                       entries for subframes without pulses are unused
 * @param laginds        the per pitch subframe integer pitch lag indices, valid only for a voiced frame,
 *                       {@value #PITCH_NUM_SUBFRAMES} entries
 * @param blocksegsIx    the pitch lag block segmentation index, valid only for a voiced frame
 * @param nrgresFrameQi  the frame level residual energy index, valid only for an unvoiced frame
 * @param nrgresShapeQi  the residual energy shape index, valid only for an unvoiced multi subframe frame
 * @param nrgresDbqQ14   the per subframe reconstructed residual energy in Q14 decibels, valid only for an
 *                       unvoiced frame; the fixed codebook gain offset window start is derived from it
 */
public record LbQuantParams(
        boolean voiced,
        int[] lsfIdx,
        int lsfInterpolIdx,
        short[] pulses,
        int[] sfPulses,
        int[] acbgIdx,
        int[] fcbgIdx,
        int[] laginds,
        int blocksegsIx,
        int nrgresFrameQi,
        int nrgresShapeQi,
        int[] nrgresDbqQ14) {
    /**
     * Linear prediction order of the MLow short term filter.
     *
     * <p>The LSF index array {@link #lsfIdx()} has one more entry than this order: the stage one centroid plus
     * the per coefficient stage two indices.
     */
    public static final int LPC_ORDER = 16;

    /**
     * Number of pitch subframes a 20 ms frame's lags span.
     *
     * <p>This is the length of {@link #laginds()}.
     */
    public static final int PITCH_NUM_SUBFRAMES = 8;
}
