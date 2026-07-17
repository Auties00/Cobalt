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
 * Represents a {@code <waiting_room_update>} signal, a refresh of the full waiting room state.
 *
 * <p>A waiting room update announces the current lobby state: the {@link #enabled() gate flag}, whether
 * the receiving device {@link #admin() is the admin}, the optional {@link #linkToken() call link token}
 * the lobby is keyed by, and the {@link #users() roster} of participants currently waiting. It is the
 * message a host receives to learn who is queued, driving the waiting room state change surfaced to the
 * application; it is also the snapshot pushed when the lobby composition changes.
 *
 * <p>On the wire the element is {@snippet lang = xml :
 * <waiting_room_update call-id="..." call-creator="..." enabled="1" is_admin="1" link-token="...">
 *     <user .../>
 *     <!-- one <user> per waiting participant -->
 * </waiting_room_update>
 *}
 *
 * <p>The element tag is taken from {@link SignalingType#WAITING_ROOM_UPDATE}, the shared attribute grammar
 * ({@code enabled}, {@code is_admin}, {@code link-token}) lives in {@link WaitingRoomStanzas}, and each
 * waiting participant is a {@code <user>} entry decoded by {@link WaitingRoomUser}.
 *
 * @see SignalingType#WAITING_ROOM_UPDATE
 * @see WaitingRoomUser
 */
public final class WaitingRoomUpdateStanza implements CallMessage {
    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * The gate state, present only when the update carried it.
     */
    private final Optional<Boolean> enabled;

    /**
     * Whether the receiving device is the admin, present only when the update carried it.
     */
    private final Optional<Boolean> admin;

    /**
     * The call link token the lobby is keyed by, if present.
     */
    private final Optional<String> linkToken;

    /**
     * The waiting participants; never {@code null}, empty when none are queued.
     */
    private final List<WaitingRoomUser> users;

    /**
     * Constructs a waiting room update signal, defensively copying the participant list.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param enabled     the gate state, present only when the update carried it
     * @param admin       whether the receiving device is the admin, present only when the update carried it
     * @param linkToken   the call link token the lobby is keyed by, if present
     * @param users       the waiting participants; never {@code null}, empty when none are queued
     * @throws NullPointerException if any argument is {@code null}
     */
    public WaitingRoomUpdateStanza(String callId,
                                   Jid callCreator,
                                   Optional<Boolean> enabled,
                                   Optional<Boolean> admin,
                                   Optional<String> linkToken,
                                   List<WaitingRoomUser> users) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(enabled, "enabled cannot be null");
        Objects.requireNonNull(admin, "admin cannot be null");
        Objects.requireNonNull(linkToken, "linkToken cannot be null");
        Objects.requireNonNull(users, "users cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.enabled = enabled;
        this.admin = admin;
        this.linkToken = linkToken;
        this.users = List.copyOf(users);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a waiting room update
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a waiting room update
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the gate state, present only when the update carried it.
     *
     * @return the gate state, or empty when the update did not carry it
     */
    public Optional<Boolean> enabled() {
        return enabled;
    }

    /**
     * Returns whether the receiving device is the admin, present only when the update carried it.
     *
     * @return the admin flag, or empty when the update did not carry it
     */
    public Optional<Boolean> admin() {
        return admin;
    }

    /**
     * Returns the call link token the lobby is keyed by, if present.
     *
     * @return the call link token, or empty when absent
     */
    public Optional<String> linkToken() {
        return linkToken;
    }

    /**
     * Returns the waiting participants.
     *
     * @return the waiting participants; never {@code null}, empty when none are queued
     */
    public List<WaitingRoomUser> users() {
        return users;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#WAITING_ROOM_UPDATE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.WAITING_ROOM_UPDATE;
    }

    /**
     * Builds the {@code <waiting_room_update call-id call-creator enabled is_admin link-token><user/>*
     * </waiting_room_update>} action stanza.
     *
     * <p>Each optional flag and the link token are omitted when their backing component is absent; the
     * element carries one {@code <user>} child per waiting participant.
     *
     * @return the waiting room update action stanza
     */
    @Override
    public Stanza toStanza() {
        return WaitingRoomStanzas.build(type().wireTag().orElseThrow(), callId, callCreator,
                enabled, linkToken, admin, users);
    }

    /**
     * Decodes a {@code <waiting_room_update>} action stanza into a {@link WaitingRoomUpdateStanza}.
     *
     * <p>Absent {@code enabled} and {@code is_admin} attributes yield empty optionals so that emitting the
     * update again preserves which flags arrived; every nested {@code <user>} child forms the waiting roster.
     *
     * @param stanza the {@code <waiting_room_update>} stanza
     * @return the decoded waiting room update signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomUpdateStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var enabled = WaitingRoomStanzas.enabled(stanza);
        var admin = WaitingRoomStanzas.admin(stanza);
        var linkToken = WaitingRoomStanzas.linkToken(stanza);
        var users = WaitingRoomStanzas.users(stanza);
        return new WaitingRoomUpdateStanza(callId, callCreator, enabled, admin, linkToken, users);
    }

    /**
     * Returns whether {@code obj} is a {@link WaitingRoomUpdateStanza} equal to this one by value.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a waiting room update equal to this one field by field
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof WaitingRoomUpdateStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && enabled.equals(that.enabled)
                && admin.equals(that.admin)
                && linkToken.equals(that.linkToken)
                && users.equals(that.users));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this waiting room update
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, enabled, admin, linkToken, users);
    }

    /**
     * Returns a debug oriented string for this waiting room update.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "WaitingRoomUpdateStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", enabled=" + enabled + ", admin=" + admin + ", linkToken=" + linkToken
                + ", users=" + users + ']';
    }
}
