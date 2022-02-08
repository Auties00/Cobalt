package it.auties.whatsapp.protobuf.setting;

import java.util.List;

public sealed interface Setting permits EphemeralSetting, LocaleSetting,
        PushNameSetting, SecurityNotificationSetting, UnarchiveChatsSetting {
    String indexName();
}
