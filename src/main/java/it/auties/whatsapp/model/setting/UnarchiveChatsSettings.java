package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncActionValue.UnarchiveChatsSetting")
public record UnarchiveChatsSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean unarchiveChats
) implements Setting {
    @Override
    public int settingVersion() {
        return 4;
    }

    @Override
    public String indexName() {
        return "setting_unarchiveChats";
    }
}
