package it.auties.whatsapp.protobuf.setting;

public sealed interface Setting permits EphemeralSetting, LocaleSetting,
        PushNameSetting, SecurityNotificationSetting, UnarchiveChatsSetting {
    String indexName();
}
