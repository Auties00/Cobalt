package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;

import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents a new read status for a chat
 */
@ProtobufMessage(name = "SyncActionValue.MarkChatAsReadAction")
public final class MarkChatAsReadAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean read;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final ActionMessageRangeSync messageRange;

    MarkChatAsReadAction(boolean read, ActionMessageRangeSync messageRange) {
        this.read = read;
        this.messageRange = messageRange;
    }

    @Override
    public String indexName() {
        return "markChatAsRead";
    }

    @Override
    public int actionVersion() {
        return 3;
    }

    public boolean read() {
        return read;
    }

    public Optional<ActionMessageRangeSync> messageRange() {
        return Optional.ofNullable(messageRange);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MarkChatAsReadAction that
                && read == that.read
                && Objects.equals(messageRange, that.messageRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(read, messageRange);
    }

    @Override
    public String toString() {
        return "MarkChatAsReadAction[" +
                "read=" + read + ", " +
                "messageRange=" + messageRange + ']';
    }
}
