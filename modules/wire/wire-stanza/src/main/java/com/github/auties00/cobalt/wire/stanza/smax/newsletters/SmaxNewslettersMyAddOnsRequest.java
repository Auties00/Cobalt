package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the outbound stanza that fetches the connected user's own
 * newsletter add-ons (reactions and poll votes).
 * The resulting IQ has shape:
 * {@snippet :
 *     <iq xmlns="newsletter" type="get" to="s.whatsapp.net">
 *         <my_addons limit="50" jid="<newsletterJid>"/>
 *     </iq>
 * }
 * Omitting {@link #newsletterJid()} fetches add-ons across every
 * newsletter the user follows.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersMyAddOnsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersSelfIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersBaseIQGetRequestMixin")
public final class SmaxNewslettersMyAddOnsRequest implements SmaxStanza.Request {
    /**
     * Holds the cap on returned {@code <messages>} blocks per round-trip.
     */
    private final int limit;

    /**
     * Holds the optional newsletter scope; {@code null} fetches across
     * every newsletter the user follows.
     */
    private final Jid newsletterJid;

    /**
     * Constructs a new request.
     * Passing {@code null} for {@code newsletterJid} fetches the user's
     * own add-ons across every followed newsletter.
     *
     * @param limit         the per-newsletter cap; must be non-negative
     * @param newsletterJid the optional newsletter scope; may be {@code null}
     */
    public SmaxNewslettersMyAddOnsRequest(int limit, Jid newsletterJid) {
        this.limit = limit;
        this.newsletterJid = newsletterJid;
    }

    /**
     * Returns the per-newsletter cap on returned messages.
     *
     * @return the cap
     */
    public int limit() {
        return limit;
    }

    /**
     * Returns the optional newsletter scope.
     *
     * @return an {@link Optional} carrying the newsletter {@link Jid}, or empty when the request fetches across every followed newsletter
     */
    public Optional<Jid> newsletterJid() {
        return Optional.ofNullable(newsletterJid);
    }

    /**
     * Builds the outbound {@code <iq>} stanza carrying the
     * {@code <my_addons/>} payload.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <my_addons/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersMyAddOnsRequest",
            exports = "makeMyAddOnsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var myAddOnsBuilder = new StanzaBuilder()
                .description("my_addons")
                .attribute("limit", limit);
        if (newsletterJid != null) {
            myAddOnsBuilder.attribute("jid", newsletterJid);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", Jid.userServer())
                .attribute("type", "get")
                .content(myAddOnsBuilder.build());
    }

    /**
     * Compares two requests for value equality on both fields.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is a request with equal {@link #limit()} and {@link #newsletterJid()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersMyAddOnsRequest) obj;
        return this.limit == that.limit && Objects.equals(this.newsletterJid, that.newsletterJid);
    }

    /**
     * Returns the hash code derived from both fields.
     *
     * @return the combined hash of {@link #limit()} and {@link #newsletterJid()}
     */
    @Override
    public int hashCode() {
        return Objects.hash(limit, newsletterJid);
    }

    /**
     * Returns a debug representation including both fields.
     *
     * @return a record-like rendering of this request
     */
    @Override
    public String toString() {
        return "SmaxNewslettersMyAddOnsRequest[limit=" + limit
                + ", newsletterJid=" + newsletterJid + ']';
    }
}
