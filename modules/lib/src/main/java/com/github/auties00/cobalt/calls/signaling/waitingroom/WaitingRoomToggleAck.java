package com.github.auties00.cobalt.calls.signaling.waitingroom;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents the acknowledgement of a {@link WaitingRoomToggleStanza} confirming the applied gate state.
 *
 * <p>A waiting room toggle ack is delivered inside the shared {@code <receipt>} envelope and confirms the
 * gate state that took effect. It carries the universal call header ({@link #callId()} and
 * {@link #callCreator()}) and, when the relay echoes it, the resulting {@link #enabled() gate state}. The
 * gate state is optional: an ack that omits it yields an empty {@link #enabled()}. This is a result model
 * produced by parsing, not a transmittable action, so it implements no {@link CallMessage} contract.
 *
 * @param callId      the call identifier; never {@code null}
 * @param callCreator the call creator's device JID; never {@code null}
 * @param enabled     the applied gate state, present only when the ack echoed it
 * @see SignalingType#WAITING_ROOM_TOGGLE_ACK
 * @see WaitingRoomToggleStanza
 */
public record WaitingRoomToggleAck(String callId, Jid callCreator, Optional<Boolean> enabled) {
    /**
     * Validates that every record component is non-{@code null}.
     *
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code enabled} is
     *                              {@code null}
     */
    public WaitingRoomToggleAck {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(enabled, "enabled cannot be null");
    }

    /**
     * Decodes a waiting room toggle ack stanza into a {@link WaitingRoomToggleAck}.
     *
     * <p>Reads the required {@code call-id} and {@code call-creator} attributes from the stanza header and
     * resolves the optional gate state through {@link WaitingRoomStanzas#enabled(Stanza)}, which is empty
     * when the ack omits it.
     *
     * @param stanza the echoed waiting room toggle stanza from the {@code <receipt>} body
     * @return the decoded toggle ack
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomToggleAck of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var enabled = WaitingRoomStanzas.enabled(stanza);
        return new WaitingRoomToggleAck(callId, callCreator, enabled);
    }
}
