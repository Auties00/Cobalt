package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncActionValue.SecurityNotificationSetting")
public record SecurityNotificationSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean showNotification
) implements Setting {
    @Override
    public int settingVersion() {
        return -1;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}
