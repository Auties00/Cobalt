package it.auties.whatsapp4j.builder;

import it.auties.whatsapp4j.model.WhatsappTextMessage;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Accessors(fluent = true)
public class WhatsappTextMessageBuilder extends WhatsappMessageBuilder<WhatsappTextMessage> {
    /**
     * The text of this message
     */
    private  @Setter String text;

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
