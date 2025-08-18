package it.auties.whatsapp.model.setting;

public sealed interface Setting permits AutoDownloadSettings, AvatarUserSettings, EphemeralSettings, LocaleSettings, PushNameSettings, SecurityNotificationSettings, UnarchiveChatsSettings {
    int settingVersion();
    String indexName();
}
