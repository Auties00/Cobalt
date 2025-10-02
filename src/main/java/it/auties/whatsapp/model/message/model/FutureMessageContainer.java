package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A container for a future message
 */
@ProtobufMessage(name = "Message.FutureProofMessage")
public final class FutureMessageContainer {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final MessageContainer value;

    FutureMessageContainer(MessageContainer value) {
        this.value = Objects.requireNonNull(value, "content cannot be null");
    }

    public MessageContainer value() {
        return this.value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FutureMessageContainer that
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "FutureMessageContainer[" +
                "content=" + value +
                ']';
    }
}