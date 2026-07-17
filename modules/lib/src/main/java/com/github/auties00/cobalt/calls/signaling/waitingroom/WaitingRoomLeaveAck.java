package com.github.auties00.cobalt.calls.signaling.waitingroom;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.NoSuchElementException;
import java.util.Objects;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents the relay's acknowledgement of a {@link WaitingRoomLeaveStanza}, confirming a withdrawal.
 *
 * <p>A waiting room leave ack arrives inside the shared {@code <receipt>} envelope and confirms that the
 * participant has withdrawn from a call's lobby. It carries only the universal call header identifying
 * which call's lobby was left: the {@code call-id} attribute and the {@code call-creator} device JID. It
 * is a parse only result model, not a transmittable action, so it implements no {@link CallMessage}
 * contract.
 *
 * @param callId      the call identifier; never {@code null}
 * @param callCreator the call creator's device JID; never {@code null}
 * @see SignalingType#WAITING_ROOM_LEAVE_ACK
 * @see WaitingRoomLeaveStanza
 */
public record WaitingRoomLeaveAck(String callId, Jid callCreator) {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public WaitingRoomLeaveAck {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
    }

    /**
     * Decodes a waiting room leave ack stanza into a {@link WaitingRoomLeaveAck}.
     *
     * <p>Reads the required {@code call-id} and {@code call-creator} attributes from the echoed stanza and
     * returns the corresponding record.
     *
     * @param stanza the echoed waiting room leave stanza from the {@code <receipt>} body
     * @return the decoded leave ack
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomLeaveAck of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        return new WaitingRoomLeaveAck(callId, callCreator);
    }
}
