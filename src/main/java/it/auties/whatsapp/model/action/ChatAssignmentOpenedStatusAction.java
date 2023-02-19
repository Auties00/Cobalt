package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
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
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send action: no index name");
    }
}
