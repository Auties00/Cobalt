package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents data about a row
 */
@ProtobufMessage(name = "MsgRowOpaqueData")
public final class ButtonRowOpaqueData {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ButtonOpaqueData currentMessage;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final ButtonOpaqueData quotedMessage;

    ButtonRowOpaqueData(ButtonOpaqueData currentMessage, ButtonOpaqueData quotedMessage) {
        this.currentMessage = currentMessage;
        this.quotedMessage = quotedMessage;
    }

    public Optional<ButtonOpaqueData> currentMessage() {
        return Optional.ofNullable(currentMessage);
    }

    public Optional<ButtonOpaqueData> quotedMessage() {
        return Optional.ofNullable(quotedMessage);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonRowOpaqueData that
                && Objects.equals(currentMessage, that.currentMessage)
                && Objects.equals(quotedMessage, that.quotedMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentMessage, quotedMessage);
    }

    @Override
    public String toString() {
        return "ButtonRowOpaqueData[" +
                "currentMessage=" + currentMessage + ", " +
                "quotedMessage=" + quotedMessage + ']';
    }
}