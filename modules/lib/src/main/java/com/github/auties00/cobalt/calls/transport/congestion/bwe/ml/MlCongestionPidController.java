package com.github.auties00.cobalt.calls.transport.congestion.bwe.ml;

import com.github.auties00.cobalt.calls.transport.congestion.bwe.PidController;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Translates a machine learning congestion signal into a multiplicative rate adjustment using an
 * anti windup proportional integral derivative controller.
 *
 * <p>The congestion model emits a scalar congestion level; this controller drives that level toward a
 * target (typically zero, meaning no congestion) through a {@link PidController} and converts the
 * controller output into a rate scaling factor the sender estimator multiplies into its target. A
 * positive congestion level produces a factor below one to back the rate off; a level at or below the
 * target leaves the rate unchanged. The factor is clamped to {@code [minFactor, 1.0]} so the
 * controller can only reduce, never inflate, the rate.
 *
 * <p>Instances are not thread safe; the owning sender estimator drives one controller from the single
 * transport thread.
 *
 * @implNote This implementation wraps the generic {@link PidController} to map the congestion model
 * output into a rate adjustment. The gains and the minimum factor are not carried by the server pushed
 * voip settings, so they are supplied by the caller through the constructor.
 */
public final class MlCongestionPidController {
    /**
     * The logger for {@link MlCongestionPidController}.
     */
    private static final System.Logger LOGGER = Log.get(MlCongestionPidController.class);

    /**
     * The underlying anti windup controller driving the congestion level toward the target.
     */
    private final PidController pid;

    /**
     * Minimum rate scaling factor the controller may produce.
     *
     * <p>Bounds how far one adjustment can back the rate off, clamping the factor's lower end. The
     * value passed to the constructor is clamped into {@code [0.0, 1.0]}.
     */
    private final double minFactor;

    /**
     * Most recently computed rate scaling factor, initialized to {@code 1.0} before the first
     * computation.
     */
    private double lastFactor = 1.0;

    /**
     * Constructs a controller with the given gains, integral bounds, congestion target, and minimum
     * factor.
     *
     * <p>The minimum factor is clamped into {@code [0.0, 1.0]} before it is stored.
     *
     * @param kp          the proportional gain
     * @param ki          the integral gain
     * @param kd          the derivative gain
     * @param integralMin the lower bound on the integral accumulator for anti windup
     * @param integralMax the upper bound on the integral accumulator for anti windup
     * @param targetLevel the congestion level the controller drives the measurement toward
     * @param minFactor   the minimum rate scaling factor, in {@code (0, 1]}
     */
    public MlCongestionPidController(double kp, double ki, double kd, double integralMin,
                                    double integralMax, double targetLevel, double minFactor) {
        this.pid = new PidController(kp, ki, kd, integralMin, integralMax, targetLevel);
        this.minFactor = Math.clamp(minFactor, 0.0, 1.0);
    }

    /**
     * Computes the rate scaling factor for a machine learning congestion level.
     *
     * <p>Runs the congestion level through the controller and maps the output into a factor at or below
     * one: the more the level exceeds the target, the lower the factor, clamped to
     * {@code [minFactor, 1.0]}. The factor is stored and returned.
     *
     * @param congestionLevel the machine learning congestion level for this round
     * @return the rate scaling factor, in {@code [minFactor, 1.0]}
     */
    public double computeFactor(double congestionLevel) {
        var output = pid.compute(congestionLevel);
        lastFactor = Math.clamp(1.0 + output, minFactor, 1.0);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "ml congestion factor computed: level={0} output={1} factor={2}",
                    congestionLevel, output, lastFactor);
        }
        return lastFactor;
    }

    /**
     * Returns the most recently computed rate scaling factor.
     *
     * @return the last factor, in {@code [minFactor, 1.0]}, or {@code 1.0} before the first computation
     */
    public double lastFactor() {
        return lastFactor;
    }

    /**
     * Resets the controller's carried state and the last factor.
     *
     * <p>Clears the underlying {@link PidController} accumulator and restores the last factor to
     * {@code 1.0}, so the next computation starts from an unbiased state.
     */
    public void reset() {
        pid.reset();
        lastFactor = 1.0;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ml congestion pid controller reset");
        }
    }
}
