package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

/**
 * A model clas that represents a label association
 */
@ProtobufMessage(name = "SyncActionValue.LabelAssociationAction")
public record LabelAssociationAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean labeled
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "label_message";
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
