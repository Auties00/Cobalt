package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model clas that represents a new read status for a chat
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public final class MarkChatAsReadAction implements Action {
    /**
     * Whether this action marks the chat as read
     */
    @ProtobufProperty(index = 1, type = BOOLEAN)
    private boolean read;

    /**
     * The message range on which this action has effect
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ActionMessageRangeSync.class)
    private ActionMessageRangeSync messageRange;

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "markChatAsRead";
    }
}
