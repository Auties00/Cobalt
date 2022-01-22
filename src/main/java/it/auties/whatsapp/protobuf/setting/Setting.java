package it.auties.whatsapp.protobuf.setting;

public sealed interface Setting permits EphemeralSetting, PushNameSetting, SecurityNotificationSetting, UnarchiveChatsSetting {
}
