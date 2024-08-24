package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents the body of a product
 */
@ProtobufMessage(name = "Message.InteractiveMessage.Body")
public record InteractiveBody(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String content
) {
    public static Optional<InteractiveBody> ofNullable(String content) {
        return Optional.ofNullable(content)
                .map(InteractiveBody::new);
    }
}