package it.auties.whatsapp.model.setting;

import it.auties.protobuf.api.model.ProtobufMessage;

public sealed interface Setting extends ProtobufMessage permits EphemeralSetting, LocaleSetting,
        PushNameSetting, SecurityNotificationSetting, UnarchiveChatsSetting {
    String indexName();
}
