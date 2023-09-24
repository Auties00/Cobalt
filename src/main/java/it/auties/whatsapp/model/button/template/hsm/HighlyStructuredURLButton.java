package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents an url button
 */
@ProtobufMessageName("TemplateButton.URLButton")
public record HighlyStructuredURLButton(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        HighlyStructuredMessage text,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        HighlyStructuredMessage url
) implements HighlyStructuredButton {
    @Override
    public Type buttonType() {
        return Type.URL;
    }
}
