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
    final MessageContainer content;

    FutureMessageContainer(MessageContainer content) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
    }

    public MessageContainer content() {
        return this.content;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FutureMessageContainer that
                && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "FutureMessageContainer[" +
                "content=" + content +
                ']';
    }
}