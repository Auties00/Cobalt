package it.auties.whatsapp.model.setting;

import it.auties.protobuf.model.ProtobufMessage;

public sealed interface Setting extends ProtobufMessage permits AutoDownloadSettings, AvatarUserSettings, EphemeralSettings, LocaleSettings, PushNameSettings, SecurityNotificationSettings, UnarchiveChatsSettings {
    String indexName();
}
