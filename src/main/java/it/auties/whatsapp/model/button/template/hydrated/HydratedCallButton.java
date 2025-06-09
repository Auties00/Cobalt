package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents a hydrated button that can start a phone call
 */
@ProtobufMessage(name = "HydratedTemplateButton.HydratedCallButton")
public final class HydratedCallButton implements HydratedButton {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String text;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String phoneNumber;

    HydratedCallButton(String text, String phoneNumber) {
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
    }

    public String text() {
        return text;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    @Override
    public Type buttonType() {
        return Type.CALL;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HydratedCallButton that
                && Objects.equals(text, that.text)
                && Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, phoneNumber);
    }

    @Override
    public String toString() {
        return "HydratedCallButton[" +
                "text=" + text + ", " +
                "phoneNumber=" + phoneNumber + ']';
    }
}