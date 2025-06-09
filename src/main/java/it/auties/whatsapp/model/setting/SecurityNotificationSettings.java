package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "SyncActionValue.SecurityNotificationSetting")
public final class SecurityNotificationSettings implements Setting {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean showNotification;

    SecurityNotificationSettings(boolean showNotification) {
        this.showNotification = showNotification;
    }

    public boolean showNotification() {
        return showNotification;
    }

    @Override
    public int settingVersion() {
        return -1;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SecurityNotificationSettings that
                && showNotification == that.showNotification;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showNotification);
    }

    @Override
    public String toString() {
        return "SecurityNotificationSettings[" +
                "showNotification=" + showNotification + ']';
    }
}