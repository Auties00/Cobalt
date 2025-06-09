package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * A model class that represents additional metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Currently empty
 */
@ProtobufMessage(name = "Message.PollUpdateMessageMetadata")
public final class PollUpdateMessageMetadata {
    PollUpdateMessageMetadata() {

    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PollUpdateMessageMetadata;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "PollUpdateMessageMetadata[]";
    }
}