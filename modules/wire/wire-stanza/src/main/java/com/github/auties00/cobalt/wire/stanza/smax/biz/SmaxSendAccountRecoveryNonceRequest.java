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
 * The outbound stanza that asks the relay to email an account
 * recovery code to the active CTWA biz user.
 *
 * <p>The CTWA recovery flow issues this request when
 * {@link SmaxRequestSilentNonceResponse.RecoveryRequired} forces the user to confirm account
 * ownership before a silent nonce can be issued. The request carries no payload; the optional
 * {@code from} attribute is the only knob and is normally left {@code null}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizCtwaAdAccountSendAccountRecoveryNonceRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBizCtwaAdAccountHackBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBizCtwaAdAccountBaseIQGetRequestMixin")
public final class SmaxSendAccountRecoveryNonceRequest implements SmaxStanza.Request {
    /**
     * The optional user JID echoed onto the outbound IQ's
     * {@code from} attribute; {@code null} omits the attribute.
     */
    private final Jid fromUserJid;

    /**
     * Constructs a request with no {@code from} echo.
     *
     * <p>This is the default form for the standard recovery-code request, which carries no
     * arguments.
     */
    public SmaxSendAccountRecoveryNonceRequest() {
        this(null);
    }

    /**
     * Constructs a request optionally echoing the supplied user JID
     * onto the {@code from} attribute.
     *
     * <p>Reserved for multi-device callers that need the outbound IQ to look like it originated
     * from a specific linked user JID; standard CTWA callers pass {@code null}.
     *
     * @param fromUserJid the optional user JID; may be {@code null}
     */
    public SmaxSendAccountRecoveryNonceRequest(Jid fromUserJid) {
        this.fromUserJid = fromUserJid;
    }

    /**
     * Returns the optional {@code from} echo.
     *
     * <p>Returns {@link Optional#empty()} when the request was built without a {@code from} echo
     * (the standard recovery-code request case).
     *
     * @return an {@link Optional} carrying the user JID
     */
    public Optional<Jid> fromUserJid() {
        return Optional.ofNullable(fromUserJid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation stamps {@code xmlns="fb:thrift_iq"}, {@code type="get"},
     * {@code to="s.whatsapp.net"} and the optional {@code from} echo. The IQ {@code id} is assigned
     * by the dispatcher.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaAdAccountSendAccountRecoveryNonceRequest",
            exports = "makeSendAccountRecoveryNonceRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaAdAccountHackBaseIQGetRequestMixin",
            exports = "mergeHackBaseIQGetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaAdAccountBaseIQGetRequestMixin",
            exports = "mergeBaseIQGetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var builder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
        if (fromUserJid != null) {
            builder.attribute("from", fromUserJid);
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxSendAccountRecoveryNonceRequest) obj;
        return Objects.equals(this.fromUserJid, that.fromUserJid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(fromUserJid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SmaxSendAccountRecoveryNonceRequest[fromUserJid=" + fromUserJid + ']';
    }
}
