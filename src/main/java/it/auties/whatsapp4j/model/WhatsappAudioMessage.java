package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an audio inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@ToString
public final class WhatsappAudioMessage extends WhatsappMediaMessage {
    /**
     * The length of this message in seconds
     */
    private final long lengthInSeconds;

    /**
     * Whether this message is a voice message or a simple audio message
     */
    private final @Getter boolean voiceMessage;

    /**
     * Constructs a WhatsappImageMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappAudioMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, WhatsappMediaMessageType.AUDIO, info.getMessage().hasAudioMessage());
        var audio = info.getMessage().getAudioMessage();
        this.voiceMessage = audio.getPtt();
        this.lengthInSeconds = audio.getSeconds();
    }

    /**
     * Constructs a new builder to create a WhatsappMediaMessage that wraps an audio or a voice message.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param media         the non null image that the new message holds
     * @param mimeType      the mime type of the new message, by default {@link WhatsappMediaMessageType#defaultMimeType()}
     * @param voiceMessage  whether the new message should be considered as a voice message or as a normal audio, by default the latter is used
     * @param quotedMessage the message that the new message should quote, by default empty
     * @param forwarded     whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newAudioMessage", buildMethodName = "create")
    public WhatsappAudioMessage(@NotNull(message = "Cannot create a WhatsappMediaMessage(Audio) with no chat") WhatsappChat chat, byte @NotNull(message = "Cannot create a WhatsappMediaMessage(Audio) with no image") [] media, @NotNull(message = "Cannot create a WhatsappMediaMessage(Audio) with no mime type") String mimeType, boolean voiceMessage, WhatsappUserMessage quotedMessage, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createAudioMessage(media, mimeType, voiceMessage, quotedMessage, forwarded), chat.jid()));
    }

    /**
     * Returns an optional Long representing the length of this audio in seconds
     *
     * @return a non empty optional if this message has a length in seconds
     */
    public @NotNull Optional<Long> title() {
        return lengthInSeconds == 0 ? Optional.empty() : Optional.of(lengthInSeconds);
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        return info.getMessage().getAudioMessage().hasContextInfo() ? Optional.of(info.getMessage().getAudioMessage().getContextInfo()) : Optional.empty();
    }
}
