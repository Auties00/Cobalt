package com.github.auties00.cobalt.calls.transport.congestion.bwe.signal;

import com.github.auties00.cobalt.calls.transport.congestion.bwe.LiveSenderBandwidthEstimator;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Produces the {@link CongestionSignals} for one feedback round by comparing round trip time and
 * packet loss ratio against round trip scaled thresholds at three sensitivity tiers, with each input
 * gated by an enable bitmask.
 *
 * <p>{@link #evaluate(long, double, double, long, long)} reads which inputs are active from the enable
 * mask: {@link #ENABLE_RTT} compares the current round trip time against a high tier and a default tier
 * round trip scaled threshold; {@link #ENABLE_REMOTE_PLR} and {@link #ENABLE_LOCAL_PLR} compare the
 * remote and local loss ratios against fractional thresholds; {@link #ENABLE_STALENESS} flags
 * congestion when no feedback has arrived within the staleness window. The detector yields a
 * {@code congested} flag from the normal and high tiers and an {@code aggressive} flag from the high
 * sensitivity tier, returned together as one {@link CongestionSignals}.
 *
 * <p>Instances are not thread safe; the owning sender estimator drives one detector from the single
 * transport thread.
 *
 * @implNote This implementation reads the enable bits as fixed literals ({@code 0x1} round trip,
 * {@code 0x200} remote loss, {@code 0x400} local loss, {@code 0x800} remote timing, {@code 0x4000}
 * local timing, {@code 0x8000} staleness) and applies a fixed {@value #STALENESS_WINDOW_MS} ms
 * staleness window. The four threshold coefficients are not carried by the server pushed voip settings,
 * so they are supplied by the caller through the constructor.
 */
public final class CongestionSignalDetector {
    /**
     * The logger for {@link CongestionSignalDetector}.
     */
    private static final System.Logger LOGGER = Log.get(CongestionSignalDetector.class);

    /**
     * Enable bit selecting the round trip time threshold check.
     */
    public static final int ENABLE_RTT = 0x0001;

    /**
     * Enable bit selecting the remote packet loss ratio check.
     */
    public static final int ENABLE_REMOTE_PLR = 0x0200;

    /**
     * Enable bit selecting the local packet loss ratio check.
     */
    public static final int ENABLE_LOCAL_PLR = 0x0400;

    /**
     * Enable bit selecting the remote inter arrival timing check.
     */
    public static final int ENABLE_REMOTE_TIMING = 0x0800;

    /**
     * Enable bit selecting the local inter arrival timing check.
     */
    public static final int ENABLE_LOCAL_TIMING = 0x4000;

    /**
     * Enable bit selecting the staleness check.
     */
    public static final int ENABLE_STALENESS = 0x8000;

    /**
     * Staleness window, in milliseconds, within which feedback must arrive to avoid a staleness flag.
     */
    static final long STALENESS_WINDOW_MS = 30_000;

    /**
     * High tier round trip time coefficient applied to the baseline round trip time.
     *
     * <p>Supplied by the caller through the constructor.
     */
    private final double rttHighCoefficient;

    /**
     * Default tier round trip time coefficient applied to the baseline round trip time.
     *
     * <p>Supplied by the caller through the constructor.
     */
    private final double rttDefaultCoefficient;

    /**
     * High tier loss fraction, in {@code [0, 1]}, at or above which the high tier trips.
     *
     * <p>Supplied by the caller through the constructor and clamped into {@code [0, 1]}.
     */
    private final double plrHighFraction;

    /**
     * Default tier loss fraction, in {@code [0, 1]}, at or above which the normal tier trips.
     *
     * <p>Supplied by the caller through the constructor and clamped into {@code [0, 1]}.
     */
    private final double plrDefaultFraction;

    /**
     * Baseline round trip time, in milliseconds, that the current value is compared against, or
     * {@code 0} when not yet seeded.
     *
     * <p>Seeded from the first observed round trip time; the high and default thresholds are this
     * baseline scaled by the coefficients.
     */
    private long baselineRttMs = 0;

    /**
     * Constructs a detector with the round trip and loss threshold coefficients.
     *
     * <p>The two loss fractions are clamped into {@code [0, 1]}; the two round trip coefficients are
     * stored as given.
     *
     * @param rttHighCoefficient    the high tier round trip coefficient
     * @param rttDefaultCoefficient the default tier round trip coefficient
     * @param plrHighFraction       the high tier loss fraction, in {@code [0, 1]}
     * @param plrDefaultFraction    the default tier loss fraction, in {@code [0, 1]}
     */
    public CongestionSignalDetector(double rttHighCoefficient, double rttDefaultCoefficient,
                                    double plrHighFraction, double plrDefaultFraction) {
        this.rttHighCoefficient = rttHighCoefficient;
        this.rttDefaultCoefficient = rttDefaultCoefficient;
        this.plrHighFraction = Math.clamp(plrHighFraction, 0.0, 1.0);
        this.plrDefaultFraction = Math.clamp(plrDefaultFraction, 0.0, 1.0);
    }

    /**
     * Evaluates the congestion signals for one feedback round.
     *
     * <p>Seeds the baseline round trip time from the first positive value, then for each enabled input
     * tests the round trip time against the high and default round trip scaled thresholds, the remote
     * and local loss ratios against the high and default fractions, and the feedback recency against
     * the staleness window. A high tier round trip or loss trip sets the aggressive flag; a default
     * tier round trip or loss trip, the staleness trip, or the aggressive trip sets the congested flag.
     * A round returning {@link CongestionSignals#AGGRESSIVE} takes precedence over
     * {@link CongestionSignals#CONGESTED}, which takes precedence over {@link CongestionSignals#NONE}.
     * The remote timing ({@link #ENABLE_REMOTE_TIMING}) and local timing ({@link #ENABLE_LOCAL_TIMING})
     * bits are accepted in {@code enableMask} but not yet evaluated, so setting them currently
     * contributes nothing to the verdict.
     *
     * @param rttMs         the current round trip time, in milliseconds; ignored when non positive
     * @param remotePlr     the remote packet loss ratio, in {@code [0, 1]}
     * @param localPlr      the local packet loss ratio, in {@code [0, 1]}
     * @param feedbackAgeMs the time, in milliseconds, since the last feedback was received
     * @param enableMask    the bitmask selecting which inputs are evaluated
     * @return the congestion signals for this round
     */
    // TODO: evaluate the remote timing (0x800) and local timing (0x4000) congestion bits. Under the
    //  remote timing bit, flag congestion when the remote timing threshold is at or below the smaller of
    //  the two remote inter arrival timing windows; under the local timing bit, gated by its own enable
    //  byte, flag congestion when the local timing threshold is at or below the smaller of the two local
    //  inter arrival windows. Both inputs are inter arrival timing windows sourced from the transport
    //  stream statistics, which are not threaded into this detector: evaluate() receives no timing
    //  measurements and the caller LiveSenderBandwidthEstimator sources none. A faithful implementation
    //  needs those measurements plumbed from the delay based estimator together with the two thresholds,
    //  so the bits stay inert rather than guessed.
    public CongestionSignals evaluate(long rttMs, double remotePlr, double localPlr, long feedbackAgeMs,
                                      long enableMask) {
        if (rttMs > 0 && baselineRttMs == 0) {
            baselineRttMs = rttMs;
        }
        var congested = false;
        var aggressive = false;

        if ((enableMask & ENABLE_RTT) != 0 && rttMs > 0 && baselineRttMs > 0) {
            if (rttMs > rttHighCoefficient * baselineRttMs) {
                aggressive = true;
            }
            if (rttMs > rttDefaultCoefficient * baselineRttMs) {
                congested = true;
            }
        }
        if ((enableMask & ENABLE_REMOTE_PLR) != 0) {
            if (remotePlr >= plrHighFraction) {
                aggressive = true;
            }
            if (remotePlr >= plrDefaultFraction) {
                congested = true;
            }
        }
        if ((enableMask & ENABLE_LOCAL_PLR) != 0) {
            if (localPlr >= plrHighFraction) {
                aggressive = true;
            }
            if (localPlr >= plrDefaultFraction) {
                congested = true;
            }
        }
        if ((enableMask & ENABLE_STALENESS) != 0 && feedbackAgeMs >= STALENESS_WINDOW_MS) {
            congested = true;
        }
        if (aggressive) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "congestion signal aggressive: rtt={0}ms baseline={1}ms remotePlr={2} localPlr={3}",
                        rttMs, baselineRttMs, remotePlr, localPlr);
            }
            return CongestionSignals.AGGRESSIVE;
        }
        if (congested && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "congestion signal congested: rtt={0}ms baseline={1}ms remotePlr={2} localPlr={3}",
                    rttMs, baselineRttMs, remotePlr, localPlr);
        }
        return congested ? CongestionSignals.CONGESTED : CongestionSignals.NONE;
    }

    /**
     * Returns the baseline round trip time that the thresholds are scaled from.
     *
     * @return the baseline round trip time, in milliseconds, or {@code 0} when not yet seeded
     */
    public long baselineRttMs() {
        return baselineRttMs;
    }

    /**
     * Resets the detector, clearing the round trip baseline.
     */
    public void reset() {
        baselineRttMs = 0;
    }
}
