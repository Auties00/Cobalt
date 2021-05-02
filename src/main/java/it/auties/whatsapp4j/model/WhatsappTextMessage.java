package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
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
    protected final @NotNull String text;

    /**
     * Constructs a WhatsappTextMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappTextMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().hasExtendedTextMessage() || info.getMessage().hasConversation());
        this.text = info.getMessage().hasConversation() ? info.getMessage().getConversation() : info.getMessage().getExtendedTextMessage().getText();
    }

    /**
     * Constructs a new builder to create a WhatsappMediaMessage that wraps an image.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param forwarded     whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newTextMessage", buildMethodName = "create")
    public WhatsappTextMessage(@NotNull(message = "Cannot create a WhatsappTextMessage with no chat") WhatsappChat chat, @NotNull(message = "Cannot create a WhatsappTextMessage with no text") String text, List<WhatsappContact> mentions, WhatsappUserMessage quotedMessage, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createTextMessage(text, quotedMessage, mentions, forwarded), chat.jid()));
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
