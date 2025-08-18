package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.Optional;

/**
 * A model clas that represents the assignment of a chat
 */
@ProtobufMessage(name = "SyncActionValue.ChatAssignmentAction")
public record ChatAssignmentAction(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Optional<String> deviceAgentId
) implements Action {

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "agentChatAssignment";
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
