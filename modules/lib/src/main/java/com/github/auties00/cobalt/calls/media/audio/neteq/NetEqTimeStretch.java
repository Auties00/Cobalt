package com.github.auties00.cobalt.calls.media.audio.neteq;

/**
 * Lengthens or shortens decoded audio by a whole pitch period without dropping a packet, the single channel
 * time stretch operations of the call audio jitter buffer.
 *
 * <p>When the jitter buffer drifts away from its target level the engine resamples time rather than
 * discarding a whole packet: {@link #accelerate(short[], int, int, boolean, boolean)} removes one pitch
 * period to drain an overfull buffer and
 * {@link #preemptiveExpand(short[], int, int, int, boolean, boolean)} duplicates one pitch period to fill an
 * underfull buffer. Both first run the shared {@link #lagSearch(short[], int, int)} to find the dominant
 * pitch period, then splice at that period with a cross fade so the removed or inserted seam is inaudible.
 *
 * <p>The operations are specialized to the single sixteen kilohertz mono channel the call audio format
 * carries, so they run over flat {@code short[]} arrays. The lag search reuses the leaf kernels of
 * {@link NetEqSignalProcessing}; the splice reuses its Q14 cross fade
 * {@link NetEqSignalProcessing#crossFade(short[], int, short[], int, short[], int, int)}.
 */
public final class NetEqTimeStretch {
    /**
     * The number of correlation lags the pitch search scans.
     */
    static final int CORRELATION_LAGS = 50;

    /**
     * The decimated analysis buffer length the lag search builds, in four kilohertz samples.
     */
    static final int ANALYSIS_WINDOW = 110;

    /**
     * The number of samples accumulated into each correlation lag, in four kilohertz samples.
     */
    static final int CORRELATION_LENGTH = 50;

    /**
     * The offset of the anchor correlation window into the decimated buffer, in four kilohertz samples.
     *
     * <p>The anchor window starts here and the sliding window starts {@link #MIN_LAG} samples earlier, so the
     * smallest lag the search can report is {@link #MIN_LAG}; this excludes the trivial zero lag self energy
     * peak.
     */
    static final int ANCHOR_OFFSET = 60;

    /**
     * The smallest lag the search reports, in four kilohertz samples, the gap between the anchor window and
     * the sliding window.
     *
     * <p>The anchor and sliding windows are {@code MIN_LAG} four kilohertz samples apart, so the search
     * covers lags {@code [MIN_LAG, MIN_LAG + CORRELATION_LAGS)}.
     */
    static final int MIN_LAG = 10;

    /**
     * The peak window the splice centers on, in samples per unit of the sample rate multiplier.
     *
     * <p>The cross fade seam is placed {@code fsMult * PEAK_WINDOW_PER_FS} samples into the frame.
     */
    static final int PEAK_WINDOW_PER_FS = 120;

    /**
     * The default correlation threshold gating whether a stretch is attempted.
     */
    static final int CORRELATION_THRESHOLD = 75_000;

    /**
     * The Q14 unity weight, the cross fade and peak clamp ceiling.
     */
    private static final int Q14_ONE = 16_384;

    /**
     * Prevents instantiation of this stateless operation holder.
     */
    private NetEqTimeStretch() {
    }

    /**
     * The outcome of one time stretch attempt: whether a stretch was applied and the resulting audio.
     *
     * <p>When {@link #stretched()} is {@code false} the {@link #output()} is the unchanged input; otherwise it
     * is one pitch period shorter (accelerate) or longer (preemptive expand) than the input.
     *
     * @param stretched whether the splice was applied
     * @param output    the resulting audio
     * @param bestLag   the pitch period lag found, in full rate samples
     */
    record Result(boolean stretched, short[] output, int bestLag) {
    }

    /**
     * Finds the dominant pitch period lag and its Q14 correlation peak in a decoded frame, the shared
     * time stretch lag search.
     *
     * <p>Decimates the input to four kilohertz, computes the {@link #CORRELATION_LAGS} lag normalized cross
     * correlation over the analysis window, and refines the dominant peak to a sub sample lag, returning the
     * lag in full rate samples and the peak value clamped to Q14 unity. A frame too short to decimate returns
     * a zero lag and a zero peak.
     *
     * @implNote This implementation decimates the input to four kilohertz through
     * {@link NetEqSignalProcessing#downsampleTo4kHz} into an {@link #ANALYSIS_WINDOW} sample buffer, then runs
     * the {@link #CORRELATION_LAGS} lag, {@link #CORRELATION_LENGTH} tap negative step correlation of the
     * anchor window at {@link #ANCHOR_OFFSET} against the sliding window {@link #MIN_LAG} samples earlier
     * through {@link NetEqSignalProcessing#crossCorrelationScaled(int[], short[], int, short[], int, int, int, int)},
     * so the curve covers lags {@code [MIN_LAG, MIN_LAG + CORRELATION_LAGS)}. The curve is normalized by the
     * {@link NetEqSignalProcessing#maxAbs32(int[], int, int)} of the correlation through a
     * {@link NetEqSignalProcessing#vectorBitShift(short[], int, int[], int, int, int)} (shift
     * {@code 18 - clz(maxAbs)}, or seventeen when the max abs is zero) before
     * {@link NetEqSignalProcessing#peakDetection(short[], int, int, int[], short[])}. The refined curve index
     * is divided by {@code 2 * fsMult} to the four kilohertz lag, offset by {@link #MIN_LAG}, then expanded to
     * full rate by the decimation factor; the peak is clamped to {@link #Q14_ONE}.
     *
     * @param input  the decoded frame, or the history plus decoded analysis buffer
     * @param length the number of samples in {@code input}
     * @param fsHz   the sample rate in hertz
     * @return the lag in full rate samples paired with the Q14 peak, both zero when the buffer is too short
     */
    static int[] lagSearch(short[] input, int length, int fsHz) {
        var factor = fsHz / 4_000;
        var fsMult = fsHz / 8_000;
        var decimated = new short[ANALYSIS_WINDOW];
        var produced = NetEqSignalProcessing.downsampleTo4kHz(decimated, input, length, ANALYSIS_WINDOW, fsHz, true);
        if (produced < ANALYSIS_WINDOW) {
            return new int[]{0, 0};
        }

        var correlation = new int[CORRELATION_LAGS];
        NetEqSignalProcessing.crossCorrelationScaled(correlation, decimated, ANCHOR_OFFSET,
                decimated, ANCHOR_OFFSET - MIN_LAG, CORRELATION_LENGTH, CORRELATION_LAGS, -1);

        var maxAbs = NetEqSignalProcessing.maxAbs32(correlation, 0, CORRELATION_LAGS);
        var shift = maxAbs == 0 ? 17 : Math.max(0, 18 - Integer.numberOfLeadingZeros(maxAbs));
        var normalized = new short[CORRELATION_LAGS];
        NetEqSignalProcessing.vectorBitShift(normalized, 0, correlation, 0, CORRELATION_LAGS, shift);

        var peakIndex = new int[1];
        var peakValue = new short[1];
        NetEqSignalProcessing.peakDetection(normalized, CORRELATION_LAGS, fsMult, peakIndex, peakValue);

        var curveLag4k = peakIndex[0] / (fsMult << 1);
        var lag = (curveLag4k + MIN_LAG) * factor;
        var peak = Math.min(peakValue[0] & 0xFFFF, Q14_ONE);
        return new int[]{lag, peak};
    }

    /**
     * Time compresses a decoded frame by one pitch period, the accelerate splice.
     *
     * <p>Runs the {@link #lagSearch(short[], int, int)} and, when the criterion permits, removes one pitch
     * period of samples at the peak window by cross fading the period into its successor, returning a frame
     * one pitch period shorter. The stretch is attempted only when the correlation peak is below the
     * threshold selected by {@code activeSpeech} and the supplied precondition holds; otherwise the input is
     * returned unchanged.
     *
     * @implNote This implementation attempts the stretch only when both
     * {@code peak <= (activeSpeech ? 14746 : 8192)} and {@code precondition} hold. The peak window is
     * {@code fsMult * }{@link #PEAK_WINDOW_PER_FS}; the removed region is one {@code bestLag} pitch period
     * cross faded into the following period through
     * {@link NetEqSignalProcessing#crossFade(short[], int, short[], int, short[], int, int)}; the output is
     * the head, the cross faded seam, and the tail shifted down by {@code bestLag}, one pitch period shorter
     * than the input.
     *
     * @param input        the decoded frame
     * @param length       the number of samples
     * @param fsHz         the sample rate in hertz
     * @param activeSpeech whether the frame carries active speech, selecting the peak threshold
     * @param precondition whether the level decision permits the stretch
     * @return the accelerate result; the input unchanged when the criterion fails or no period fits
     */
    static Result accelerate(short[] input, int length, int fsHz, boolean activeSpeech, boolean precondition) {
        var search = lagSearch(input, length, fsHz);
        var bestLag = search[0];
        var peak = search[1];
        var fsMult = fsHz / 8_000;
        var peakWindow = fsMult * PEAK_WINDOW_PER_FS;
        var threshold = activeSpeech ? 14_746 : 8_192;
        var criterion = peak <= threshold && precondition;
        if (!criterion || bestLag <= 0 || peakWindow + 2 * bestLag > length) {
            return new Result(false, input, bestLag);
        }
        var outLength = length - bestLag;
        var out = new short[outLength];
        System.arraycopy(input, 0, out, 0, peakWindow);
        NetEqSignalProcessing.crossFade(out, peakWindow, input, peakWindow,
                input, peakWindow + bestLag, bestLag);
        var tail = length - (peakWindow + 2 * bestLag);
        System.arraycopy(input, peakWindow + 2 * bestLag, out, peakWindow + bestLag, tail);
        return new Result(true, out, bestLag);
    }

    /**
     * Time stretches a decoded frame by one pitch period, the preemptive expand splice.
     *
     * <p>Runs the {@link #lagSearch(short[], int, int)} and, when the criterion permits, inserts one pitch
     * period of samples at the peak window by cross fading a duplicate of the period in, returning a frame
     * one pitch period longer. The stretch is attempted when the level decision did not request a stretch, or
     * when the old data is short and the correlation peak is high; otherwise the input is returned unchanged.
     *
     * @implNote This implementation attempts the stretch when {@code !doStretch}, or when
     * {@code oldDataLength <= fsMult * }{@link #PEAK_WINDOW_PER_FS}{@code  && peakOverride}. The peak window is
     * {@code fsMult * }{@link #PEAK_WINDOW_PER_FS}; the inserted region is one {@code bestLag} pitch period, a
     * duplicate of the period before the window cross faded into the period after it through
     * {@link NetEqSignalProcessing#crossFade(short[], int, short[], int, short[], int, int)}; the output is
     * one pitch period longer than the input.
     *
     * @param input         the decoded frame
     * @param length        the number of samples
     * @param fsHz          the sample rate in hertz
     * @param oldDataLength the number of already played samples preceding this frame
     * @param peakOverride  whether the frame carries a high enough correlation peak
     * @param doStretch     whether the level decision requested a stretch
     * @return the preemptive expand result; the input unchanged when the criterion fails or no period fits
     */
    static Result preemptiveExpand(short[] input, int length, int fsHz, int oldDataLength, boolean peakOverride,
                                   boolean doStretch) {
        var search = lagSearch(input, length, fsHz);
        var bestLag = search[0];
        var fsMult = fsHz / 8_000;
        var peakWindow = fsMult * PEAK_WINDOW_PER_FS;
        var criterion = !doStretch || (oldDataLength <= peakWindow && peakOverride);
        if (!criterion || bestLag <= 0 || peakWindow + 2 * bestLag > length) {
            return new Result(false, input, bestLag);
        }
        var outLength = length + bestLag;
        var out = new short[outLength];
        System.arraycopy(input, 0, out, 0, peakWindow + bestLag);
        NetEqSignalProcessing.crossFade(out, peakWindow + bestLag, input, peakWindow + bestLag,
                input, peakWindow, bestLag);
        var tail = length - (peakWindow + bestLag);
        System.arraycopy(input, peakWindow + bestLag, out, peakWindow + 2 * bestLag, tail);
        return new Result(true, out, bestLag);
    }
}
