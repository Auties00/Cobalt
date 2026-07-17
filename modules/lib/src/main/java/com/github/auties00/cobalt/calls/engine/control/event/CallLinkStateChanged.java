package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.state.CallLinkState;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that the call link join state advanced.
 *
 * <p>While joining a call through a call link, the engine advances a join state through the query and join
 * legs. It emits this event on each transition, carrying the new {@link #state() link state}, so the host
 * can reflect the join progress. The event type is {@link CallEventType#CALL_LINK_STATE_CHANGED}.
 *
 * @param state the new call link join state; never {@code null}
 */
public record CallLinkStateChanged(CallLinkState state) implements ControlCallEvent {
    /**
     * Constructs the event, rejecting a {@code null} state.
     *
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public CallLinkStateChanged {
        Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#CALL_LINK_STATE_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.CALL_LINK_STATE_CHANGED;
    }
}
