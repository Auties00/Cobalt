package com.github.auties00.cobalt.calls.transport.congestion.bwe.combine;

import java.util.HashMap;
import java.util.Map;

/**
 * Selects how the {@link BitrateCombiner} fuses the sender side estimate with the remote receiver
 * estimate into one combined target.
 *
 * <p>The mode is a configuration value carried in the voip parameters and read per call. The first
 * three modes are the plain WebRTC style fusions ({@link #MIN}, {@link #MAX}, {@link #AVG}); the
 * remaining five are WhatsApp extensions that latch on inflection points, gate on the remote estimate
 * crossing a threshold, blend toward the sender estimate with an exponential moving average, or route
 * into the early congestion detector. Each constant carries the integer the voip parameter uses so the
 * decode is a direct value lookup.
 *
 * @implNote This implementation defaults an unrecognized selector to {@link #MIN_FLOOR}, the policy
 * value WhatsApp's shipped voip configuration selects.
 */
public enum CombineMode {
    /**
     * Takes the minimum of the sender and remote estimates.
     */
    MIN(0),

    /**
     * Takes the maximum of the sender and remote estimates.
     */
    MAX(1),

    /**
     * Takes the arithmetic mean of the sender and remote estimates.
     */
    AVG(2),

    /**
     * Takes the minimum with an inflection point floor, the value WhatsApp's shipped configuration
     * selects.
     *
     * <p>Uses the sender versus remote inflection latch so that once the sender estimate has crossed
     * the remote estimate the combined value does not drop below the latched floor.
     */
    MIN_FLOOR(3),

    /**
     * Routes the combine into the remote picture in picture second stage gated by
     * {@link RemotePipPhase}.
     */
    REMOTE_PIP(4),

    /**
     * Averages only when the remote estimate is at or above the remote picture in picture enter
     * threshold and no inflection has latched, otherwise holds.
     */
    AVG_GATED(5),

    /**
     * Blends the combined value toward the sender estimate with an exponential moving average once the
     * inflection latch is set.
     */
    EMA_BLEND(6),

    /**
     * Routes the combine into the early congestion detect path that trusts the receiver estimate.
     */
    EARLY_CONGESTION(7);

    /**
     * Resolves a voip parameter selector integer to its mode, backing {@link #ofValue(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #value}, so a selector resolves to
     * its mode in constant time rather than by scanning {@link #values()}. A selector with no entry falls
     * back to {@link #MIN_FLOOR} in {@link #ofValue(int)}.
     */
    private static final Map<Integer, CombineMode> BY_VALUE;

    static {
        var byValue = new HashMap<Integer, CombineMode>();
        for (var mode : values()) {
            if (byValue.put(mode.value, mode) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_VALUE = Map.copyOf(byValue);
    }

    /**
     * The integer the voip parameter uses to select this mode.
     */
    private final int value;

    /**
     * Constructs a combine mode bound to its voip parameter integer.
     *
     * @param value the selector integer
     */
    CombineMode(int value) {
        this.value = value;
    }

    /**
     * Returns the integer the voip parameter uses to select this mode.
     *
     * @return the selector integer
     */
    public int value() {
        return value;
    }

    /**
     * Returns the combine mode for a voip parameter selector integer, falling back to
     * {@link #MIN_FLOOR} for an unrecognized value.
     *
     * <p>{@link #MIN_FLOOR} is the fallback because it is the policy value WhatsApp's shipped
     * configuration selects, so an unknown selector degrades to that default rather than to a more or
     * less aggressive fusion.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_VALUE} map rather than
     * scanning {@link #values()}.
     * @param value the selector integer read from the voip parameters
     * @return the matching mode, or {@link #MIN_FLOOR} when none matches
     */
    public static CombineMode ofValue(int value) {
        var mode = BY_VALUE.get(value);
        return mode != null ? mode : MIN_FLOOR;
    }
}
