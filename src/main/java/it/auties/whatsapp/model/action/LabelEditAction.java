package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

/**
 * A model clas that represents an edit to a label
 */
@ProtobufMessage(name = "SyncActionValue.LabelEditAction")
public record LabelEditAction(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name,
        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
        int color,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int id,
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        boolean deleted
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "label_edit";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 3;
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
