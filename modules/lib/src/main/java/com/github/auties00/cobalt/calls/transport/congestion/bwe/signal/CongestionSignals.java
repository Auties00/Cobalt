package com.github.auties00.cobalt.calls.transport.congestion.bwe.signal;

/**
 * Holds the two boolean flags {@link CongestionSignalDetector} produces from a feedback round.
 *
 * <p>The {@code congested} flag drives the sender estimator's decrease and hold logic; the
 * {@code aggressive} flag marks the higher sensitivity tier, used to back off more sharply or to gate
 * the audio quality controller. The two flags are independent: a round can be congested without being
 * aggressive, and the detector sets each from its own threshold tier. When {@code aggressive} is set,
 * {@code congested} is always set as well, so the reachable combinations are {@link #NONE},
 * {@link #CONGESTED}, and {@link #AGGRESSIVE}.
 *
 * @param congested  whether the link is judged congested this round
 * @param aggressive whether the higher sensitivity tier also tripped, calling for a sharper response
 */
public record CongestionSignals(boolean congested, boolean aggressive) {
    /**
     * A signals value with neither flag set, denoting a round judged not congested.
     */
    public static final CongestionSignals NONE = new CongestionSignals(false, false);

    /**
     * A signals value with the {@code congested} flag set but not the {@code aggressive} flag,
     * denoting a trip of the default sensitivity tier.
     */
    public static final CongestionSignals CONGESTED = new CongestionSignals(true, false);

    /**
     * A signals value with both flags set, denoting a trip of the high sensitivity tier; the
     * {@code aggressive} flag always implies the {@code congested} flag.
     */
    public static final CongestionSignals AGGRESSIVE = new CongestionSignals(true, true);
}
