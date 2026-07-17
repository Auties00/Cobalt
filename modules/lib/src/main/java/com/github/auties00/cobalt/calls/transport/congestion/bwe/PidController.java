package com.github.auties00.cobalt.calls.transport.congestion.bwe;

/**
 * A generic PID (proportional, integral, derivative) controller with clamping on the integral term
 * to prevent integral windup.
 *
 * <p>Each {@link #compute(double)} reads a measurement, forms the error as setpoint minus measurement,
 * accumulates the error into the integral while clamping it to {@code [integralMin, integralMax]} so it
 * cannot grow without bound, and returns
 * {@code Kd * (error - previousError) + Kp * error + Ki * integral}. The previous error and the clamped
 * integral are carried to the next call. {@link #reset()} zeroes the carried state without changing the
 * gains or the setpoint.
 *
 * <p>Instances are not thread safe; the owning controller drives one instance from the single transport
 * thread.
 *
 * @implNote This implementation carries the setpoint, previous error, and clamped integral as mutable
 * fields, and applies explicit lower and upper integral bounds so the accumulated error cannot grow
 * without bound while the measurement stays on one side of the setpoint.
 */
public final class PidController {
    /**
     * Proportional gain applied to the current error.
     */
    private final double kp;

    /**
     * Integral gain applied to the clamped accumulated error.
     */
    private final double ki;

    /**
     * Derivative gain applied to the change in error since the previous compute.
     */
    private final double kd;

    /**
     * Lower bound on the integral accumulator that prevents integral windup.
     */
    private final double integralMin;

    /**
     * Upper bound on the integral accumulator that prevents integral windup.
     */
    private final double integralMax;

    /**
     * Target value the measurement is driven toward.
     */
    private double setpoint;

    /**
     * Error from the previous compute, used by the derivative term.
     */
    private double previousError = 0.0;

    /**
     * Clamped accumulated error.
     */
    private double integral = 0.0;

    /**
     * Constructs a controller with the given gains, integral bounds, and setpoint.
     *
     * @param kp          the proportional gain
     * @param ki          the integral gain
     * @param kd          the derivative gain
     * @param integralMin the lower bound on the integral accumulator
     * @param integralMax the upper bound on the integral accumulator
     * @param setpoint    the target value the measurement is driven toward
     */
    public PidController(double kp, double ki, double kd, double integralMin, double integralMax,
                         double setpoint) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.integralMin = integralMin;
        this.integralMax = integralMax;
        this.setpoint = setpoint;
    }

    /**
     * Computes one controller output from a measurement.
     *
     * <p>Forms the error as setpoint minus measurement, clamps the running integral after adding the
     * error, and returns the sum of the derivative, proportional, and integral terms. Carries the error
     * and the clamped integral to the next call.
     *
     * @param measurement the current measured value
     * @return the controller output
     */
    public double compute(double measurement) {
        var error = setpoint - measurement;
        integral = Math.clamp(integral + error, integralMin, integralMax);
        var output = kd * (error - previousError) + kp * error + ki * integral;
        previousError = error;
        return output;
    }

    /**
     * Updates the target value the measurement is driven toward.
     *
     * @param setpoint the new setpoint
     */
    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    /**
     * Returns the target value the measurement is driven toward.
     *
     * @return the setpoint
     */
    public double setpoint() {
        return setpoint;
    }

    /**
     * Resets the carried state, zeroing the previous error and the integral accumulator.
     *
     * <p>Leaves the gains and the setpoint unchanged.
     */
    public void reset() {
        previousError = 0.0;
        integral = 0.0;
    }
}
