package com.github.auties00.cobalt.wire.stanza.iq.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

/**
 * Models the outbound legacy-IQ stanza that fetches the calling user's status-privacy setting.
 *
 * <p>Dispatching this request as an {@link IqStanza.Request} produces an
 * {@code <iq xmlns="status" type="get">} envelope addressed to {@link JidServer#user()} and wrapping a
 * single empty {@code <privacy>} child. The relay replies with the current audience selector and the
 * paired JID list, parsed by {@link IqQueryStatusPrivacyResponse}. This request carries no state; it
 * is the read counterpart of {@link IqSetStatusPrivacyRequest}.
 */
@WhatsAppWebModule(moduleName = "WAWebUserPrefsStatus")
public final class IqQueryStatusPrivacyRequest implements IqStanza.Request {
    /**
     * Constructs a query-status-privacy request.
     *
     * <p>The request is stateless because the query carries no payload beyond the empty
     * {@code <privacy>} child.
     */
    public IqQueryStatusPrivacyRequest() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="status" type="get">} envelope addressed to
     * {@link JidServer#user()} wrapping a single empty {@code <privacy>} child. The IQ {@code id}
     * attribute is assigned by the dispatch layer.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the {@code <privacy>}
     *         payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsStatus", exports = "getStatusPrivacySetting",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var privacyNode = new StanzaBuilder()
                .description("privacy")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "status")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(privacyNode);
    }

    /**
     * Compares this request with another object for equality.
     *
     * <p>All query-status-privacy requests are equal because the type carries no state.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link IqQueryStatusPrivacyRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    /**
     * Returns a hash code derived from the runtime class.
     *
     * @return the class-derived hash code
     */
    @Override
    public int hashCode() {
        return IqQueryStatusPrivacyRequest.class.hashCode();
    }

    /**
     * Returns a debug string for this request.
     *
     * @return a parameterless string representation
     */
    @Override
    public String toString() {
        return "IqQueryStatusPrivacyRequest[]";
    }
}
