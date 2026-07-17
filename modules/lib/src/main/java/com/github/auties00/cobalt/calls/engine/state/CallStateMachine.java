package com.github.auties00.cobalt.calls.engine.state;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.engine.event.CallLifecycleEventSink;
import com.github.auties00.cobalt.telemetry.log.Log;

/**
 * Guards every internal state change of a call and implements the {@link CallStateTransition} seam.
 *
 * <p>This is the one chokepoint through which every {@link CallLifecycleState} change for a
 * {@link CallContext} passes. It accepts a transition to the current state as a no op, enforces the two
 * closed in call transition sets ({@link CallLifecycleState#CALL_ACTIVE} only to
 * {@link CallLifecycleState#NONE} or {@link CallLifecycleState#CONNECTED_LONELY}, and
 * {@link CallLifecycleState#CONNECTED_LONELY} only to {@link CallLifecycleState#NONE} or
 * {@link CallLifecycleState#CALL_ACTIVE}), treats transitions into {@link CallLifecycleState#LINK} and
 * {@link CallLifecycleState#ENDING} (and a teardown to {@link CallLifecycleState#NONE} from either) as
 * silent, and otherwise applies the transition, performing the duration accounting and the connected lonely
 * timer scheduling and cancellation attached to entering and leaving the two in call states. An illegal
 * transition is rejected without mutating the context; the guard logs and reports failure rather than
 * forcing the state.
 *
 * <p>The guard is exposed at two levels. The {@link CallStateTransition} seam method
 * {@link #transition(String, CallLifecycleState)} is the cross unit entry the lifecycle controller calls: it
 * resolves the call by id through the injected {@link CallManager}, takes the context's lock, runs the
 * guard, and reports the prior state on acceptance or an empty result on rejection (or when no call exists).
 * The context level {@link #transition(CallContext, CallLifecycleState)} and
 * {@link #transition(CallContext, CallLifecycleState, long)} methods run the guard against an already
 * resolved context and return the richer {@link Transition} record; a caller using them holds the context's
 * {@linkplain CallContext#lock() lock} itself.
 *
 * <p>The {@link Transition} record reports whether the change was accepted, the previous and new states, and
 * whether the caller should fire the listener facing
 * {@linkplain CallEventType#CALL_STATE_CHANGED call state changed} event. The guarded mutation and the emit
 * decision are separated: this guard performs the mutation and returns the emit decision so the lifecycle
 * controller can fire the event (through {@link CallLifecycleEventSink}) after the transition, but it never
 * fires the event itself, because the event bus is owned by a sibling subsystem. The silent
 * {@link CallLifecycleState#LINK} and {@link CallLifecycleState#ENDING} transitions report no event.
 *
 * @implNote This implementation keeps the guarded state mutation and the event emit decision separate:
 * {@link #transition(CallContext, CallLifecycleState, long)} applies the state and folds the emit decision
 * into the returned {@link Transition}, leaving the actual event firing to the lifecycle controller. The
 * guard never touches the call result; that lives in the separate {@link CallContext#result(CallResult)}
 * field and is never conflated with the state. The per participant server state assignment on entering
 * {@link CallLifecycleState#CALL_ACTIVE} is owned by the membership subsystem and is not performed here.
 */
public final class CallStateMachine implements CallStateTransition {
    /**
     * The logger for {@link CallStateMachine}.
     */
    private static final System.Logger LOGGER = Log.get(CallStateMachine.class);

    /**
     * The manager the {@link #transition(String, CallLifecycleState)} seam resolves a call context from.
     */
    private final CallManager manager;

    /**
     * Constructs a state machine that resolves call contexts through the given manager.
     *
     * <p>The guard logic itself holds no per call state; the manager is held only so the
     * {@link CallStateTransition} seam can resolve a context by call id. The context level
     * {@link #transition(CallContext, CallLifecycleState)} overloads do not use the manager and run against
     * the context the caller passes. One instance per engine is sufficient and may be shared across calls.
     *
     * @param manager the manager the call id seam resolves contexts from
     * @throws NullPointerException if {@code manager} is {@code null}
     */
    public CallStateMachine(CallManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the call by id through the {@link CallManager}, takes the resolved context's
     * {@linkplain CallContext#lock() lock}, and runs the guard against it. When no call exists for the id
     * the result is empty; otherwise the guard runs and the result holds the prior state on acceptance
     * (including a no op transition to the current state, which returns the unchanged current state) or is
     * empty when the guard rejected the transition.
     *
     * @param callId   {@inheritDoc}
     * @param newState {@inheritDoc}
     * @return an {@link Optional} holding the prior state when the transition was accepted, or empty when
     *         the guard rejected it or no call exists for the identifier
     * @throws NullPointerException if {@code callId} or {@code newState} is {@code null}
     * @implNote This implementation holds the resolved context's {@link CallContext#lock()} for the whole
     * duration of the guard, serializing concurrent transitions to the same call.
     */
    @Override
    public Optional<CallLifecycleState> transition(String callId, CallLifecycleState newState) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(newState, "newState cannot be null");
        var context = manager.getByCallId(callId).orElse(null);
        if (context == null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "state transition to {0} ignored: no call for id {1}", newState, callId);
            }
            return Optional.empty();
        }
        context.lock().lock();
        try {
            var result = transition(context, newState);
            return result.accepted() ? Optional.of(result.oldState()) : Optional.empty();
        } finally {
            context.lock().unlock();
        }
    }

    /**
     * Attempts to transition a call context to a new state, timestamping any segment effect with the
     * current wall clock time.
     *
     * <p>This is the common entry point; it delegates to {@link #transition(CallContext,
     * CallLifecycleState, long)} with {@link System#currentTimeMillis()} as the segment timestamp. The caller
     * holds the context's {@linkplain CallContext#lock() lock}.
     *
     * @implNote This implementation reads the segment timestamp once from
     * {@link System#currentTimeMillis()} so the closing and opening segments share the same instant.
     *
     * @param context  the call context to transition
     * @param newState the state to transition to
     * @return the {@link Transition} describing the outcome
     * @throws NullPointerException if {@code context} or {@code newState} is {@code null}
     */
    public Transition transition(CallContext context, CallLifecycleState newState) {
        return transition(context, newState, System.currentTimeMillis());
    }

    /**
     * Attempts to transition a call context to a new state, using the given timestamp for any segment
     * effect.
     *
     * <p>The guard applies these rules in order:
     * <ul>
     *   <li>A transition to the current state is a no op success: the context is unchanged and the result
     *       reports the transition was accepted but carries no event.</li>
     *   <li>From {@link CallLifecycleState#CONNECTED_LONELY} only {@link CallLifecycleState#NONE} and
     *       {@link CallLifecycleState#CALL_ACTIVE} are legal; any other target is rejected without
     *       mutation.</li>
     *   <li>From {@link CallLifecycleState#CALL_ACTIVE} only {@link CallLifecycleState#NONE} and
     *       {@link CallLifecycleState#CONNECTED_LONELY} are legal; any other target is rejected without
     *       mutation.</li>
     *   <li>A transition into {@link CallLifecycleState#LINK}, or to {@link CallLifecycleState#NONE} from
     *       {@link CallLifecycleState#LINK}, applies the state silently and reports no event.</li>
     *   <li>A transition into {@link CallLifecycleState#ENDING}, or to {@link CallLifecycleState#NONE} from
     *       {@link CallLifecycleState#ENDING}, applies the state silently and reports no event.</li>
     *   <li>Otherwise the transition is applied with full accounting: leaving
     *       {@link CallLifecycleState#CONNECTED_LONELY} closes the lonely segment and cancels the connected
     *       lonely timer; leaving {@link CallLifecycleState#CALL_ACTIVE} closes the active segment; entering
     *       {@link CallLifecycleState#CONNECTED_LONELY} opens a lonely segment and schedules the connected
     *       lonely timer; entering {@link CallLifecycleState#CALL_ACTIVE} cancels the connected lonely timer
     *       and opens an active segment; and the result reports an event should be fired.</li>
     * </ul>
     *
     * <p>The {@code nowMillis} timestamp is used only to close and open the duration segments, so a caller
     * can supply a deterministic clock in tests. The caller holds the context's
     * {@linkplain CallContext#lock() lock}.
     *
     * @param context   the call context to transition
     * @param newState  the state to transition to
     * @param nowMillis the wall clock millisecond timestamp used for segment accounting
     * @return the {@link Transition} describing the outcome
     * @throws NullPointerException if {@code context} or {@code newState} is {@code null}
     */
    public Transition transition(CallContext context, CallLifecycleState newState, long nowMillis) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(newState, "newState cannot be null");
        var current = context.state();
        if (newState == current) {
            return Transition.noChange(current);
        }
        if (current == CallLifecycleState.CONNECTED_LONELY && !isLegalFromConnectedLonely(newState)) {
            return reject(context, current, newState);
        }
        if (current == CallLifecycleState.CALL_ACTIVE && !isLegalFromCallActive(newState)) {
            return reject(context, current, newState);
        }
        if (isSilentLink(current, newState)) {
            context.state(newState);
            return Transition.silent(current, newState);
        }
        if (isSilentEnding(current, newState)) {
            context.state(newState);
            return Transition.silent(current, newState);
        }
        applyAccounting(context, current, nowMillis);
        context.state(newState);
        applyEntryEffects(context, newState, nowMillis);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "change_call_state call id {0}: [{1} -> {2}]",
                    context.callId(), current, newState);
        }
        return Transition.accepted(current, newState);
    }

    /**
     * Returns whether a target state is a legal successor of {@link CallLifecycleState#CONNECTED_LONELY}.
     *
     * <p>Only {@link CallLifecycleState#NONE} (teardown) and {@link CallLifecycleState#CALL_ACTIVE} (a peer
     * reconnected) are legal successors; the transition set out of the lonely state is closed to any other
     * target.
     *
     * @param newState the candidate target state
     * @return {@code true} if the transition is legal
     */
    private static boolean isLegalFromConnectedLonely(CallLifecycleState newState) {
        return newState == CallLifecycleState.NONE || newState == CallLifecycleState.CALL_ACTIVE;
    }

    /**
     * Returns whether a target state is a legal successor of {@link CallLifecycleState#CALL_ACTIVE}.
     *
     * <p>Only {@link CallLifecycleState#NONE} (teardown) and {@link CallLifecycleState#CONNECTED_LONELY} (the
     * last peer left) are legal successors; the transition set out of the active state is closed to any
     * other target.
     *
     * @param newState the candidate target state
     * @return {@code true} if the transition is legal
     */
    private static boolean isLegalFromCallActive(CallLifecycleState newState) {
        return newState == CallLifecycleState.NONE || newState == CallLifecycleState.CONNECTED_LONELY;
    }

    /**
     * Returns whether a transition is the silent {@link CallLifecycleState#LINK} path.
     *
     * <p>A transition into {@link CallLifecycleState#LINK}, and a teardown to {@link CallLifecycleState#NONE}
     * from {@link CallLifecycleState#LINK}, apply the state without taking the event path.
     *
     * @param current  the current state
     * @param newState the target state
     * @return {@code true} if the transition is a silent link transition
     */
    private static boolean isSilentLink(CallLifecycleState current, CallLifecycleState newState) {
        return newState == CallLifecycleState.LINK
                || (newState == CallLifecycleState.NONE && current == CallLifecycleState.LINK);
    }

    /**
     * Returns whether a transition is the silent {@link CallLifecycleState#ENDING} path.
     *
     * <p>A transition into {@link CallLifecycleState#ENDING}, and a teardown to
     * {@link CallLifecycleState#NONE} from {@link CallLifecycleState#ENDING}, apply the state without taking
     * the event path.
     *
     * @param current  the current state
     * @param newState the target state
     * @return {@code true} if the transition is a silent ending transition
     */
    private static boolean isSilentEnding(CallLifecycleState current, CallLifecycleState newState) {
        return newState == CallLifecycleState.ENDING
                || (newState == CallLifecycleState.NONE && current == CallLifecycleState.ENDING);
    }

    /**
     * Applies the duration accounting and lonely timer cancellation for the state being left.
     *
     * <p>Leaving {@link CallLifecycleState#CONNECTED_LONELY} closes the open lonely segment; leaving
     * {@link CallLifecycleState#CALL_ACTIVE} closes the open active segment. This runs before the state
     * field is written so the segment that is closing is still the current state's segment. The connected
     * lonely timer is armed and cancelled by the lifecycle controller around its transition call, not
     * here, so this guard is a pure segment accountant.
     *
     * @param context   the call context being transitioned
     * @param current   the state being left
     * @param nowMillis the timestamp used to close the leaving segment
     */
    private static void applyAccounting(CallContext context, CallLifecycleState current, long nowMillis) {
        if (current == CallLifecycleState.CONNECTED_LONELY) {
            context.closeLonelySegment(nowMillis);
        } else if (current == CallLifecycleState.CALL_ACTIVE) {
            context.closeActiveSegment(nowMillis);
        }
    }

    /**
     * Applies the segment opening for the state being entered.
     *
     * <p>Entering {@link CallLifecycleState#CONNECTED_LONELY} opens a lonely segment; entering
     * {@link CallLifecycleState#CALL_ACTIVE} opens an active segment. This runs after the state field is
     * written. The connected lonely timer scheduling and cancellation is driven by the lifecycle
     * controller around its transition call, keyed off the prior and new states this transition reports,
     * not by this guard.
     *
     * @param context   the call context being transitioned
     * @param newState  the state being entered
     * @param nowMillis the timestamp used to open the entered segment
     */
    private static void applyEntryEffects(CallContext context, CallLifecycleState newState, long nowMillis) {
        switch (newState) {
            case CONNECTED_LONELY -> context.openLonelySegment(nowMillis);
            case CALL_ACTIVE -> context.openActiveSegment(nowMillis);
            default -> {
                // The other states have no entry effect here; their setup is driven by the lifecycle
                // controller, not by the state guard.
            }
        }
    }

    /**
     * Rejects an illegal transition, logging it and leaving the context unchanged.
     *
     * @param context  the call context whose transition is rejected
     * @param current  the current state
     * @param newState the rejected target state
     * @return a rejected {@link Transition} reporting the unchanged current state
     */
    private static Transition reject(CallContext context, CallLifecycleState current, CallLifecycleState newState) {
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING,
                    "rejecting illegal call state transition for call {0}: [{1} -> {2}]",
                    context.callId(), current, newState);
        }
        return Transition.rejected(current, newState);
    }

    /**
     * Describes the outcome of a state transition attempt.
     *
     * <p>A transition is either accepted (the state changed, or a no op transition to the current state) or
     * rejected (the change was illegal and the context was not mutated). An accepted transition that took
     * the event path reports {@link #shouldEmitEvent()} {@code true} and carries the
     * {@linkplain CallEventType#CALL_STATE_CHANGED call state changed} event the lifecycle controller fires
     * after the transition; a no op transition to the current state and the silent
     * {@link CallLifecycleState#LINK} and {@link CallLifecycleState#ENDING} transitions report {@code false}.
     * The {@link #oldState()} and {@link #newState()} record the transition's endpoints; for a rejected
     * transition {@link #newState()} is the target that was refused and the context remains in
     * {@link #oldState()}.
     *
     * @param accepted        whether the transition was applied (including a no op transition to the current
     *                        state)
     * @param oldState        the state the context was in before the attempt
     * @param newState        the state transitioned to, or the refused target for a rejected transition
     * @param shouldEmitEvent whether the caller should fire the call state changed event
     */
    public record Transition(boolean accepted, CallLifecycleState oldState, CallLifecycleState newState,
                             boolean shouldEmitEvent) {
        /**
         * Canonicalizes the record components.
         *
         * @throws NullPointerException if {@code oldState} or {@code newState} is {@code null}
         */
        public Transition {
            Objects.requireNonNull(oldState, "oldState cannot be null");
            Objects.requireNonNull(newState, "newState cannot be null");
        }

        /**
         * Returns a no op transition outcome for the given unchanged state.
         *
         * <p>The endpoints are both {@code state}, the transition is accepted, and no event is emitted; a
         * transition to the current state succeeds without effect.
         *
         * @param state the unchanged current state
         * @return an accepted transition with equal endpoints carrying no event
         */
        static Transition noChange(CallLifecycleState state) {
            return new Transition(true, state, state, false);
        }

        /**
         * Returns an accepted transition outcome that carries an event.
         *
         * @param oldState the state left
         * @param newState the state entered
         * @return an accepted transition that should fire the call state changed event
         */
        static Transition accepted(CallLifecycleState oldState, CallLifecycleState newState) {
            return new Transition(true, oldState, newState, true);
        }

        /**
         * Returns an accepted but silent transition outcome.
         *
         * <p>The silent {@link CallLifecycleState#LINK} and {@link CallLifecycleState#ENDING} transitions
         * apply the state but report no event.
         *
         * @param oldState the state left
         * @param newState the state entered
         * @return an accepted transition that should not fire an event
         */
        static Transition silent(CallLifecycleState oldState, CallLifecycleState newState) {
            return new Transition(true, oldState, newState, false);
        }

        /**
         * Returns a rejected transition outcome.
         *
         * <p>The context was not mutated and remains in {@code oldState}; {@code newState} records the
         * refused target.
         *
         * @param oldState the unchanged current state
         * @param newState the refused target state
         * @return a rejected transition carrying no event
         */
        static Transition rejected(CallLifecycleState oldState, CallLifecycleState newState) {
            return new Transition(false, oldState, newState, false);
        }

        /**
         * Returns whether this transition actually changed the state.
         *
         * <p>A transition changed the state when it was accepted and its endpoints differ; a no op
         * transition to the current state and a rejected transition both report {@code false}.
         *
         * @return {@code true} when the state changed
         */
        public boolean changedState() {
            return accepted && oldState != newState;
        }

        /**
         * Returns the {@link CallEventType#CALL_STATE_CHANGED} event to fire when this transition took the
         * event path.
         *
         * <p>The result is present only when {@link #shouldEmitEvent()} is {@code true}, and it is always
         * {@link CallEventType#CALL_STATE_CHANGED}; the lifecycle controller fires it on the event bus after
         * the transition. A no op, silent, or rejected transition yields an empty result.
         *
         * @return an {@link Optional} holding the call state changed event, or empty when no event fires
         */
        public Optional<CallEventType> event() {
            return shouldEmitEvent ? Optional.of(CallEventType.CALL_STATE_CHANGED) : Optional.empty();
        }
    }
}
