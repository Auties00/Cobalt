package com.github.auties00.cobalt.wire.stanza.smax.usernotice;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="tos" type="get">} stanza that asks the relay for the
 * user-facing legal disclosures the account must acknowledge.
 *
 * <p>The relay answers with one {@code <notice>} per outstanding disclosure (terms-of-service
 * updates, regional privacy notices, biz-broadcast opt-in prompts, and similar prompts), parsed
 * by {@link SmaxUserNoticeGetDisclosuresResponse}. The reply lets a client render the prompts so
 * the user can read and accept them. The single {@code getUserDisclosuresT} field carries the
 * client-side fetch timestamp the relay uses to decide which disclosures to surface.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutUserNoticeGetDisclosuresRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutUserNoticeBaseIQGetRequestMixin")
public final class SmaxUserNoticeGetDisclosuresRequest implements SmaxStanza.Request {
    /**
     * Holds the client-side fetch timestamp, in seconds since the UNIX epoch, that the relay uses
     * to decide which disclosures to return.
     */
    private final long getUserDisclosuresT;

    /**
     * Constructs a request carrying the given fetch timestamp.
     *
     * <p>The timestamp is the current wall-clock time in seconds; the relay uses it to decide
     * which disclosures to surface.
     *
     * @param getUserDisclosuresT the fetch timestamp in seconds since the UNIX epoch
     */
    public SmaxUserNoticeGetDisclosuresRequest(long getUserDisclosuresT) {
        this.getUserDisclosuresT = getUserDisclosuresT;
    }

    /**
     * Returns the client-side fetch timestamp.
     *
     * <p>The value is stamped onto the {@code t} attribute of the {@code <get_user_disclosures>}
     * child by {@link #toStanza()}.
     *
     * @return the fetch timestamp in seconds since the UNIX epoch
     */
    public long getUserDisclosuresT() {
        return getUserDisclosuresT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="tos" type="get" to="s.whatsapp.net">} envelope wrapping a
     * single {@code <get_user_disclosures t="..."/>} child carrying the fetch timestamp.
     *
     * @implNote
     * This implementation addresses the envelope to {@link JidServer#user()} and hard-codes the
     * {@code tos} namespace and {@code get} type, matching the wire shape of the polled relay
     * endpoint.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutUserNoticeGetDisclosuresRequest",
            exports = "makeGetDisclosuresRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var getUserDisclosuresNode = new StanzaBuilder()
                .description("get_user_disclosures")
                .attribute("t", getUserDisclosuresT)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "tos")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(getUserDisclosuresNode);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Two requests are equal when they carry the same fetch timestamp.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUserNoticeGetDisclosuresRequest) obj;
        return this.getUserDisclosuresT == that.getUserDisclosuresT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The hash is derived from the fetch timestamp.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getUserDisclosuresT);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the type name and the fetch timestamp in the record-like form shared across the
     * {@code Smax} stanza family.
     */
    @Override
    public String toString() {
        return "SmaxUserNoticeGetDisclosuresRequest[getUserDisclosuresT=" + getUserDisclosuresT + ']';
    }
}
