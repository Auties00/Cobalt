package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.send.bot.BotMessageSecret;
import com.github.auties00.cobalt.message.send.bot.BotProtobufTransform;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Builds the optional {@code <bot>} child of an outgoing {@code <message>} stanza, both in its encrypted-fanout form and
 * its metadata-only form.
 * <p>
 * Two surfaces share the {@code <bot>} stanza. The encrypted-fanout form
 * {@code <bot><to jid="...@bot"><enc .../></to></bot>} delivers a separately encrypted copy of the body to a Meta AI bot
 * device or to the original bot for a feedback message. The metadata-only form
 * {@code <bot type="..." local_automated_type="..." client_thread_id="..." />} carries routing attributes for AI thread
 * bookkeeping, business-bot classification, and the user's AI-mode selection. {@link ChatFanoutStanza} composes the two
 * forms as siblings; this class produces the one or two nodes.
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgCreateFanoutStanza")
@WhatsAppWebModule(moduleName = "WAWebSendGroupSkmsgJob")
public final class BotStanza {
    /**
     * The logger for {@link BotStanza}.
     */
    private static final System.Logger LOGGER = Log.get(BotStanza.class);

    /**
     * Encrypts the bot body for the bot device.
     */
    private final MessageEncryption encryption;

    /**
     * Transforms the message body before bot-targeted encryption.
     */
    private final BotProtobufTransform protobufTransform;

    /**
     * Constructs a builder backed by the given encryption and bot transform services.
     * <p>
     * The bot body is recomputed per send, so the instance is reusable across sends.
     *
     * @param encryption        the {@link MessageEncryption} service
     * @param protobufTransform the {@link BotProtobufTransform} service
     * @throws NullPointerException if any argument is {@code null}
     */
    public BotStanza(MessageEncryption encryption, BotProtobufTransform protobufTransform) {
        this.encryption = Objects.requireNonNull(encryption, "encryption");
        this.protobufTransform = Objects.requireNonNull(protobufTransform, "protobufTransform");
    }

    /**
     * Builds the encrypted {@code <bot>} stanza for a 1:1 fanout send.
     * <p>
     * Emitted whenever the chat targets a bot directly (chat JID ends in {@code @bot}) or the message is a bot-feedback
     * {@link ProtocolMessage} whose original key identifies a bot sender. Returns {@code null} when no bot is involved or
     * when encryption fails.
     *
     * @implNote This implementation derives a bot-scoped message secret via {@link BotMessageSecret#derive(byte[])},
     * applies the CAPI, FBID, and bot protobuf transforms in order, encrypts the result for the resolved bot device with
     * {@link MessageEncryption#encryptForDevice(Jid, byte[])}, and wraps the ciphertext in
     * {@code <bot><to jid="..."><enc .../></to></bot>}. The {@code type="feedback"} outer attribute is set only for bot
     * feedback messages.
     *
     * @param messageInfo the outgoing {@link ChatMessageInfo}
     * @param chatJid     the recipient chat {@link Jid}
     * @return the {@code <bot>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza build(ChatMessageInfo messageInfo, Jid chatJid) {
        var botJid = resolveBotJid(messageInfo, chatJid);
        if (botJid == null) {
            return null;
        }

        var isFeedback = isBotFeedback(messageInfo);
        var container = messageInfo.message();

        var messageSecret = container.messageContextInfo()
                .flatMap(ChatMessageContextInfo::messageSecret)
                .orElse(null);
        byte[] botSecret = null;
        if (messageSecret != null) {
            try {
                botSecret = BotMessageSecret.derive(messageSecret);
            } catch (GeneralSecurityException e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "failed to derive bot message secret", e);
                }
            }
        }

        protobufTransform.transformForCapi(container, botSecret);
        if (isFbidBot(botJid)) {
            protobufTransform.transformForFbidBot(container);
        }
        protobufTransform.transformForBot(container);

        var plaintext = LinkedMessageContainerSpec.encode(container);
        MessageEncryptedPayload payload;
        try {
            payload = encryption.encryptForDevice(botJid, plaintext);
        } catch (Exception e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "bot encryption failed for " + new LogRedactable.User(String.valueOf(botJid)), e);
            }
            return null;
        }

        var encNode = new StanzaBuilder()
                .description("enc")
                .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                .attribute("type", payload.type().protocolValue())
                .content(payload.ciphertext())
                .build();
        var toNode = new StanzaBuilder()
                .description("to")
                .attribute("jid", botJid)
                .content(encNode)
                .build();
        return new StanzaBuilder()
                .description("bot")
                .attribute("type", isFeedback ? "feedback" : null)
                .content(toNode)
                .build();
    }

    /**
     * Builds the metadata-only {@code <bot>} stanza carrying the bot-routing attributes that the server uses for AI thread
     * bookkeeping and analytics.
     * <p>
     * The {@code type} attribute is one of {@code "prompt"}, {@code "command"}, {@code "request_welcome"}, or
     * {@code "feedback"}. The {@code local_automated_type} attribute carries the business-bot class
     * ({@code "1p_partial"} for first-party partial integrations, {@code "3p_full"} for third-party full integrations).
     * The {@code client_thread_id} attribute carries the AI conversation thread id. The {@code mode_selection} attribute
     * carries the user-selected AI mode label such as {@code "default"} or {@code "think_hard"}, and
     * {@code mode_selected} carries a dynamic-mode override string. Returns {@code null} when no attribute applies so
     * {@link ChatFanoutStanza} can suppress the empty stanza.
     *
     * @param botMsgBodyType the bot message body type, or {@code null}
     * @param bizBotType     the business-bot classification, or {@code null}
     * @param clientThreadId the AI conversation thread id, or {@code null}
     * @param modeSelection  the user-selected AI mode label, or {@code null}
     * @param modeSelected   the dynamic mode override string, or {@code null}
     * @return the {@code <bot>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Stanza buildMetadata(
            String botMsgBodyType,
            String bizBotType,
            String clientThreadId,
            String modeSelection,
            String modeSelected
    ) {
        if (botMsgBodyType == null && bizBotType == null && clientThreadId == null
                && modeSelection == null && modeSelected == null) {
            return null;
        }

        return new StanzaBuilder()
                .description("bot")
                .attribute("type", botMsgBodyType)
                .attribute("local_automated_type", bizBotType)
                .attribute("client_thread_id", clientThreadId)
                .attribute("mode_selection", modeSelection)
                .attribute("mode_selected", modeSelected)
                .build();
    }

    /**
     * Builds the metadata-only {@code <bot>} stanza without the AI mode selection attributes.
     * <p>
     * Delegates to {@link #buildMetadata(String, String, String, String, String)} with the last two arguments
     * {@code null}, for senders that do not run the AI mode selector surface.
     *
     * @param botMsgBodyType the bot message body type, or {@code null}
     * @param bizBotType     the business-bot classification, or {@code null}
     * @param clientThreadId the AI conversation thread id, or {@code null}
     * @return the {@code <bot>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Stanza buildMetadata(
            String botMsgBodyType,
            String bizBotType,
            String clientThreadId
    ) {
        return buildMetadata(botMsgBodyType, bizBotType, clientThreadId, null, null);
    }

    /**
     * Builds the encrypted {@code <bot>} stanza for a group SKMSG send into an open-bot group.
     * <p>
     * When the group has the open Meta AI bot enabled, a separately encrypted copy of the body is sent to the Meta AI
     * FBID bot account in addition to the SKMSG-encrypted group payload. Returns {@code null} when {@code isOpenBotGroup}
     * is {@code false} or when bot encryption fails.
     *
     * @implNote This implementation always targets {@link Jid#metaAiBotAccount()}. The CAPI transform runs before the
     * generic bot transform because the open-bot path delivers the message into a multi-user thread.
     *
     * @param messageInfo    the outgoing {@link ChatMessageInfo}
     * @param isOpenBotGroup whether the group has the open Meta AI bot enabled
     * @return the {@code <bot>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupSkmsgJob", exports = "encryptAndSendSenderKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildForGroup(ChatMessageInfo messageInfo, boolean isOpenBotGroup) {
        if (!isOpenBotGroup) {
            return null;
        }

        var botJid = Jid.metaAiBotAccount();
        var container = messageInfo.message();

        var messageSecret = container.messageContextInfo()
                .flatMap(ChatMessageContextInfo::messageSecret)
                .orElse(null);
        byte[] botSecret = null;
        if (messageSecret != null) {
            try {
                botSecret = BotMessageSecret.derive(messageSecret);
            } catch (GeneralSecurityException e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "failed to derive bot message secret for open group bot", e);
                }
            }
        }

        protobufTransform.transformForCapi(container, botSecret);
        protobufTransform.transformForBot(container);

        var plaintext = LinkedMessageContainerSpec.encode(container);
        MessageEncryptedPayload payload;
        try {
            payload = encryption.encryptForDevice(botJid, plaintext);
        } catch (Exception e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "open group bot encryption failed", e);
            }
            return null;
        }

        var encNode = new StanzaBuilder()
                .description("enc")
                .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                .attribute("type", payload.type().protocolValue())
                .content(payload.ciphertext())
                .build();
        var toNode = new StanzaBuilder()
                .description("to")
                .attribute("jid", botJid)
                .content(encNode)
                .build();
        return new StanzaBuilder()
                .description("bot")
                .content(toNode)
                .build();
    }

    /**
     * Resolves the bot device {@link Jid} that should receive the encrypted bot body for this send.
     * <p>
     * Two cases are recognised: the chat itself is a bot account (the chat JID server is {@code "bot"}), or the message
     * is a bot-feedback protocol message whose original {@link MessageKey#senderJid()} identifies a bot. Anything else
     * returns {@code null}, suppressing the {@code <bot>} child.
     *
     * @param messageInfo the outgoing {@link ChatMessageInfo}
     * @param chatJid     the recipient chat {@link Jid}
     * @return the bot device {@link Jid}, or {@code null} when no bot is involved
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static Jid resolveBotJid(ChatMessageInfo messageInfo, Jid chatJid) {
        if (chatJid.hasBotServer()) {
            return chatJid;
        }

        if (isBotFeedback(messageInfo)
                && messageInfo.message().content() instanceof ProtocolMessage pm) {
            return pm.key()
                    .flatMap(MessageKey::senderJid)
                    .filter(Jid::hasBotServer)
                    .orElse(null);
        }

        return null;
    }

    /**
     * Returns whether the outgoing message is a bot-feedback {@link ProtocolMessage}.
     * <p>
     * Bot feedback rewires the {@code <bot>} child to address the original bot rather than the chat target, and forces
     * {@code type="feedback"} on the wrapping stanza.
     *
     * @param messageInfo the outgoing {@link ChatMessageInfo}
     * @return {@code true} when the wrapped content is a {@link ProtocolMessage.Type#BOT_FEEDBACK_MESSAGE}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgGetters", exports = "getIsBotFeedbackMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isBotFeedback(ChatMessageInfo messageInfo) {
        return messageInfo.message().content() instanceof ProtocolMessage pm
                && pm.type().orElse(null) == ProtocolMessage.Type.BOT_FEEDBACK_MESSAGE;
    }

    /**
     * Returns whether the given {@link Jid} identifies an FBID bot account.
     * <p>
     * The FBID bot family lives under the dedicated {@code @bot} JID server, as opposed to Meta AI's legacy PN-form bot
     * under {@code @c.us}.
     *
     * @implNote This implementation qualifies a JID when its server is {@code "bot"} and the device id is either absent
     * or zero (the primary device); the user component is not inspected.
     *
     * @param jid the {@link Jid} to test
     * @return {@code true} when the JID is an FBID bot
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isFbidBot",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isFbidBot(Jid jid) {
        return jid.hasBotServer() && jid.isPrimaryDevice();
    }
}
