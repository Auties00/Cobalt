package it.auties.whatsapp4j.builder;

import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@NoArgsConstructor
@Accessors(fluent = true)
public class WhatsappMediaMessageBuilder implements WhatsappMessageBuilder<WhatsappMediaMessage> {
    /**
     * The chat where this message is stored
     */
    private @Setter WhatsappChat chat;

    /**
     * A nullable {@link WhatsappMessage} representing the message quoted by this message if in memory
     */
    private @Setter WhatsappUserMessage quotedMessage;

    /**
     * Whether this message was forwarded or not
     */
    private @Setter boolean forwarded;

    /**
     * The raw media that this message holds
     */
    private @Setter byte [] media;

    /**
     * The type of media that this object wraps
     */
    private @Setter WhatsappMediaMessageType type;

    /**
     * The raw media that this message holds
     */
    private @Setter String caption;

    /**
     * Builds a {@link WhatsappMediaMessage} from the data provided
     *
     * @return a non null WhatsappMediaMessage
     */
    @Override
    public @NotNull WhatsappMediaMessage create() {
        Objects.requireNonNull(chat, "WhatsappAPI: Cannot create a WhatsappText with a null chat");
        Objects.requireNonNull(media, "WhatsappAPI: Cannot create a WhatsappText with a null media");
        Objects.requireNonNull(type, "WhatsappAPI: Cannot create a WhatsappMediaMessage with a null type");
        return new WhatsappMediaMessage(ProtobufUtils.createMessageInfo(ProtobufUtils.createMediaMessage(caption, media, type, quotedMessage, forwarded), chat.jid()));
    }
}
