package com.github.auties00.cobalt.calls.transport.congestion.ratecontrol;

/**
 * The six quality states the unified audio quality control machine moves between as congestion signals
 * worsen and recover.
 *
 * <p>The state captures how aggressively the audio path is managing for bandwidth. {@link #PROBING}
 * ramps the rate up under forward error correction protection until the receiver estimated maximum
 * bitrate proves the link has headroom; {@link #HIGH_QUALITY} is the highest steady operating point,
 * entered when the link is very healthy; {@link #BANDWIDTH_MANAGED} is the nominal state where the rate
 * tracks the estimate; {@link #LOSSY} is the random loss regime entered when loss rises while round
 * trip time stays acceptable; {@link #DRAIN} backs the rate off when the link is genuinely congested;
 * and {@link #ULTRA_LOW_BANDWIDTH} is the deepest back off for a severely constrained link. The states
 * form an ordered ladder from best ({@link #PROBING}) to worst ({@link #ULTRA_LOW_BANDWIDTH}); the
 * aggregated congestion verdict moves one rung toward a better or a worse state per evaluation.
 *
 * <p>The declared order places the better state first so that {@link #ordinal()} expresses the better
 * or worse move the aggregate transition makes: a worsening verdict advances to a higher ordinal, a
 * recovering verdict retreats to a lower one. Each state carries a target bitrate supplied by the live
 * voip settings ({@code high_quality} 25000, {@code bandwidth_managed} 24000, {@code probing} 15000,
 * {@code drain} 15000, {@code ultra_low_bandwidth} 12000), and each carries its own family of transition
 * thresholds. The routing into and out of {@link #HIGH_QUALITY} and {@link #ULTRA_LOW_BANDWIDTH} and the
 * per state threshold and target bitrate configuration live in the owning quality control service, not
 * in this enum.
 */
public enum UaqcState {
    /**
     * Ramps the rate up under forward error correction protection to discover link headroom.
     *
     * <p>Exits to {@link #BANDWIDTH_MANAGED} when the receiver estimated maximum bitrate exceeds the
     * probing exit threshold.
     */
    PROBING,

    /**
     * The highest steady operating point, entered when the link is very healthy.
     *
     * <p>The best state outside probing, carrying the highest per state target bitrate; it steps down
     * toward {@link #BANDWIDTH_MANAGED} as the congestion signals worsen.
     */
    HIGH_QUALITY,

    /**
     * The nominal state: the rate tracks the bandwidth estimate without special loss handling.
     *
     * <p>Moves to {@link #LOSSY} when random loss is detected and to {@link #DRAIN} when the estimate
     * approaches the upper threshold.
     */
    BANDWIDTH_MANAGED,

    /**
     * The random loss regime: loss is elevated while round trip time remains acceptable.
     *
     * <p>Moves to {@link #DRAIN} on round trip, loss, or estimate congestion and back to
     * {@link #BANDWIDTH_MANAGED} when loss recovers.
     */
    LOSSY,

    /**
     * The back off state: the link is congested and the rate is reduced.
     *
     * <p>Recovers toward {@link #BANDWIDTH_MANAGED} when the congestion signals clear, and steps down to
     * {@link #ULTRA_LOW_BANDWIDTH} when the link stays severely constrained.
     */
    DRAIN,

    /**
     * The deepest back off state for a severely constrained link.
     *
     * <p>The worst state, carrying the lowest per state target bitrate; it recovers upward when the
     * congestion signals clear.
     */
    ULTRA_LOW_BANDWIDTH
}
