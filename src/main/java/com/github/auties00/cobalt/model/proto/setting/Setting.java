package com.github.auties00.cobalt.model.proto.setting;

public sealed interface Setting permits AutoDownloadSettings, AvatarUserSettings, EphemeralSettings, LocaleSettings, PushNameSettings, SecurityNotificationSettings, UnarchiveChatsSettings {
    int settingVersion();
    String indexName();
}
