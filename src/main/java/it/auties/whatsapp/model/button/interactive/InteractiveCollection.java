package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;

import java.util.Objects;

/**
 * A model class that represents a business collection
 */
@ProtobufMessage(name = "Message.InteractiveMessage.CollectionMessage")
public final class InteractiveCollection implements InteractiveMessageContent {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid business;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int version;

    InteractiveCollection(Jid business, String id, int version) {
        this.business = Objects.requireNonNull(business, "business cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.version = version;
    }

    public Jid business() {
        return business;
    }

    public String id() {
        return id;
    }

    public int version() {
        return version;
    }

    @Override
    public Type contentType() {
        return Type.COLLECTION;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveCollection that
                && Objects.equals(business, that.business)
                && Objects.equals(id, that.id)
                && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(business, id, version);
    }

    @Override
    public String toString() {
        return "InteractiveCollection[" +
                "business=" + business + ", " +
                "id=" + id + ", " +
                "version=" + version + ']';
    }
}