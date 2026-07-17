package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that a peer's permission to send video changed.
 *
 * <p>In a managed call a peer may be granted or denied permission to turn its camera on. The engine emits
 * this event when that permission changes for a {@link #participant() participant}, carrying the new
 * {@link #allowed() allowed} flag. It is distinct from {@link VideoStateChanged}: this reports whether the
 * peer is permitted to send video, not whether it currently is doing so. Its {@link #type()} is
 * {@link CallEventType#PEER_VIDEO_PERMISSION_CHANGED}.
 *
 * @param participant the device {@link Jid} whose video permission changed; never {@code null}
 * @param allowed     {@code true} when the participant is now permitted to send video
 */
// TODO: model the remaining native payload fields for this event once their byte layout is recovered; only
//  the participant and the permission flag are carried today
public record PeerVideoPermissionChanged(Jid participant, boolean allowed) implements ControlCallEvent {
    /**
     * Validates the record components, rejecting a {@code null} participant.
     *
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public PeerVideoPermissionChanged {
        Objects.requireNonNull(participant, "participant cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#PEER_VIDEO_PERMISSION_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.PEER_VIDEO_PERMISSION_CHANGED;
    }
}
