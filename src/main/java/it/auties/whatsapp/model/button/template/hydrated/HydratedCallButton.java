package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a hydrated button that can start a phone call
 */
public record HydratedCallButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String text,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String phoneNumber
) implements HydratedButton {
    @Override
    public HydratedButtonType buttonType() {
        return HydratedButtonType.CALL;
    }
}
