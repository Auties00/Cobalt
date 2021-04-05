package it.auties.whatsapp4j.builder;

import it.auties.whatsapp4j.model.WhatsappMediaMessage;
import it.auties.whatsapp4j.model.WhatsappMediaMessageType;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Accessors(fluent = true)
public class WhatsappMediaMessageBuilder extends WhatsappMessageBuilder<WhatsappMediaMessage> {
    /**
     * The raw media that this message holds
     */
    private @Setter byte @Nullable [] media;

    /**
     * The type of media that this object wraps
     */
    private @Nullable @Setter WhatsappMediaMessageType type;

    /**
     * The raw media that this message holds
     */
    private @Nullable @Setter String caption;

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
        return new WhatsappMediaMessage(ProtobufUtils.createMessageInfo(ProtobufUtils.createMediaMessage(caption, media, type), chat.jid()));
    }
}
