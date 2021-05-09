package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a sticker inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@ToString
public final class WhatsappStickerMessage extends WhatsappMediaMessage {
    /**
     * Constructs a WhatsappGifMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappStickerMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, WhatsappMediaMessageType.STICKER, info.getMessage().hasStickerMessage());
    }
    
    /**
     * Constructs a new builder to create a WhatsappMediaMessage that wraps a WhatsappStickerMessage.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param media         the non null image that the new message holds
     * @param quotedMessage the message that the new message should quote, by default empty
     * @param forwarded     whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newStickerMessage", buildMethodName = "create")
    public WhatsappStickerMessage(@NotNull(message = "Cannot create a WhatsappMediaMessage(Sticker) with no chat") WhatsappChat chat, byte @NotNull(message = "Cannot create a WhatsappMediaMessage(Sticker) with no image") [] media, WhatsappUserMessage quotedMessage, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createStickerMessage(media, quotedMessage, forwarded), chat.jid()));
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        return info.getMessage().getStickerMessage().hasContextInfo() ? Optional.of(info.getMessage().getStickerMessage().getContextInfo()) : Optional.empty();
    }
}
