package com.github.auties00.cobalt.calls.transport.congestion.bwe.delay;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Drives the delay based send bitrate with WebRTC's additive increase and multiplicative decrease rate
 * control, decreasing the estimate on overuse and increasing it otherwise.
 *
 * <p>The control consumes the {@link BandwidthUsage} the {@link TrendlineEstimator} emits and the
 * recently acknowledged throughput, and maintains one rate estimate in bits per second. On
 * {@link BandwidthUsage#OVERUSING} it switches to the {@link RateControlState#DECREASE} state and
 * multiplies the estimate by {@link #DECREASE_BETA}, then reseeds the tracked link capacity from the
 * acknowledged throughput. On {@link BandwidthUsage#NORMAL} it increases: multiplicatively when the
 * estimate is far below the tracked link capacity (still ramping after a recent drop) and additively
 * when it is near capacity, so the estimate approaches the ceiling without overshooting.
 * {@link BandwidthUsage#UNDERUSING} holds the estimate. The candidate is clamped so it never exceeds
 * {@link #MAX_HEADROOM_FACTOR} times the acknowledged throughput, the configured maximum, or falls
 * below the configured minimum.
 *
 * <p>Instances are not thread safe; the owning {@link GccDelayBasedEstimator} drives one control from
 * the single call transport thread.
 *
 * @implNote This implementation applies the WebRTC additive increase and multiplicative decrease model.
 * The decrease factor is {@link #DECREASE_BETA} = {@value #DECREASE_BETA}, the value the shipped
 * WhatsApp voip build uses, rather than the {@code 0.85} upstream WebRTC default. The additive increase
 * step uses the WebRTC response time and packet size model, and the boundary between multiplicative and
 * additive increase sits {@link #CAPACITY_DEVIATIONS} deviations below the tracked link capacity.
 */
public final class AimdRateControl {
    /**
     * The logger for {@link AimdRateControl}.
     */
    private static final System.Logger LOGGER = Log.get(AimdRateControl.class);

    /**
     * Multiplicative decrease factor applied to the estimate on overuse.
     *
     * <p>The shipped WhatsApp voip build uses {@code 0.95}; {@code 0.85} is only the upstream WebRTC
     * default and is deliberately not used here.
     */
    static final double DECREASE_BETA = 0.95;

    /**
     * Maximum multiple of acknowledged throughput the candidate estimate may reach.
     *
     * <p>Clamping the candidate to this headroom over what the network has actually delivered prevents
     * the estimate from running far ahead of demonstrated capacity.
     */
    static final double MAX_HEADROOM_FACTOR = 1.5;

    /**
     * Smoothing factor for the link capacity estimate updated after each decrease.
     *
     * <p>The tracked capacity blends this fraction of the new acknowledged sample with the
     * complementary fraction of the previous estimate.
     */
    static final double LINK_CAPACITY_ALPHA = 0.05;

    /**
     * Assumed round trip time, in milliseconds, used in the additive increase response time model
     * when no measured value is supplied.
     *
     * <p>The additive step is one expected packet's worth of bits spread over the response time of one
     * round trip plus the WebRTC fixed processing allowance.
     */
    static final double DEFAULT_RTT_MS = 200.0;

    /**
     * Fixed processing and feedback allowance, in milliseconds, added to the round trip time in the
     * additive increase response time model.
     */
    static final double RESPONSE_TIME_OVERHEAD_MS = 100.0;

    /**
     * Assumed average packet size, in bits, used as the additive increase quantum.
     *
     * <p>WebRTC uses a nominal MTU of twelve hundred bytes as the increase unit per response.
     */
    static final double AVERAGE_PACKET_SIZE_BITS = 1200.0 * 8.0;

    /**
     * Additive increase quantum, in bits per millisecond, spreading one {@link #AVERAGE_PACKET_SIZE_BITS}
     * over the response time of {@link #DEFAULT_RTT_MS} plus {@link #RESPONSE_TIME_OVERHEAD_MS}.
     *
     * <p>Precomputed from the compile time response time model so the additive step scales it by the
     * elapsed time without recomputing the quotient on each increase.
     */
    static final double ADDITIVE_INCREASE_BITS_PER_MS = AVERAGE_PACKET_SIZE_BITS / (DEFAULT_RTT_MS + RESPONSE_TIME_OVERHEAD_MS);

    /**
     * Number of standard deviations defining the near maximum band around the tracked link capacity.
     *
     * <p>While the estimate is below capacity minus this many deviations of the capacity estimate the
     * control increases multiplicatively; once inside the band it increases additively.
     */
    static final double CAPACITY_DEVIATIONS = 3.0;

    /**
     * Multiplicative increase factor applied per second while the estimate is far below link capacity.
     *
     * <p>A ramp of eight percent per second lets the estimate recover quickly after a decrease without
     * the aggression that would congest the link again.
     */
    static final double MULTIPLICATIVE_INCREASE_PER_SECOND = 0.08;

    /**
     * Names the additive increase and multiplicative decrease phase the control is in.
     */
    enum RateControlState {
        /**
         * The estimate holds at its current value; entered on underuse or while awaiting the first
         * overuse after a decrease.
         */
        HOLD,

        /**
         * The estimate increases, additively near link capacity and multiplicatively far below it.
         */
        INCREASE,

        /**
         * The estimate decreases multiplicatively in response to overuse.
         */
        DECREASE
    }

    /**
     * Lower bound, in bits per second, the estimate is clamped to.
     */
    private final long minBitrateBps;

    /**
     * Upper bound, in bits per second, the estimate is clamped to.
     */
    private long maxBitrateBps;

    /**
     * Current rate estimate, in bits per second.
     */
    private long currentBitrateBps;

    /**
     * Current additive increase and multiplicative decrease phase.
     */
    private RateControlState rateControlState = RateControlState.HOLD;

    /**
     * Estimated bottleneck link capacity, in bits per second, or a negative value when not yet seeded.
     *
     * <p>Updated from acknowledged throughput on each decrease and used as the boundary between
     * additive and multiplicative increase.
     */
    private double linkCapacityEstimateBps = -1.0;

    /**
     * Running deviation, in bits per second, of the link capacity estimate.
     *
     * <p>Three of these deviations below the capacity estimate marks the near maximum band boundary.
     */
    private double linkCapacityDeviationBps = 0.0;

    /**
     * Timestamp, in milliseconds, of the last increase step, or {@code -1} when none has occurred.
     *
     * <p>Supplies the elapsed time the multiplicative and additive increases scale by.
     */
    private long lastIncreaseMs = -1;

    /**
     * Constructs a rate control bounded to the given bitrate range, starting at the minimum in the
     * hold state.
     *
     * @param minBitrateBps the lower bound on the estimate, in bits per second; must be positive
     * @param maxBitrateBps the upper bound on the estimate, in bits per second; must be at least the
     *                      minimum
     * @throws IllegalArgumentException if {@code minBitrateBps} is not positive or
     *                                  {@code maxBitrateBps} is below it
     */
    public AimdRateControl(long minBitrateBps, long maxBitrateBps) {
        if (minBitrateBps <= 0) {
            throw new IllegalArgumentException("minBitrateBps must be positive: " + minBitrateBps);
        }
        if (maxBitrateBps < minBitrateBps) {
            throw new IllegalArgumentException("maxBitrateBps below minBitrateBps: " + maxBitrateBps);
        }
        this.minBitrateBps = minBitrateBps;
        this.maxBitrateBps = maxBitrateBps;
        this.currentBitrateBps = minBitrateBps;
    }

    /**
     * Updates the rate estimate from a delay based classification and the acknowledged throughput, and
     * returns the new estimate.
     *
     * <p>The classification selects the phase: overuse forces {@link RateControlState#DECREASE},
     * normal forces {@link RateControlState#INCREASE}, and underuse forces
     * {@link RateControlState#HOLD}. The decrease multiplies the estimate by {@link #DECREASE_BETA} and
     * reseeds the link capacity tracker from the acknowledged throughput; the increase ramps
     * multiplicatively below the near maximum band and additively inside it; the hold leaves the
     * estimate unchanged. The result is clamped to {@link #MAX_HEADROOM_FACTOR} times the acknowledged
     * throughput and to the configured range.
     *
     * @param usage             the delay based classification from the {@link TrendlineEstimator}
     * @param ackedThroughputBps the recently acknowledged throughput, in bits per second; a
     *                           nonpositive value disables the throughput headroom clamp and the
     *                           capacity reseed for this step
     * @param nowMs             the monotonic timestamp, in milliseconds, at which this update runs
     * @return the updated rate estimate, in bits per second
     */
    public long update(BandwidthUsage usage, long ackedThroughputBps, long nowMs) {
        var previousState = rateControlState;
        switch (usage) {
            case OVERUSING -> rateControlState = RateControlState.DECREASE;
            case NORMAL -> rateControlState = RateControlState.INCREASE;
            case UNDERUSING -> rateControlState = RateControlState.HOLD;
        }
        if (Log.DEBUG && rateControlState != previousState) {
            LOGGER.log(Level.DEBUG, "aimd rate control: state {0} -> {1}, usage={2}",
                    previousState, rateControlState, usage);
        }
        switch (rateControlState) {
            case DECREASE -> applyDecrease(ackedThroughputBps);
            case INCREASE -> applyIncrease(ackedThroughputBps, nowMs);
            case HOLD -> lastIncreaseMs = nowMs;
        }
        currentBitrateBps = clampCandidate(currentBitrateBps, ackedThroughputBps);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "aimd rate control: usage={0} acked={1} bitrate={2}",
                    usage, ackedThroughputBps, currentBitrateBps);
        }
        return currentBitrateBps;
    }

    /**
     * Applies the multiplicative decrease and reseeds the link capacity tracker.
     *
     * <p>Multiplies the estimate by {@link #DECREASE_BETA}, then blends the acknowledged throughput
     * into {@link #linkCapacityEstimateBps} and updates {@link #linkCapacityDeviationBps} so the
     * near maximum increase boundary follows the latest demonstrated capacity.
     *
     * @param ackedThroughputBps the acknowledged throughput, in bits per second; ignored when
     *                           nonpositive
     */
    private void applyDecrease(long ackedThroughputBps) {
        currentBitrateBps = (long) (currentBitrateBps * DECREASE_BETA);
        if (ackedThroughputBps > 0) {
            updateLinkCapacity(ackedThroughputBps);
        }
        lastIncreaseMs = -1;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "aimd rate control: decrease applied, bitrate={0} linkCapacity={1}",
                    currentBitrateBps, linkCapacityEstimateBps);
        }
    }

    /**
     * Applies the additive or multiplicative increase for one step.
     *
     * <p>When the acknowledged throughput exceeds the tracked link capacity's upper bound (the estimate
     * plus {@link #CAPACITY_DEVIATIONS} deviations), the link capacity tracker is reset so a network that
     * has just demonstrated more capacity than the estimate no longer pins the increase into the additive
     * near maximum branch; the reset drops the estimate to unseeded, which routes this and following steps
     * back through the multiplicative ramp until the capacity reseeds on the next decrease. The reset is
     * skipped while the capacity is unseeded (upper bound infinity) or the acknowledged throughput is
     * unavailable.
     *
     * <p>When the estimate is below the link capacity minus {@link #CAPACITY_DEVIATIONS} deviations,
     * or the capacity is not yet seeded, the estimate grows by
     * {@link #MULTIPLICATIVE_INCREASE_PER_SECOND} scaled by the elapsed seconds. Otherwise it grows by
     * the additive quantum: one {@link #AVERAGE_PACKET_SIZE_BITS} per response time of
     * {@link #DEFAULT_RTT_MS} plus {@link #RESPONSE_TIME_OVERHEAD_MS}, scaled by the elapsed time.
     *
     * @param ackedThroughputBps the acknowledged throughput, in bits per second; when it overshoots the
     *                           tracked link capacity upper bound the tracker is reset, and a nonpositive
     *                           value disables the reset for this step
     * @param nowMs              the monotonic timestamp, in milliseconds, of this step
     */
    private void applyIncrease(long ackedThroughputBps, long nowMs) {
        if (ackedThroughputBps > 0 && linkCapacityEstimateBps >= 0
                && ackedThroughputBps > linkCapacityEstimateBps + CAPACITY_DEVIATIONS * linkCapacityDeviationBps) {
            linkCapacityEstimateBps = -1.0;
            linkCapacityDeviationBps = 0.0;
        }
        var elapsedMs = lastIncreaseMs < 0 ? 0.0 : (double) (nowMs - lastIncreaseMs);
        lastIncreaseMs = nowMs;
        var nearMaxBoundary = linkCapacityEstimateBps - CAPACITY_DEVIATIONS * linkCapacityDeviationBps;
        if (linkCapacityEstimateBps < 0 || currentBitrateBps < nearMaxBoundary) {
            var multiplier = Math.pow(1.0 + MULTIPLICATIVE_INCREASE_PER_SECOND, Math.min(elapsedMs / 1000.0, 1.0));
            currentBitrateBps = Math.max(currentBitrateBps, (long) (currentBitrateBps * multiplier));
        } else {
            var increaseBits = Math.max(AVERAGE_PACKET_SIZE_BITS, ADDITIVE_INCREASE_BITS_PER_MS * elapsedMs);
            currentBitrateBps += (long) increaseBits;
        }
    }

    /**
     * Blends an acknowledged throughput sample into the link capacity estimate and its deviation.
     *
     * <p>Seeds both directly on the first sample; thereafter the estimate is an exponential moving
     * average at {@link #LINK_CAPACITY_ALPHA} and the deviation is the same average of the absolute
     * residual, giving the near maximum band its width.
     *
     * @param ackedThroughputBps the acknowledged throughput, in bits per second
     */
    private void updateLinkCapacity(long ackedThroughputBps) {
        if (linkCapacityEstimateBps < 0) {
            linkCapacityEstimateBps = ackedThroughputBps;
            linkCapacityDeviationBps = 0.0;
            return;
        }
        var residual = Math.abs(ackedThroughputBps - linkCapacityEstimateBps);
        linkCapacityEstimateBps += LINK_CAPACITY_ALPHA * (ackedThroughputBps - linkCapacityEstimateBps);
        linkCapacityDeviationBps += LINK_CAPACITY_ALPHA * (residual - linkCapacityDeviationBps);
    }

    /**
     * Clamps a candidate estimate into the throughput headroom and the configured range.
     *
     * <p>When an acknowledged throughput is supplied the candidate is first capped at
     * {@link #MAX_HEADROOM_FACTOR} times it; the candidate is then clamped to
     * {@code [minBitrateBps, maxBitrateBps]}.
     *
     * @param candidateBps       the proposed estimate, in bits per second
     * @param ackedThroughputBps the acknowledged throughput, in bits per second; ignored when
     *                           nonpositive
     * @return the clamped estimate, in bits per second
     */
    private long clampCandidate(long candidateBps, long ackedThroughputBps) {
        var clamped = candidateBps;
        if (ackedThroughputBps > 0) {
            var headroom = (long) (ackedThroughputBps * MAX_HEADROOM_FACTOR);
            if (clamped > headroom) {
                clamped = headroom;
            }
        }
        return Math.clamp(clamped, minBitrateBps, maxBitrateBps);
    }

    /**
     * Returns the current rate estimate.
     *
     * @return the estimate, in bits per second
     */
    public long currentBitrateBps() {
        return currentBitrateBps;
    }

    /**
     * Returns the current additive increase and multiplicative decrease phase.
     *
     * @return the rate control state
     */
    RateControlState rateControlState() {
        return rateControlState;
    }

    /**
     * Returns the tracked link capacity estimate.
     *
     * @return the link capacity estimate, in bits per second, or a negative value when not yet seeded
     */
    public double linkCapacityEstimateBps() {
        return linkCapacityEstimateBps;
    }

    /**
     * Updates the upper bound the estimate is clamped to.
     *
     * <p>Raising or lowering the maximum takes effect on the next {@link #update(BandwidthUsage, long,
     * long)}; the current estimate is not clamped again here.
     *
     * @param maxBitrateBps the new upper bound, in bits per second; clamped to at least the minimum
     */
    public void setMaxBitrateBps(long maxBitrateBps) {
        this.maxBitrateBps = Math.max(minBitrateBps, maxBitrateBps);
    }

    /**
     * Resets the control to the state it had immediately after construction, at the minimum bitrate.
     *
     * <p>Restores the hold state, clears the link capacity tracker, and drops the increase timestamp.
     */
    public void reset() {
        currentBitrateBps = minBitrateBps;
        rateControlState = RateControlState.HOLD;
        linkCapacityEstimateBps = -1.0;
        linkCapacityDeviationBps = 0.0;
        lastIncreaseMs = -1;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "aimd rate control: reset");
    }
}
