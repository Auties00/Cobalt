package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model clas that represents whether a user was muted
 */
@ProtobufMessage(name = "SyncActionValue.UserStatusMuteAction")
public final class UserStatusMuteAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean muted;

    UserStatusMuteAction(boolean muted) {
        this.muted = muted;
    }

    @Override
    public String indexName() {
        return "userStatusMute";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public boolean muted() {
        return muted;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UserStatusMuteAction that
                && muted == that.muted;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(muted);
    }

    @Override
    public String toString() {
        return "UserStatusMuteAction[" +
                "muted=" + muted + ']';
    }
}
