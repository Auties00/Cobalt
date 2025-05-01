package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents an url button
 */
@ProtobufMessage(name = "TemplateButton.URLButton")
public record HighlyStructuredURLButton(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage text,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage url
) implements HighlyStructuredButton {
    @Override
    public Type buttonType() {
        return Type.URL;
    }
}
