package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} reporting that a group call participant raised or lowered a hand.
 *
 * <p>In a group call a participant can raise a hand to request to speak. The engine emits this event when a
 * participant's hand state changes, whether from a local gesture or from an inbound report about another
 * device. It carries the {@link #participant() participant} device {@link Jid}, the new {@link #raised()
 * raised} flag, and a {@link #self() self} flag that marks the local user's own gesture. The raised state
 * feeds the grid ranking so a raised hand sorts ahead of lowered ones.
 *
 * @param participant the device {@link Jid} whose hand state changed; never {@code null}
 * @param raised      {@code true} when the hand is now raised, {@code false} when lowered
 * @param self        {@code true} when the participant is the local user
 */
public record RaiseHandStateChanged(Jid participant, boolean raised, boolean self) implements ControlCallEvent {
    /**
     * Validates the record components, rejecting a {@code null} {@link #participant()}.
     *
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public RaiseHandStateChanged {
        Objects.requireNonNull(participant, "participant cannot be null");
    }

    /**
     * Returns the discriminator identifying this event as a raise hand state change.
     *
     * @return {@link CallEventType#RAISE_HAND_STATE_CHANGED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.RAISE_HAND_STATE_CHANGED;
    }
}
