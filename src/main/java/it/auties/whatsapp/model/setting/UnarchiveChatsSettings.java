package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "SyncActionValue.UnarchiveChatsSetting")
public final class UnarchiveChatsSettings implements Setting {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean unarchiveChats;

    UnarchiveChatsSettings(boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
    }

    public boolean unarchiveChats() {
        return unarchiveChats;
    }

    @Override
    public int settingVersion() {
        return 4;
    }

    @Override
    public String indexName() {
        return "setting_unarchiveChats";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnarchiveChatsSettings that
                && unarchiveChats == that.unarchiveChats;
    }

    @Override
    public int hashCode() {
        return Objects.hash(unarchiveChats);
    }

    @Override
    public String toString() {
        return "UnarchiveChatsSettings[" +
                "unarchiveChats=" + unarchiveChats +
                ']';
    }
}