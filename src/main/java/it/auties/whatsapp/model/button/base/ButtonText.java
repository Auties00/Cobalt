package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents the text of a button
 */
@ProtobufMessage
public final class ButtonText implements ButtonBody {

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String content;

    ButtonText(String content) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
    }

    public String content() {
        return content;
    }

    @Override
    public Type bodyType() {
        return Type.TEXT;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonText that
                && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "ButtonText[" +
                "content=" + content + ']';
    }
}