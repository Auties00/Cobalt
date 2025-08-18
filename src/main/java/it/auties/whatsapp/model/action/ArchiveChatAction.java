package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;

import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents an archived chat
 */
@ProtobufMessage(name = "SyncActionValue.ArchiveChatAction")
public final class ArchiveChatAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean archived;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final ActionMessageRangeSync messageRange;

    ArchiveChatAction(boolean archived, ActionMessageRangeSync messageRange) {
        this.archived = archived;
        this.messageRange = messageRange;
    }

    @Override
    public String indexName() {
        return "archive";
    }

    @Override
    public int actionVersion() {
        return 3;
    }

    public boolean archived() {
        return archived;
    }

    public Optional<ActionMessageRangeSync> messageRange() {
        return Optional.ofNullable(messageRange);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ArchiveChatAction that
                && archived == that.archived
                && Objects.equals(messageRange, that.messageRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(archived, messageRange);
    }

    @Override
    public String toString() {
        return "ArchiveChatAction[" +
                "archived=" + archived + ", " +
                "messageRange=" + messageRange + ']';
    }
}
