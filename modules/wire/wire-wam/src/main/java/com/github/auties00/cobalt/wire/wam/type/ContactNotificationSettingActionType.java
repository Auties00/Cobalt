package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumContactNotificationSettingActionType")
@WamEnum
public enum ContactNotificationSettingActionType {
    @WamEnumConstant(1) MESSAGES_MUTE_ALWAYS,
    @WamEnumConstant(2) MESSAGES_MUTE_8H,
    @WamEnumConstant(3) MESSAGES_MUTE_1W,
    @WamEnumConstant(4) MESSAGES_UNMUTE,
    @WamEnumConstant(5) CALLS_MUTE_ALWAYS,
    @WamEnumConstant(6) CALLS_MUTE_8H,
    @WamEnumConstant(7) CALLS_MUTE_1W,
    @WamEnumConstant(8) CALLS_UNMUTE,
    @WamEnumConstant(9) CHANGED_MESSAGE_ALERT_TONE,
    @WamEnumConstant(10) CHANGED_MESSAGE_VIBRATION,
    @WamEnumConstant(11) CHANGED_CALL_TONE,
    @WamEnumConstant(12) CHANGED_CALL_VIBRATION,
    @WamEnumConstant(13) CHANGED_MESSAGE_NOTIFICATION_THEME,
    @WamEnumConstant(14) CHANGED_MESSAGE_POPUP_NOTIFICATION,
    @WamEnumConstant(15) CHANGED_MESSAGE_NOTIFICATION_PRIORITY,
    @WamEnumConstant(16) STATUSES_MUTE,
    @WamEnumConstant(17) STATUSES_UNMUTE,
    @WamEnumConstant(18) MUTE_MENTION_EVERYONE_OFF,
    @WamEnumConstant(19) MUTE_MENTION_EVERYONE_ON
}
