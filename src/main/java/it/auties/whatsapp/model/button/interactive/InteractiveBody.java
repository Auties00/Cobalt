package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents the body of a product
 */
@ProtobufMessage(name = "Message.InteractiveMessage.Body")
public final class InteractiveBody {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String content;

    InteractiveBody(String content) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
    }

    public String content() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveBody that
                && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "InteractiveBody[" +
                "content=" + content + ']';
    }
}