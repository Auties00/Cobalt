package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

public record ButtonsMessageHeaderText(String text) implements ButtonsMessageHeader {
    @ProtobufDeserializer
    public static ButtonsMessageHeaderText of(String text) {
        return new ButtonsMessageHeaderText(text);
    }

    @ProtobufSerializer
    public String text() {
        return text;
    }

    @Override
    public Type buttonHeaderType() {
        return Type.TEXT;
    }
}
