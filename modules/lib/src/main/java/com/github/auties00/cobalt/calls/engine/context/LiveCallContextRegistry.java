package com.github.auties00.cobalt.calls.engine.context;

import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallStateMachine;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.Call;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Allocates and frees engine call contexts in a {@link CallManager} on behalf of the controller.
 *
 * <p>A context is built from the controller's {@link Call} so it carries the same call id the controller
 * tracks, which lets the {@link CallStateMachine} resolve the very context this registry adopted when the
 * controller later requests a transition by id. The primary slot is filled first; a second concurrent call
 * fills the secondary (dual) slot. The connected lonely timer is armed and cancelled by the lifecycle
 * controller around its state transitions, so this registry wires no timer seams onto the context.
 *
 * @param manager the call manager that holds the at most two call contexts
 */
public record LiveCallContextRegistry(CallManager manager) implements CallContextRegistry {
    /**
     * The logger for {@link LiveCallContextRegistry}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallContextRegistry.class);

    /**
     * Canonicalizes the registry over its manager.
     *
     * @throws NullPointerException if {@code manager} is {@code null}
     */
    public LiveCallContextRegistry {
        Objects.requireNonNull(manager, "manager cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation builds a {@link CallContext} pinned to {@code call}'s call id, then
     * adopts it into the {@link CallManager}'s primary slot, or the secondary slot when a primary call is
     * already live, so the dual call ceiling is the manager's authoritative backstop, and returns the adopted
     * context so the controller can hand it to the outbound call log sink at teardown.
     */
    @Override
    public CallContext allocate(Call call, CallLifecycleState initialState) {
        Objects.requireNonNull(call, "call cannot be null");
        Objects.requireNonNull(initialState, "initialState cannot be null");
        var direction = call.isOutgoing()
                ? CallContext.CallDirection.OUTGOING
                : CallContext.CallDirection.INCOMING;
        var role = manager.hasPrimary()
                ? CallContext.CallRole.SECONDARY
                : CallContext.CallRole.PRIMARY;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "allocating {0} call context for call {1}", role, call.callId());
        var context = new CallContext(call.callId(), role, direction, call.peer(), call.creator(),
                call.creator(), call.chatJid(), call.isGroup(), call.isVideo());
        context.lock().lock();
        try {
            context.state(initialState);
        } finally {
            context.lock().unlock();
        }
        if (role == CallContext.CallRole.SECONDARY) {
            manager.startDualCall(context);
        } else {
            manager.startCall(context);
        }
        return context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "releasing call context {0}", callId);
        manager.endCall(callId);
    }
}
