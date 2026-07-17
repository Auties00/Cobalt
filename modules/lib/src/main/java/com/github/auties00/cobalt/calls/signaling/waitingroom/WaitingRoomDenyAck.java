package com.github.auties00.cobalt.calls.signaling.waitingroom;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents the relay acknowledgement of a {@link WaitingRoomDenyStanza}, confirming which lobby
 * participants were rejected.
 *
 * <p>A waiting room deny ack arrives inside the shared {@code <receipt>} envelope of the stanza layer
 * and echoes back the roster of participants the relay denied entry to the call. It pairs the universal
 * call header ({@code call-id} and {@code call-creator}) with the {@link #users() denied participant
 * list}. It is a read only result model, not a transmittable action, so it does not implement the
 * {@link CallMessage} contract.
 *
 * @param callId      the call identifier; never {@code null}
 * @param callCreator the call creator's device JID; never {@code null}
 * @param users       the denied participants the relay echoed back; never {@code null}, empty when none
 * @see SignalingType#WAITING_ROOM_DENY_ACK
 * @see WaitingRoomDenyStanza
 * @see WaitingRoomUser
 */
public record WaitingRoomDenyAck(String callId, Jid callCreator, List<WaitingRoomUser> users) {
    /**
     * Validates the record components and defensively copies the participant list.
     *
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code users} is
     *                              {@code null}
     */
    public WaitingRoomDenyAck {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(users, "users cannot be null");
        users = List.copyOf(users);
    }

    /**
     * Decodes an echoed waiting room deny stanza into a {@link WaitingRoomDenyAck}.
     *
     * <p>Reads the mandatory {@code call-id} and {@code call-creator} attributes from the stanza header
     * and the denied {@code <user>} entries via {@link WaitingRoomStanzas#users(Stanza)}, each mapped to
     * a {@link WaitingRoomUser}.
     *
     * @param stanza the echoed waiting room deny stanza carried in the {@code <receipt>} body
     * @return the decoded deny ack
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomDenyAck of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var users = WaitingRoomStanzas.users(stanza);
        return new WaitingRoomDenyAck(callId, callCreator, users);
    }
}
