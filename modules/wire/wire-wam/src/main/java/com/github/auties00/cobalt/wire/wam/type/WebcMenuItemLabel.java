package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcMenuItemLabel")
@WamEnum
public enum WebcMenuItemLabel {
    @WamEnumConstant(1) NEW_GROUP,
    @WamEnumConstant(2) CREATE_A_ROOM,
    @WamEnumConstant(3) PROFILE,
    @WamEnumConstant(4) CATALOG,
    @WamEnumConstant(5) ARCHIVED,
    @WamEnumConstant(6) STARRED,
    @WamEnumConstant(7) LABELS,
    @WamEnumConstant(8) SETTINGS,
    @WamEnumConstant(9) LOG_OUT,
    @WamEnumConstant(10) CONTACT_INFO,
    @WamEnumConstant(11) SELECT_MESSAGES,
    @WamEnumConstant(12) CLOSE_CHAT,
    @WamEnumConstant(13) MUTE_NOTIFICATIONS,
    @WamEnumConstant(14) CLEAR_MESSAGES,
    @WamEnumConstant(15) DELETE_CHAT,
    @WamEnumConstant(16) REPORT_BUSINESS,
    @WamEnumConstant(17) BLOCK,
    @WamEnumConstant(18) SETTINGS_NOTIFICATIONS,
    @WamEnumConstant(19) SETTINGS_THEME,
    @WamEnumConstant(20) SETTINGS_CHAT_WALLPAPER,
    @WamEnumConstant(21) SETTINGS_BLOCKED,
    @WamEnumConstant(22) SETTINGS_HELP,
    @WamEnumConstant(23) OPEN,
    @WamEnumConstant(24) CLOSE,
    @WamEnumConstant(25) BUSINESS_TOOLS,
    @WamEnumConstant(26) APP_LOCK
}
