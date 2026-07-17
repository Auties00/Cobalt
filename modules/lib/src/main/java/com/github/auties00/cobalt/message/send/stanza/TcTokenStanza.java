package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.privacy.TrustedContactTokenService;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Builds the optional {@code <tctoken>} child of an outgoing {@code <message>} stanza carrying the
 * trusted-contact privacy token for the recipient.
 *
 * <p>The TC token is the privacy-tier handshake that opts a contact pair into receiving
 * privacy-enhanced features such as verified history and identity pin. On every outgoing 1:1
 * message the sender may attach the most recent token it holds for the recipient to demonstrate the
 * trust relationship. {@link ChatFanoutStanza} composes the {@code <tctoken>} child ahead of the
 * lower-priority {@code <cstoken>} fallback in {@link CsTokenStanza}. Token lifetime policy lives
 * in {@link TrustedContactTokenService}; this class only consults it.
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgCreateFanoutStanza")
public final class TcTokenStanza {
    /**
     * The logger for {@link TcTokenStanza}.
     */
    private static final System.Logger LOGGER = Log.get(TcTokenStanza.class);

    /**
     * Holds the {@link LinkedWhatsAppStore} used to look up the recipient chat.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Holds the {@link ABPropsService} consulted for the token-emission gate.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the {@link TrustedContactTokenService} consulted for the receiver-side validity window.
     */
    private final TrustedContactTokenService trustedContactTokenService;

    /**
     * Constructs a builder bound to the given store, AB-props service, and token-lifetime service.
     *
     * <p>Constructed once per client; the builder is otherwise stateless.
     *
     * @param store                      the {@link LinkedWhatsAppStore}
     * @param abPropsService             the {@link ABPropsService}
     * @param trustedContactTokenService the {@link TrustedContactTokenService}
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public TcTokenStanza(LinkedWhatsAppStore store, ABPropsService abPropsService,
                         TrustedContactTokenService trustedContactTokenService) {
        this.store = Objects.requireNonNull(store, "store");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService");
        this.trustedContactTokenService = Objects.requireNonNull(trustedContactTokenService, "trustedContactTokenService");
    }

    /**
     * Builds the {@code <tctoken>} child for the recipient chat, or returns {@code null} when
     * emission does not apply.
     *
     * <p>Returns {@code null} when any of the four gates fails:
     * {@link ABProp#PRIVACY_TOKEN_SENDING_ON_ALL_1_ON_1_MESSAGES} is disabled, the chat is not found
     * in the store, the chat has no recorded TC token, or the token timestamp is past the
     * receiver-side expiry cutoff.
     *
     * @param chatJid the recipient chat {@link Jid}
     * @return the {@code <tctoken>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza build(Jid chatJid) {
        var tcTokenEnabled = abPropsService.getBool(ABProp.PRIVACY_TOKEN_SENDING_ON_ALL_1_ON_1_MESSAGES);
        if (!tcTokenEnabled) {
            return null;
        }

        var chat = store.chatStore().findChatByJid(chatJid).orElse(null);
        if (chat == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tctoken build skipped: chat {0} not found", chatJid);
            return null;
        }

        var tcToken = chat.tcToken().orElse(null);
        var tcTokenTimestamp = chat.tcTokenTimestamp().orElse(null);

        if (tcToken == null || tcTokenTimestamp == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tctoken build skipped: no token recorded for chat {0}", chatJid);
            return null;
        }

        if (trustedContactTokenService.hasTokenExpired(tcTokenTimestamp, TrustedContactTokenService.TcTokenMode.RECEIVER)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tctoken build skipped: token expired for chat {0}", chatJid);
            return null;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tctoken attached for chat {0}", chatJid);
        return new StanzaBuilder()
                .description("tctoken")
                .content(tcToken)
                .build();
    }
}
