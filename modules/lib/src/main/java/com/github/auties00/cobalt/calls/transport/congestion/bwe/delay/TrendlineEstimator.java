package com.github.auties00.cobalt.calls.transport.congestion.bwe.delay;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Estimates delay based congestion by fitting a least squares trendline over a window of smoothed
 * inter arrival delay samples and applying an adaptive overuse detector.
 *
 * <p>Each call to {@link #update(double, double, int, long)} receives one packet group's send time
 * spacing and arrival time spacing relative to the previous group; their difference is the one way
 * delay growth, the instantaneous fill direction of the bottleneck queue. The growth is accumulated,
 * exponentially smoothed by {@link #SMOOTHING_COEFFICIENT}, and pushed into a sliding window of
 * {@link #WINDOW_SIZE} samples. Once the window fills, a least squares line is fit across it and its
 * slope, scaled by the window size and {@link #THRESHOLD_GAIN}, becomes the modified trend the
 * detector compares against the adaptive threshold. A modified trend above the threshold for at least
 * {@link #OVERUSING_TIME_MS} across more than one consecutive excursion yields
 * {@link BandwidthUsage#OVERUSING}; below the negated threshold yields {@link BandwidthUsage#UNDERUSING};
 * otherwise {@link BandwidthUsage#NORMAL}. The threshold adapts on each call, rising with gain
 * {@link #K_UP} while overusing to suppress repeat triggers and falling with gain {@link #K_DOWN}
 * otherwise to recover sensitivity, clamped to {@code [MIN_THRESHOLD, MAX_THRESHOLD]}.
 *
 * <p>Instances are not thread safe; the owning {@link GccDelayBasedEstimator} drives one estimator
 * from the single call transport thread.
 *
 * @implNote This implementation uses the WebRTC delay based estimator tuning: a window of
 * {@link #WINDOW_SIZE} samples, up gain {@link #K_UP}, down gain {@link #K_DOWN}, an overuse time
 * threshold of {@link #OVERUSING_TIME_MS}, and a smoothing coefficient of {@link #SMOOTHING_COEFFICIENT}.
 * These are the upstream WebRTC defaults: the server pushed voip settings enable adaptive threshold
 * adaption and the delay based bitrate estimator but carry no override for the trendline constants, so
 * the defaults apply.
 */
public final class TrendlineEstimator {
    /**
     * The logger for {@link TrendlineEstimator}.
     */
    private static final System.Logger LOGGER = Log.get(TrendlineEstimator.class);

    /**
     * Number of samples the least squares regression operates over.
     *
     * <p>The sliding window retains this many (smoothed time, smoothed delay) points, evicting the
     * oldest once full.
     */
    static final int WINDOW_SIZE = 20;

    /**
     * Exponential smoothing coefficient applied to the running accumulated delay sum.
     *
     * <p>Each update blends this fraction of the previous smoothed delay with the complementary
     * fraction of the freshly accumulated delay.
     */
    static final double SMOOTHING_COEFFICIENT = 0.9;

    /**
     * Multiplier on {@code slope * windowSize} that produces the modified trend the detector compares
     * against the threshold.
     */
    static final double THRESHOLD_GAIN = 4.0;

    /**
     * Initial value of the adaptive threshold.
     */
    static final double INITIAL_THRESHOLD = 12.5;

    /**
     * Lower bound on the adaptive threshold.
     */
    static final double MIN_THRESHOLD = 6.0;

    /**
     * Upper bound on the adaptive threshold.
     */
    static final double MAX_THRESHOLD = 600.0;

    /**
     * Up adapt gain applied to the threshold while the modified trend magnitude is at or above it.
     *
     * <p>Raising the threshold while overusing suppresses repeated {@link BandwidthUsage#OVERUSING}
     * triggers from one sustained excursion.
     */
    static final double K_UP = 0.0087;

    /**
     * Down adapt gain applied to the threshold while the modified trend magnitude is below it.
     *
     * <p>Lowering the threshold when the link is not overusing recovers detector sensitivity.
     */
    static final double K_DOWN = 0.039;

    /**
     * Minimum continuous duration, in milliseconds, the modified trend must exceed the threshold
     * before {@link BandwidthUsage#OVERUSING} fires.
     *
     * <p>This filters out single spike excursions so they do not trigger an overuse classification.
     */
    static final double OVERUSING_TIME_MS = 10.0;

    /**
     * Cap, in milliseconds, on the inter update time delta fed into the threshold adapt step.
     *
     * <p>Capping the time delta guards against a pathologically large gap between updates making the
     * threshold jump.
     */
    static final double MAX_TIME_DELTA_MS = 100.0;

    /**
     * Guard margin above the threshold beyond which the adapt step is skipped.
     *
     * <p>When the modified trend magnitude overshoots the threshold by more than this margin the
     * threshold is left unchanged so a large transient does not drag it upward.
     */
    static final double ADAPT_SKIP_MARGIN = 15.0;

    /**
     * Sliding window of samples used by the least squares regression.
     *
     * <p>Bounded to {@link #WINDOW_SIZE} entries; once full the oldest sample is evicted from the
     * front before a new one is appended to the back.
     */
    private final Deque<Sample> window = new ArrayDeque<>(WINDOW_SIZE);

    /**
     * Running exponentially smoothed sum of per group delay deltas, in milliseconds.
     */
    private double smoothedDelayMs = 0.0;

    /**
     * Running unsmoothed sum of per group delay deltas, in milliseconds.
     *
     * <p>This is the input to the exponential moving average that produces {@link #smoothedDelayMs}.
     */
    private double accumulatedDelayMs = 0.0;

    /**
     * Reference timestamp, in milliseconds, of the first packet group seen, or {@code -1} when none
     * has been seen.
     *
     * <p>Sample timestamps are stored relative to this value so the regression operates on
     * small magnitude doubles.
     */
    private long firstArrivalMs = -1;

    /**
     * Most recently computed slope, in delay milliseconds per smoothed time millisecond.
     */
    private double trend = 0.0;

    /**
     * Current adaptive threshold.
     */
    private double threshold = INITIAL_THRESHOLD;

    /**
     * Cumulative time, in milliseconds, the detector has spent in overuse candidate territory.
     *
     * <p>Reset whenever the modified trend leaves the over threshold band; compared against
     * {@link #OVERUSING_TIME_MS} to gate the overuse classification.
     */
    private double overusingTimeMs = 0.0;

    /**
     * Count of consecutive updates in which the modified trend exceeded the threshold.
     *
     * <p>Requiring more than one consecutive excursion enforces monotonic delay growth rather than a
     * single isolated overshoot.
     */
    private int overuseCounter = 0;

    /**
     * Modified trend from the previous {@link #update(double, double, int, long)} call.
     *
     * <p>The overuse confirmation requires the current modified trend to be at least this value, so a
     * trend that has begun to reverse (a current modified trend below the previous one) is filtered out
     * and does not declare an overuse. Initialized to {@code 0.0} so the first excursion is not blocked
     * by a stale prior.
     */
    private double prevModifiedTrend = 0.0;

    /**
     * Most recent classification returned by {@link #update(double, double, int, long)} and exposed
     * via {@link #state()}.
     */
    private BandwidthUsage state = BandwidthUsage.NORMAL;

    /**
     * Timestamp, in milliseconds, of the last {@link #update(double, double, int, long)} call, or
     * {@code -1} when none has occurred.
     *
     * <p>The difference between successive timestamps supplies the time delta used by the threshold
     * adapt step.
     */
    private long lastUpdateMs = -1;

    /**
     * Holds one sample point fed to the least squares regression.
     *
     * @param tMs   the smoothed time in milliseconds, relative to the first packet group
     * @param delay the smoothed accumulated delay in milliseconds
     */
    private record Sample(double tMs, double delay) {
    }

    /**
     * Constructs a fresh estimator with the canonical WebRTC tuning.
     */
    public TrendlineEstimator() {
    }

    /**
     * Feeds one packet group inter arrival measurement, updates the trendline, adapts the threshold,
     * and returns the new classification.
     *
     * <p>The measurement is taken over a packet group in the WebRTC sense: packets sent within a short
     * send time burst are coalesced by the caller, so this method receives one tuple of send delta,
     * arrival delta, and payload per group. It accumulates the one way delay growth, updates the
     * smoothed delay, appends a window sample, refits the slope once the window is full, then
     * classifies and adapts the threshold before returning the resulting {@link BandwidthUsage}.
     *
     * @param sendDeltaMs    the send time difference between this group and the previous one
     * @param arrivalDeltaMs the arrival time difference between this group and the previous one
     * @param payloadBytes   the total payload bytes in this group; carried for parity with the WebRTC
     *                       API but unused by the slope only estimator
     * @param nowMs          the monotonic timestamp, in milliseconds, at which this update is processed
     * @return the classification for this packet group
     */
    @SuppressWarnings("unused")
    public BandwidthUsage update(double sendDeltaMs, double arrivalDeltaMs, int payloadBytes, long nowMs) {
        var previousState = state;
        var oneWayDelayDelta = arrivalDeltaMs - sendDeltaMs;
        accumulatedDelayMs += oneWayDelayDelta;
        smoothedDelayMs = SMOOTHING_COEFFICIENT * smoothedDelayMs
                + (1.0 - SMOOTHING_COEFFICIENT) * accumulatedDelayMs;

        if (firstArrivalMs < 0) {
            firstArrivalMs = nowMs;
        }
        var relativeTimeMs = (double) (nowMs - firstArrivalMs);
        if (window.size() == WINDOW_SIZE) {
            window.removeFirst();
        }
        window.addLast(new Sample(relativeTimeMs, smoothedDelayMs));

        if (window.size() == WINDOW_SIZE) {
            trend = leastSquaresSlope(window);
        }

        var modifiedTrend = trend * Math.min(window.size(), WINDOW_SIZE) * THRESHOLD_GAIN;
        var dtMs = lastUpdateMs < 0 ? 0.0 : (double) (nowMs - lastUpdateMs);
        lastUpdateMs = nowMs;
        classify(modifiedTrend, dtMs);
        adaptThreshold(modifiedTrend, dtMs);
        prevModifiedTrend = modifiedTrend;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "trendline estimator: trend={0} modifiedTrend={1} threshold={2} state={3}",
                    trend, modifiedTrend, threshold, state);
        }
        if (Log.DEBUG && state != previousState) {
            LOGGER.log(Level.DEBUG, "trendline estimator: bandwidth usage {0} -> {1}", previousState, state);
        }
        return state;
    }

    /**
     * Classifies the link as overusing, underusing, or normal and updates {@link #state}.
     *
     * <p>A modified trend above {@link #threshold} accrues {@link #overusingTimeMs} (capped per step
     * at {@link #MAX_TIME_DELTA_MS}) and increments {@link #overuseCounter}; an overuse is declared
     * only once the accrued time reaches {@link #OVERUSING_TIME_MS} across more than one consecutive
     * excursion and the trend has not just reversed, which is the duration filter that prevents
     * single spike noise from triggering it. The reversal check compares the current modified trend
     * against {@link #prevModifiedTrend}, the value from the prior call, so a current trend below the
     * previous one (a reversing trend) is filtered out. A modified trend below the negated threshold
     * yields under use, anything in between yields normal, and both reset the overuse accumulators.
     *
     * @param modifiedTrend the slope scaled by the window size and {@link #THRESHOLD_GAIN}
     * @param dtMs          the time since the previous update, in milliseconds
     */
    private void classify(double modifiedTrend, double dtMs) {
        if (modifiedTrend > threshold) {
            overusingTimeMs += Math.min(dtMs, MAX_TIME_DELTA_MS);
            overuseCounter++;
            if (overusingTimeMs >= OVERUSING_TIME_MS && overuseCounter > 1) {
                if (modifiedTrend >= prevModifiedTrend) {
                    overusingTimeMs = 0.0;
                    overuseCounter = 0;
                    state = BandwidthUsage.OVERUSING;
                }
            }
        } else if (modifiedTrend < -threshold) {
            overusingTimeMs = 0.0;
            overuseCounter = 0;
            state = BandwidthUsage.UNDERUSING;
        } else {
            overusingTimeMs = 0.0;
            overuseCounter = 0;
            state = BandwidthUsage.NORMAL;
        }
    }

    /**
     * Adapts {@link #threshold} toward the magnitude of the modified trend.
     *
     * <p>The threshold moves with gain {@link #K_UP} when the trend magnitude is at or above it,
     * suppressing repeat triggers, and with gain {@link #K_DOWN} when below, recovering sensitivity.
     * The step is scaled by the time delta since the previous adapt step, capped at
     * {@link #MAX_TIME_DELTA_MS}, and the result is clamped to {@code [MIN_THRESHOLD, MAX_THRESHOLD]}.
     * When the trend magnitude overshoots the threshold by more than {@link #ADAPT_SKIP_MARGIN} the
     * adapt step is skipped so a large transient does not drag the threshold upward.
     *
     * @param modifiedTrend the current modified trend
     * @param dtMs          the time since the previous update, in milliseconds
     */
    private void adaptThreshold(double modifiedTrend, double dtMs) {
        var absTrend = Math.abs(modifiedTrend);
        if (absTrend > threshold + ADAPT_SKIP_MARGIN) {
            return;
        }
        var k = absTrend < threshold ? K_DOWN : K_UP;
        var clampedDt = Math.min(dtMs, MAX_TIME_DELTA_MS);
        threshold += k * (absTrend - threshold) * clampedDt;
        if (threshold < MIN_THRESHOLD) {
            threshold = MIN_THRESHOLD;
        } else if (threshold > MAX_THRESHOLD) {
            threshold = MAX_THRESHOLD;
        }
    }

    /**
     * Returns the most recent classification.
     *
     * <p>Equals the value returned by the last {@link #update(double, double, int, long)} call, or
     * {@link BandwidthUsage#NORMAL} before any update.
     *
     * @return the current state
     */
    public BandwidthUsage state() {
        return state;
    }

    /**
     * Returns the latest computed slope in delay milliseconds per smoothed time millisecond.
     *
     * <p>The slope is {@code 0.0} until the window has filled to {@link #WINDOW_SIZE} samples.
     *
     * @return the trend slope
     */
    public double trendSlope() {
        return trend;
    }

    /**
     * Returns the current adaptive threshold.
     *
     * <p>Primarily useful for diagnostics, since the threshold is maintained internally by the adapt
     * step.
     *
     * @return the current threshold value
     */
    public double threshold() {
        return threshold;
    }

    /**
     * Resets the estimator to its just constructed state.
     *
     * <p>Clears the sample window, the smoothed and accumulated delays, the overuse accumulators, and
     * the timestamps, and restores the threshold to {@link #INITIAL_THRESHOLD}. Called when the owning
     * estimator restarts for a new connection.
     */
    public void reset() {
        window.clear();
        smoothedDelayMs = 0.0;
        accumulatedDelayMs = 0.0;
        firstArrivalMs = -1;
        trend = 0.0;
        threshold = INITIAL_THRESHOLD;
        overusingTimeMs = 0.0;
        overuseCounter = 0;
        prevModifiedTrend = 0.0;
        state = BandwidthUsage.NORMAL;
        lastUpdateMs = -1;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "trendline estimator: reset");
    }

    /**
     * Computes the slope of the least squares fit line through a set of samples.
     *
     * <p>Returns {@code 0.0} when the sample set is empty or when the variance in time is zero, that
     * is when every sample shares the same instant; the latter does not occur in practice because
     * successive groups carry distinct monotonic timestamps.
     *
     * @param samples the sample window
     * @return the slope, or {@code 0.0} when it is undefined
     */
    private static double leastSquaresSlope(Iterable<Sample> samples) {
        var sumT = 0.0;
        var sumD = 0.0;
        var n = 0;
        for (var s : samples) {
            sumT += s.tMs;
            sumD += s.delay;
            n++;
        }
        if (n == 0) {
            return 0.0;
        }
        var avgT = sumT / n;
        var avgD = sumD / n;
        var num = 0.0;
        var den = 0.0;
        for (var s : samples) {
            var dt = s.tMs - avgT;
            num += dt * (s.delay - avgD);
            den += dt * dt;
        }
        return den == 0.0 ? 0.0 : num / den;
    }
}
