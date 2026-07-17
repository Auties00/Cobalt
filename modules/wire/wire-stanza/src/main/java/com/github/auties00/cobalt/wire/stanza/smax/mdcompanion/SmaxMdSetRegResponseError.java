package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Models the outbound {@code <iq type="error"><error text="not-authorized" code="401"/></iq>}
 * reply a companion emits when it refuses to complete the pair-device handshake.
 *
 * <p>Sent when the device-identity HMAC check fails, when the account-signature verification
 * fails, or when any other validation step in the pair-success handler rejects the inbound
 * stanza; the not-authorized 401 is the only error variant defined for this RPC, after which the
 * companion logs out. Successful pairings reply with {@link SmaxMdSetRegResponseClient} or
 * {@link SmaxMdSetRegResponseHostedClient} instead.
 *
 * @implNote This implementation folds the WA Web not-authorized error mixin into the builder by
 * inlining the {@code <error text="not-authorized" code="401"/>} shape; the outer envelope is
 * pinned to {@code <iq to="s.whatsapp.net" type="error">} with the original {@code id} echoed back
 * so the relay can pair the error with its pending request.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutMdSetRegResponseError")
@WhatsAppWebModule(moduleName = "WASmaxOutMdIQErrorNotAuthorizedMixin")
public final class SmaxMdSetRegResponseError implements SmaxStanza.Request {
    /**
     * Holds the {@code id} of the inbound IQ being replied to.
     *
     * <p>Echoed into the outbound {@code <iq id="..."/>} attribute.
     */
    private final String iqId;

    /**
     * Constructs an error reply.
     *
     * <p>Callers typically derive {@code iqId} from the rejected {@link SmaxMdSetRegResponse}.
     *
     * @param iqId the inbound IQ id; never {@code null}
     * @throws NullPointerException if {@code iqId} is {@code null}
     */
    public SmaxMdSetRegResponseError(String iqId) {
        this.iqId = Objects.requireNonNull(iqId, "iqId cannot be null");
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
     * Builds the outbound error stanza.
     *
     * <p>Returns the unfinished {@link StanzaBuilder} so the dispatch path can stamp the wire-level
     * identifiers before flushing, matching the contract of {@link SmaxStanza.Request#toStanza()}.
     *
     * @implNote This implementation hardcodes the inner {@code <error>} child to
     * {@code text="not-authorized"} and {@code code=401} because the WA Web not-authorized error
     * mixin pins the same two values; no other error variant is defined for this RPC.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutMdSetRegResponseError",
            exports = "makeSetRegResponseError",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var errorNode = new StanzaBuilder()
                .description("error")
                .attribute("text", "not-authorized")
                .attribute("code", 401)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", iqId)
                .attribute("to", "s.whatsapp.net")
                .attribute("type", "error")
                .content(errorNode);
    }

    /**
     * Compares this error reply to another object for value equality.
     *
     * <p>Two error replies are equal when their IQ id matches.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal error reply
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMdSetRegResponseError) obj;
        return Objects.equals(this.iqId, that.iqId);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code derived from the IQ id
     */
    @Override
    public int hashCode() {
        return Objects.hash(iqId);
    }

    /**
     * Returns a debug string listing the IQ id.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdSetRegResponseError[iqId=" + iqId + ']';
    }
}
