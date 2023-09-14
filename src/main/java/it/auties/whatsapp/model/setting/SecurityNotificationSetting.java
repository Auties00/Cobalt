package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

public record SecurityNotificationSetting(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean showNotification
) implements Setting {
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}
