package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufConverter;

/**
 * A model class that represents a hydrated four row template
 */
public record HydratedFourRowTemplateTextTitle(
        String text
) implements HydratedFourRowTemplateTitle {
    @ProtobufConverter
    public static HydratedFourRowTemplateTextTitle of(String text) {
        return new HydratedFourRowTemplateTextTitle(text);
    }

    @ProtobufConverter
    public String text() {
        return text;
    }

    @Override
    public Type hydratedTitleType() {
        return Type.TEXT;
    }
}