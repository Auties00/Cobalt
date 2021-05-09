package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.model.WhatsappProtobuf.VideoMessage.VideoMessageAttribution;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an video inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@ToString
public final class WhatsappGifMessage extends WhatsappVideoMessage {
    /**
     * The source from where this gif comes from
     */
    private final @NotNull VideoMessageAttribution attribution;

    /**
     * Constructs a WhatsappGifMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappGifMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().getVideoMessage().hasGifPlayback());
        this.attribution = info.getMessage().getVideoMessage().getGifAttribution();
    }

    /**
     * Constructs a new builder to create a WhatsappMediaMessage that wraps a video that will be played as a gif.
     * Wrapping a gif file instead of a video will result in an exception if detected or in an unplayable message.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param media         the non null image that the new message holds
     * @param mimeType      the mime type of the new message, by default {@link WhatsappMediaMessageType#defaultMimeType()}
     * @param quotedMessage the message that the new message should quote, by default empty
     * @param forwarded     whether this message is forwarded or not, by default false
     * @throws IllegalArgumentException if {@code mimeType} == image/gif or if the media is detected to be a gif file
     */
    @Builder(builderMethodName = "newGifMessage", buildMethodName = "create")
    public WhatsappGifMessage(@NotNull(message = "Cannot create a WhatsappMediaMessage(Gif) with no chat") WhatsappChat chat, byte @NotNull(message = "Cannot create a WhatsappMediaMessage(Gif) with no image") [] media, String mimeType, VideoMessageAttribution attribution, String caption, WhatsappUserMessage quotedMessage, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createVideoMessage(media, mimeType, Optional.ofNullable(attribution).orElse(VideoMessageAttribution.NONE), caption, quotedMessage, forwarded), chat.jid()));
    }
}