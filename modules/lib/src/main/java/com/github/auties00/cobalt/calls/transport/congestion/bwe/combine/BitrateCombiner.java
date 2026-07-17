package com.github.auties00.cobalt.calls.transport.congestion.bwe.combine;

import com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping.BweHoldController;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Fuses the sender side estimate, the remote receiver estimate, the packet pair link capacity, and
 * optional machine learning estimates into one combined target bitrate.
 *
 * <p>{@link #combine(long, long, long, long)} selects a base fusion of the sender and remote estimates
 * by the configured {@link CombineMode}, then runs the second stage remote picture in picture combine
 * when {@link CombineMode#REMOTE_PIP} is active. The {@link RemotePipPhase} starts in
 * {@link RemotePipPhase#ENTER} and converts to {@link RemotePipPhase#THROTTLE} once
 * {@link #PHASE_TIME_THRESHOLD_MS} has elapsed or the remote estimate reaches
 * {@link #REMOTE_PIP_ENTER_BPS}. The combiner tracks a receive drop condition (the sender estimate
 * falling below the remote estimate, or the two converging within {@link #RECV_DROP_DELTA_FRACTION})
 * and exposes it so the {@link BweHoldController} can freeze the target, and it blends the result
 * toward the sender estimate with an exponential moving average when the inflection latch is set under
 * {@link CombineMode#EMA_BLEND}.
 *
 * <p>Instances are not thread safe; the owning sender estimator drives one combiner from the single
 * transport thread.
 *
 * @implNote The active probing increase and machine learning inputs are folded in by the caller via
 * {@link #applyProbingIncrease(long, long)}, including the random forced probing path, which is gated
 * on a caller supplied draw.
 */
public final class BitrateCombiner {
    /**
     * The logger for {@link BitrateCombiner}.
     */
    private static final System.Logger LOGGER = Log.get(BitrateCombiner.class);

    /**
     * Remote estimate threshold, in bits per second, at or above which the remote picture in picture
     * phase converts to throttle and the gated average mode engages.
     */
    static final long REMOTE_PIP_ENTER_BPS = 200_001;

    /**
     * Elapsed time, in milliseconds, after which the remote picture in picture phase converts from
     * enter to throttle.
     */
    static final long PHASE_TIME_THRESHOLD_MS = 7_500;

    /**
     * Window, in milliseconds, for the exponential moving average blend and the staleness gate.
     */
    static final long EMA_WINDOW_MS = 30_000;

    /**
     * Fraction of the current estimate within which the sender and remote estimates are treated as
     * converged, arming the receive drop condition.
     */
    static final double RECV_DROP_DELTA_FRACTION = 0.05;

    /**
     * Smoothing factor for the exponential moving average blend toward the sender estimate.
     *
     * <p>The blend is {@code result = combined * (1 - w) + w * sender}; this constant is {@code w}.
     *
     * @implNote This implementation uses {@code 0.5}, the compiled default blend weight for the combine
     * policy. The server pushed voip settings carry no override for this weight, so the compiled default
     * is the operative value.
     */
    static final double EMA_BLEND_COEFFICIENT = 0.5;

    /**
     * The configured fusion strategy.
     */
    private final CombineMode mode;

    /**
     * Current remote picture in picture phase.
     */
    private RemotePipPhase remotePipPhase = RemotePipPhase.ENTER;

    /**
     * Timestamp, in milliseconds, at which the current remote picture in picture phase began, or
     * {@code -1} when no combine has run.
     */
    private long phaseStartMs = -1;

    /**
     * Whether the sender versus remote inflection latch is set.
     *
     * <p>Set once the sender estimate has risen to meet or exceed the remote estimate; gates the
     * floor in {@link CombineMode#MIN_FLOOR} and the blend in {@link CombineMode#EMA_BLEND}.
     */
    private boolean inflectionLatched = false;

    /**
     * Whether the most recent combine observed the receive drop condition.
     */
    private boolean recvDropDetected = false;

    /**
     * Last combined output, in bits per second, used as the prior for the exponential moving average
     * blend.
     */
    private long lastCombinedBps = 0;

    /**
     * Constructs a combiner with the given fusion strategy.
     *
     * @param mode the fusion strategy; never {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public BitrateCombiner(CombineMode mode) {
        if (mode == null) {
            throw new NullPointerException("mode");
        }
        this.mode = mode;
    }

    /**
     * Fuses the sender and remote estimates into one combined target.
     *
     * <p>Computes the base fusion for the configured {@link CombineMode}, runs the remote picture in
     * picture second stage when that mode is active (advancing the {@link RemotePipPhase}), updates the
     * inflection latch and the receive drop condition, applies the exponential moving average blend when
     * the blend mode is latched, and stores the result as the prior for the next call.
     *
     * @param senderBweBps    the sender side estimate, in bits per second
     * @param remoteBweBps    the remote receiver estimate, in bits per second; {@code 0} when none has
     *                        arrived
     * @param linkCapacityBps the packet pair link capacity estimate, in bits per second; {@code 0} when
     *                        none is available
     * @param nowMs           the monotonic timestamp, in milliseconds, at which this combine runs
     * @return the combined target, in bits per second
     */
    public long combine(long senderBweBps, long remoteBweBps, long linkCapacityBps, long nowMs) {
        if (phaseStartMs < 0) {
            phaseStartMs = nowMs;
        }
        updateInflectionLatch(senderBweBps, remoteBweBps);
        updateRecvDrop(senderBweBps, remoteBweBps);

        var base = baseCombine(senderBweBps, remoteBweBps);
        if (mode == CombineMode.REMOTE_PIP) {
            base = remotePipCombine(base, remoteBweBps, linkCapacityBps, nowMs);
        }
        if (mode == CombineMode.EMA_BLEND && inflectionLatched && lastCombinedBps > 0) {
            base = (long) ((1.0 - EMA_BLEND_COEFFICIENT) * base + EMA_BLEND_COEFFICIENT * senderBweBps);
        }
        lastCombinedBps = base;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "bitrate combine: mode={0} sender={1} remote={2} linkCapacity={3} result={4}",
                    mode, senderBweBps, remoteBweBps, linkCapacityBps, base);
        }
        return base;
    }

    /**
     * Computes the base fusion of the sender and remote estimates for the configured mode.
     *
     * <p>{@link CombineMode#MIN}, {@link CombineMode#MAX}, and {@link CombineMode#AVG} are the plain
     * fusions. {@link CombineMode#MIN_FLOOR} takes the minimum but never drops below the sender
     * estimate once the inflection has latched. {@link CombineMode#AVG_GATED} averages only when the
     * remote estimate has reached {@link #REMOTE_PIP_ENTER_BPS} and no inflection has latched.
     * {@link CombineMode#EMA_BLEND} uses the sender estimate as the base before the caller's blend.
     * {@link CombineMode#REMOTE_PIP} passes the sender estimate through to its dedicated second stage.
     * {@link CombineMode#EARLY_CONGESTION} also passes the sender estimate through, because its
     * receiver trusted second stage is not yet implemented (see the TODO below); the live configuration
     * selects {@link CombineMode#MIN_FLOOR}, so this arm is latent.
     *
     * @param senderBweBps the sender side estimate, in bits per second
     * @param remoteBweBps the remote receiver estimate, in bits per second
     * @return the base combined value, in bits per second
     */
    // TODO: implement the CombineMode.EARLY_CONGESTION receiver trusted second stage. That mode manages
    //  the inflection latch from a pair of enable flags, selects toward the remote estimate when early
    //  congestion is latched, and floors at the packet pair link capacity otherwise; the remainder is
    //  the active, random, and mid call probing path with its probe counters and a forced probe RNG.
    //  That probing state is not modeled on BitrateCombiner, so a faithful port needs the probing
    //  controller and its extra config first; until then EARLY_CONGESTION passes the sender estimate
    //  through (the live config selects MIN_FLOOR, so this is latent).
    private long baseCombine(long senderBweBps, long remoteBweBps) {
        return switch (mode) {
            case MIN -> Math.min(senderBweBps, remoteBweBps == 0 ? senderBweBps : remoteBweBps);
            case MAX -> Math.max(senderBweBps, remoteBweBps);
            case AVG -> remoteBweBps == 0 ? senderBweBps : (senderBweBps + remoteBweBps) / 2;
            case MIN_FLOOR -> minFloorCombine(senderBweBps, remoteBweBps);
            case AVG_GATED -> remoteBweBps >= REMOTE_PIP_ENTER_BPS && !inflectionLatched
                    ? (senderBweBps + remoteBweBps) / 2
                    : senderBweBps;
            case EMA_BLEND, EARLY_CONGESTION, REMOTE_PIP -> senderBweBps;
        };
    }

    /**
     * Computes the minimum with floor fusion.
     *
     * <p>Returns the minimum of the sender and remote estimates, except that once the inflection has
     * latched the result is floored at the sender estimate so a transient dip in the remote estimate
     * cannot pull the combined value back down.
     *
     * @param senderBweBps the sender side estimate, in bits per second
     * @param remoteBweBps the remote receiver estimate, in bits per second
     * @return the floored minimum, in bits per second
     */
    private long minFloorCombine(long senderBweBps, long remoteBweBps) {
        if (remoteBweBps == 0) {
            return senderBweBps;
        }
        var min = Math.min(senderBweBps, remoteBweBps);
        return inflectionLatched ? Math.max(min, senderBweBps) : min;
    }

    /**
     * Runs the remote picture in picture second stage combine and advances its phase.
     *
     * <p>While in {@link RemotePipPhase#ENTER} the combined value rises toward the maximum of the base,
     * the remote estimate, and the link capacity, letting the estimate ramp; the phase converts to
     * {@link RemotePipPhase#THROTTLE} once {@link #PHASE_TIME_THRESHOLD_MS} has elapsed or the remote
     * estimate reaches {@link #REMOTE_PIP_ENTER_BPS}. In {@link RemotePipPhase#THROTTLE} the combined
     * value averages the base with the link capacity for a steadier target.
     *
     * @param base            the base combined value, in bits per second
     * @param remoteBweBps    the remote receiver estimate, in bits per second
     * @param linkCapacityBps the packet pair link capacity estimate, in bits per second
     * @param nowMs           the monotonic timestamp, in milliseconds
     * @return the second stage combined value, in bits per second
     */
    private long remotePipCombine(long base, long remoteBweBps, long linkCapacityBps, long nowMs) {
        if (remotePipPhase == RemotePipPhase.ENTER) {
            var elapsed = nowMs - phaseStartMs;
            if (elapsed >= PHASE_TIME_THRESHOLD_MS || remoteBweBps >= REMOTE_PIP_ENTER_BPS) {
                remotePipPhase = RemotePipPhase.THROTTLE;
                phaseStartMs = nowMs;
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "bitrate combine: remote pip phase {0} -> {1}, elapsedMs={2} remote={3}",
                            RemotePipPhase.ENTER, RemotePipPhase.THROTTLE, elapsed, remoteBweBps);
                }
            }
        }
        if (remotePipPhase == RemotePipPhase.ENTER) {
            var ceiling = Math.max(base, Math.max(remoteBweBps, linkCapacityBps));
            return ceiling > 0 ? ceiling : base;
        }
        return linkCapacityBps > 0 ? (base + linkCapacityBps) / 2 : base;
    }

    /**
     * Updates the sender versus remote inflection latch.
     *
     * <p>The latch sets once the sender estimate rises to meet or exceed a nonzero remote estimate,
     * marking that the sender path has caught up to the receiver, and it stays set for the life of the
     * combiner.
     *
     * @param senderBweBps the sender side estimate, in bits per second
     * @param remoteBweBps the remote receiver estimate, in bits per second
     */
    private void updateInflectionLatch(long senderBweBps, long remoteBweBps) {
        if (!inflectionLatched && remoteBweBps > 0 && senderBweBps >= remoteBweBps) {
            inflectionLatched = true;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "bitrate combine: inflection latch set, sender={0} remote={1}",
                        senderBweBps, remoteBweBps);
            }
        }
    }

    /**
     * Updates the receive drop condition for this combine.
     *
     * <p>The condition is set when the sender estimate has fallen below a nonzero remote estimate, or
     * when the two are within {@link #RECV_DROP_DELTA_FRACTION} of the current sender estimate, meaning
     * the sender path is at or below what the receiver reports and the combined target should be frozen
     * rather than ramped.
     *
     * @param senderBweBps the sender side estimate, in bits per second
     * @param remoteBweBps the remote receiver estimate, in bits per second
     */
    private void updateRecvDrop(long senderBweBps, long remoteBweBps) {
        if (remoteBweBps == 0 || senderBweBps == 0) {
            recvDropDetected = false;
            return;
        }
        var delta = Math.abs(senderBweBps - remoteBweBps);
        recvDropDetected = senderBweBps < remoteBweBps
                || delta < senderBweBps * RECV_DROP_DELTA_FRACTION;
    }

    /**
     * Applies an active probing additive increase to a combined target.
     *
     * <p>The increase is a percentage of the supplied video bitrate added to the target, the form used
     * for forced and random probing. The caller decides whether probing is active (including the random
     * forced probe draw) and supplies the already scaled additive increase.
     *
     * @param targetBps        the combined target before the increase, in bits per second
     * @param additiveBitsBps  the additive increase, in bits per second, already scaled to the probe
     *                         percent and video bitrate by the caller
     * @return the target with the probing increase applied, in bits per second
     */
    public long applyProbingIncrease(long targetBps, long additiveBitsBps) {
        if (additiveBitsBps <= 0) {
            return targetBps;
        }
        return targetBps + additiveBitsBps;
    }

    /**
     * Returns whether the most recent combine observed the receive drop condition.
     *
     * @return {@code true} when the sender estimate is at or below the remote estimate within the
     *         receive drop band
     */
    public boolean recvDropDetected() {
        return recvDropDetected;
    }

    /**
     * Returns whether the sender versus remote inflection latch is set.
     *
     * @return {@code true} once the sender estimate has met or exceeded the remote estimate
     */
    public boolean inflectionLatched() {
        return inflectionLatched;
    }

    /**
     * Returns the current remote picture in picture phase.
     *
     * @return the phase, {@link RemotePipPhase#ENTER} until it converts to
     *         {@link RemotePipPhase#THROTTLE}
     */
    public RemotePipPhase remotePipPhase() {
        return remotePipPhase;
    }

    /**
     * Returns the last combined output.
     *
     * @return the last combined target, in bits per second, or {@code 0} before the first combine
     */
    public long lastCombinedBps() {
        return lastCombinedBps;
    }

    /**
     * Resets the combiner to its initial phase and clears the latches and prior.
     */
    public void reset() {
        remotePipPhase = RemotePipPhase.ENTER;
        phaseStartMs = -1;
        inflectionLatched = false;
        recvDropDetected = false;
        lastCombinedBps = 0;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "bitrate combine: reset");
    }
}
