package it.auties.whatsapp4j.builder;

import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappMessage;
import it.auties.whatsapp4j.model.WhatsappTextMessage;
import it.auties.whatsapp4j.model.WhatsappUserMessage;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@NoArgsConstructor
@Accessors(fluent = true)
public class WhatsappTextMessageBuilder implements WhatsappMessageBuilder<WhatsappTextMessage> {
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
     * The text of this message
     */
    private @Setter String text;

    /**
     * Builds a {@link WhatsappTextMessage} from the data provided
     *
     * @return a non null WhatsappTextMessage
     */
    @Override
    public @NotNull WhatsappTextMessage create() {
        Objects.requireNonNull(chat, "WhatsappAPI: Cannot create a WhatsappText with a null chat");
        Objects.requireNonNull(text, "WhatsappAPI: Cannot create a WhatsappText with a null text");
        return new WhatsappTextMessage(ProtobufUtils.createMessageInfo(ProtobufUtils.createTextMessage(text, quotedMessage, forwarded), chat.jid()));
    }
}
