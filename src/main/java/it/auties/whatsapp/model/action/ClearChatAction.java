package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;

import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents a cleared chat
 */
@ProtobufMessage(name = "SyncActionValue.ClearChatAction")
public final class ClearChatAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ActionMessageRangeSync messageRange;

    ClearChatAction(ActionMessageRangeSync messageRange) {
        this.messageRange = messageRange;
    }

    @Override
    public String indexName() {
        return "clearChat";
    }

    @Override
    public int actionVersion() {
        return 6;
    }

    public Optional<ActionMessageRangeSync> messageRange() {
        return Optional.ofNullable(messageRange);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClearChatAction that
                && Objects.equals(messageRange, that.messageRange);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messageRange);
    }

    @Override
    public String toString() {
        return "ClearChatAction[" +
                "messageRange=" + messageRange + ']';
    }
}
