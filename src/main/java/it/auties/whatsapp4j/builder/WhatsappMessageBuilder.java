package it.auties.whatsapp4j.builder;

import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappMessage;
import it.auties.whatsapp4j.model.WhatsappUserMessage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor
@Data
abstract class WhatsappMessageBuilder<R> {
    /**
     * The chat where this message is stored
     */
    protected @Nullable @Setter WhatsappChat chat;

    /**
     * A nullable {@link WhatsappMessage} representing the message quoted by this message if in memory
     */
    protected @Nullable @Setter WhatsappUserMessage quotedMessage;

    /**
     * Whether this message was forwarded or not
     */
    protected @Setter boolean forwarded;

    /**
     * Builds a WhatsappMessage from the data provided
     *
     * @return a non null object
     */
    public abstract @NotNull R create();
}
