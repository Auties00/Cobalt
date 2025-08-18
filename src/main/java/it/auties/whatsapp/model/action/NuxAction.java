package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Unknown
 */
@ProtobufMessage(name = "SyncActionValue.NuxAction")
public final class NuxAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean acknowledged;

    NuxAction(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    @Override
    public String indexName() {
        return "nux";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public boolean acknowledged() {
        return acknowledged;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NuxAction nuxAction
                && acknowledged == nuxAction.acknowledged;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(acknowledged);
    }

    @Override
    public String toString() {
        return "NuxAction[" +
                "acknowledged=" + acknowledged + ']';
    }
}
