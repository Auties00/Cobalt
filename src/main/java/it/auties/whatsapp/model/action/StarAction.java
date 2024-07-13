package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

/**
 * A model clas that represents a new star status for a message
 */
@ProtobufMessage(name = "SyncActionValue.StarAction")
public record StarAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean starred
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "star";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 2;
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
