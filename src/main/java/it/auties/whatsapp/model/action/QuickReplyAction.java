package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model clas that represents the addition or deletion of a quick reply
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class QuickReplyAction implements Action {
    /**
     * The shortcut for this reply
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String shortcut;

    /**
     * The full reply
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String message;

    /**
     * The keywords for this reply
     */
    @ProtobufProperty(index = 3, type = STRING, repeated = true)
    private List<String> keywords;

    /**
     * The count for this reply
     */
    @ProtobufProperty(index = 4, type = INT32)
    private int count;

    /**
     * Whether this quick reply was deleted
     */
    @ProtobufProperty(index = 5, type = BOOL)
    private boolean deleted;

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "quick_reply";
    }
}
