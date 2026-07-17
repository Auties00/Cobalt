package com.github.auties00.cobalt.calls.engine.state;

import java.util.Optional;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.event.CallLifecycleEventSink;

/**
 * Drives a single call's internal state through the voip transition guard.
 *
 * <p>The engine routes every call state change through one chokepoint that enforces which transitions
 * are legal, accounts for the time spent in the active and connected lonely states, and schedules or
 * cancels the connected lonely timer as the call enters or leaves those states. This seam exposes that
 * chokepoint: the {@link LifecycleController} asks for a transition to a new {@link CallLifecycleState},
 * and the implementer applies the guard and reports the prior state when the transition was accepted, or
 * an empty result when the guard rejected it.
 *
 * <p>The guard's rules are the engine's, not the controller's: a transition to the same state leaves the
 * state unchanged, the two in call states {@link CallLifecycleState#CALL_ACTIVE} and
 * {@link CallLifecycleState#CONNECTED_LONELY} may only move to each other or to {@link CallLifecycleState#NONE},
 * and the {@link CallLifecycleState#LINK} and {@link CallLifecycleState#ENDING} transitions are silent. The
 * controller does not reimplement those rules; it only requests the target state and reacts to whether the
 * transition was accepted. This guard never fires an event: firing the {@code call_state_event} after a
 * successful, state changing transition is the controller's responsibility through
 * {@link CallLifecycleEventSink}.
 *
 * <p>This is an internal engine collaborator, not a public surface; embedders never call it directly.
 */
public interface CallStateTransition {
    /**
     * Transitions the identified call to a new internal state through the transition guard.
     *
     * <p>When the transition is accepted this returns the state the call held before the change, so the
     * controller can tell whether the state actually moved: a transition to the current state returns that
     * same state and must fire no event, whereas a move to a different state returns the earlier state and
     * warrants a {@code call_state_event}. When the guard rejects the transition (an illegal successor, or
     * no call exists for the identifier) it returns an empty result and the call's state is unchanged.
     *
     * @implSpec Implementations apply the engine's legal successor rules for the call's current state and
     * must not mutate the call's state when the transition is rejected. Accounting for the time spent in
     * {@link CallLifecycleState#CALL_ACTIVE} and {@link CallLifecycleState#CONNECTED_LONELY} on leaving each
     * of those states, and scheduling or cancelling the connected lonely timer on entering or leaving
     * {@link CallLifecycleState#CONNECTED_LONELY}, are performed here rather than by the caller. This method
     * never posts an event; the caller fires {@code call_state_event} for an accepted, state changing move.
     * @param callId   the identifier of the call to transition
     * @param newState the target internal state
     * @return an {@link Optional} holding the prior state when the transition was accepted, or empty when
     *         the guard rejected it or no call exists for the identifier
     * @throws NullPointerException if {@code callId} or {@code newState} is {@code null}
     */
    Optional<CallLifecycleState> transition(String callId, CallLifecycleState newState);
}
