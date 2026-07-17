package com.github.auties00.cobalt.calls.signaling.waitingroom;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <waiting_room_deny>} signal, the host denying waiting room participants.
 *
 * <p>A waiting room deny is sent by the call host to reject one or more queued participants, removing
 * them from the lobby without admitting them to the call. It carries the universal call header and the
 * {@link #users() participant list} to deny; an empty list expresses a deny all action. The relay answers
 * on a waiting room deny receipt.
 *
 * <p>The host learns which participants are waiting from the inbound waiting room state events before
 * issuing this deny.
 *
 * <p>On the wire the element is:
 * {@snippet lang="xml" :
 * <waiting_room_deny call-id="..." call-creator="...">
 *   <user jid="..."/>
 *   ...
 * </waiting_room_deny>
 * }
 * where the tag is taken from {@link SignalingType#WAITING_ROOM_DENY}, the {@code <waiting_room>} grammar
 * is centralized in {@link WaitingRoomStanzas}, and each denied participant is a {@code <user>} entry
 * modeled by {@link WaitingRoomUser}.
 *
 * @see SignalingType#WAITING_ROOM_DENY
 * @see WaitingRoomUser
 */
public final class WaitingRoomDenyStanza implements CallMessage {
    /**
     * The call identifier carried in this deny's {@code call-id} header.
     */
    private final String callId;

    /**
     * The call creator device {@link Jid} carried in this deny's {@code call-creator} header.
     */
    private final Jid callCreator;

    /**
     * The participants to deny; never {@code null}, empty for the deny all action.
     */
    private final List<WaitingRoomUser> users;

    /**
     * Constructs a waiting room deny signal, defensively copying the participant list.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device {@link Jid}; never {@code null}
     * @param users       the participants to deny; never {@code null}, empty for the deny all action
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code users} is
     *                              {@code null}
     */
    public WaitingRoomDenyStanza(String callId, Jid callCreator, List<WaitingRoomUser> users) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(users, "users cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.users = List.copyOf(users);
    }

    /**
     * Returns a deny signal targeting the single participant with the given {@link Jid}.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device {@link Jid}
     * @param userJid     the participant to deny
     * @return the deny signal
     * @throws NullPointerException if any argument is {@code null}
     */
    public static WaitingRoomDenyStanza of(String callId, Jid callCreator, Jid userJid) {
        return new WaitingRoomDenyStanza(callId, callCreator, List.of(WaitingRoomUser.of(userJid)));
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a waiting room deny
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device {@link Jid}, always present for a waiting room deny
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the participants to deny.
     *
     * @return the participant list; never {@code null}, empty for the deny all action
     */
    public List<WaitingRoomUser> users() {
        return users;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#WAITING_ROOM_DENY}
     */
    @Override
    public SignalingType type() {
        return SignalingType.WAITING_ROOM_DENY;
    }

    /**
     * Builds the {@code <waiting_room_deny call-id call-creator><user/>*</waiting_room_deny>} action
     * stanza.
     *
     * <p>The element carries one {@code <user>} child per denied participant.
     *
     * @return the waiting room deny action stanza
     */
    @Override
    public Stanza toStanza() {
        return WaitingRoomStanzas.build(type().wireTag().orElseThrow(), callId, callCreator,
                Optional.empty(), Optional.empty(), Optional.empty(), users);
    }

    /**
     * Decodes a {@code <waiting_room_deny>} action stanza into a {@link WaitingRoomDenyStanza}.
     *
     * @param stanza the {@code <waiting_room_deny>} stanza
     * @return the decoded waiting room deny signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomDenyStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var users = WaitingRoomStanzas.users(stanza);
        return new WaitingRoomDenyStanza(callId, callCreator, users);
    }

    /**
     * Returns whether {@code obj} is a {@link WaitingRoomDenyStanza} with the same header and
     * participant list.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal waiting room deny
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof WaitingRoomDenyStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && users.equals(that.users));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this waiting room deny
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, users);
    }

    /**
     * Returns a debug oriented string for this waiting room deny.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "WaitingRoomDenyStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", users=" + users + ']';
    }
}
