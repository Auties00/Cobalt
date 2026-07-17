package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomUser;

import java.util.List;
import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that a waiting room deny request was acknowledged.
 *
 * <p>When the relay acknowledges a host's request to deny waiting room participants, the engine emits this
 * event carrying the {@link #denied() denied participants} that the acknowledgement echoed back, so the host
 * can confirm which queued participants were turned away. The event type is
 * {@link CallEventType#WAITING_ROOM_DENY_ACKED}.
 *
 * @param denied the denied participants echoed by the acknowledgement; never {@code null}, defensively copied
 *               and unmodifiable
 */
public record WaitingRoomDenyAcked(List<WaitingRoomUser> denied) implements ControlCallEvent {
    /**
     * Validates the record component and defensively copies the {@code denied} list into an unmodifiable
     * {@link List}.
     *
     * @throws NullPointerException if {@code denied} is {@code null} or contains a {@code null} element
     */
    public WaitingRoomDenyAcked {
        Objects.requireNonNull(denied, "denied cannot be null");
        denied = List.copyOf(denied);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#WAITING_ROOM_DENY_ACKED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.WAITING_ROOM_DENY_ACKED;
    }
}
