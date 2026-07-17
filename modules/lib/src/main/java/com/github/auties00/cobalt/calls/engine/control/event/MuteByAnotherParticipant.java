package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that another participant requested the local user to mute.
 *
 * <p>In a group call an admin can ask a participant to mute itself. The engine emits this event when
 * such a request arrives addressed to the local user, carrying the device {@link Jid} of the
 * {@link #requester() requesting participant}. The host typically surfaces a prompt or mutes
 * automatically; acting on the request mutes the local microphone and emits a {@link MuteStateChanged}.
 * This event is the inbound request from a peer, distinct from the local user muting itself, which is
 * the resulting state change.
 *
 * @param requester the device {@link Jid} of the participant that requested the local user to mute;
 *                  never {@code null}
 */
// TODO: carry the full inbound mute request payload once its byte layout is recovered; only the
//  requesting participant is modelled today
public record MuteByAnotherParticipant(Jid requester) implements ControlCallEvent {
    /**
     * Constructs a {@link MuteByAnotherParticipant} and rejects a {@code null} {@link #requester()}.
     *
     * @throws NullPointerException if {@code requester} is {@code null}
     */
    public MuteByAnotherParticipant {
        Objects.requireNonNull(requester, "requester cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#MUTE_BY_ANOTHER_PARTICIPANT}
     */
    @Override
    public CallEventType type() {
        return CallEventType.MUTE_BY_ANOTHER_PARTICIPANT;
    }
}
