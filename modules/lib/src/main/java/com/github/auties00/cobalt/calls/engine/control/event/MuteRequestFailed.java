package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that a peer mute request the local user sent failed to be confirmed.
 *
 * <p>When the local user, acting as an admin, asks a participant to mute, the engine retries the request and,
 * if it exhausts its retry budget without ever being confirmed, emits this failure carrying the
 * {@link #target() target participant} the request was addressed to. The host can surface that the request did
 * not take effect. This is the failure counterpart of the outbound peer mute request, distinct from the
 * inbound {@link MuteByAnotherParticipant} and from the resulting {@link MuteStateChanged}.
 *
 * @param target the device JID of the participant the failed mute request was addressed to; never
 *               {@code null}
 */
public record MuteRequestFailed(Jid target) implements ControlCallEvent {
    // TODO: carry the remaining fields of the mute request failed payload once its full byte layout is known;
    //  only the target participant is modelled today.

    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public MuteRequestFailed {
        Objects.requireNonNull(target, "target cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#MUTE_REQUEST_FAILED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.MUTE_REQUEST_FAILED;
    }
}
