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
 * Represents a {@code <waiting_room_admit>} signal by which the host admits waiting room participants.
 *
 * <p>A waiting room admit is sent by the call host to release one or more queued participants from the
 * lobby into the active call. It carries the universal call header and the {@link #users() participant
 * list} to admit; an empty list expresses the admit all action the host issues to release every pending
 * participant at once. The relay answers with a waiting room admit receipt. The host learns which
 * participants are waiting from the inbound waiting room state events before issuing this admit.
 *
 * <p>On the wire the element is:
 * {@snippet lang="xml" :
 * <waiting_room_admit call-id="..." call-creator="...">
 *   <user jid="..."/>
 *   <!-- zero or more <user> children -->
 * </waiting_room_admit>
 * }
 *
 * @implNote This implementation shares the {@code <waiting_room>} grammar centralized in
 * {@link WaitingRoomStanzas}; the element tag is taken from {@link SignalingType#WAITING_ROOM_ADMIT} and
 * each admitted participant is a {@code <user>} entry decoded by {@link WaitingRoomUser}.
 *
 * @see SignalingType#WAITING_ROOM_ADMIT
 * @see WaitingRoomUser
 */
public final class WaitingRoomAdmitStanza implements CallMessage {
    /**
     * The call identifier carried by this admit's {@code call-id} header.
     */
    private final String callId;

    /**
     * The call creator device JID carried by this admit's {@code call-creator} header.
     */
    private final Jid callCreator;

    /**
     * The participants to admit; never {@code null}, empty for the admit all action.
     */
    private final List<WaitingRoomUser> users;

    /**
     * Constructs a waiting room admit signal, defensively copying the participant list.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param users       the participants to admit; never {@code null}, empty for the admit all action
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code users} is
     *                              {@code null}
     */
    public WaitingRoomAdmitStanza(String callId, Jid callCreator, List<WaitingRoomUser> users) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(users, "users cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.users = List.copyOf(users);
    }

    /**
     * Returns an admit signal expressing the admit all action with no explicit participants.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @return the admit all signal
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public static WaitingRoomAdmitStanza all(String callId, Jid callCreator) {
        return new WaitingRoomAdmitStanza(callId, callCreator, List.of());
    }

    /**
     * Returns an admit signal targeting the single participant with the given JID.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param userJid     the participant to admit
     * @return the admit signal
     * @throws NullPointerException if any argument is {@code null}
     */
    public static WaitingRoomAdmitStanza of(String callId, Jid callCreator, Jid userJid) {
        return new WaitingRoomAdmitStanza(callId, callCreator, List.of(WaitingRoomUser.of(userJid)));
    }

    /**
     * Returns whether this signal expresses the admit all action.
     *
     * @return {@code true} when the participant list is empty; {@code false} otherwise
     */
    public boolean isAdmitAll() {
        return users.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a waiting room admit
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a waiting room admit
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the participants to admit.
     *
     * @return the participant list; never {@code null}, empty for the admit all action
     */
    public List<WaitingRoomUser> users() {
        return users;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#WAITING_ROOM_ADMIT}
     */
    @Override
    public SignalingType type() {
        return SignalingType.WAITING_ROOM_ADMIT;
    }

    /**
     * Builds the {@code <waiting_room_admit call-id call-creator><user/>*</waiting_room_admit>} action
     * stanza.
     *
     * <p>The element carries one {@code <user>} child per admitted participant, or none for the
     * admit all action.
     *
     * @return the waiting room admit action stanza
     */
    @Override
    public Stanza toStanza() {
        return WaitingRoomStanzas.build(type().wireTag().orElseThrow(), callId, callCreator,
                Optional.empty(), Optional.empty(), Optional.empty(), users);
    }

    /**
     * Decodes a {@code <waiting_room_admit>} action stanza into a {@link WaitingRoomAdmitStanza}.
     *
     * <p>An absent {@code <user>} list classifies to the admit all action.
     *
     * @param stanza the {@code <waiting_room_admit>} stanza
     * @return the decoded waiting room admit signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomAdmitStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var users = WaitingRoomStanzas.users(stanza);
        return new WaitingRoomAdmitStanza(callId, callCreator, users);
    }

    /**
     * Returns whether {@code obj} is a {@link WaitingRoomAdmitStanza} with the same header and
     * participant list.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal waiting room admit
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof WaitingRoomAdmitStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && users.equals(that.users));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this waiting room admit
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, users);
    }

    /**
     * Returns a debug oriented string for this waiting room admit.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "WaitingRoomAdmitStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", users=" + users + ']';
    }
}
