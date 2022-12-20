package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model clas that represents an edit to a label
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class LabelEditAction
        implements Action {
    /**
     * The name of the label
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String name;

    /**
     * The color of the label
     */
    @ProtobufProperty(index = 2, type = INT32)
    private int color;

    /**
     * The id of the label
     */
    @ProtobufProperty(index = 3, type = INT32)
    private int id;

    /**
     * Whether this label was deleted
     */
    @ProtobufProperty(index = 4, type = BOOL)
    private boolean deleted;

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "label_edit";
    }
}
