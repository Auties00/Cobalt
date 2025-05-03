package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * A model class that represents the cypher data to decode the votes of a user inside {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage}
 */
@ProtobufMessage(name = "Message.PollVoteMessage")
public final class PollUpdateEncryptedOptions {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final List<byte[]> selectedOptions;

    PollUpdateEncryptedOptions(List<byte[]> selectedOptions) {
        this.selectedOptions = Objects.requireNonNullElse(selectedOptions, List.of());
    }

    public List<byte[]> selectedOptions() {
        return selectedOptions;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PollUpdateEncryptedOptions that
                && Objects.equals(selectedOptions, that.selectedOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectedOptions);
    }

    @Override
    public String toString() {
        return "PollUpdateEncryptedOptions[" +
                "selectedOptions=" + selectedOptions +
                ']';
    }
}