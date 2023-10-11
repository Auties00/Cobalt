package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;

/**
 * A model class that represents a button that can start a phone call
 */
@ProtobufMessageName("TemplateButton.CallButton")
public record HighlyStructuredCallButton(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        HighlyStructuredMessage text,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        HighlyStructuredMessage phoneNumber
) implements HighlyStructuredButton {
    @Override
    public Type buttonType() {
        return Type.CALL;
    }
}
