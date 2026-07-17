package com.github.auties00.cobalt.calls.util;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Smooths a stream of round trip time samples into a single exponential moving
 * average and tracks a slowly decaying minimum, the path round trip time floor.
 *
 * <p>The estimator holds two independent values updated by separate feed methods.
 * {@link #update(double, double)} maintains the smoothed estimate: the first valid
 * sample seeds the estimate directly, and every later sample blends in with a
 * caller supplied smoothing factor. {@link #updateMin(double, double)} maintains the
 * minimum estimate, which decays toward a new lower sample through the same exponential blend
 * but snaps immediately up to any sample that is not strictly below the current
 * minimum, so the floor follows genuine improvements gradually yet abandons a stale
 * low promptly. Both methods ignore nonpositive samples, treating them as missing
 * measurements rather than data points. Every stored value is clamped at zero and
 * truncated toward zero to a whole number of the sample's unit, typically
 * milliseconds.
 *
 * <p>An estimate of {@code 0} denotes the uninitialized state: until the first valid
 * sample arrives, {@link #estimate()} and {@link #minEstimate()} both report
 * {@code 0}, and the first valid sample seeds rather than blends. Instances are not
 * thread safe; the owning bandwidth estimation or transport path drives a single
 * estimator from one thread.
 *
 * @implNote This implementation holds each stored value as a Java {@code long} rather
 * than the unsigned 32 bit integer of the reference floating point pipeline, because
 * round trip times never approach that range. The exponential blend
 * {@code (1 - alpha) * previous + alpha * sample} is computed in {@code double} and
 * only then clamped at zero, truncated toward zero, and stored, so the whole number
 * estimate carries no accumulated fractional drift. The strict {@code minimum - 1}
 * guard in {@link #updateMin(double, double)} decays the floor only when a sample
 * beats the current minimum by more than one whole unit, which suppresses jitter
 * around the floor while still tracking a genuine improvement.
 */
public final class RttEstimator {
    /**
     * The logger for {@link RttEstimator}.
     */
    private static final System.Logger LOGGER = Log.get(RttEstimator.class);

    /**
     * Lower bound applied to the smoothing factor before it is used.
     *
     * <p>A factor at or below this value makes {@link #update(double, double)} ignore
     * the new sample entirely and retain the previous estimate.
     */
    private static final double ALPHA_LOW = 0.0;

    /**
     * Upper bound applied to the smoothing factor before it is used.
     *
     * <p>A factor at or above this value makes {@link #update(double, double)} adopt
     * the new sample entirely and discard the previous estimate.
     */
    private static final double ALPHA_HIGH = 1.0;

    /**
     * Current smoothed round trip time estimate, or {@code 0} when uninitialized.
     *
     * <p>Held as a nonnegative whole number in the unit of the supplied samples;
     * seeded by the first valid sample and blended on each later sample.
     */
    private long estimate;

    /**
     * Current minimum round trip time estimate, or {@code 0} when uninitialized.
     *
     * <p>Held as a nonnegative whole number in the unit of the supplied samples;
     * decays downward through the exponential blend and snaps upward to any sample not
     * strictly below it.
     */
    private long minEstimate;

    /**
     * Constructs an uninitialized estimator whose estimate and minimum are both
     * {@code 0}.
     *
     * <p>The first valid sample fed to {@link #update(double, double)} or
     * {@link #updateMin(double, double)} seeds the corresponding value.
     */
    public RttEstimator() {
        this.estimate = 0;
        this.minEstimate = 0;
    }

    /**
     * Blends a round trip time sample into the smoothed estimate and returns the
     * updated value.
     *
     * <p>Ignores a sample that is not strictly positive and returns the unchanged
     * estimate. Otherwise clamps {@code alpha} into {@code [0, 1]}; if the current
     * estimate is {@code 0} the sample seeds the estimate directly, else the estimate
     * becomes {@code (1 - alpha) * estimate + alpha * sample}. The result is clamped at
     * zero, truncated toward zero, and stored.
     *
     * @param sample the round trip time measurement in its native unit; ignored when
     *               not strictly positive
     * @param alpha  the smoothing factor weighting the new sample, clamped into
     *               {@code [0, 1]} where higher values track the sample more closely
     * @return the updated smoothed estimate after applying the sample
     */
    public long update(double sample, double alpha) {
        if (sample <= 0) {
            return estimate;
        }
        var clampedAlpha = clampAlpha(alpha);
        double next;
        if (estimate == 0) {
            next = sample;
        } else {
            next = (1.0 - clampedAlpha) * estimate + clampedAlpha * sample;
        }
        estimate = truncateNonNegative(next);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "rtt estimate updated to {0} from sample {1} alpha {2}", estimate, sample, clampedAlpha);
        return estimate;
    }

    /**
     * Updates the minimum round trip time estimate with a sample and returns the
     * updated value.
     *
     * <p>Ignores a sample that is not strictly positive and returns the unchanged
     * minimum. When the minimum is uninitialized, or the sample is at least the current
     * minimum, the minimum snaps directly to the truncated sample. When the sample is
     * strictly below {@code minimum - 1} the minimum decays toward it through the
     * exponential blend at the given smoothing factor, so a new lower floor is
     * approached gradually rather than adopted in one step.
     *
     * @param sample the round trip time measurement in its native unit; ignored when
     *               not strictly positive
     * @param alpha  the smoothing factor used for the downward decay, clamped into
     *               {@code [0, 1]}
     * @return the updated minimum estimate after applying the sample
     */
    public long updateMin(double sample, double alpha) {
        if (sample <= 0) {
            return minEstimate;
        }
        if (minEstimate != 0 && sample < minEstimate - 1) {
            var clampedAlpha = clampAlpha(alpha);
            var next = (1.0 - clampedAlpha) * minEstimate + clampedAlpha * sample;
            minEstimate = truncateNonNegative(next);
        } else {
            minEstimate = truncateNonNegative(sample);
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "rtt min estimate updated to {0} from sample {1}", minEstimate, sample);
        return minEstimate;
    }

    /**
     * Returns the current smoothed round trip time estimate.
     *
     * @return the smoothed estimate in the sample unit, or {@code 0} if no valid
     *         sample has been applied
     */
    public long estimate() {
        return estimate;
    }

    /**
     * Returns the current minimum round trip time estimate.
     *
     * @return the minimum estimate in the sample unit, or {@code 0} if no valid sample
     *         has been applied
     */
    public long minEstimate() {
        return minEstimate;
    }

    /**
     * Clamps a smoothing factor into the closed interval {@code [0, 1]}.
     *
     * <p>Returns {@link #ALPHA_LOW} for a factor below the interval and
     * {@link #ALPHA_HIGH} for a factor above it, leaving an in range factor unchanged.
     *
     * @param alpha the smoothing factor to clamp
     * @return the factor confined to {@code [0, 1]}
     */
    private static double clampAlpha(double alpha) {
        if (alpha < ALPHA_LOW) {
            return ALPHA_LOW;
        }
        if (alpha > ALPHA_HIGH) {
            return ALPHA_HIGH;
        }
        return alpha;
    }

    /**
     * Clamps a value at zero and truncates it toward zero to a whole number.
     *
     * <p>Returns {@code 0} for any value below zero, otherwise the integer part of the
     * value with its fractional component dropped.
     *
     * @param value the value to clamp and truncate
     * @return the nonnegative truncated value
     */
    private static long truncateNonNegative(double value) {
        if (value <= 0) {
            return 0;
        }
        return (long) value;
    }
}
