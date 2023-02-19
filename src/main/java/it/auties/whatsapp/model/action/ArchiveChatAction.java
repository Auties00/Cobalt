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
 * A model clas that represents an archived chat
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public final class ArchiveChatAction implements Action {
    /**
     * Whether the chat was archived
     */
    @ProtobufProperty(index = 1, type = BOOL)
    private boolean archived;

    /**
     * The message range on which this action has effect
     * If this field is empty, all messages should be considered as affected
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ActionMessageRangeSync.class)
    private ActionMessageRangeSync messageRange;

    /**
     * Returns the range of messages that were archived
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
        return "archive";
    }
}
