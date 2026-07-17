package com.github.auties00.cobalt.calls.media.audio.neteq;

import java.util.Arrays;
import java.util.Objects;

/**
 * Resamples a fixed size block of PCM from one sample rate to another with a windowed sinc filter, the
 * shared kernel the audio path uses ahead of and behind the codec when the device rate differs from the
 * codec rate.
 *
 * <p>The resampler is configured once for a source and destination rate and a fixed source block length,
 * and then converts each pushed block through {@link #resample(float[], float[])}. The conversion
 * convolves the source with a Blackman windowed sinc low pass whose cutoff is set by the rate ratio so the
 * destination carries no aliasing and no imaging; the filter state from the tail of one block carries into
 * the next so block boundaries are seamless. The destination length is the source length scaled by the
 * rate ratio.
 *
 * <p>The resampler is single writer: the audio pump drives one instance from its thread. It is pure Java
 * convolution with no native state.
 *
 * @implNote This implementation builds the kernel once at construction into a table of
 * {@value #KERNEL_OFFSET_COUNT} plus one subsample shifted copies of {@value #KERNEL_SIZE} taps each. Each
 * tap is the sinc {@code sin(s*pre) / pre} evaluated at the subsample offset, scaled by the cutoff
 * {@code (io_ratio > 1 ? 1/io_ratio : 1) * 0.9}, times the Blackman window
 * {@code A0 - A1*cos(2*pi*x) + A2*cos(4*pi*x)} with alpha {@code 0.16}. Each output sample maps to a
 * fractional source position; the scalar convolution linearly interpolates between the two adjacent
 * subsample kernels by the fractional offset before convolving over {@value #KERNEL_SIZE} source taps
 * reaching into the carried history.
 */
public final class PushSincResampler {
    /**
     * The number of taps in each subsample kernel of the windowed sinc filter.
     *
     * <p>Each output sample convolves this many source taps; the filter group delay is half this count.
     */
    private static final int KERNEL_SIZE = 32;

    /**
     * The number of subsample offsets the kernel table is precomputed at.
     *
     * <p>The table holds this many plus one kernels so an output sample can interpolate between the two
     * kernels bracketing its fractional source position; the extra kernel is the {@code 1.0} offset.
     */
    private static final int KERNEL_OFFSET_COUNT = 32;

    /**
     * The first Blackman window coefficient.
     */
    private static final double WINDOW_A0 = 0.5 * (1.0 - 0.16);

    /**
     * The second Blackman window coefficient.
     */
    private static final double WINDOW_A1 = 0.5;

    /**
     * The third Blackman window coefficient.
     */
    private static final double WINDOW_A2 = 0.5 * 0.16;

    /**
     * The fixed source block length in samples this resampler is configured for.
     */
    private final int sourceBlockLength;

    /**
     * The destination block length in samples, the source length scaled by the rate ratio.
     */
    private final int destinationBlockLength;

    /**
     * The ratio of source rate over destination rate, the step in source samples per destination sample.
     */
    private final double ioSampleRateRatio;

    /**
     * The precomputed kernel table, {@code (KERNEL_OFFSET_COUNT + 1) * KERNEL_SIZE} taps stored row major by
     * subsample offset.
     *
     * <p>Row {@code o} holds the {@value #KERNEL_SIZE} tap kernel for subsample offset
     * {@code o / KERNEL_OFFSET_COUNT}; an output sample blends the two rows bracketing its fractional
     * source position.
     */
    private final float[] kernel;

    /**
     * The history of source samples preceding the current block, the filter memory.
     *
     * <p>Sized to {@value #KERNEL_SIZE} so the convolution at the start of a block can reach back into the
     * previous block; shifted in on each {@link #resample(float[], float[])}.
     */
    private final float[] history;

    /**
     * Constructs a resampler for the given rates and fixed source block length.
     *
     * @param sourceRate        the source sample rate in Hz; must be positive
     * @param destinationRate   the destination sample rate in Hz; must be positive
     * @param sourceBlockLength the fixed source block length in samples; must be positive
     * @throws IllegalArgumentException if any argument is not positive
     */
    public PushSincResampler(int sourceRate, int destinationRate, int sourceBlockLength) {
        if (sourceRate <= 0 || destinationRate <= 0 || sourceBlockLength <= 0) {
            throw new IllegalArgumentException("rates and block length must be positive");
        }
        this.sourceBlockLength = sourceBlockLength;
        this.destinationBlockLength = (int) ((long) sourceBlockLength * destinationRate / sourceRate);
        this.ioSampleRateRatio = (double) sourceRate / destinationRate;
        this.kernel = new float[(KERNEL_OFFSET_COUNT + 1) * KERNEL_SIZE];
        this.history = new float[KERNEL_SIZE];
        initializeKernel();
    }

    /**
     * Returns the destination block length one {@link #resample(float[], float[])} call produces.
     *
     * @return the destination block length in samples
     */
    public int destinationBlockLength() {
        return destinationBlockLength;
    }

    /**
     * Resamples one source block into the destination buffer.
     *
     * <p>For each destination sample, maps its position to a fractional source position, selects the two
     * adjacent subsample kernels and the interpolation factor between them, and convolves both over the
     * source neighbourhood reaching into the carried history before blending them. The destination buffer
     * must hold {@link #destinationBlockLength()} samples; the source buffer must hold the configured source
     * block length. The source tail is retained as history for the next call.
     *
     * @param source      the source block, exactly the configured source block length; never {@code null}
     * @param destination the destination buffer, at least {@link #destinationBlockLength()} long; never
     *                    {@code null}
     * @return the number of destination samples written, {@link #destinationBlockLength()}
     * @throws NullPointerException     if {@code source} or {@code destination} is {@code null}
     * @throws IllegalArgumentException if {@code source} is not the configured length or
     *                                  {@code destination} is too short
     */
    public int resample(float[] source, float[] destination) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        if (source.length != sourceBlockLength) {
            throw new IllegalArgumentException("source length " + source.length
                    + " does not match configured block length " + sourceBlockLength);
        }
        if (destination.length < destinationBlockLength) {
            throw new IllegalArgumentException("destination length " + destination.length
                    + " is below required " + destinationBlockLength);
        }
        var extended = new float[KERNEL_SIZE + sourceBlockLength + KERNEL_SIZE / 2];
        System.arraycopy(history, 0, extended, 0, KERNEL_SIZE);
        System.arraycopy(source, 0, extended, KERNEL_SIZE, sourceBlockLength);

        for (var j = 0; j < destinationBlockLength; j++) {
            var virtualSourceIdx = j * ioSampleRateRatio;
            var sourceIdx = (int) virtualSourceIdx;
            var subsampleRemainder = virtualSourceIdx - sourceIdx;
            var virtualOffsetIdx = subsampleRemainder * KERNEL_OFFSET_COUNT;
            var offsetIdx = (int) virtualOffsetIdx;
            var interpolationFactor = virtualOffsetIdx - offsetIdx;
            destination[j] = convolve(extended, KERNEL_SIZE / 2 + sourceIdx, offsetIdx, interpolationFactor);
        }

        System.arraycopy(source, sourceBlockLength - KERNEL_SIZE, history, 0, KERNEL_SIZE);
        return destinationBlockLength;
    }

    /**
     * Clears the filter history so the next block is convolved without carrying prior samples.
     *
     * <p>Used when the stream is reconfigured; without it a resumed stream would smear the previous block's
     * tail into the new block.
     */
    public void reset() {
        Arrays.fill(history, 0.0f);
    }

    /**
     * Builds the precomputed subsample kernel table once at construction.
     *
     * <p>Fills each of the {@value #KERNEL_OFFSET_COUNT} plus one subsample shifted rows with the taps that
     * multiply the sinc by the Blackman window for that offset, the sinc cut off at the scale factor derived
     * from the rate ratio.
     */
    private void initializeKernel() {
        var sincScaleFactor = sincScaleFactor(ioSampleRateRatio);
        for (var offsetIdx = 0; offsetIdx <= KERNEL_OFFSET_COUNT; offsetIdx++) {
            var subsampleOffset = (double) offsetIdx / KERNEL_OFFSET_COUNT;
            for (var i = 0; i < KERNEL_SIZE; i++) {
                var idx = i + offsetIdx * KERNEL_SIZE;
                var preSinc = Math.PI * (i - KERNEL_SIZE / 2 - subsampleOffset);
                var x = (i - subsampleOffset) / KERNEL_SIZE;
                var window = WINDOW_A0 - WINDOW_A1 * Math.cos(2.0 * Math.PI * x)
                        + WINDOW_A2 * Math.cos(4.0 * Math.PI * x);
                var sinc = preSinc == 0.0 ? sincScaleFactor : Math.sin(sincScaleFactor * preSinc) / preSinc;
                kernel[idx] = (float) (window * sinc);
            }
        }
    }

    /**
     * Convolves the two kernels bracketing the fractional source position and blends them.
     *
     * <p>Computes the dot product of the {@value #KERNEL_SIZE} taps starting at the given buffer index
     * against each of the two adjacent subsample kernels, then linearly interpolates between the two sums
     * by the interpolation factor.
     *
     * @param extended            the source buffer extended at its front with the carried history
     * @param base                the index in {@code extended} the leftmost kernel tap reads
     * @param offsetIdx           the lower of the two bracketing subsample kernel rows
     * @param interpolationFactor the fraction toward the upper bracketing kernel, in {@code [0, 1)}
     * @return the interpolated, convolved sample value
     */
    private float convolve(float[] extended, int base, int offsetIdx, double interpolationFactor) {
        var k1Base = offsetIdx * KERNEL_SIZE;
        var k2Base = (offsetIdx + 1) * KERNEL_SIZE;
        var sum1 = 0.0;
        var sum2 = 0.0;
        for (var i = 0; i < KERNEL_SIZE; i++) {
            var value = extended[base + i];
            sum1 += value * kernel[k1Base + i];
            sum2 += value * kernel[k2Base + i];
        }
        return (float) ((1.0 - interpolationFactor) * sum1 + interpolationFactor * sum2);
    }

    /**
     * Returns the sinc cutoff scale factor for a ratio of source rate over destination rate.
     *
     * <p>For a downsample the cutoff is the reciprocal of the ratio so the destination Nyquist is not
     * exceeded; for an upsample it is one. The result is narrowed to ninety percent to leave a transition
     * band.
     *
     * @param ioRatio the ratio of source rate over destination rate
     * @return the sinc cutoff scale factor
     */
    private static double sincScaleFactor(double ioRatio) {
        var scale = ioRatio > 1.0 ? 1.0 / ioRatio : 1.0;
        return scale * 0.9;
    }
}
