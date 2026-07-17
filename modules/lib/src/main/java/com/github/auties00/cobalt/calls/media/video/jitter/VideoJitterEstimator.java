package com.github.auties00.cobalt.calls.media.video.jitter;

import com.github.auties00.cobalt.calls.media.audio.neteq.MovingMedianFilter;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Estimates the network jitter of a video stream with a two state Kalman filter over frame size and
 * inter frame delay, and reports the playout delay a frame must wait to absorb that jitter.
 *
 * <p>For each received frame the estimator is given the frame's compressed size in bytes and the delay
 * of that frame relative to the steady inter frame interval, measured as the wall clock arrival gap
 * minus the timestamp gap. From these it maintains a Kalman estimate of two coupled quantities: the
 * transmission rate (the slope, milliseconds of delay per byte of frame size increase) and the queuing
 * noise (the line, a fixed per frame delay). The filter rejects samples whose deviation exceeds a
 * multiple of the estimated standard deviation so a single late frame does not poison the estimate, and
 * it tracks the running standard deviations of the frame size and of the delay residual that gate that
 * rejection. {@link #frameNackCount(int)} and {@link #lastFrameSizeBytes()} feed the extensions that
 * bound the estimate during retransmission storms.
 *
 * <p>{@link #jitterEstimateMs()} returns the current jitter allowance: the modelled delay for a frame
 * of the running maximum size, plus a multiple of the estimated noise standard deviation. The
 * {@link VideoTimingController} adds decode, render, and round trip time margins to it to compute the
 * total target playout delay. The estimator holds no clock; the caller supplies the relative delay and
 * a current time reading so the filter math stays deterministic and unit testable. Instances are not
 * thread safe; the single video receive thread that owns one drives all updates and reads.
 *
 * @implNote This implementation reproduces the two state Kalman update (the theta covariance, the
 * estimate gain, and the deviation gated measurement) and the exponential noise estimator (the noise
 * mean and variance averages with the forgetting count ramp) of the WebRTC video jitter estimator. The
 * process noise constants {@link #PHI} and {@link #PSI} and the theta base {@link #THETA_LOW} are its
 * defaults (approximately {@code 0.9999}, {@code 0.99}, and {@code 300.0}); the outlier multipliers
 * {@link #NUM_STD_DEV_DELAY_OUTLIER}, {@link #NUM_STD_DEV_SIZE_OUTLIER}, and
 * {@link #NUM_STD_DEV_DELAY_CLAMP} are the upstream defaults ({@code 15}, {@code 3}, {@code 3.5}). The
 * frame size {@link MovingMedianFilter} feeding the running maximum reads a high percentile rather than
 * the strict maximum. The congestion rejection refinement is active: a frame whose size delta marks the
 * link as congested updates the noise estimate but is rejected from the Kalman channel. The forgetting
 * weight of the noise estimate is scaled by the observed frame rate so it forgets at a comparable wall
 * clock pace regardless of the stream's frame rate.
 */
public final class VideoJitterEstimator {
    /**
     * The logger for {@link VideoJitterEstimator}.
     */
    private static final System.Logger LOGGER = Log.get(VideoJitterEstimator.class);

    /**
     * The Kalman process noise gain on the transmission rate state.
     *
     * <p>Scales how quickly the estimated milliseconds per byte slope is allowed to drift between
     * frames; a value just below one keeps the slope nearly stationary.
     */
    private static final double PHI = 0.9999;

    /**
     * The Kalman process noise gain on the queuing noise state.
     *
     * <p>Scales how quickly the estimated fixed per frame delay is allowed to drift; slightly more
     * permissive than {@link #PHI} so the line term tracks queuing changes faster than the rate term.
     */
    private static final double PSI = 0.99;

    /**
     * The base theta covariance seeded into the rate state at construction and reset.
     *
     * <p>Sets the initial uncertainty of the transmission rate estimate so early frames move the
     * estimate substantially before the covariance shrinks.
     */
    private static final double THETA_LOW = 300.0;

    /**
     * The initial value of the transmission rate state, in milliseconds of delay per byte.
     *
     * <p>The reciprocal of a nominal link rate; the Kalman update refines it from observed frame
     * size and delay pairs.
     */
    private static final double INITIAL_SLOPE = 1.0 / (512e3 / 8.0);

    /**
     * The deviation multiple beyond which a delay sample is treated as an outlier and down weighted.
     *
     * <p>A frame whose delay residual exceeds this many estimated standard deviations contributes a
     * clamped residual to the noise estimate rather than the raw value.
     */
    private static final double NUM_STD_DEV_DELAY_OUTLIER = 15.0;

    /**
     * The deviation multiple beyond which a frame size sample is treated as an outlier.
     *
     * <p>A frame whose size deviates from the running average by more than this many standard
     * deviations is excluded from the frame size statistics so a keyframe does not distort them.
     */
    private static final double NUM_STD_DEV_SIZE_OUTLIER = 3.0;

    /**
     * The deviation multiple at which the delay residual is clamped before it updates the noise
     * estimate.
     *
     * <p>Bounds the influence of a single very late frame on the running noise standard deviation.
     */
    private static final double NUM_STD_DEV_DELAY_CLAMP = 3.5;

    /**
     * The fraction of the running maximum frame size below which a frame size delta marks the link as
     * congested, so the late frame is rejected from the Kalman channel update.
     *
     * <p>A frame whose size delta does not exceed this many times the filtered maximum frame size is
     * treated as arriving on a congested link; its delay is folded into the noise estimate but not into
     * the rate and line Kalman channel, so a queue build up does not bias the transmission rate slope.
     *
     * @implNote This implementation uses {@code -0.25}, the congestion rejection factor default of the
     * WebRTC jitter estimator; the noise estimate is updated on every accepted frame while only the
     * Kalman channel update is gated by congestion.
     */
    private static final double CONGESTION_REJECTION_FACTOR = -0.25;

    /**
     * The reference frame rate, in hertz, the noise estimate forgetting weight is scaled against.
     *
     * <p>The exponential forgetting of the noise estimate is exponentiated by the ratio of this
     * reference rate to the observed frame rate so the noise average forgets at a comparable wall clock
     * pace regardless of the stream's frame rate.
     */
    private static final double NOISE_RATE_REFERENCE_FPS = 30.0;

    /**
     * The upper bound, in hertz, on the frame rate estimate the noise forgetting weight is scaled by.
     *
     * <p>Caps the inferred frame rate so a burst of closely spaced frames cannot drive the forgetting
     * weight to an extreme value.
     */
    private static final double MAX_FRAME_RATE_ESTIMATE_FPS = 200.0;

    /**
     * The number of frame deltas folded in before the frame rate scaling of the noise weight is fully
     * applied.
     *
     * <p>During the first frames the rate scaling is blended in linearly with the unscaled weight so an
     * unsettled frame rate estimate does not distort the early noise average.
     */
    private static final double FRAME_PROCESSING_STARTUP_COUNT = 30.0;

    /**
     * The number of recent inter frame intervals the frame rate estimate averages over.
     *
     * <p>The frame rate accumulator keeps this many of the most recent intervals and reports their mean,
     * a windowed rather than cumulative average.
     *
     * @implNote This implementation uses {@code 30}, the frame rate window of the WebRTC jitter
     * estimator's rolling accumulator.
     */
    private static final int FPS_WINDOW = 30;

    /**
     * The exponential averaging weight ceiling for the noise estimator.
     *
     * <p>The noise average ramps its forgetting weight from a low count up to this maximum over the
     * first frames so early samples are weighted more heavily before the average settles.
     */
    private static final double ALPHA_COUNT_MAX = 400.0;

    /**
     * The floor applied to the returned jitter estimate, in milliseconds.
     *
     * <p>The estimate is never reported below one millisecond so the timing controller always reserves
     * a minimal buffer.
     */
    private static final double MIN_JITTER_MS = 1.0;

    /**
     * The running median (or configured percentile) of recent compressed frame sizes, in bytes.
     *
     * <p>Feeds the running maximum frame size the jitter estimate is evaluated at, smoothing out the
     * size spikes of keyframes.
     */
    private final MovingMedianFilter frameSizeFilter;

    /**
     * The two element Kalman state: index {@code 0} the transmission rate slope (ms per byte), index
     * {@code 1} the fixed queuing noise delay (ms).
     */
    private final double[] thetaState;

    /**
     * The two by two Kalman estimate covariance of {@link #thetaState}, row major.
     */
    private final double[][] thetaCov;

    /**
     * The exponentially averaged frame size, in bytes, used for the frame size deviation gate.
     */
    private double avgFrameSizeBytes;

    /**
     * The exponentially averaged variance of the frame size, in bytes squared.
     */
    private double varFrameSizeBytes;

    /**
     * The running maximum frame size observed, in bytes, the jitter estimate is evaluated at.
     */
    private double maxFrameSizeBytes;

    /**
     * The exponentially averaged delay residual, in milliseconds, the queuing noise mean.
     */
    private double avgNoiseMs;

    /**
     * The exponentially averaged variance of the delay residual, in milliseconds squared.
     */
    private double varNoiseMs;

    /**
     * The number of frame deltas folded into the noise estimate, capped at {@link #ALPHA_COUNT_MAX}.
     */
    private double alphaCount;

    /**
     * The compressed size, in bytes, of the most recently inserted frame.
     */
    private long lastFrameSizeBytes;

    /**
     * Whether at least one frame delta has been observed since construction or the last reset.
     */
    private boolean started;

    /**
     * The rolling window of the most recent inter frame intervals in milliseconds, the frame rate
     * accumulator.
     *
     * <p>The mean of the live entries is the mean frame period whose reciprocal is the observed frame
     * rate the noise estimate forgetting weight is scaled against; sized to {@link #FPS_WINDOW} and
     * overwritten oldest first.
     */
    private final double[] frameIntervalsMs;

    /**
     * The number of inter frame intervals recorded in {@link #frameIntervalsMs}, capped at its length.
     */
    private int frameIntervalCount;

    /**
     * The index in {@link #frameIntervalsMs} the next interval overwrites, wrapping at the window length.
     */
    private int frameIntervalIndex;

    /**
     * The running sum, in milliseconds, of the {@link #frameIntervalCount} live inter frame intervals in
     * {@link #frameIntervalsMs}.
     *
     * <p>Maintained incrementally as each interval fills a new slot or evicts the oldest slot so
     * {@link #frameRateFps()} need not sum the window again on every call. It stays exactly equal to the
     * sequential sum the loop would compute because the intervals are integer valued millisecond gaps
     * whose running total remains within the exact integer range of a {@code double}, so the incremental
     * total is bit identical to the total a fresh summation would produce.
     */
    private double frameIntervalSumMs;

    /**
     * The time of the previous frame in milliseconds, or {@code -1} before the first frame.
     *
     * <p>The inter frame interval folded into {@link #frameIntervalsMs} is the gap between the current
     * and this previous frame time.
     */
    private long lastFrameTimeMs;

    /**
     * Constructs a jitter estimator with the given frame size percentile window.
     *
     * <p>The Kalman state is seeded to the nominal slope and base covariance and the statistics are
     * cleared; the first {@link #updateEstimate(double, long, long)} starts the filter.
     *
     * @param frameSizeWindow the number of recent frames the frame size percentile filter retains; must
     *                        be positive
     * @throws IllegalArgumentException if {@code frameSizeWindow} is not positive
     */
    public VideoJitterEstimator(int frameSizeWindow) {
        this.frameSizeFilter = new MovingMedianFilter(frameSizeWindow, MAX_FRAME_SIZE_PERCENTILE);
        this.thetaState = new double[2];
        this.thetaCov = new double[2][2];
        this.frameIntervalsMs = new double[FPS_WINDOW];
        reset();
    }

    /**
     * The percentile of the frame size window used as the running maximum frame size.
     *
     * <p>Reads a high percentile rather than the strict maximum so a single oversized frame does not
     * permanently inflate the estimate.
     *
     * @implNote This implementation uses {@code 0.95}, the maximum frame size percentile default of the
     * WebRTC jitter estimator.
     */
    private static final double MAX_FRAME_SIZE_PERCENTILE = 0.95;

    /**
     * Folds one received frame into the Kalman estimate and returns the updated jitter estimate.
     *
     * <p>Computes the frame size delta against the previous frame, updates the frame size average and
     * variance and the running maximum, then clamps the relative delay to {@link #NUM_STD_DEV_DELAY_CLAMP}
     * estimated standard deviations and runs the Kalman prediction. When the clamped delay residual is
     * within {@link #NUM_STD_DEV_DELAY_OUTLIER} estimated standard deviations or the frame is a positive
     * size outlier, the residual updates the running noise estimate and, unless the frame size delta marks
     * the link as congested (the delta not exceeding {@link #CONGESTION_REJECTION_FACTOR} times the
     * filtered maximum frame size), the Kalman channel is corrected against the clamped delay. A delay
     * that is an outlier and not a size outlier folds only a bounded residual into the noise estimate and
     * leaves the channel untouched.
     *
     * @param frameDelayMs    the frame's delay relative to the steady inter frame interval, in
     *                        milliseconds: the arrival gap minus the timestamp gap, which may be
     *                        negative for an early frame
     * @param frameSizeBytes  the compressed size of the frame in bytes; must not be negative
     * @param nowMs           the current time in milliseconds, from a monotonic source
     * @return the updated jitter estimate in milliseconds, never below {@link #MIN_JITTER_MS}
     * @throws IllegalArgumentException if {@code frameSizeBytes} is negative
     */
    public double updateEstimate(double frameDelayMs, long frameSizeBytes, long nowMs) {
        if (frameSizeBytes < 0) {
            throw new IllegalArgumentException("frameSizeBytes must be non-negative, got " + frameSizeBytes);
        }
        var deltaFrameBytes = (double) frameSizeBytes - lastFrameSizeBytes;
        frameSizeFilter.insert(frameSizeBytes);
        var filteredMaxFrameSizeBytes = frameSizeFilter.filteredValue();
        maxFrameSizeBytes = Math.max(maxFrameSizeBytes, filteredMaxFrameSizeBytes);
        lastFrameSizeBytes = frameSizeBytes;

        if (!started) {
            started = true;
            avgFrameSizeBytes = frameSizeBytes;
            return jitterEstimateMs();
        }

        if (!isFrameSizeOutlier(frameSizeBytes)) {
            updateFrameSizeStats(frameSizeBytes);
        }

        kalmanPredict();
        var noiseStdDevRaw = Math.sqrt(varNoiseMs);
        var noiseStdDev = Math.max(noiseStdDevRaw, 1.0);
        var maxTimeDeviationMs = NUM_STD_DEV_DELAY_CLAMP * noiseStdDevRaw + 0.5;
        var clampedFrameDelayMs = Math.clamp(frameDelayMs, -maxTimeDeviationMs, maxTimeDeviationMs);
        var residualMs = clampedFrameDelayMs - deviation(deltaFrameBytes);
        var delayIsNotOutlier = Math.abs(residualMs) < NUM_STD_DEV_DELAY_OUTLIER * noiseStdDev;
        var sizeIsPositiveOutlier = isFrameSizeOutlier(frameSizeBytes);
        if (delayIsNotOutlier || sizeIsPositiveOutlier) {
            var isNotCongested = deltaFrameBytes > CONGESTION_REJECTION_FACTOR * filteredMaxFrameSizeBytes;
            updateNoiseEstimate(residualMs, nowMs);
            if (isNotCongested) {
                kalmanCorrect(deltaFrameBytes, clampedFrameDelayMs);
            }
        } else {
            var outlierResidualMs = (residualMs >= 0.0 ? NUM_STD_DEV_DELAY_OUTLIER : -NUM_STD_DEV_DELAY_OUTLIER)
                    * noiseStdDev;
            updateNoiseEstimate(outlierResidualMs, nowMs);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "video jitter estimator: delay outlier rejected, residualMs={0}", residualMs);
            }
        }
        var estimate = jitterEstimateMs();
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "video jitter estimator: frameSizeBytes={0} frameDelayMs={1} jitterMs={2}",
                    frameSizeBytes, frameDelayMs, estimate);
        }
        return estimate;
    }

    /**
     * Returns the current jitter allowance, in milliseconds.
     *
     * <p>The modelled transmission delay for a frame of the running maximum size, plus a confidence
     * margin of one estimated noise standard deviation, floored at {@link #MIN_JITTER_MS}.
     *
     * @return the jitter estimate in milliseconds
     */
    public double jitterEstimateMs() {
        var transmissionMs = thetaState[0] * maxFrameSizeBytes + thetaState[1];
        var noiseMargin = Math.sqrt(Math.max(varNoiseMs, 0.0));
        var estimate = transmissionMs + noiseMargin;
        return Math.max(estimate, MIN_JITTER_MS);
    }

    /**
     * Bounds the estimate during a retransmission storm by capping the round trip time contribution the
     * controller may add for the given pending NACK count.
     *
     * <p>This is the seam the {@link VideoTimingController} consults so that a burst of retransmissions
     * does not let the round trip time term inflate the playout delay without bound; the estimator does
     * not itself add the round trip term but reports whether the NACK count is within the configured
     * limit.
     *
     * @param pendingNackCount the number of frames currently awaiting retransmission
     * @return {@code true} when the round trip time margin should still be applied, {@code false} when
     *         the NACK count exceeds the limit and the margin is suppressed
     */
    public boolean frameNackCount(int pendingNackCount) {
        return pendingNackCount <= NACK_LIMIT;
    }

    /**
     * The maximum number of outstanding retransmissions for which the round trip time margin is still
     * applied.
     *
     * <p>Beyond this count the controller stops adding the round trip term so a retransmission storm
     * does not inflate the playout delay.
     *
     * @implNote This implementation uses {@code 3}, the default NACK limit of the video timing
     * controller.
     */
    private static final int NACK_LIMIT = 3;

    /**
     * Returns the compressed size, in bytes, of the most recently inserted frame.
     *
     * @return the last frame size in bytes, or {@code 0} before any frame has been inserted
     */
    public long lastFrameSizeBytes() {
        return lastFrameSizeBytes;
    }

    /**
     * Clears the estimate, returning the filter to its construction state.
     *
     * <p>Reseeds the Kalman state to the nominal slope and base covariance, zeroes the frame size and
     * noise statistics, and marks the estimator unstarted so the next frame seeds rather than blends.
     */
    public void reset() {
        thetaState[0] = INITIAL_SLOPE;
        thetaState[1] = 0.0;
        thetaCov[0][0] = THETA_LOW;
        thetaCov[0][1] = 0.0;
        thetaCov[1][0] = 0.0;
        thetaCov[1][1] = THETA_LOW;
        avgFrameSizeBytes = 0.0;
        varFrameSizeBytes = 0.0;
        maxFrameSizeBytes = 0.0;
        avgNoiseMs = 0.0;
        varNoiseMs = 0.0;
        alphaCount = 1.0;
        lastFrameSizeBytes = 0;
        started = false;
        frameIntervalCount = 0;
        frameIntervalIndex = 0;
        frameIntervalSumMs = 0.0;
        lastFrameTimeMs = -1;
        frameSizeFilter.reset();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video jitter estimator: reset");
    }

    /**
     * Returns the modelled transmission delay for a frame size delta, in milliseconds.
     *
     * <p>The dot product of the Kalman state with the {@code (deltaFrameBytes, 1)} observation vector:
     * the rate slope times the size delta plus the fixed queuing term.
     *
     * @param deltaFrameBytes the frame size delta against the previous frame, in bytes
     * @return the modelled delay contribution in milliseconds
     */
    private double deviation(double deltaFrameBytes) {
        return thetaState[0] * deltaFrameBytes + thetaState[1];
    }

    /**
     * Returns whether a frame size deviates from the running average beyond the outlier threshold.
     *
     * @param frameSizeBytes the frame size to test, in bytes
     * @return {@code true} when the size exceeds the average by more than
     *         {@link #NUM_STD_DEV_SIZE_OUTLIER} standard deviations
     */
    private boolean isFrameSizeOutlier(long frameSizeBytes) {
        var stdDev = Math.sqrt(Math.max(varFrameSizeBytes, 0.0));
        return frameSizeBytes > avgFrameSizeBytes + NUM_STD_DEV_SIZE_OUTLIER * stdDev;
    }

    /**
     * Folds a non outlier frame size into the running frame size average and variance.
     *
     * @param frameSizeBytes the frame size to fold in, in bytes
     */
    private void updateFrameSizeStats(long frameSizeBytes) {
        var alpha = 1.0 - 0.05;
        var prevAvg = avgFrameSizeBytes;
        avgFrameSizeBytes = alpha * avgFrameSizeBytes + (1.0 - alpha) * frameSizeBytes;
        var deviation = frameSizeBytes - prevAvg;
        varFrameSizeBytes = alpha * varFrameSizeBytes + (1.0 - alpha) * deviation * deviation;
    }

    /**
     * Runs the Kalman prediction step, inflating the estimate covariance by the process noise.
     *
     * <p>Adds the diagonal process noise terms scaled by {@link #PHI} and {@link #PSI} so the estimate
     * is allowed to drift between observations.
     */
    private void kalmanPredict() {
        thetaCov[0][0] = PHI * thetaCov[0][0] + (1.0 - PHI) * THETA_LOW;
        thetaCov[1][1] = PSI * thetaCov[1][1] + (1.0 - PSI) * THETA_LOW;
    }

    /**
     * Runs the Kalman correction step against an observed (size delta, delay) pair.
     *
     * <p>Computes the innovation between the observed delay and the modelled delay, the Kalman gain from
     * the covariance and the observation vector against the measurement noise, then updates the state
     * and shrinks the covariance.
     *
     * @param deltaFrameBytes the frame size delta against the previous frame, in bytes
     * @param frameDelayMs    the observed relative delay, in milliseconds
     */
    private void kalmanCorrect(double deltaFrameBytes, double frameDelayMs) {
        var h0 = deltaFrameBytes;
        var h1 = 1.0;
        var measurementNoise = Math.max(varNoiseMs, 1.0);

        var covH0 = thetaCov[0][0] * h0 + thetaCov[0][1] * h1;
        var covH1 = thetaCov[1][0] * h0 + thetaCov[1][1] * h1;
        var innovationCov = h0 * covH0 + h1 * covH1 + measurementNoise;
        if (innovationCov <= 0.0) {
            return;
        }

        var gain0 = covH0 / innovationCov;
        var gain1 = covH1 / innovationCov;
        var innovation = frameDelayMs - deviation(deltaFrameBytes);
        thetaState[0] += gain0 * innovation;
        thetaState[1] += gain1 * innovation;

        var c00 = thetaCov[0][0];
        var c01 = thetaCov[0][1];
        var c10 = thetaCov[1][0];
        var c11 = thetaCov[1][1];
        thetaCov[0][0] = c00 - gain0 * covH0;
        thetaCov[0][1] = c01 - gain0 * covH1;
        thetaCov[1][0] = c10 - gain1 * covH0;
        thetaCov[1][1] = c11 - gain1 * covH1;
    }

    /**
     * Folds a delay residual into the running noise mean and variance, scaling the forgetting weight by
     * the observed frame rate.
     *
     * <p>Updates the inter frame interval estimate from the frame time, ramps the forgetting count up to
     * {@link #ALPHA_COUNT_MAX}, raises the base forgetting weight to the ratio of
     * {@link #NOISE_RATE_REFERENCE_FPS} to the observed frame rate (blended in over the first
     * {@link #FRAME_PROCESSING_STARTUP_COUNT} frames), then updates the exponential noise mean and
     * variance with that weight; the variance is floored at one squared millisecond. The residual is
     * folded in unclamped because the caller has already bounded the delay it derives from to
     * {@link #NUM_STD_DEV_DELAY_CLAMP} standard deviations.
     *
     * @implNote This implementation omits the {@code use_proportional_decay}, {@code enable_time_based_fps},
     * and {@code apply_var_fix} jitter estimator variants: all three default off in WhatsApp's production
     * configuration, so this unconditional path matches the shipped behaviour and the switches are not
     * modelled.
     * @param residualMs the delay residual, in milliseconds
     * @param nowMs      the current time in milliseconds, from a monotonic source
     */
    private void updateNoiseEstimate(double residualMs, long nowMs) {
        updateFrameRate(nowMs);
        if (alphaCount < ALPHA_COUNT_MAX) {
            alphaCount += 1.0;
        }
        var alpha = (alphaCount - 1.0) / alphaCount;
        var fps = frameRateFps();
        if (fps > 0.0) {
            var rateScale = NOISE_RATE_REFERENCE_FPS / fps;
            if (alphaCount < FRAME_PROCESSING_STARTUP_COUNT) {
                rateScale = (alphaCount * rateScale + (FRAME_PROCESSING_STARTUP_COUNT - alphaCount))
                        / FRAME_PROCESSING_STARTUP_COUNT;
            }
            alpha = Math.pow(alpha, rateScale);
        }
        var prevAvg = avgNoiseMs;
        avgNoiseMs = alpha * avgNoiseMs + (1.0 - alpha) * residualMs;
        var deviation = residualMs - prevAvg;
        varNoiseMs = alpha * varNoiseMs + (1.0 - alpha) * deviation * deviation;
        if (varNoiseMs < 1.0) {
            varNoiseMs = 1.0;
        }
    }

    /**
     * Folds the inter frame interval into the rolling frame rate window.
     *
     * <p>The first frame only records the frame time; each later frame appends the gap since the
     * previous frame into the rolling window, overwriting the oldest interval once the window is full.
     *
     * @param nowMs the current frame time in milliseconds, from a monotonic source
     */
    private void updateFrameRate(long nowMs) {
        if (lastFrameTimeMs >= 0) {
            var interval = (double) (nowMs - lastFrameTimeMs);
            if (frameIntervalCount < frameIntervalsMs.length) {
                frameIntervalSumMs += interval;
                frameIntervalCount++;
            } else {
                frameIntervalSumMs += interval - frameIntervalsMs[frameIntervalIndex];
            }
            frameIntervalsMs[frameIntervalIndex] = interval;
            frameIntervalIndex = (frameIntervalIndex + 1) % frameIntervalsMs.length;
        }
        lastFrameTimeMs = nowMs;
    }

    /**
     * Returns the observed frame rate in hertz, the reciprocal of the mean inter frame interval over the
     * rolling window.
     *
     * @return the frame rate in hertz, bounded above by {@link #MAX_FRAME_RATE_ESTIMATE_FPS}, or
     *         {@code 0} before two frames have been timed or when the mean interval is not positive
     */
    private double frameRateFps() {
        if (frameIntervalCount == 0) {
            return 0.0;
        }
        var meanFramePeriodMs = frameIntervalSumMs / frameIntervalCount;
        if (meanFramePeriodMs <= 0.0) {
            return 0.0;
        }
        return Math.min(1000.0 / meanFramePeriodMs, MAX_FRAME_RATE_ESTIMATE_FPS);
    }
}
