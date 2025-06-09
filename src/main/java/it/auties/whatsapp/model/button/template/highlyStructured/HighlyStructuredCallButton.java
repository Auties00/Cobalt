package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents a button that can start a phone call
 */
@ProtobufMessage(name = "TemplateButton.CallButton")
public final class HighlyStructuredCallButton implements HighlyStructuredButton {

    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage text;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage phoneNumber;

    HighlyStructuredCallButton(HighlyStructuredMessage text, HighlyStructuredMessage phoneNumber) {
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
    }

    public HighlyStructuredMessage text() {
        return text;
    }

    public HighlyStructuredMessage phoneNumber() {
        return phoneNumber;
    }

    @Override
    public Type buttonType() {
        return Type.CALL;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredCallButton that
                && Objects.equals(text, that.text)
                && Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, phoneNumber);
    }

    @Override
    public String toString() {
        return "HighlyStructuredCallButton[" +
                "text=" + text + ", " +
                "phoneNumber=" + phoneNumber + ']';
    }
}