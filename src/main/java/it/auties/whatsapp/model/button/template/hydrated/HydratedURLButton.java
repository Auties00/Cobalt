package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a hydrated url button
 */
@ProtobufMessageName("HydratedTemplateButton.HydratedURLButton")
public record HydratedURLButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String text,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String url
) implements HydratedButton {
    @Override
    public Type buttonType() {
        return Type.URL;
    }
}
