package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;

/**
 * A {@link ControlCallEvent} reporting that the local user's waiting room admission was denied.
 *
 * <p>When the local user is waiting in a call link lobby and the host denies admission, the engine emits
 * this event so the host can inform the user that it will not be joining. The event carries no further
 * detail: the denial is terminal for the current join attempt, and there is no payload beyond the fact of
 * the denial, so this is a marker event whose {@link #type()} is {@link CallEventType#WAITING_ROOM_DENIED}.
 */
public record WaitingRoomDenied() implements ControlCallEvent {
    /**
     * Returns the discriminator identifying this event as a waiting room denial.
     *
     * @return {@link CallEventType#WAITING_ROOM_DENIED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.WAITING_ROOM_DENIED;
    }
}
