package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents additional metadata about a {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 */
@ProtobufMessage(name = "PollAdditionalMetadata")
public final class PollAdditionalMetadata {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean pollInvalidated;

    PollAdditionalMetadata(boolean pollInvalidated) {
        this.pollInvalidated = pollInvalidated;
    }

    public boolean pollInvalidated() {
        return pollInvalidated;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PollAdditionalMetadata that
                && pollInvalidated == that.pollInvalidated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollInvalidated);
    }

    @Override
    public String toString() {
        return "PollAdditionalMetadata[" +
                "pollInvalidated=" + pollInvalidated +
                ']';
    }
}