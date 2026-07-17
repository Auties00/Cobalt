package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;

/**
 * Reports that a host's request to toggle the call's waiting room was acknowledged.
 *
 * <p>A {@link ControlCallEvent} the engine emits after the relay acknowledges a host request to enable or
 * disable the call's waiting room. It carries the {@link #enabled() applied setting}, letting the host
 * confirm whether the waiting room is now on or off. Its {@linkplain #type() event type} is always
 * {@link CallEventType#WAITING_ROOM_TOGGLE_ACKED}.
 *
 * @param enabled {@code true} when the waiting room is now enabled, {@code false} when disabled
 */
// TODO: carry the full acknowledgement payload once its byte layout is recovered; only the applied enabled flag is decoded today.
public record WaitingRoomToggleAcked(boolean enabled) implements ControlCallEvent {
    /**
     * Returns {@link CallEventType#WAITING_ROOM_TOGGLE_ACKED}, the type identifying this control event.
     *
     * @return {@link CallEventType#WAITING_ROOM_TOGGLE_ACKED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.WAITING_ROOM_TOGGLE_ACKED;
    }
}
