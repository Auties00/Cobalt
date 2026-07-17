package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <reconfigure_bot>} in call action: a request to reconfigure the GenAI bot
 * participating in the call.
 *
 * <p>A reconfigure bot action asks the call's GenAI bot to reload and reapply its configuration. It
 * carries the universal call header ({@code call-id} and {@code call-creator}) and a numeric
 * {@code req_id} attribute correlating the reconfiguration request with its acknowledgement. The
 * {@code req_id} attribute is mandatory: a stanza that omits it is not a valid reconfigure bot
 * action.
 *
 * <p>On the wire the element takes the following shape:
 * {@snippet lang = xml:
 * <reconfigure_bot call-id="..." call-creator="..." req_id="N"/>
 *}
 *
 * @see SignalingType#RECONFIGURE_BOT
 */
public final class ReconfigureBotStanza implements InCallActionStanza {
    /**
     * The wire element tag {@code reconfigure_bot} identifying this action.
     */
    public static final String ELEMENT = "reconfigure_bot";

    /**
     * The wire attribute name {@code req_id} carrying the reconfiguration request correlation id.
     */
    private static final String REQUEST_ID_ATTRIBUTE = "req_id";

    /**
     * The call identifier carried in this action's {@code call-id} header attribute.
     */
    private final String callId;

    /**
     * The call creator device JID carried in this action's {@code call-creator} header attribute.
     */
    private final Jid callCreator;

    /**
     * The reconfiguration request correlation id carried in the {@code req_id} attribute.
     */
    private final int requestId;

    /**
     * Constructs a reconfigure bot action from its call header and request id.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param requestId   the reconfiguration request correlation id
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public ReconfigureBotStanza(String callId, Jid callCreator, int requestId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.requestId = requestId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a reconfigure bot action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a reconfigure bot action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the reconfiguration request correlation id.
     *
     * @return the request correlation id
     */
    public int requestId() {
        return requestId;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#RECONFIGURE_BOT}
     */
    @Override
    public SignalingType type() {
        return SignalingType.RECONFIGURE_BOT;
    }

    /**
     * Builds the {@code <reconfigure_bot call-id call-creator req_id/>} action stanza.
     *
     * <p>Stamps the common {@code call-id} and {@code call-creator} header attributes onto the
     * {@link #ELEMENT} node, then appends the {@code req_id} attribute.
     *
     * @return the reconfigure bot action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(REQUEST_ID_ATTRIBUTE, requestId)
                .build();
    }

    /**
     * Decodes a {@code <reconfigure_bot>} action stanza into a {@link ReconfigureBotStanza}.
     *
     * <p>Reads the mandatory {@code call-id} and {@code call-creator} header attributes and the
     * mandatory {@code req_id} attribute; any absent attribute fails the decode.
     *
     * @param stanza the {@code <reconfigure_bot>} stanza
     * @return the decoded reconfigure bot action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id}, {@code call-creator}, or
     *                                {@code req_id} attribute is absent
     */
    public static ReconfigureBotStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var requestId = stanza.getAttributeAsInt(REQUEST_ID_ATTRIBUTE)
                .orElseThrow(() -> new NoSuchElementException("reconfigure_bot requires a req_id attribute"));
        return new ReconfigureBotStanza(callId, callCreator, requestId);
    }

    /**
     * Returns whether {@code obj} is a {@link ReconfigureBotStanza} with the same call header and request
     * id.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal reconfigure bot action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ReconfigureBotStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && requestId == that.requestId);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this reconfigure bot action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, requestId);
    }

    /**
     * Returns a debug string for this reconfigure bot action listing its call header and request id.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "ReconfigureBotStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", requestId=" + requestId + ']';
    }
}
