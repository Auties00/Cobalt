package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that a participant's audio mute state changed.
 *
 * <p>The engine emits this whenever a participant's microphone mute state is observed to change, either
 * the local user toggling their own microphone or an inbound {@code mute_v2} self state report from a
 * peer. It carries the affected {@link #participant() participant} device {@link Jid}, the new
 * {@link #muted() mute state}, and a {@link #self() self} flag that distinguishes the local user's own
 * change from a peer's.
 *
 * @param participant the device {@link Jid} whose mute state changed; never {@code null}
 * @param muted       {@code true} when the participant is now muted, {@code false} when unmuted
 * @param self        {@code true} when the participant is the local user
 */
// TODO: model the full native mute payload; only the participant and mute flag are currently carried
public record MuteStateChanged(Jid participant, boolean muted, boolean self) implements ControlCallEvent {
    /**
     * Constructs a mute state change event, rejecting a {@code null} participant.
     *
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public MuteStateChanged {
        Objects.requireNonNull(participant, "participant cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#MUTE_STATE_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.MUTE_STATE_CHANGED;
    }
}
