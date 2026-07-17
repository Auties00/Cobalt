package com.github.auties00.cobalt.calls.transport.congestion.bwe.delay;

/**
 * Classifies the delay based congestion state of a link as one of the three states the delay based
 * over use detector emits.
 *
 * <p>The {@link TrendlineEstimator} produces one of these values per packet group by comparing the
 * smoothed inter arrival delay trend against an adaptive threshold: a sustained positive trend means
 * the bottleneck queue is filling ({@link #OVERUSING}), a sustained negative trend means it is
 * draining ({@link #UNDERUSING}), and anything in between means the link is in equilibrium
 * ({@link #NORMAL}). The {@link AimdRateControl} consumes the classification to drive its
 * multiplicative decrease, additive increase, and hold transitions.
 *
 * @implNote This implementation relies on the declared ordinal order ({@link #NORMAL},
 * {@link #UNDERUSING}, {@link #OVERUSING}) so the rate controller's decision table can branch on the
 * constant directly; the order must not be reordered without updating that table.
 */
public enum BandwidthUsage {
    /**
     * Signals that the link is in equilibrium: the smoothed delay trend stays within the adaptive
     * threshold band.
     *
     * <p>The rate controller is free to probe upward (additive or multiplicative increase) while in
     * this state.
     */
    NORMAL,

    /**
     * Signals that the bottleneck queue is draining: the smoothed delay trend is below the negated
     * adaptive threshold.
     *
     * <p>The rate controller holds the current estimate rather than increasing, because a falling
     * queue often follows a recent decrease and an immediate ramp would congest the link again.
     */
    UNDERUSING,

    /**
     * Signals that the bottleneck queue is filling: the smoothed delay trend stays above the adaptive
     * threshold for at least the overusing time threshold.
     *
     * <p>The rate controller applies a multiplicative decrease in response to this state.
     */
    OVERUSING
}
