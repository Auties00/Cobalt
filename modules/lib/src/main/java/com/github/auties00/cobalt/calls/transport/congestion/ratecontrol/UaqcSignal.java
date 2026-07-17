package com.github.auties00.cobalt.calls.transport.congestion.ratecontrol;

/**
 * The congestion verdict a single unified audio quality control input signal yields.
 *
 * <p>Each of the packet loss ratio, round trip time, and receiver estimated maximum bitrate signals is
 * evaluated with hysteresis against a lower and an upper threshold and reduced to one of these levels:
 * a value at or below the lower threshold is {@link #NON_CONGESTED}, a value at or above the upper
 * threshold (or whose trend exceeds the upward slope) is {@link #CONGESTED}, and anything in between is
 * {@link #HOLD}. The {@link UnifiedAudioQualityControl} aggregates the three levels into a single move on
 * the quality state: any {@link #NON_CONGESTED} signal moves the state toward a better one, otherwise any
 * {@link #CONGESTED} signal moves it toward a worse one, and all {@link #HOLD} keeps the state.
 *
 * @implNote This implementation orders the constants so their ordinals are {@code 0}, {@code 1}, and
 * {@code 2}, matching the integer levels the aggregate comparison in {@link UnifiedAudioQualityControl}
 * expects.
 */
public enum UaqcSignal {
    /**
     * The signal is at or below the lower threshold: the link shows no congestion on this input.
     *
     * <p>A single signal at this level lets the quality state move toward a better state.
     */
    NON_CONGESTED,

    /**
     * The signal sits between the lower and upper thresholds: inconclusive.
     *
     * <p>A signal at this level neither improves nor worsens the quality state on its own.
     */
    HOLD,

    /**
     * The signal is at or above the upper threshold, or its trend exceeds the upward slope: congested.
     *
     * <p>When no signal is {@link #NON_CONGESTED}, a signal at this level moves the quality state toward
     * a worse state.
     */
    CONGESTED
}
