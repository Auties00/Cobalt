package com.github.auties00.cobalt.wire.stanza.smax.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * The outbound {@code <status to=NEWSLETTER_JID>} stanza builder for publishing a newsletter
 * status.
 *
 * <p>The request carries either a brand-new status or a status-reaction keyed by the target's
 * server-id, selected through its {@link SmaxStatusPublishPostNewsletterStatusPayload}. The relay
 * answers with a {@link SmaxStatusPublishPostNewsletterStatusResponse}: a
 * {@link SmaxStatusPublishPostNewsletterStatusResponse.Success} when the publish landed, or a
 * {@link SmaxStatusPublishPostNewsletterStatusResponse.Negative} when it was rejected.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutStatusPublishPostNewsletterStatusRequest")
public final class SmaxStatusPublishPostNewsletterStatusRequest implements SmaxStanza.Request {
    /**
     * The target newsletter JID, routed verbatim into the status's {@code to} attribute.
     */
    private final Jid newsletterJid;

    /**
     * The disjunctive publish payload selecting between server-id addressing and brand-new-post
     * addressing.
     */
    private final SmaxStatusPublishPostNewsletterStatusPayload payload;

    /**
     * Constructs a newsletter-status publish request.
     *
     * @param newsletterJid the target newsletter JID; never {@code null}
     * @param payload       the disjunctive publish payload; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxStatusPublishPostNewsletterStatusRequest(Jid newsletterJid, SmaxStatusPublishPostNewsletterStatusPayload payload) {
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        this.payload = Objects.requireNonNull(payload, "payload cannot be null");
    }

    /**
     * Returns the target newsletter JID.
     *
     * @return the JID; never {@code null}
     */
    public Jid newsletterJid() {
        return newsletterJid;
    }

    /**
     * Returns the publish payload.
     *
     * @return the payload; never {@code null}
     */
    public SmaxStatusPublishPostNewsletterStatusPayload payload() {
        return payload;
    }

    /**
     * Builds the outbound {@code <status>} stanza ready for dispatch.
     *
     * <p>The server-id arm stamps the {@code server_id} attribute and embeds the inner content
     * directly; the brand-new-post arm only embeds the inner client-id content.
     *
     * @implNote
     * This implementation dispatches on the
     * {@link SmaxStatusPublishPostNewsletterStatusPayload} sealed-interface variants via a Java
     * pattern-matching switch.
     *
     * @return a {@link StanzaBuilder} carrying the partially-built status envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutStatusPublishPostNewsletterStatusRequest",
            exports = "makePostNewsletterStatusRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var builder = new StanzaBuilder()
                .description("status")
                .attribute("to", newsletterJid);
        switch (payload) {
            case SmaxStatusPublishPostNewsletterStatusPayload.WithServerId withServerId -> {
                builder.attribute("id", withServerId.stanzaId());
                builder.attribute("server_id", withServerId.statusServerId());
                builder.content(withServerId.innerContent());
            }
            case SmaxStatusPublishPostNewsletterStatusPayload.WithClientIdOnly withClientId -> {
                builder.attribute("id", withClientId.stanzaId());
                builder.content(withClientId.clientIdContent());
            }
        }
        return builder;
    }

    /**
     * Compares this request to another for value equality.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxStatusPublishPostNewsletterStatusRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxStatusPublishPostNewsletterStatusRequest) obj;
        return Objects.equals(this.newsletterJid, that.newsletterJid)
                && Objects.equals(this.payload, that.payload);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(newsletterJid, payload);
    }

    /**
     * Returns a debug-friendly representation of this request.
     *
     * <p>The format is intended for logging and is not part of any contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxStatusPublishPostNewsletterStatusRequest[newsletterJid=" + newsletterJid
                + ", payload=" + payload + ']';
    }
}
