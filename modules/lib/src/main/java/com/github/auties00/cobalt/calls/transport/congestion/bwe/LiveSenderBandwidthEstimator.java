package com.github.auties00.cobalt.calls.transport.congestion.bwe;

import com.github.auties00.cobalt.calls.transport.congestion.bwe.combine.BitrateCombiner;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping.BweHoldController;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping.BweHoldReason;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping.ConservativeInitMode;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.signal.CongestionSignalDetector;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.signal.CongestionSignals;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * WhatsApp's sender side bandwidth estimator: runs an AIMD loss and round trip time controller, fuses
 * its output with the remote receiver estimate and the packet pair link capacity through the
 * {@link BitrateCombiner}, and gates the result through the {@link BweHoldController} and the
 * conservative init clamp.
 *
 * <p>Each feedback round is reported through {@link #onFeedback(double, long, long, long, long, long)}.
 * The estimator updates its sender AIMD estimate (delegated to an {@link AudioSenderBandwidthEstimator}
 * for the loss and round trip math), evaluates the {@link CongestionSignalDetector}, fuses the sender
 * estimate with the remote estimate and link capacity, and then applies the hold machine: a combined
 * value below the previous target arms a congestion hold, the combiner's receive drop condition arms a
 * receive drop hold, and while any reason holds the target is frozen. The {@link ConservativeInitMode}
 * keeps the startup estimate cautious until it leaves the configured band. The combined, gated value is
 * the target the rate controllers consume.
 *
 * <p>Instances are not thread safe; the call session drives one estimator from the single transport
 * thread.
 */
public final class LiveSenderBandwidthEstimator implements BandwidthEstimator {
    /**
     * The logger for {@link LiveSenderBandwidthEstimator}.
     */
    private static final System.Logger LOGGER = Log.get(LiveSenderBandwidthEstimator.class);

    /**
     * The sender side AIMD loss and round trip time estimator producing the sender estimate.
     */
    private final AudioSenderBandwidthEstimator senderAimd;

    /**
     * The fusion combining the sender, remote, and link capacity estimates.
     */
    private final BitrateCombiner combiner;

    /**
     * The hold machine freezing the target after clamp downs and receive drops.
     */
    private final BweHoldController holdController;

    /**
     * The conservative startup clamp keeping the initial estimate cautious.
     */
    private final ConservativeInitMode conservativeInit;

    /**
     * The triple tier congestion detector evaluated each feedback round.
     */
    private final CongestionSignalDetector congestionDetector;

    /**
     * The congestion signal enable bitmask passed to the detector each round.
     */
    private final long congestionEnableMask;

    /**
     * Lower bound, in bits per second, the combined target is clamped to.
     */
    private final long minBitrateBps;

    /**
     * Upper bound, in bits per second, the combined target is clamped to.
     */
    private final long maxBitrateBps;

    /**
     * Current combined and gated target, in bits per second.
     */
    private long targetBps;

    /**
     * Combined target from the previous round, in bits per second, used to detect a clamp down.
     */
    private long previousTargetBps;

    /**
     * Monotonic timestamp, in milliseconds, of the previous feedback round, or {@code -1} before any
     * round has run.
     *
     * <p>The difference between this and the current round's timestamp is the elapsed time since the
     * last feedback was processed, threaded into both the congestion detector's staleness check and the
     * sender AIMD's no feedback gate so the congestion condition for no RTCP arriving within the window
     * fires on the main feedback path rather than only on a separate periodic fallback. A real report
     * received each round keeps the gap small (not stale); a stalled report stream that the periodic
     * fallback runs again with the same monotonic clock grows the gap past the staleness window.
     */
    private long lastFeedbackNowMs = -1;

    /**
     * Most recent congestion signals from the detector.
     */
    private CongestionSignals lastSignals = CongestionSignals.NONE;

    /**
     * Constructs a sender estimator from its composed parts.
     *
     * @param senderAimd          the sender side AIMD estimator; never {@code null}
     * @param combiner            the fusion combiner; never {@code null}
     * @param holdController      the hold machine; never {@code null}
     * @param conservativeInit    the conservative init clamp; never {@code null}
     * @param congestionDetector  the congestion detector; never {@code null}
     * @param congestionEnableMask the congestion signal enable bitmask
     * @param minBitrateBps       the lower bound on the target, in bits per second; must be positive
     * @param maxBitrateBps       the upper bound on the target, in bits per second; must be at least the
     *                            minimum
     * @param startBitrateBps     the initial target, in bits per second; clamped into the bounds
     * @throws NullPointerException     if any composed part is {@code null}
     * @throws IllegalArgumentException if the bounds are not a positive ordered pair
     */
    public LiveSenderBandwidthEstimator(AudioSenderBandwidthEstimator senderAimd, BitrateCombiner combiner,
                                        BweHoldController holdController, ConservativeInitMode conservativeInit,
                                        CongestionSignalDetector congestionDetector, long congestionEnableMask,
                                        long minBitrateBps, long maxBitrateBps, long startBitrateBps) {
        if (senderAimd == null || combiner == null || holdController == null
                || conservativeInit == null || congestionDetector == null) {
            throw new NullPointerException("composed BWE part is null");
        }
        if (minBitrateBps <= 0) {
            throw new IllegalArgumentException("minBitrateBps must be positive: " + minBitrateBps);
        }
        if (maxBitrateBps < minBitrateBps) {
            throw new IllegalArgumentException("maxBitrateBps below minBitrateBps: " + maxBitrateBps);
        }
        this.senderAimd = senderAimd;
        this.combiner = combiner;
        this.holdController = holdController;
        this.conservativeInit = conservativeInit;
        this.congestionDetector = congestionDetector;
        this.congestionEnableMask = congestionEnableMask;
        this.minBitrateBps = minBitrateBps;
        this.maxBitrateBps = maxBitrateBps;
        this.targetBps = Math.clamp(startBitrateBps, minBitrateBps, maxBitrateBps);
        this.previousTargetBps = this.targetBps;
    }

    /**
     * Updates the estimate from one feedback round and returns the new combined, gated target.
     *
     * <p>Evaluates the congestion detector, advances the sender AIMD estimate, fuses it with the remote
     * estimate and link capacity, applies the conservative init clamp while it is active, then runs the
     * hold machine: a combined value below the previous target arms a congestion hold and, on the
     * combiner's receive drop condition, a receive drop hold, while a value at or above the previous
     * target with no holds active ends the congestion and receive drop holds. The returned target is the
     * frozen value while any reason holds, otherwise the freshly combined value clamped into the bounds.
     *
     * <p>The elapsed time since the previous round is computed from {@code nowMs} and threaded into both
     * the congestion detector's staleness check and the sender AIMD's no feedback gate, so the
     * congestion condition for no RTCP arriving within the window fires on this main feedback path. The
     * first round, having no predecessor, passes a negative age so neither staleness gate trips.
     *
     * @param plr                 the packet loss ratio, in {@code [0, 1]}
     * @param rttNs               the current round trip time, in nanoseconds; ignored when not positive
     * @param remoteBweBps        the latest remote receiver estimate, in bits per second; {@code 0} when
     *                            none has arrived
     * @param minRemoteBitrateEstimateBps the per round {@code min_remote_bitrate_estimate}, in bits per
     *                            second (the minimum across connected participants of each participant's
     *                            remote estimate), or {@code 0}; the sender AIMD seeds its latched
     *                            increase factor floor once from the first nonzero value
     * @param linkCapacityBps     the packet pair link capacity estimate, in bits per second; {@code 0}
     *                            when none is available
     * @param nowMs               the monotonic timestamp, in milliseconds, at which this update runs
     * @return the combined, gated target, in bits per second
     */
    public long onFeedback(double plr, long rttNs, long remoteBweBps, long minRemoteBitrateEstimateBps,
                           long linkCapacityBps, long nowMs) {
        var rttMs = rttNs > 0 ? rttNs / 1_000_000 : 0;
        var feedbackAgeMs = lastFeedbackNowMs < 0 ? -1 : nowMs - lastFeedbackNowMs;
        lastFeedbackNowMs = nowMs;
        lastSignals = congestionDetector.evaluate(rttMs, plr, plr, feedbackAgeMs, congestionEnableMask);

        var senderBwe = senderAimd.update(plr, rttNs, remoteBweBps, minRemoteBitrateEstimateBps, feedbackAgeMs);
        var combined = combiner.combine(senderBwe, remoteBweBps, linkCapacityBps, nowMs);

        if (conservativeInit.isActive()) {
            conservativeInit.onInitEstimate(combined);
        }
        combined = Math.clamp(combined, minBitrateBps, maxBitrateBps);

        applyHoldMachine(combined, nowMs);

        targetBps = holdController.heldTargetBps(combined);
        previousTargetBps = combined;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE,
                    "bwe feedback round: senderBwe={0}bps, combined={1}bps, target={2}bps, signals={3}",
                    senderBwe, combined, targetBps, lastSignals);
        }
        return targetBps;
    }

    /**
     * Runs the hold machine for a freshly combined target.
     *
     * <p>A combined value below the previous target is a clamp down: it arms the congestion hold and,
     * when the combiner reports the receive drop condition, the receive drop hold, freezing the target
     * the holds captured. A combined value at or above the previous target ends both holds so the live
     * estimate resumes driving the output.
     *
     * @param combined the freshly combined target, in bits per second
     * @param nowMs    the monotonic timestamp, in milliseconds
     */
    private void applyHoldMachine(long combined, long nowMs) {
        if (combined < previousTargetBps) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "bwe congestion hold armed: previousTarget={0}bps, combined={1}bps",
                        previousTargetBps, combined);
            }
            holdController.startHold(BweHoldReason.CONGESTION, previousTargetBps, nowMs);
            if (combiner.recvDropDetected()) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "bwe receive-drop hold armed: previousTarget={0}bps",
                            previousTargetBps);
                }
                holdController.startHold(BweHoldReason.RECV_DROP, previousTargetBps, nowMs);
            }
        } else {
            holdController.endHold(BweHoldReason.CONGESTION, nowMs);
            holdController.endHold(BweHoldReason.RECV_DROP, nowMs);
        }
    }

    /**
     * Returns the most recent congestion signals from the detector.
     *
     * @return the last congestion signals, or {@link CongestionSignals#NONE} before the first round
     */
    public CongestionSignals lastSignals() {
        return lastSignals;
    }

    /**
     * Returns the hold machine driving the target freeze.
     *
     * @return the hold controller
     */
    public BweHoldController holdController() {
        return holdController;
    }

    /**
     * Returns the combiner fusing the sender, remote, and link capacity estimates.
     *
     * @return the bitrate combiner
     */
    public BitrateCombiner combiner() {
        return combiner;
    }

    /**
     * {@inheritDoc}
     *
     * @return the current combined, gated target, in bits per second
     */
    @Override
    public long currentTargetBps() {
        return targetBps;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation resets the combiner, hold machine, and congestion detector and
     * seeds the sender AIMD estimator again at the current target; the conservative init latch is not
     * armed again because its band is fixed for the call.
     */
    @Override
    public void reset() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "bwe estimator reset: target={0}bps", targetBps);
        }
        senderAimd.reset(targetBps);
        combiner.reset();
        holdController.reset();
        congestionDetector.reset();
        lastSignals = CongestionSignals.NONE;
        previousTargetBps = targetBps;
        lastFeedbackNowMs = -1;
    }
}
