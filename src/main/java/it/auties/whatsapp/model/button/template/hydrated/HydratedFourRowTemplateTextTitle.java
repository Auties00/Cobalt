package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * A model class that represents a hydrated four row template
 */
public record HydratedFourRowTemplateTextTitle(
        String text
) implements HydratedFourRowTemplateTitle {
    @ProtobufDeserializer
    public static HydratedFourRowTemplateTextTitle of(String text) {
        return new HydratedFourRowTemplateTextTitle(text);
    }

    @ProtobufSerializer
    public String text() {
        return text;
    }

    @Override
    public Type hydratedTitleType() {
        return Type.TEXT;
    }
}