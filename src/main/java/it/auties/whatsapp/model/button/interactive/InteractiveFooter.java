package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents the footer of a product
 */
@ProtobufMessageName("Message.InteractiveMessage.Footer")
public record InteractiveFooter(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String content
) implements ProtobufMessage {

    public static Optional<InteractiveFooter> ofNullable(String content) {
        return Optional.ofNullable(content)
                .map(InteractiveFooter::new);
    }
}