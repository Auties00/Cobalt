package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.participant.VideoStreamState;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that a participant's video stream state changed.
 *
 * <p>The engine emits this whenever a participant's {@link VideoStreamState} changes: the camera on and off
 * lifecycle and every step of the video upgrade negotiation (request, accept, reject, cancel, and their
 * timeout variants). It carries the affected {@link #participant() participant} device JID, the new
 * {@link #state() video state}, and a {@link #self() self} flag distinguishing the local user's own change
 * from a peer's. The transition is driven both by a local camera or upgrade action and by an inbound
 * {@code video_state} report from a peer, and every case surfaces through this single event carrying the
 * {@link CallEventType#VIDEO_STATE_CHANGED} type.
 *
 * @param participant the device JID whose video state changed; never {@code null}
 * @param state       the new video stream state; never {@code null}
 * @param self        {@code true} when the participant is the local user
 */
public record VideoStateChanged(Jid participant, VideoStreamState state, boolean self) implements ControlCallEvent {
    // TODO: model the full native VIDEO_STATE_CHANGED payload; only participant, state, and self are carried today
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code participant} or {@code state} is {@code null}
     */
    public VideoStateChanged {
        Objects.requireNonNull(participant, "participant cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#VIDEO_STATE_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.VIDEO_STATE_CHANGED;
    }
}
