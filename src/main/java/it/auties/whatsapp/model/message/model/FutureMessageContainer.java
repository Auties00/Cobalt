package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A container for a future message
 */
@AllArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
@Jacksonized
@ProtobufName("FutureProofMessage")
public class FutureMessageContainer implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = MessageContainer.class)
    private MessageContainer content;

    protected static FutureMessageContainer of(Message message) {
        return new FutureMessageContainer(MessageContainer.of(message));
    }

    protected static FutureMessageContainer of(MessageContainer container) {
        return new FutureMessageContainer(container);
    }

    protected Message unbox() {
        return content.content();
    }
}