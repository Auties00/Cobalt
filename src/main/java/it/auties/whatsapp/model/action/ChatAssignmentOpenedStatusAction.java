package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

/**
 * A model clas that represents the assignment of a chat as opened
 */
@ProtobufMessageName("SyncActionValue.ChatAssignmentOpenedStatusAction")
public record ChatAssignmentOpenedStatusAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean chatOpened
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "agentChatAssignmentOpenedStatus";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 7;
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
