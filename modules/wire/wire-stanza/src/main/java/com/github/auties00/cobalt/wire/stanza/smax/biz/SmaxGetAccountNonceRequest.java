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
 * Builds the outbound {@code GetAccountNonce} IQ request for the SMB business-linking nonce bridge.
 * <p>
 * Drives the click-to-WhatsApp ads page-binding handshake: the relay returns a one-shot
 * account-binding nonce that the CTWA ad-creation surface forwards to Meta's ads backend to prove
 * ownership of the linked Facebook page.
 *
 * @implNote This implementation stamps the static {@code xmlns="fb:thrift_iq"} envelope; the
 * {@code id} attribute is not stamped here because Cobalt's send path delegates id generation to
 * {@code LinkedWhatsAppClient}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizLinkingGetAccountNonceRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBizLinkingHackBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBizLinkingBaseIQGetRequestMixin")
public final class SmaxGetAccountNonceRequest implements SmaxStanza.Request {
    /**
     * The optional {@code scope} attribute of the {@code <identifier>} child; {@code null} omits the
     * child entirely.
     */
    private final String identifierScope;

    /**
     * The optional {@code from} attribute echoed onto the outbound IQ; the active user {@link Jid}
     * is the only legal value and {@code null} omits the attribute.
     */
    private final Jid fromUserJid;

    /**
     * Constructs a request without an {@code <identifier>} child and with no {@code from} echo.
     * <p>
     * This is the default shape: a bare IQ-get with no payload, suitable for the standard CTWA
     * ad-creation nonce fetch.
     */
    public SmaxGetAccountNonceRequest() {
        this(null, null);
    }

    /**
     * Constructs a request optionally carrying an {@code <identifier scope="..."/>} child and no
     * {@code from} echo.
     * <p>
     * The scope literal is passed through verbatim to the relay.
     *
     * @param identifierScope the {@code scope} attribute; may be {@code null} to omit the child
     */
    public SmaxGetAccountNonceRequest(String identifierScope) {
        this(identifierScope, null);
    }

    /**
     * Constructs a request with the optional scope and an optional {@code from} echo.
     * <p>
     * Supply {@code fromUserJid} when a companion linked device proxies the request on behalf of the
     * active user; the relay validates that the echoed JID matches the authenticated session.
     *
     * @param identifierScope the {@code scope} attribute; may be {@code null} to omit the child
     * @param fromUserJid     the optional user {@link Jid} to echo onto the {@code from} attribute;
     *                        may be {@code null}
     */
    public SmaxGetAccountNonceRequest(String identifierScope, Jid fromUserJid) {
        this.identifierScope = identifierScope;
        this.fromUserJid = fromUserJid;
    }

    /**
     * Returns the optional identifier scope.
     * <p>
     * Empty when the request was constructed without an {@code <identifier>} child.
     *
     * @return an {@link Optional} carrying the scope, or empty when the child is omitted
     */
    public Optional<String> identifierScope() {
        return Optional.ofNullable(identifierScope);
    }

    /**
     * Returns the optional {@code from} echo.
     * <p>
     * Empty unless a companion-device proxy supplied the active user's {@link Jid}.
     *
     * @return an {@link Optional} carrying the user {@link Jid}, or empty when no echo was supplied
     */
    public Optional<Jid> fromUserJid() {
        return Optional.ofNullable(fromUserJid);
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     *
     * @implNote This implementation stamps the {@code xmlns="fb:thrift_iq"} envelope, the {@code to}
     * and optional {@code from} attributes, and {@code type="get"} in a single pass; the {@code id}
     * attribute is appended by Cobalt's send path.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the optional {@code <identifier/>}
     *         payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizLinkingGetAccountNonceRequest",
            exports = "makeGetAccountNonceRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizLinkingHackBaseIQGetRequestMixin",
            exports = "mergeHackBaseIQGetRequestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizLinkingBaseIQGetRequestMixin",
            exports = "mergeBaseIQGetRequestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var builder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
        if (fromUserJid != null) {
            builder.attribute("from", fromUserJid);
        }
        if (identifierScope != null) {
            var identifierNode = new StanzaBuilder()
                    .description("identifier")
                    .attribute("scope", identifierScope)
                    .build();
            builder.content(identifierNode);
        }
        return builder;
    }

    /**
     * Compares this request to {@code obj} for structural equality on the scope and the optional
     * {@code from} echo.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxGetAccountNonceRequest} with matching
     *         {@link #identifierScope()} and {@link #fromUserJid()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetAccountNonceRequest) obj;
        return Objects.equals(this.identifierScope, that.identifierScope)
                && Objects.equals(this.fromUserJid, that.fromUserJid);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash of the scope and the optional {@code from} echo
     */
    @Override
    public int hashCode() {
        return Objects.hash(identifierScope, fromUserJid);
    }

    /**
     * Returns a debug-friendly rendering naming the scope and the optional {@code from} echo.
     *
     * @return a record-style string with the scope and {@code from} echo
     */
    @Override
    public String toString() {
        return "SmaxGetAccountNonceRequest[identifierScope=" + identifierScope
                + ", fromUserJid=" + fromUserJid + ']';
    }
}
