package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.Optional;

/**
 * A model clas that represents a cleared chat
 */
@ProtobufMessage(name = "SyncActionValue.ClearChatAction")
public record ClearChatAction(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Optional<ActionMessageRangeSync> messageRange
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "clearChat";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 6;
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
