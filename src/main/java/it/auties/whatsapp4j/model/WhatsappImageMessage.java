package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an image inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@ToString
public final class WhatsappImageMessage extends WhatsappMediaMessage {
    /**
     * The caption, that is the text below the image, of this image message
     */
    private final @NotNull String caption;

    /**
     * Constructs a WhatsappImageMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappImageMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, WhatsappMediaMessageType.IMAGE, info.getMessage().hasImageMessage());
        this.caption = info.getMessage().getImageMessage().getCaption();
    }

    /**
     * Constructs a new builder to create a WhatsappMediaMessage that wraps an image.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param media         the non null image that the new message holds
     * @param caption       the caption of the new message, by default empty
     * @param mimeType      the mime type of the new message, by default {@link WhatsappMediaMessageType#defaultMimeType()}
     * @param quotedMessage the message that the new message should quote, by default empty
     * @param forwarded     whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newImageMessage", buildMethodName = "create")
    public WhatsappImageMessage(@NotNull(message = "Cannot create a WhatsappMediaMessage(Image) with no chat") WhatsappChat chat, byte @NotNull(message = "Cannot create a WhatsappMediaMessage(Image) with no image") [] media, String caption, String mimeType, WhatsappUserMessage quotedMessage, List<WhatsappContact> captionMentions, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createImageMessage(media, mimeType, caption, quotedMessage, captionMentions, forwarded), chat.jid()));
    }

    /**
     * Returns an optional String representing the caption of this image message
     *
     * @return a non empty optional if this message has a caption
     */
    public @NotNull Optional<String> caption(){
        return caption.isBlank() ? Optional.empty() : Optional.of(caption);
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        return info.getMessage().getImageMessage().hasContextInfo() ? Optional.of(info.getMessage().getImageMessage().getContextInfo()) : Optional.empty();
    }
}
