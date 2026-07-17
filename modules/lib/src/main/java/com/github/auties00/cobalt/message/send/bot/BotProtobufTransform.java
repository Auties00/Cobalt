package com.github.auties00.cobalt.message.send.bot;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.bot.feedback.BotFeedbackMessage;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.system.FutureProofMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Applies the bot-specific protobuf rewrites that precede encryption of a
 * message bound for a bot or FBID-bot recipient.
 *
 * <p>Each method mutates the supplied {@link LinkedMessageContainer} in place via
 * setters on the {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo}
 * or {@link com.github.auties00.cobalt.wire.core.message.MessageKey} it carries, so
 * the same container instance must not be shared across recipient fanouts that
 * need different transforms. The transforms run on a per-device copy of the
 * proto right before
 * {@link com.github.auties00.cobalt.message.send.crypto.MessageEncryption#encryptForDevice}.
 */
@WhatsAppWebModule(moduleName = "WAWebE2EProtoGenerator")
public final class BotProtobufTransform {
    /**
     * The logger for {@link BotProtobufTransform}.
     */
    private static final System.Logger LOGGER = Log.get(BotProtobufTransform.class);

    /**
     * Holds the store consulted for LID-to-PN lookups when retargeting FBID-bot
     * participants.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Constructs a transform bound to the given store.
     *
     * <p>The bound store provides JID resolution; its
     * {@link LinkedWhatsAppContactStore#findLidByPhone(com.github.auties00.cobalt.wire.core.jid.Jid)}
     * upgrades legacy PN participants to LID before the FBID-bot transforms emit
     * the key.
     *
     * @param store the store providing JID resolution
     * @throws NullPointerException if {@code store} is {@code null}
     */
    public BotProtobufTransform(LinkedWhatsAppStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * Applies the CAPI bot-invoke transform to the supplied container.
     *
     * <p>Replaces the user-level message secret with the supplied
     * {@code botMessageSecret} derived via {@link BotMessageSecret#derive(byte[])},
     * strips the quoted-message body from the {@link ContextInfo} when the quoted
     * author is not itself a bot, and clears the {@code remoteJid} on every
     * carried protocol-message key.
     *
     * @implNote
     * This implementation passes {@code null} for {@code botMessageSecret}
     * through unchanged; the caller is expected to supply the already-derived
     * bot secret rather than re-derive it per fanout.
     *
     * @param container        the {@link LinkedMessageContainer} to mutate
     * @param botMessageSecret the derived bot message secret to install, or
     *                         {@code null} to merely clear the user secret
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "updateBotInvokeMsgProtoCopyForCapi",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void transformForCapi(LinkedMessageContainer container, byte[] botMessageSecret) {
        container.messageContextInfo().ifPresent(info -> {
            info.setMessageSecret(null);
            info.setBotMessageSecret(botMessageSecret);
        });
        stripQuotedMessageForNonBot(container);
        stripProtocolMessageRemoteJid(container);
    }

    /**
     * Applies the FBID-bot transform that upgrades quoted-message participant
     * JIDs from PN to LID.
     *
     * <p>The server only accepts LID-form participant JIDs on the
     * {@link ContextInfo#quotedMessageSenderJid()} field for messages destined to
     * an FBID (Facebook-account) bot, so any leftover PN is rewritten via
     * {@link LinkedWhatsAppContactStore#findLidByPhone(com.github.auties00.cobalt.wire.core.jid.Jid)}.
     * Participants already on a bot server are left untouched.
     *
     * @param container the {@link LinkedMessageContainer} to mutate
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "updateFbidBotProtobuf",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void transformForFbidBot(LinkedMessageContainer container) {
        var contextInfo = resolveInnerContextInfo(container);
        if (contextInfo == null) {
            return;
        }

        var participant = contextInfo.quotedMessageSenderJid();
        if (participant.isEmpty() || participant.get().hasBotServer()) {
            return;
        }

        store.contactStore().findLidByPhone(participant.get())
                .ifPresent(lid -> {
                    contextInfo.setQuotedMessageSenderJid(lid);
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "upgraded quoted sender to lid for fbid bot");
                    }
                });
    }

    /**
     * Applies the FBID-bot-invoke transform that retargets protocol-message key
     * senders from PN to LID.
     *
     * <p>Rewrites the {@link com.github.auties00.cobalt.wire.core.message.MessageKey#senderJid()}
     * of the {@link ProtocolMessage#key()} payload to its LID form so the FBID
     * bot can resolve the originating user. Senders already on a bot or LID
     * server are left untouched.
     *
     * @param container the {@link LinkedMessageContainer} to mutate
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "updateFbidBotInvokeProtobuf",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void transformForFbidBotInvoke(LinkedMessageContainer container) {
        if (!(container.content() instanceof ProtocolMessage pm)) {
            return;
        }

        var key = pm.key().orElse(null);
        if (key == null) {
            return;
        }

        var participant = key.senderJid().orElse(null);
        if (participant == null || participant.hasBotServer() || participant.hasLidServer()) {
            return;
        }

        store.contactStore().findLidByPhone(participant).ifPresent(lid -> {
            key.setSenderJid(lid);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "upgraded protocol key sender to lid for fbid bot invoke");
            }
        });
    }

    /**
     * Applies the generic bot transform that strips identifying fields from
     * protocol-message keys.
     *
     * <p>Clears the {@code parentJid} and the {@code senderJid} on the
     * {@link com.github.auties00.cobalt.wire.core.message.MessageKey} of the carried
     * {@link ProtocolMessage} so the bot does not observe the user-side
     * addressing context.
     *
     * @param container the {@link LinkedMessageContainer} to mutate
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "updateBotProtobuf",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void transformForBot(LinkedMessageContainer container) {
        if (!(container.content() instanceof ProtocolMessage pm)) {
            return;
        }

        pm.key().ifPresent(key -> {
            key.setParentJid(null);
            key.setSenderJid(null);
        });
    }

    /**
     * Returns the inner {@link ContextInfo} carried by the container, unwrapping
     * a {@link FutureProofMessage} bot-invoke wrapper transparently.
     *
     * <p>Lets {@link #transformForFbidBot(LinkedMessageContainer)} and
     * {@link #stripQuotedMessageForNonBot(LinkedMessageContainer)} treat both wire
     * shapes the same.
     *
     * @param container the {@link LinkedMessageContainer} to inspect
     * @return the inner {@link ContextInfo}, or {@code null} when none is present
     */
    private static ContextInfo resolveInnerContextInfo(LinkedMessageContainer container) {
        return container.content() instanceof ContextualMessage contextualMessage
                ? contextualMessage.contextInfo().orElse(null)
                : null;
    }

    /**
     * Clears the quoted-message body when the quoted author is not itself a bot.
     *
     * <p>Keeps bot-to-bot quotes intact so a bot can reference another bot's
     * reply, while stripping user-to-user quote chains so the bot sees only the
     * immediate prompt.
     *
     * @param container the {@link LinkedMessageContainer} to mutate
     */
    private static void stripQuotedMessageForNonBot(LinkedMessageContainer container) {
        var contextInfo = resolveInnerContextInfo(container);
        if (contextInfo == null) {
            return;
        }

        var participant = contextInfo.quotedMessageSenderJid().orElse(null);
        if (participant == null || participant.hasBotServer()) {
            return;
        }

        contextInfo.clearQuotedMessage();
    }

    /**
     * Strips the {@code parentJid} from every protocol-message key carried by the
     * container so the bot does not observe the user-side parent thread.
     *
     * <p>Clears the parent on both the protocol message's primary key and the
     * optional {@link BotFeedbackMessage#messageKey()}.
     *
     * @param container the {@link LinkedMessageContainer} to mutate
     */
    private static void stripProtocolMessageRemoteJid(LinkedMessageContainer container) {
        if (!(container.content() instanceof ProtocolMessage pm)) {
            return;
        }

        pm.key().ifPresent(key -> key.setParentJid(null));
        pm.botFeedbackMessage()
                .flatMap(BotFeedbackMessage::messageKey)
                .ifPresent(key -> key.setParentJid(null));
    }
}
