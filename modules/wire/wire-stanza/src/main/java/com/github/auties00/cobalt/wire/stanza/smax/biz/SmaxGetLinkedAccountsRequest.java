package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code GetLinkedAccounts} IQ request stanza.
 * <p>
 * The request enumerates the linked Facebook page, Facebook business, Instagram-professional
 * and WhatsApp ad-identity records associated with the active user, feeding both the CTWA
 * ad-creation pipeline and the SMB linked-accounts settings surface. The {@code from}
 * attribute is echoed only when a companion linked device proxies the request on behalf of
 * the active user, in which case the relay validates that the echoed JID matches the
 * authenticated session.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizLinkingGetLinkedAccountsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBizLinkingHackBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBizLinkingBaseIQGetRequestMixin")
public final class SmaxGetLinkedAccountsRequest implements SmaxStanza.Request {
    /**
     * The optional {@code from} attribute echoed onto the outbound IQ.
     * <p>
     * The active user {@link Jid} is the only legal value; {@code null} omits the attribute.
     */
    private final Jid fromUserJid;

    /**
     * Constructs a request with no {@code from} echo.
     * <p>
     * Produces a bare IQ-get carrying only the empty {@code <linked_accounts/>} payload.
     */
    public SmaxGetLinkedAccountsRequest() {
        this(null);
    }

    /**
     * Constructs a request optionally echoing the supplied user {@link Jid} onto the
     * {@code from} attribute.
     * <p>
     * Used when a companion linked device proxies the request on behalf of the active user;
     * the relay validates that the echoed JID matches the authenticated session.
     *
     * @param fromUserJid the optional user {@link Jid} to echo onto the {@code from} attribute; may be {@code null}
     */
    public SmaxGetLinkedAccountsRequest(Jid fromUserJid) {
        this.fromUserJid = fromUserJid;
    }

    /**
     * Returns the optional {@code from} echo.
     *
     * @return an {@link Optional} carrying the user {@link Jid}, or empty when no echo was supplied
     */
    public Optional<Jid> fromUserJid() {
        return Optional.ofNullable(fromUserJid);
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * Stamps the {@code xmlns="fb:thrift_iq"} envelope with {@code to} addressed to the user
     * server and {@code type="get"}, emits an empty {@code <linked_accounts/>} child, and
     * echoes the optional {@code from} attribute when supplied. The {@code id} attribute is
     * appended by Cobalt's send path.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the empty {@code <linked_accounts/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizLinkingGetLinkedAccountsRequest",
            exports = "makeGetLinkedAccountsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizLinkingHackBaseIQGetRequestMixin",
            exports = "mergeHackBaseIQGetRequestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizLinkingBaseIQGetRequestMixin",
            exports = "mergeBaseIQGetRequestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var linkedAccountsNode = new StanzaBuilder()
                .description("linked_accounts")
                .build();
        var builder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(linkedAccountsNode);
        if (fromUserJid != null) {
            builder.attribute("from", fromUserJid);
        }
        return builder;
    }

    /**
     * Compares this request to another object for value equality on the {@code from} echo.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxGetLinkedAccountsRequest} with an equal {@code from} echo
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetLinkedAccountsRequest) obj;
        return Objects.equals(this.fromUserJid, that.fromUserJid);
    }

    /**
     * Returns a hash code derived from the {@code from} echo.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(fromUserJid);
    }

    /**
     * Returns a debug rendering listing the optional {@code from} echo.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGetLinkedAccountsRequest[fromUserJid=" + fromUserJid + ']';
    }
}
