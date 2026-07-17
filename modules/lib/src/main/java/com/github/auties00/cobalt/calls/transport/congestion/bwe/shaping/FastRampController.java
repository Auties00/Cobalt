package com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping;

import com.github.auties00.cobalt.calls.util.RttEstimator;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gates an additive bitrate ramp extender on link health, keeping the ramp active while round trip time
 * and loss stay low and stopping it the moment congestion appears.
 *
 * <p>Each received packet's round trip time and the running loss ratio are reported through
 * {@link #onRxRtp(double, double, long)}. The controller smooths the round trip time into an
 * exponential moving average and a slowly decaying minimum, fits a least squares slope over a sliding
 * window of recent round trip samples, and declares congestion when the smoothed round trip time rises
 * above the minimum by {@link #aboveMinThreshold}, the slope exceeds {@link #slopeThresholdMsPerS}, or
 * the loss ratio crosses {@link #lossThreshold}. While not congested the ramp extender stays active so
 * the sender estimator may keep ramping; the first congestion signal deactivates it.
 *
 * <p>Instances are not thread safe; the owning sender estimator drives one controller from the single
 * transport thread.
 *
 * @implNote This implementation reuses {@link RttEstimator} for the round trip exponential moving
 * average and its decaying minimum, and fits the sliding window slope by ordinary least squares over the
 * bounded {@link #slopeWindow}. The congestion thresholds are not carried by the negotiated voip
 * settings, so they are supplied by the caller through the constructor.
 */
public final class FastRampController {
    /**
     * The logger for {@link FastRampController}.
     */
    private static final System.Logger LOGGER = Log.get(FastRampController.class);

    /**
     * Smoothing factor for the round trip time exponential moving average.
     */
    static final double RTT_EMA_ALPHA = 0.1;

    /**
     * Number of round trip samples the slope regression operates over.
     */
    static final int SLOPE_WINDOW = 20;

    /**
     * Minimum number of samples required before the slope gate is evaluated.
     */
    static final int SLOPE_MIN_POINTS = 5;

    /**
     * Smooths and tracks the minimum of the round trip time.
     */
    private final RttEstimator rttEstimator = new RttEstimator();

    /**
     * Sliding window of (time, round trip) samples used by the slope regression.
     *
     * <p>Bounded to {@link #SLOPE_WINDOW} entries; the oldest is evicted before a new one is appended.
     */
    private final Deque<RttSample> slopeWindow = new ArrayDeque<>(SLOPE_WINDOW);

    /**
     * Multiple of the minimum round trip time above which the smoothed round trip time signals
     * congestion.
     */
    private final double aboveMinThreshold;

    /**
     * Round trip slope, in milliseconds per second, above which the link signals congestion.
     */
    private final double slopeThresholdMsPerS;

    /**
     * Loss ratio, in {@code [0, 1]}, at or above which the link signals congestion.
     */
    private final double lossThreshold;

    /**
     * Whether the additive ramp extender is currently active.
     */
    private boolean rampActive = true;

    /**
     * Holds one round trip sample for the slope regression.
     *
     * @param tMs    the sample time in milliseconds
     * @param rttMs  the round trip time in milliseconds
     */
    private record RttSample(double tMs, double rttMs) {
    }

    /**
     * Constructs a fast ramp controller with the congestion thresholds.
     *
     * <p>The loss threshold is clamped into {@code [0, 1]}; the other thresholds are stored as given.
     *
     * @param aboveMinThreshold    the multiple of the minimum round trip time gating congestion
     * @param slopeThresholdMsPerS the round trip slope, in milliseconds per second, gating congestion
     * @param lossThreshold        the loss ratio, in {@code [0, 1]}, gating congestion
     */
    public FastRampController(double aboveMinThreshold, double slopeThresholdMsPerS, double lossThreshold) {
        this.aboveMinThreshold = aboveMinThreshold;
        this.slopeThresholdMsPerS = slopeThresholdMsPerS;
        this.lossThreshold = Math.clamp(lossThreshold, 0.0, 1.0);
    }

    /**
     * Reports one received packet's round trip time and the running loss ratio, updating the ramp gate.
     *
     * <p>Blends the round trip time into the smoothed estimate and the minimum, appends a slope sample,
     * then deactivates the ramp extender when the smoothed round trip time exceeds the minimum by
     * {@link #aboveMinThreshold}, the windowed slope exceeds {@link #slopeThresholdMsPerS} with at least
     * {@link #SLOPE_MIN_POINTS} samples, or the loss ratio reaches {@link #lossThreshold}. A non positive
     * round trip time updates neither the smoothed estimate nor the slope window, but the loss gate is
     * still evaluated.
     *
     * @param rttMs     the packet's round trip time, in milliseconds; ignored when non positive
     * @param lossRatio the running loss ratio, in {@code [0, 1]}
     * @param nowMs     the monotonic timestamp, in milliseconds, of this packet
     * @return {@code true} when the ramp extender remains active after this packet
     */
    public boolean onRxRtp(double rttMs, double lossRatio, long nowMs) {
        if (rttMs > 0) {
            rttEstimator.update(rttMs, RTT_EMA_ALPHA);
            rttEstimator.updateMin(rttMs, RTT_EMA_ALPHA);
            if (slopeWindow.size() == SLOPE_WINDOW) {
                slopeWindow.removeFirst();
            }
            slopeWindow.addLast(new RttSample(nowMs, rttMs));
        }
        if (rampActive && isCongested(lossRatio)) {
            rampActive = false;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "fast ramp deactivated: rtt={0}ms loss={1} smoothedRtt={2}ms",
                        rttMs, lossRatio, rttEstimator.estimate());
            }
        }
        return rampActive;
    }

    /**
     * Decides whether the link is congested for this packet.
     *
     * <p>The link is congested when the loss ratio is at or above {@link #lossThreshold}, the smoothed
     * round trip time exceeds the minimum by {@link #aboveMinThreshold}, or the windowed slope exceeds
     * {@link #slopeThresholdMsPerS} once at least {@link #SLOPE_MIN_POINTS} samples are present.
     *
     * @param lossRatio the running loss ratio, in {@code [0, 1]}
     * @return {@code true} when the link is judged congested
     */
    private boolean isCongested(double lossRatio) {
        if (lossRatio >= lossThreshold) {
            return true;
        }
        var min = rttEstimator.minEstimate();
        var ema = rttEstimator.estimate();
        if (min > 0 && ema > min * aboveMinThreshold) {
            return true;
        }
        return slopeWindow.size() >= SLOPE_MIN_POINTS && slope() > slopeThresholdMsPerS;
    }

    /**
     * Computes the least squares slope of the round trip window in milliseconds per second.
     *
     * <p>Returns {@code 0.0} when the window is empty or its time variance is zero. The per millisecond
     * slope is scaled to per second to compare against {@link #slopeThresholdMsPerS}.
     *
     * @return the round trip slope, in milliseconds per second
     */
    private double slope() {
        var sumT = 0.0;
        var sumR = 0.0;
        var n = 0;
        for (var s : slopeWindow) {
            sumT += s.tMs;
            sumR += s.rttMs;
            n++;
        }
        if (n == 0) {
            return 0.0;
        }
        var avgT = sumT / n;
        var avgR = sumR / n;
        var num = 0.0;
        var den = 0.0;
        for (var s : slopeWindow) {
            var dt = s.tMs - avgT;
            num += dt * (s.rttMs - avgR);
            den += dt * dt;
        }
        return den == 0.0 ? 0.0 : num / den * 1000.0;
    }

    /**
     * Returns whether the additive ramp extender is currently active.
     *
     * @return {@code true} while no congestion has been signalled since the last reset
     */
    public boolean isRampActive() {
        return rampActive;
    }

    /**
     * Returns the smoothed round trip time estimate.
     *
     * @return the smoothed round trip time, in milliseconds, or {@code 0} before the first sample
     */
    public long smoothedRttMs() {
        return rttEstimator.estimate();
    }

    /**
     * Re arms the ramp extender and clears the slope window.
     *
     * <p>Restores the ramp to active for a new ramp attempt; the round trip estimator retains its
     * smoothed and minimum values so the gate stays calibrated.
     */
    public void reset() {
        rampActive = true;
        slopeWindow.clear();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "fast ramp controller reset");
        }
    }
}
