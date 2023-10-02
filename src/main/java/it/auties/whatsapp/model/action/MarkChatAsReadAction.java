package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.Optional;

/**
 * A model clas that represents a new read status for a chat
 */
@ProtobufMessageName("SyncActionValue.MarkChatAsReadAction")
public record MarkChatAsReadAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean read,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<ActionMessageRangeSync> messageRange
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "markChatAsRead";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 3;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType actionType() {
        return null;
    }
}
