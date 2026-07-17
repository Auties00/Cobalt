package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Represents the outbound stanza that opts the connected client into
 * live updates for a newsletter.
 * Once the relay accepts the subscription it pushes
 * {@link SmaxNewslettersLiveUpdatesNotificationResponse} stanzas for the
 * requested newsletter until the TTL surfaced by
 * {@link SmaxNewslettersSubscribeToLiveUpdatesResponse.Success#duration()}
 * elapses. The resulting IQ has shape:
 * {@snippet :
 *     <iq xmlns="newsletter" type="set" to="<newsletterJid>">
 *         <live_updates/>
 *     </iq>
 * }
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersSubscribeToLiveUpdatesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersBaseIQSetRequestMixin")
public final class SmaxNewslettersSubscribeToLiveUpdatesRequest implements SmaxStanza.Request {
    /**
     * Holds the newsletter {@link Jid} to subscribe to; routed verbatim
     * into the IQ's {@code to} attribute.
     */
    private final Jid newsletterJid;

    /**
     * Constructs a new subscription request.
     *
     * @param newsletterJid the newsletter to subscribe to; never {@code null}
     * @throws NullPointerException if {@code newsletterJid} is {@code null}
     */
    public SmaxNewslettersSubscribeToLiveUpdatesRequest(Jid newsletterJid) {
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
    }

    /**
     * Returns the newsletter being subscribed to.
     *
     * @return the newsletter {@link Jid}; never {@code null}
     */
    public Jid newsletterJid() {
        return newsletterJid;
    }

    /**
     * Builds the outbound {@code <iq>} stanza carrying the bare
     * {@code <live_updates/>} payload.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <live_updates/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersSubscribeToLiveUpdatesRequest",
            exports = "makeSubscribeToLiveUpdatesRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var liveUpdatesNode = new StanzaBuilder()
                .description("live_updates")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", newsletterJid)
                .attribute("type", "set")
                .content(liveUpdatesNode);
    }

    /**
     * Compares two requests for value equality on {@link #newsletterJid()}.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is a request carrying an equal {@link #newsletterJid()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersSubscribeToLiveUpdatesRequest) obj;
        return Objects.equals(this.newsletterJid, that.newsletterJid);
    }

    /**
     * Returns the hash code derived from {@link #newsletterJid()}.
     *
     * @return the {@link Jid}'s hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(newsletterJid);
    }

    /**
     * Returns a debug representation including the newsletter {@link Jid}.
     *
     * @return a record-like rendering of this request
     */
    @Override
    public String toString() {
        return "SmaxNewslettersSubscribeToLiveUpdatesRequest[newsletterJid=" + newsletterJid + ']';
    }
}
