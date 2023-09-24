package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

/**
 * A container for a future message
 */
@ProtobufMessageName("Message.FutureProofMessage")
public record FutureMessageContainer(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        MessageContainer content
) implements ProtobufMessage {
    static FutureMessageContainer of(Message message) {
        return new FutureMessageContainer(MessageContainer.of(message));
    }

    static FutureMessageContainer of(MessageContainer container) {
        return new FutureMessageContainer(container);
    }

    Message unbox() {
        return content.content();
    }
}