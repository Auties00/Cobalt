package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents the body of a product
 */
@ProtobufMessage(name = "Message.InteractiveResponseMessage.Body")
public record InteractiveResponseBody(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String content
) {
    public static Optional<InteractiveResponseBody> ofNullable(String content) {
        return Optional.ofNullable(content)
                .map(InteractiveResponseBody::new);
    }
}