package com.github.auties00.cobalt.calls.signaling.waitingroom;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.NoSuchElementException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <waiting_room_leave>} signal, a joiner withdrawing from the waiting room.
 *
 * <p>This signal is sent by a participant queued in a call link lobby to cancel its pending join
 * before the host admits or denies it. It carries the universal call header and, when the lobby is
 * keyed by a call link, the {@link #linkToken() link token} the participant is leaving. The relay
 * answers with a receipt.
 *
 * <p>On the wire the element is {@snippet lang="xml" :
 * <waiting_room_leave call-id="..." call-creator="..." link-token="..."/>
 * }
 * The element carries no {@code <user>} children, and the {@code link-token} attribute is omitted
 * when no link token is named.
 *
 * @implNote This implementation shares the {@code <waiting_room>} attribute grammar centralized in
 * {@link WaitingRoomStanzas} with the other waiting room signals; the element tag is taken from
 * {@link SignalingType#WAITING_ROOM_LEAVE}.
 *
 * @see SignalingType#WAITING_ROOM_LEAVE
 */
public final class WaitingRoomLeaveStanza implements CallMessage {
    /**
     * The call identifier carried in this signal's {@code call-id} header attribute.
     */
    private final String callId;

    /**
     * The call creator device JID carried in this signal's {@code call-creator} header attribute.
     */
    private final Jid callCreator;

    /**
     * The call link token being left, or {@linkplain Optional#empty() empty} when none is named.
     */
    private final Optional<String> linkToken;

    /**
     * Constructs a waiting room leave signal.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param linkToken   the call link token being left, if present; never {@code null}
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code linkToken} is
     *                              {@code null}
     */
    public WaitingRoomLeaveStanza(String callId, Jid callCreator, Optional<String> linkToken) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(linkToken, "linkToken cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.linkToken = linkToken;
    }

    /**
     * Returns a leave signal that withdraws from the lobby without naming a call link token.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @return the leave signal
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public static WaitingRoomLeaveStanza of(String callId, Jid callCreator) {
        return new WaitingRoomLeaveStanza(callId, callCreator, Optional.empty());
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a waiting room leave
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a waiting room leave
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the call link token being left, if present.
     *
     * @return the call link token, or {@linkplain Optional#empty() empty} when none is named
     */
    public Optional<String> linkToken() {
        return linkToken;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#WAITING_ROOM_LEAVE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.WAITING_ROOM_LEAVE;
    }

    /**
     * Builds the {@code <waiting_room_leave call-id call-creator link-token/>} action stanza.
     *
     * <p>An absent link token is omitted from the element, which carries no {@code <user>} children.
     *
     * @return the waiting room leave action stanza
     */
    @Override
    public Stanza toStanza() {
        return WaitingRoomStanzas.build(type().wireTag().orElseThrow(), callId, callCreator,
                Optional.empty(), linkToken, Optional.empty(), List.of());
    }

    /**
     * Decodes a {@code <waiting_room_leave>} action stanza into a {@link WaitingRoomLeaveStanza}.
     *
     * @param stanza the {@code <waiting_room_leave>} stanza
     * @return the decoded waiting room leave signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomLeaveStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var linkToken = WaitingRoomStanzas.linkToken(stanza);
        return new WaitingRoomLeaveStanza(callId, callCreator, linkToken);
    }

    /**
     * Returns whether {@code obj} is a {@link WaitingRoomLeaveStanza} with the same header and link
     * token.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal waiting room leave
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof WaitingRoomLeaveStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && linkToken.equals(that.linkToken));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this waiting room leave
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, linkToken);
    }

    /**
     * Returns a debug oriented string for this waiting room leave.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "WaitingRoomLeaveStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", linkToken=" + linkToken + ']';
    }
}
