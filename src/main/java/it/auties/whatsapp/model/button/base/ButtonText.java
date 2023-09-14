package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents the text of a button
 */
public record ButtonText(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String content
) implements ButtonBody {
    @Override
    public ButtonBodyType bodyType() {
        return ButtonBodyType.TEXT;
    }
}
