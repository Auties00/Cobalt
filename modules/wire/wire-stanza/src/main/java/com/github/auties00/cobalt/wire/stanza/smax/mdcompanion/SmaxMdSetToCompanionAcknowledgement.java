package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Models the outbound {@code <iq type="result"/>} stanza a companion emits after consuming a
 * {@link SmaxMdSetToCompanionResponse}.
 *
 * <p>Sent once the inbound pair-device refs have been accepted into the rotation timer, so the
 * relay can mark the pair-device IQ delivered. The two echoed fields ({@code id} and {@code to})
 * are taken from the inbound IQ. The usual entry point is
 * {@link #from(SmaxMdSetToCompanionResponse)}, which derives them from an already-parsed
 * projection.
 *
 * @implNote This implementation emits the bare result envelope
 * {@code <iq id="..." to="..." type="result"/>} with no inner payload. The {@code to} attribute
 * echoes the inbound {@code from} (always the {@code s.whatsapp.net} server domain) rather than
 * pinning the literal in the builder.
 *
 * @deprecated superseded by the inline companion-pairing flow in {@code IqStreamHandler}.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxOutMdSetToCompanionResponseClientResponse")
public final class SmaxMdSetToCompanionAcknowledgement implements SmaxStanza.Request {
    /**
     * Holds the {@code id} of the inbound IQ being acknowledged.
     *
     * <p>Echoed into the outbound {@code <iq id="..."/>} attribute so the relay can pair the
     * response with its pending request.
     */
    private final String iqId;

    /**
     * Holds the destination JID, echoed from the inbound IQ's {@code from} attribute.
     *
     * <p>Becomes the ack's {@code to} attribute; always the {@code s.whatsapp.net} server domain
     * for valid inbound stanzas.
     */
    private final Jid iqTo;

    /**
     * Constructs an ack from already-resolved component fields.
     *
     * <p>Most callers use {@link #from(SmaxMdSetToCompanionResponse)} to derive both echoed fields
     * from a parsed pair-device projection; this constructor is exposed so unit tests can build
     * fixtures directly.
     *
     * @param iqId the inbound IQ id; never {@code null}
     * @param iqTo the destination JID; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxMdSetToCompanionAcknowledgement(String iqId, Jid iqTo) {
        this.iqId = Objects.requireNonNull(iqId, "iqId cannot be null");
        this.iqTo = Objects.requireNonNull(iqTo, "iqTo cannot be null");
    }

    /**
     * Derives the ack from an already-parsed {@link SmaxMdSetToCompanionResponse} projection.
     *
     * <p>Copies the inbound {@link SmaxMdSetToCompanionResponse#iqId()} and
     * {@link SmaxMdSetToCompanionResponse#iqFrom()} into the new ack.
     *
     * @param inbound the parsed inbound pair-device projection
     * @return a new {@link SmaxMdSetToCompanionAcknowledgement}
     * @throws NullPointerException if {@code inbound} is {@code null}
     */
    public static SmaxMdSetToCompanionAcknowledgement from(SmaxMdSetToCompanionResponse inbound) {
        Objects.requireNonNull(inbound, "inbound cannot be null");
        return new SmaxMdSetToCompanionAcknowledgement(inbound.iqId(), inbound.iqFrom());
    }

    /**
     * Returns the IQ id being echoed.
     *
     * @return the id; never {@code null}
     */
    public String iqId() {
        return iqId;
    }

    /**
     * Returns the destination JID that becomes the ack's {@code to} attribute.
     *
     * @return the JID; never {@code null}
     */
    public Jid iqTo() {
        return iqTo;
    }

    /**
     * Builds the outbound ack stanza.
     *
     * <p>Returns the unfinished {@link StanzaBuilder} so the dispatch path can stamp the wire-level
     * identifiers before flushing, matching the contract of {@link SmaxStanza.Request#toStanza()}.
     *
     * @implNote This implementation emits an {@code <iq>} with no children, the bare-result shape
     * the WA Web pair-device response builder produces.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutMdSetToCompanionResponseClientResponse",
            exports = "makeSetToCompanionResponseClientResponse",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", iqId)
                .attribute("to", iqTo)
                .attribute("type", "result");
    }

    /**
     * Compares this acknowledgement to another object for value equality.
     *
     * <p>Two acknowledgements are equal when their IQ id and destination JID match.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal acknowledgement
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMdSetToCompanionAcknowledgement) obj;
        return Objects.equals(this.iqId, that.iqId)
                && Objects.equals(this.iqTo, that.iqTo);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code derived from the IQ id and destination JID
     */
    @Override
    public int hashCode() {
        return Objects.hash(iqId, iqTo);
    }

    /**
     * Returns a debug string listing the IQ id and destination JID.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdSetToCompanionAcknowledgement[iqId=" + iqId
                + ", iqTo=" + iqTo + ']';
    }
}
