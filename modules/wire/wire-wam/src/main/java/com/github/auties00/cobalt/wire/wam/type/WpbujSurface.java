package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWpbujSurface")
@WamEnum
public enum WpbujSurface {
    @WamEnumConstant(1) APP_THEME,
    @WamEnumConstant(2) APP_ICON,
    @WamEnumConstant(3) RINGTONE,
    @WamEnumConstant(4) LIST,
    @WamEnumConstant(5) CHAT_THEME,
    @WamEnumConstant(6) ALERT_TONE,
    @WamEnumConstant(7) PINNED_CHAT,
    @WamEnumConstant(8) STICKER_TRAY,
    @WamEnumConstant(9) STICKER_RECEIVED,
    @WamEnumConstant(10) STICKER_STORE,
    @WamEnumConstant(11) APPEARANCE_SETTINGS,
    @WamEnumConstant(12) NOTIFICATION_SETTINGS,
    @WamEnumConstant(13) WHATSAPP_SETTINGS,
    @WamEnumConstant(14) STICKER_GENERIC
}
