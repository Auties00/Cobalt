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
 * Represents a {@code <waiting_room_toggle>} signal, the host flipping the waiting room gate during a call.
 *
 * <p>A waiting room toggle is sent by the call host to enable or disable the lobby gate of an active
 * call. It carries the universal call header, the desired {@link #enabled() gate state}, and, when the
 * gate is keyed by a call link, the {@link #linkToken() link token}. The relay answers on a
 * waiting room toggle receipt.
 *
 * <p>This is the in call signaling plane toggle, distinct from the call link admin toggle issued out of
 * call as an SMAX operation; both share the {@code waiting_room_toggle} element tag.
 *
 * <p>On the wire the element is:
 * {@snippet lang = xml:
 * <waiting_room_toggle call-id="..." call-creator="..." enabled="1" link-token="..."/>
 *}
 *
 * @implNote This implementation shares the {@code <waiting_room>} element grammar centralized in
 * {@link WaitingRoomStanzas}. The element tag is taken from {@link SignalingType#WAITING_ROOM_TOGGLE}
 * and the {@code enabled} flag serializes as the {@code '1'} or {@code '0'} boolean literal rather than
 * {@code "true"}/{@code "false"}.
 * @see SignalingType#WAITING_ROOM_TOGGLE
 */
public final class WaitingRoomToggleStanza implements CallMessage {
    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * The desired gate state.
     */
    private final boolean enabled;

    /**
     * The targeted call link token, if present.
     */
    private final Optional<String> linkToken;

    /**
     * Constructs a waiting room toggle signal.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param enabled     the desired gate state
     * @param linkToken   the targeted call link token, if present
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code linkToken} is
     *                              {@code null}
     */
    public WaitingRoomToggleStanza(String callId, Jid callCreator, boolean enabled, Optional<String> linkToken) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(linkToken, "linkToken cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.enabled = enabled;
        this.linkToken = linkToken;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a waiting room toggle
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a waiting room toggle
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the desired gate state.
     *
     * @return the desired gate state
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Returns the targeted call link token, if present.
     *
     * @return the targeted call link token, or empty when absent
     */
    public Optional<String> linkToken() {
        return linkToken;
    }

    /**
     * Returns a toggle signal that flips the gate of an active call without naming a call link token.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param enabled     the desired gate state
     * @return the toggle signal
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public static WaitingRoomToggleStanza of(String callId, Jid callCreator, boolean enabled) {
        return new WaitingRoomToggleStanza(callId, callCreator, enabled, Optional.empty());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#WAITING_ROOM_TOGGLE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.WAITING_ROOM_TOGGLE;
    }

    /**
     * Builds the {@code <waiting_room_toggle call-id call-creator enabled link-token/>} action stanza.
     *
     * <p>The {@code enabled} attribute is always written as {@code '1'} or {@code '0'}; an absent link
     * token is omitted and the element carries no {@code <user>} children.
     *
     * @return the waiting room toggle action stanza
     */
    @Override
    public Stanza toStanza() {
        return WaitingRoomStanzas.build(type().wireTag().orElseThrow(), callId, callCreator,
                Optional.of(enabled), linkToken, Optional.empty(), List.of());
    }

    /**
     * Decodes a {@code <waiting_room_toggle>} action stanza into a {@link WaitingRoomToggleStanza}.
     *
     * <p>An absent {@code enabled} attribute classifies to {@code false}.
     *
     * @param stanza the {@code <waiting_room_toggle>} stanza
     * @return the decoded waiting room toggle signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static WaitingRoomToggleStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var enabled = "1".equals(stanza.getAttributeAsString(WaitingRoomStanzas.ENABLED_ATTRIBUTE).orElse("0"));
        var linkToken = WaitingRoomStanzas.linkToken(stanza);
        return new WaitingRoomToggleStanza(callId, callCreator, enabled, linkToken);
    }

    /**
     * Returns whether {@code obj} is a {@link WaitingRoomToggleStanza} equal to this one by value.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal waiting room toggle
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof WaitingRoomToggleStanza that
                && enabled == that.enabled
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && linkToken.equals(that.linkToken));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this waiting room toggle
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, enabled, linkToken);
    }

    /**
     * Returns a debug oriented string for this waiting room toggle.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "WaitingRoomToggleStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", enabled=" + enabled + ", linkToken=" + linkToken + ']';
    }
}
