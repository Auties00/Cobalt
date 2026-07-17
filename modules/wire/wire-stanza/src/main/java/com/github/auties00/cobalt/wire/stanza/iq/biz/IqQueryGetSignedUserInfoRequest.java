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
 * Models the outbound {@code <iq xmlns="w:biz:catalog" type="get">} stanza that fetches the signed user-info bundle of a single merchant.
 *
 * <p>The buyer-side direct-connection flow uses the merchant's signed phone number, TTL timestamp,
 * signature blob and claimed business domain to validate that the out-of-band-supplied phone
 * number belongs to the merchant; the cart UI ships this stanza right after the public-key fetch.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryGetSignedUserInfoJob")
public final class IqQueryGetSignedUserInfoRequest implements IqStanza.Request {
    /**
     * Holds the merchant JID stamped into the {@code biz_jid} attribute of the {@code <signed_user_info/>} child.
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
    public IqQueryGetSignedUserInfoRequest(Jid businessJid) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
    }

    /**
     * Returns the merchant JID the stanza names.
     *
     * <p>The value is routed verbatim into the {@code biz_jid} attribute of the resulting
     * {@code <signed_user_info/>} child.
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
     * {@code WAWebQueryGetSignedUserInfoJob} export: a single {@code <signed_user_info biz_jid/>}
     * child wrapped in the {@code w:biz:catalog get} IQ frame routed to the WhatsApp service via
     * {@link JidServer#user()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryGetSignedUserInfoJob",
            exports = "QueryGetSignedUserInfo", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var signedUserInfo = new StanzaBuilder()
                .description("signed_user_info")
                .attribute("biz_jid", businessJid)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:catalog")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(signedUserInfo);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryGetSignedUserInfoRequest) obj;
        return Objects.equals(this.businessJid, that.businessJid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJid);
    }

    @Override
    public String toString() {
        return "IqQueryGetSignedUserInfoRequest[businessJid=" + businessJid + ']';
    }
}
