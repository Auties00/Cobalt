package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;

import java.util.Objects;
import com.github.auties00.cobalt.calls.engine.control.WaitingRoomUserState;

/**
 * A {@link ControlCallEvent} reporting that the local user's own state in a call link lobby changed.
 *
 * <p>When the local user joins a call link gated by a waiting room, it sits in the lobby until the host
 * admits it. This event fires each time the local user's own lobby state advances, carrying the new
 * {@link #state() waiting room state}, so a caller can reflect whether the local user is still waiting, has
 * been admitted, or has been turned away.
 *
 * @param state the local user's new lobby state; never {@code null}
 */
public record CallLinkLobbySelfStateChanged(WaitingRoomUserState state) implements ControlCallEvent {
    // TODO: only the typed WaitingRoomUserState is carried; model the remaining fields of the native
    //  CALL_LINK_LOBBY_SELF_STATE_CHANGED payload once their byte layout is recovered.

    /**
     * Validates the record components.
     *
     * @param state the local user's new lobby state
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public CallLinkLobbySelfStateChanged {
        Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#CALL_LINK_LOBBY_SELF_STATE_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.CALL_LINK_LOBBY_SELF_STATE_CHANGED;
    }
}
