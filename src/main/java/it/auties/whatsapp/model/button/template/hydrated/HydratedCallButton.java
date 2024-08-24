package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a hydrated button that can start a phone call
 */
@ProtobufMessage(name = "HydratedTemplateButton.HydratedCallButton")
public record HydratedCallButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String text,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String phoneNumber
) implements HydratedButton {
    @Override
    public Type buttonType() {
        return Type.CALL;
    }
}
