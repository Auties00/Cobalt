package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model clas that represents a new star status for a message
 */
@ProtobufMessage(name = "SyncActionValue.StarAction")
public final class StarAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean starred;

    StarAction(boolean starred) {
        this.starred = starred;
    }

    @Override
    public String indexName() {
        return "star";
    }

    @Override
    public int actionVersion() {
        return 2;
    }

    public boolean starred() {
        return starred;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StarAction that
                && starred == that.starred;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(starred);
    }

    @Override
    public String toString() {
        return "StarAction[" +
                "starred=" + starred + ']';
    }
}
