package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model clas that represents a label association
 */
@ProtobufMessage(name = "SyncActionValue.LabelAssociationAction")
public final class LabelAssociationAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean labeled;

    LabelAssociationAction(boolean labeled) {
        this.labeled = labeled;
    }

    @Override
    public String indexName() {
        return "label_message";
    }

    @Override
    public int actionVersion() {
        return 3;
    }

    public boolean labeled() {
        return labeled;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LabelAssociationAction that
                && labeled == that.labeled;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(labeled);
    }

    @Override
    public String toString() {
        return "LabelAssociationAction[" +
                "labeled=" + labeled + ']';
    }
}
