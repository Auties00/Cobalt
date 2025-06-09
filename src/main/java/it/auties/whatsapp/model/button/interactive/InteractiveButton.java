package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a native flow button
 */
@ProtobufMessage(name = "Message.InteractiveMessage.NativeFlowMessage.NativeFlowButton")
public final class InteractiveButton {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String parameters;

    InteractiveButton(String name, String parameters) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.parameters = parameters;
    }

    public String name() {
        return name;
    }

    public Optional<String> parameters() {
        return Optional.ofNullable(parameters);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveButton that
                && Objects.equals(name, that.name)
                && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }

    @Override
    public String toString() {
        return "InteractiveButton[" +
                "name=" + name + ", " +
                "parameters=" + parameters + ']';
    }
}