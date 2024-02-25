package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufConverter;

public record ButtonsMessageHeaderText(String text) implements ButtonsMessageHeader {
    @ProtobufConverter
    public static ButtonsMessageHeaderText of(String text) {
        return new ButtonsMessageHeaderText(text);
    }

    @ProtobufConverter
    public String text() {
        return text;
    }

    @Override
    public Type buttonHeaderType() {
        return Type.TEXT;
    }
}
