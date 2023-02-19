package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

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
    @ProtobufProperty(index = 1, type = BOOL)
    private boolean read;

    /**
     * The message range on which this action has effect
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ActionMessageRangeSync.class)
    private ActionMessageRangeSync messageRange;

    /**
     * Returns the range of messages that were marked as read or not
     *
     * @return an optional
     */
    public Optional<ActionMessageRangeSync> messageRange() {
        return Optional.ofNullable(messageRange);
    }

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
