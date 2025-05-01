package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a button that can start a phone call
 */
@ProtobufMessage(name = "TemplateButton.CallButton")
public record HighlyStructuredCallButton(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage text,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage phoneNumber
) implements HighlyStructuredButton {
    @Override
    public Type buttonType() {
        return Type.CALL;
    }
}
