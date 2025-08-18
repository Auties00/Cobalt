package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model clas that represents unsupported actions for android
 */
@ProtobufMessage(name = "SyncActionValue.AndroidUnsupportedActions")
public final class AndroidUnsupportedActions implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean allowed;

    AndroidUnsupportedActions(boolean allowed) {
        this.allowed = allowed;
    }

    @Override
    public String indexName() {
        return "android_unsupported_actions";
    }

    @Override
    public int actionVersion() {
        return 4;
    }

    public boolean allowed() {
        return allowed;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AndroidUnsupportedActions that
                && allowed == that.allowed;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(allowed);
    }

    @Override
    public String toString() {
        return "AndroidUnsupportedActions[" +
                "allowed=" + allowed + ']';
    }
}
