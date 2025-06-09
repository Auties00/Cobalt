package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Objects;

/**
 * A model class that represents a selected option in a {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 */
@ProtobufMessage
public final class SelectedPollOption {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    SelectedPollOption(Jid jid, String name) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    public Jid jid() {
        return jid;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SelectedPollOption that
                && Objects.equals(jid, that.jid)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, name);
    }

    @Override
    public String toString() {
        return "SelectedPollOption[" +
                "jid=" + jid +
                ", name=" + name +
                ']';
    }
}