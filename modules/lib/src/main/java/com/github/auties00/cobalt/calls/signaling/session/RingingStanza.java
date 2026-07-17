package com.github.auties00.cobalt.calls.signaling.session;

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
 * Represents a {@code <ringing>} signal: the callee's device is alerting the local user.
 *
 * <p>A ringing signal is a contentless acknowledgement the callee emits when it begins alerting the
 * user, before any preaccept or accept. It carries only the universal call header: the call
 * identifier and the call creator, the same two attributes stamped on every action element. Unlike
 * most signaling actions it has no entry in the numeric {@code voip_signaling_message_type} table;
 * it is a thin status element keyed on its wire tag.
 *
 * <p>On the wire the element is:
 * {@snippet lang="xml" :
 * <ringing call-id="..." call-creator="..."/>
 * }
 *
 * @see SignalingType
 */
public final class RingingStanza implements CallMessage {
    /**
     * The wire element tag for a ringing signal.
     */
    public static final String ELEMENT = "ringing";

    /**
     * The call identifier this ringing signal's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this ringing signal's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * Constructs a ringing signal.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public RingingStanza(String callId, Jid callCreator) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a ringing signal
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a ringing signal
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A ringing signal has no entry in the numeric {@code voip_signaling_message_type} table, so
     * this projection has no {@link SignalingType} and the method returns {@code null}; the
     * element is dispatched on its {@link #ELEMENT wire tag} instead.
     *
     * @return {@code null}, since a ringing signal carries no taxonomy ordinal
     */
    @Override
    public SignalingType type() {
        return null;
    }

    /**
     * Builds the {@code <ringing call-id call-creator/>} action stanza.
     *
     * @return the ringing action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .build();
    }

    /**
     * Decodes a {@code <ringing>} action stanza into a {@link RingingStanza}.
     *
     * @param stanza the {@code <ringing>} stanza
     * @return the decoded ringing signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static RingingStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        return new RingingStanza(callId, callCreator);
    }

    /**
     * Returns whether {@code obj} is a {@link RingingStanza} with the same call header.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal ringing signal
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RingingStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this ringing signal
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator);
    }

    /**
     * Returns a debug oriented string for this ringing signal.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "RingingStanza[callId=" + callId + ", callCreator=" + callCreator + ']';
    }
}
