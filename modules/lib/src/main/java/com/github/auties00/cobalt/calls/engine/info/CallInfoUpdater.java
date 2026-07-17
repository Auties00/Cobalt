package com.github.auties00.cobalt.calls.engine.info;
import com.github.auties00.cobalt.calls.telemetry.CallResult;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;

/**
 * Folds a lifecycle event into a call's periodically refreshed info snapshot, mapping it to the result
 * and log state fields.
 *
 * <p>The engine keeps a periodically refreshed immutable snapshot of each call's info (its durations, its
 * result, its link info) for the host and for field stats, and updates that snapshot's result and log
 * state fields from the call events as they occur. This seam exposes that update: the
 * {@link LifecycleController} reports each lifecycle {@link CallEventType} it raises, and the implementer
 * maps the event to the call's {@link CallResult} and call log state, then refreshes the snapshot. The
 * mapping belongs to the engine, not to the controller; the controller only reports the event and lets
 * the info manager record its effect on the result.
 *
 * @apiNote This is an internal engine collaborator, not a public surface; embedders never call it. The
 * snapshot the implementer maintains is read by listeners and by the end of call telemetry, not through
 * this seam.
 */
@FunctionalInterface
public interface CallInfoUpdater {
    /**
     * Records the effect of one lifecycle event on the identified call's result and refreshes its info
     * snapshot.
     *
     * <p>The implementer maps the event to the call's result and log state, then re snapshots the call
     * info. An event that does not affect the result leaves it unchanged but may still refresh the
     * snapshot's durations; an unknown call identifier is a no op.
     *
     * @implSpec Implementations must treat an unrecognized {@code callId} as a no op and must not alter
     * the result for an event that carries no result mapping, though they may still update the snapshot's
     * durations for such an event.
     * @param callId    the identifier of the call the event belongs to
     * @param eventType the lifecycle event to fold into the call's result
     * @throws NullPointerException if {@code callId} or {@code eventType} is {@code null}
     */
    void updateForEvent(String callId, CallEventType eventType);
}
