package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.model.WhatsappUserMessage;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * An utility class used to make it easier to build a raw {@link it.auties.whatsapp4j.model.WhatsappProtobuf.WebMessageInfo}.
 * This class specifies the content, recipient and metadata of said message.
 * Various named constructors are available to initialize this class easily.
 * An associated builder class is accessible using the static {@link WhatsappMessageRequest#builder()} method.
 */
@Builder
@Accessors(fluent = true)
public class WhatsappMessageRequest {
    private final @NotNull WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();
    private final @NotNull @Default String id = WhatsappUtils.randomId();
    private final @NotNull String recipient;
    private final @NotNull String text;
    private final @Nullable WhatsappUserMessage quotedMessage;
    private final boolean forwarded;

    public static WhatsappMessageRequest ofText(@NotNull String recipient, @NotNull String text) {
        return WhatsappMessageRequest
                .builder()
                .recipient(recipient)
                .text(text)
                .build();
    }

    public static WhatsappMessageRequest ofQuotedText(@NotNull String recipient, @NotNull String text, @NotNull WhatsappUserMessage quotedMessage) {
        return WhatsappMessageRequest
                .builder()
                .recipient(recipient)
                .quotedMessage(quotedMessage)
                .text(text)
                .build();
    }

    public @NotNull WhatsappProtobuf.WebMessageInfo buildMessage() {
        if (quotedMessage == null) {
            return applyKey(!forwarded ? WhatsappProtobuf.WebMessageInfo.newBuilder().setMessage(WhatsappProtobuf.Message.newBuilder().setConversation(text)) : WhatsappProtobuf.WebMessageInfo.newBuilder()
                    .setMessage(WhatsappProtobuf.Message.newBuilder()
                            .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
                                    .setText(text)
                                    .setContextInfo(WhatsappProtobuf.ContextInfo.newBuilder().setIsForwarded(true).build())
                                    .build())
                            .build()));
        }

        return applyKey(WhatsappProtobuf.WebMessageInfo.newBuilder()
                .setMessage(WhatsappProtobuf.Message.newBuilder()
                        .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
                                .setText(text)
                                .setContextInfo(WhatsappProtobuf.ContextInfo.newBuilder()
                                        .setQuotedMessage(quotedMessage.info().getMessage())
                                        .setParticipant(quotedMessage.senderJid().orElse(MANAGER.phoneNumber()))
                                        .setStanzaId(quotedMessage.info().getKey().getId())
                                        .setRemoteJid(quotedMessage.info().getKey().getRemoteJid())
                                        .setIsForwarded(forwarded)
                                        .build())
                                .build())
                        .build()));
    }

    private @NotNull WhatsappProtobuf.WebMessageInfo applyKey(@NotNull WhatsappProtobuf.WebMessageInfo.Builder builder) {
        return builder
                .setKey(WhatsappProtobuf.MessageKey.newBuilder()
                        .setFromMe(true)
                        .setRemoteJid(recipient)
                        .setId(WhatsappUtils.randomId())
                        .build())
                .setMessageTimestamp(Instant.now().getEpochSecond())
                .setStatus(WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.PENDING)
                .build();
    }
}
