package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="w:biz:catalog" type="get">} stanza that fetches the catalog public key of a single merchant.
 *
 * <p>The buyer-side direct-connection flow uses the PEM-encoded ECC certificate this request
 * obtains to encrypt the postcode and phone-number payloads forwarded to the merchant; without the
 * certificate the cart UI cannot ship an {@link IqVerifyPostcodeRequest} for that merchant.
 *
 * @implNote
 * This implementation models the legacy WAP-IQ path only; WA Web routes the same call through a
 * Relay GraphQL fetch when the {@code isGraphQLForGetPublicKeyEnabled} gating flag is on, but
 * Cobalt keeps the WAP-IQ payload as the single transport.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryGetPublicKeyJob")
public final class IqQueryGetPublicKeyRequest implements IqStanza.Request {
    /**
     * Holds the merchant JID stamped into the {@code jid} attribute of the {@code <public_key/>} child.
     */
    private final Jid businessJid;

    /**
     * Constructs a request over the given merchant JID.
     *
     * <p>Every other field on the wire is fixed by the relay schema.
     *
     * @param businessJid the merchant JID; never {@code null}
     * @throws NullPointerException if {@code businessJid} is {@code null}
     */
    public IqQueryGetPublicKeyRequest(Jid businessJid) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
    }

    /**
     * Returns the merchant JID the stanza names.
     *
     * <p>The value is routed verbatim into the {@code jid} attribute of the resulting
     * {@code <public_key/>} child.
     *
     * @return the merchant JID; never {@code null}
     */
    public Jid businessJid() {
        return businessJid;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation materialises the WAP envelope produced by the
     * {@code WAWebQueryGetPublicKeyJob} export: a single {@code <public_key jid/>} child wrapped in
     * the {@code w:biz:catalog get} IQ frame routed to the WhatsApp service via
     * {@link JidServer#user()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryGetPublicKeyJob",
            exports = "QueryGetPublicKey", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var publicKey = new StanzaBuilder()
                .description("public_key")
                .attribute("jid", businessJid)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:catalog")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(publicKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryGetPublicKeyRequest) obj;
        return Objects.equals(this.businessJid, that.businessJid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJid);
    }

    @Override
    public String toString() {
        return "IqQueryGetPublicKeyRequest[businessJid=" + businessJid + ']';
    }
}
