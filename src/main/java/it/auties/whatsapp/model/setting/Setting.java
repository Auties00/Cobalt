package it.auties.whatsapp.model.setting;

public sealed interface Setting permits EphemeralSetting, LocaleSetting,
        PushNameSetting, SecurityNotificationSetting, UnarchiveChatsSetting {
    String indexName();
}
