package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model clas that represents a new pin status for a chat
 */
@ProtobufMessage(name = "SyncActionValue.PinAction")
public final class PinAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean pinned;

    PinAction(boolean pinned) {
        this.pinned = pinned;
    }

    @Override
    public String indexName() {
        return "pin_v1";
    }

    @Override
    public int actionVersion() {
        return 5;
    }

    public boolean pinned() {
        return pinned;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PinAction pinAction
                && pinned == pinAction.pinned;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pinned);
    }

    @Override
    public String toString() {
        return "PinAction[" +
                "pinned=" + pinned + ']';
    }
}
