package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessageName("SyncActionValue.UnarchiveChatsSetting")
public record UnarchiveChatsSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean unarchiveChats
) implements Setting {
    @Override
    public String indexName() {
        return "setting_unarchiveChats";
    }
}
