package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents an option in a
 * {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 */
@ProtobufMessage(name = "MsgOpaqueData.PollOption")
public final class PollOption {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    PollOption(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PollOption that
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "PollOption[" +
                "name=" + name +
                ']';
    }
}