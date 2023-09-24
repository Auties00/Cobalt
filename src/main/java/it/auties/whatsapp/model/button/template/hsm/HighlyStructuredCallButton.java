package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a button that can start a phone call
 */
@ProtobufMessageName("TemplateButton.CallButton")
public record HighlyStructuredCallButton(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        HighlyStructuredMessage text,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        HighlyStructuredMessage phoneNumber
) implements HighlyStructuredButton {
    @Override
    public Type buttonType() {
        return Type.CALL;
    }
}
