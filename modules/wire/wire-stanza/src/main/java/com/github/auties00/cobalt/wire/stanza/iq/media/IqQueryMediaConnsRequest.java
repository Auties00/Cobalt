package com.github.auties00.cobalt.wire.stanza.iq.media;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

/**
 * Requests the current media-server connection configuration the client uses for uploads and
 * downloads.
 *
 * <p>The request is an outbound {@code <iq xmlns="w:m" type="set">} stanza wrapping a single
 * bare {@code <media_conn/>} child. The relay replies with the bearer token, host routes, and
 * retry budgets routed to the media CDN, modelled by {@link IqQueryMediaConnsResponse}. The
 * media-upload and media-download pipelines issue this request to acquire and periodically
 * refresh that configuration, re-issuing it shortly before the token expiry the relay returns.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryMediaConnsJob")
public final class IqQueryMediaConnsRequest implements IqStanza.Request {
    /**
     * Constructs a new query-media-conn request.
     *
     * <p>The request carries no payload; the relay derives the entire reply from the
     * authenticated session.
     */
    public IqQueryMediaConnsRequest() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq xmlns="w:m" type="set">} envelope addressed to
     * {@link JidServer#user()} and wrapping a single bare {@code <media_conn/>} child. A fresh
     * stanza id is minted via {@link RandomIdUtils#newId()} on every call.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the
     *         {@code <media_conn/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob",
            exports = "queryMediaConn", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var mediaConnNode = new StanzaBuilder()
                .description("media_conn")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", RandomIdUtils.newId())
                .attribute("xmlns", "w:m")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(mediaConnNode);
    }

    /**
     * Indicates whether the given object is another request of this type.
     *
     * <p>Every {@link IqQueryMediaConnsRequest} is stateless and therefore interchangeable, so
     * equality reduces to a runtime-class identity check.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@link IqQueryMediaConnsRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    /**
     * Returns a constant hash consistent with the class-identity equality contract.
     *
     * @return the hash of the {@link IqQueryMediaConnsRequest} class
     */
    @Override
    public int hashCode() {
        return IqQueryMediaConnsRequest.class.hashCode();
    }

    /**
     * Returns the canonical string form of this stateless request.
     *
     * @return the fixed string {@code "IqQueryMediaConnsRequest[]"}
     */
    @Override
    public String toString() {
        return "IqQueryMediaConnsRequest[]";
    }
}
