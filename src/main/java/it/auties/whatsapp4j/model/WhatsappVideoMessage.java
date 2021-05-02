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
 * A model class that represents a WhatsappMessage sent by a contact and that holds an video inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@ToString
public sealed class WhatsappVideoMessage extends WhatsappMediaMessage permits WhatsappGifMessage {
    /**
     * The caption, that is the text below the video, of this video message
     */
    private final @NotNull String caption;

    /**
     * Constructs a WhatsappVideoMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     * @param condition the condition to meet
     */
    public WhatsappVideoMessage(@NotNull WhatsappProtobuf.WebMessageInfo info, boolean condition) {
        super(info, WhatsappMediaMessageType.VIDEO, info.getMessage().hasVideoMessage() && condition);
        this.caption = info.getMessage().getVideoMessage().getCaption();
    }

    /**
     * Constructs a WhatsappVideoMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappVideoMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        this(info, true);
    }

    /**
     * Constructs a new builder to create a WhatsappMediaMessage that wraps an video.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param media         the non null video that the new message holds
     * @param caption       the caption of the new message, by default empty
     * @param mimeType      the mime type of the new message, by default {@link WhatsappMediaMessageType#defaultMimeType()}
     * @param quotedMessage the message that the new message should quote, by default empty
     * @param forwarded     whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newVideoMessage", buildMethodName = "create")
    public WhatsappVideoMessage(@NotNull(message = "Cannot create a WhatsappMediaMessage(Video) with no chat") WhatsappChat chat, byte @NotNull(message = "Cannot create a WhatsappMediaMessage(Video) with no video") [] media, String caption, String mimeType, WhatsappUserMessage quotedMessage, List<WhatsappContact> captionMentions, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createVideoMessage(media, mimeType, null, caption, quotedMessage, captionMentions, forwarded), chat.jid()));
    }

    /**
     * Returns an optional String representing the caption of this video message
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
        return info.getMessage().getVideoMessage().hasContextInfo() ? Optional.of(info.getMessage().getVideoMessage().getContextInfo()) : Optional.empty();
    }
}
