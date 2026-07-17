package com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.EnumMap;
import java.util.Map;

/**
 * Freezes the bandwidth estimate target for a bounded time after specific events to keep the estimate
 * from oscillating, tracking each {@link BweHoldReason} independently behind one shared hold slot.
 *
 * <p>Entering a reason through {@link #startHold(BweHoldReason, long, long)} records the reason as
 * active, stamps its start time, and captures the held target the first time any reason engages. While
 * held, {@link #heldTargetBps(long)} returns the frozen target rather than the estimate the upstream
 * pipeline would produce, until {@link #endHold(BweHoldReason, long)} clears the reason; the slot stays
 * held until the last reason ends. Each reason carries its own start time so the controller reports the
 * duration of each hold and ends them in any order.
 *
 * <p>Instances are not thread safe; the owning sender estimator drives one controller from the single
 * transport thread.
 *
 * @implNote This implementation models the hold slot with a single frozen target and an {@link EnumMap}
 * keyed by {@link BweHoldReason}: a reason present in the map is holding, and its value is the start
 * timestamp. The held state is derived from whether the map is empty rather than kept as a separate
 * flag, and {@link #activeReasonMask()} recomputes the bitmask by folding {@link BweHoldReason#mask()}
 * over the active keys.
 */
public final class BweHoldController {
    /**
     * The logger for {@link BweHoldController}.
     */
    private static final System.Logger LOGGER = Log.get(BweHoldController.class);

    /**
     * Start times, in milliseconds, for the reasons currently holding.
     *
     * <p>A reason present in the map is active; its value is the timestamp at which it engaged, used to
     * report the hold duration when it ends.
     */
    private final Map<BweHoldReason, Long> reasonStartMs = new EnumMap<>(BweHoldReason.class);

    /**
     * The target, in bits per second, frozen when the first reason engaged.
     *
     * <p>Captured once on entry and returned by {@link #heldTargetBps(long)} for as long as any reason
     * holds.
     */
    private long heldTargetBps = 0;

    /**
     * Constructs a hold controller with no active holds.
     */
    public BweHoldController() {
    }

    /**
     * Enters a hold for the given reason, freezing the supplied target when no reason was already
     * active.
     *
     * <p>Records the reason as active and stamps its start time. When this is the first active reason
     * the supplied target is captured as the frozen target; a reason that is already active has its
     * start time left unchanged so a repeated entry does not extend its measured duration.
     *
     * @param reason    the reason to enter; never {@code null}
     * @param targetBps the current target to freeze, in bits per second
     * @param nowMs     the monotonic timestamp, in milliseconds, at which the hold begins
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    public void startHold(BweHoldReason reason, long targetBps, long nowMs) {
        if (reason == null) {
            throw new NullPointerException("reason");
        }
        if (reasonStartMs.isEmpty()) {
            heldTargetBps = targetBps;
        }
        var alreadyActive = reasonStartMs.putIfAbsent(reason, nowMs) != null;
        if (Log.DEBUG && !alreadyActive) {
            LOGGER.log(Level.DEBUG, "bwe hold started: reason={0} target={1}bps", reason, targetBps);
        }
    }

    /**
     * Ends a hold for the given reason and returns how long it held.
     *
     * <p>Removes the reason from the active set; the hold slot stays held until the last reason ends.
     * A reason that was not active returns {@code -1}.
     *
     * @param reason the reason to end; never {@code null}
     * @param nowMs  the monotonic timestamp, in milliseconds, at which the hold ends
     * @return the elapsed hold duration, in milliseconds, or {@code -1} when the reason was not active
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    public long endHold(BweHoldReason reason, long nowMs) {
        if (reason == null) {
            throw new NullPointerException("reason");
        }
        var start = reasonStartMs.remove(reason);
        if (start == null) {
            return -1;
        }
        var durationMs = nowMs - start;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "bwe hold ended: reason={0} duration={1}ms", reason, durationMs);
        }
        return durationMs;
    }

    /**
     * Returns whether any reason is currently holding.
     *
     * @return {@code true} when at least one reason is active
     */
    public boolean isHeld() {
        return !reasonStartMs.isEmpty();
    }

    /**
     * Returns whether the given reason is currently holding.
     *
     * @param reason the reason to test; never {@code null}
     * @return {@code true} when the reason is active
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    public boolean isHeld(BweHoldReason reason) {
        if (reason == null) {
            throw new NullPointerException("reason");
        }
        return reasonStartMs.containsKey(reason);
    }

    /**
     * Returns the combined bitmask of the reasons currently holding.
     *
     * <p>Each active reason contributes its {@link BweHoldReason#mask()} bit.
     *
     * @return the active reason bitmask, or {@code 0} when nothing holds
     */
    public int activeReasonMask() {
        var mask = 0;
        for (var reason : reasonStartMs.keySet()) {
            mask |= reason.mask();
        }
        return mask;
    }

    /**
     * Returns the target to emit while held, or the supplied live target when nothing holds.
     *
     * <p>While any reason is active this returns the frozen target captured on entry; once the last
     * reason has ended it returns the live target unchanged so the upstream estimate resumes driving
     * the output.
     *
     * @param liveTargetBps the target the upstream pipeline would emit, in bits per second
     * @return the frozen target while held, otherwise {@code liveTargetBps}
     */
    public long heldTargetBps(long liveTargetBps) {
        return isHeld() ? heldTargetBps : liveTargetBps;
    }

    /**
     * Clears all active holds and the frozen target.
     */
    public void reset() {
        reasonStartMs.clear();
        heldTargetBps = 0;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "bwe hold controller reset");
        }
    }
}
