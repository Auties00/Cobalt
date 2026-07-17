package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomUser;

import java.util.List;
import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting the current set of participants waiting in a call's lobby.
 *
 * <p>The engine emits this event with the current {@link #waiting() waiting participants} whenever the
 * lobby membership changes, letting the call host learn who is waiting to be admitted and present an admit
 * or deny choice. An empty list means no participant is currently waiting. The carried
 * {@link WaitingRoomUser} entries are the decoded lobby update the host receives.
 *
 * @param waiting the participants currently waiting in the lobby; never {@code null}, defensively copied
 *                and unmodifiable
 */
public record WaitingRoomStateChanged(List<WaitingRoomUser> waiting) implements ControlCallEvent {
    /**
     * Validates the record components and defensively copies the waiting list into an unmodifiable
     * {@link List}.
     *
     * @throws NullPointerException if {@code waiting} is {@code null} or contains a {@code null} element
     */
    public WaitingRoomStateChanged {
        Objects.requireNonNull(waiting, "waiting cannot be null");
        waiting = List.copyOf(waiting);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#WAITING_ROOM_STATE_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.WAITING_ROOM_STATE_CHANGED;
    }
}
