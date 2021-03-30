package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds text inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
@ToString
public final class WhatsappTextMessage extends WhatsappUserMessage {
    /**
     * The text wrapped by this object
     */
    private final @NotNull String text;

    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappTextMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().hasExtendedTextMessage() || info.getMessage().hasConversation());
        this.text = info.getMessage().hasConversation() ? info.getMessage().getConversation() : info.getMessage().getExtendedTextMessage().getText();
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        if(!info.getMessage().hasExtendedTextMessage()){
            return Optional.empty();
        }

        return info.getMessage().getExtendedTextMessage().hasContextInfo() ? Optional.of(info.getMessage().getExtendedTextMessage().getContextInfo()) : Optional.empty();
    }
}
