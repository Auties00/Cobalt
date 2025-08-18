package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.util.Objects;

/**
 * A model class that represents a hydrated four row template
 */
public final class HydratedFourRowTemplateTextTitle implements HydratedFourRowTemplateTitle {
    final String text;

    HydratedFourRowTemplateTextTitle(String text) {
        this.text = Objects.requireNonNull(text, "text cannot be null");
    }

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

    @Override
    public boolean equals(Object o) {
        return o instanceof HydratedFourRowTemplateTextTitle that
                && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "HydratedFourRowTemplateTextTitle[" +
                "text=" + text + ']';
    }
}