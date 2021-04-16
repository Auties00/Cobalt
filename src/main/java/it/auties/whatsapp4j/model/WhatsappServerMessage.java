package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by Whatsapp.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class WhatsappServerMessage extends WhatsappMessage {
    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappServerMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, !info.hasMessage());
    }

    /**
     * Returns a non null enumerated type describing the type of this server message
     *
     * @return the non null global status of this message
     */
    public @NotNull WhatsappProtobuf.WebMessageInfo.WebMessageInfoStubType type(){
        return info.getMessageStubType();
    }

    /**
     * Returns always an empty optional as a message sent by whatsapp cannot have context
     *
     * @return an empty optional
     */
    @Override
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        return Optional.empty();
    }
}
