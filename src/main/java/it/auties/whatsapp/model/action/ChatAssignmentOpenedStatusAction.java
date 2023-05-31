package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.binary.PatchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BOOL;

/**
 * A model clas that represents the assignment of a chat as opened
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("ChatAssignmentOpenedStatusAction")
public final class ChatAssignmentOpenedStatusAction implements Action {
    /**
     * Whether the chat was opened
     */
    @ProtobufProperty(index = 1, name = "chatOpened", type = BOOL)
    private boolean chatOpened;

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
    public int version() {
        return 7;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType type() {
        return null;
    }
}
