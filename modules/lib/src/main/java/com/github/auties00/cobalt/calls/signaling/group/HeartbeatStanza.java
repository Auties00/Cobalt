package com.github.auties00.cobalt.calls.signaling.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <heartbeat>} signal, a periodic group call membership keepalive.
 *
 * <p>Each active participant of a group call emits a heartbeat on a fixed cadence so the call server
 * keeps the participant's membership fresh and does not evict it. The element carries only the
 * universal call header, the call identifier and the call creator, the same two attributes stamped on
 * every action element, and has no further attributes or nested tree.
 *
 * <p>On the wire the element is:
 * {@snippet lang = xml:
 * <heartbeat call-id="0023CDF8FF5621A306A3337E63C9F0B5" call-creator="...@lid"/>
 *}
 *
 * @see SignalingType#HEARTBEAT
 */
public final class HeartbeatStanza implements CallMessage {
    /**
     * The wire element tag for a heartbeat signal.
     */
    public static final String ELEMENT = "heartbeat";

    /**
     * The call identifier this heartbeat's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this heartbeat's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * Constructs a heartbeat signal.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public HeartbeatStanza(String callId, Jid callCreator) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a heartbeat
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a heartbeat
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#HEARTBEAT}
     */
    @Override
    public SignalingType type() {
        return SignalingType.HEARTBEAT;
    }

    /**
     * Builds the {@code <heartbeat call-id call-creator/>} action stanza.
     *
     * @return the heartbeat action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .build();
    }

    /**
     * Decodes a {@code <heartbeat>} action stanza into a {@link HeartbeatStanza}.
     *
     * @param stanza the {@code <heartbeat>} stanza
     * @return the decoded heartbeat signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static HeartbeatStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        return new HeartbeatStanza(callId, callCreator);
    }

    /**
     * Returns whether {@code obj} is a {@link HeartbeatStanza} with the same call header.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a heartbeat with an equal call identifier and call creator
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof HeartbeatStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this heartbeat
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator);
    }

    /**
     * Returns a debug string for this heartbeat.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "HeartbeatStanza[callId=" + callId + ", callCreator=" + callCreator + ']';
    }
}
