package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomUser;

import java.util.List;
import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that a waiting room admit request was acknowledged.
 *
 * <p>When the relay acknowledges a host's request to admit waiting room participants, the engine emits this
 * event carrying the {@link #admitted() admitted participants} the acknowledgement echoed back, so the host
 * can confirm which queued participants were released into the call. An empty list reflects the admit all
 * action, where the host released every queued participant rather than a named subset.
 *
 * @param admitted the admitted participants the acknowledgement echoed; never {@code null}, defensively
 *                 copied and unmodifiable
 */
public record WaitingRoomAdmitAcked(List<WaitingRoomUser> admitted) implements ControlCallEvent {
    /**
     * Validates the record components and defensively copies the admitted list.
     *
     * @throws NullPointerException if {@code admitted} is {@code null} or contains a {@code null} element
     */
    // TODO: carry the full native admit ack payload once its byte layout is recovered; only the echoed WaitingRoomUser entries are modelled today
    public WaitingRoomAdmitAcked {
        Objects.requireNonNull(admitted, "admitted cannot be null");
        admitted = List.copyOf(admitted);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#WAITING_ROOM_ADMIT_ACKED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.WAITING_ROOM_ADMIT_ACKED;
    }
}
