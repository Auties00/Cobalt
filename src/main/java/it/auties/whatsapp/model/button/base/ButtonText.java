package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents the text of a button
 */
@ProtobufMessage
public record ButtonText(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String content
) implements ButtonBody {
    @Override
    public Type bodyType() {
        return Type.TEXT;
    }
}
