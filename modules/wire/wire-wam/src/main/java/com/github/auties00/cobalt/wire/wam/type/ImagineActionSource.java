package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumImagineActionSource")
@WamEnum
public enum ImagineActionSource {
    @WamEnumConstant(0) CHAT_TRAY,
    @WamEnumConstant(1) ATTACHMEMT_TRAY,
    @WamEnumConstant(2) SETTINGS,
    @WamEnumConstant(3) SEARCH,
    @WamEnumConstant(4) RETAKE_INLINE,
    @WamEnumConstant(5) USER_PROFILE_PAGE_PICTURE,
    @WamEnumConstant(6) GROUP_PROFILE_PAGE_PICTURE,
    @WamEnumConstant(7) AI_CREATION_EDIT,
    @WamEnumConstant(8) AI_CREATION_NEW_CREATE,
    @WamEnumConstant(9) AR_EFFECTS_IN_CALLING,
    @WamEnumConstant(10) AR_EFFECTS_IN_PRECAPTURE,
    @WamEnumConstant(11) STATUS,
    @WamEnumConstant(12) CHAT_THEMES,
    @WamEnumConstant(13) MESSAGE_QUICK_ACTION,
    @WamEnumConstant(14) CHAT_WALLPAPER,
    @WamEnumConstant(15) CHAT_THEME,
    @WamEnumConstant(16) MEDIA_PICKER,
    @WamEnumConstant(17) CAMERA,
    @WamEnumConstant(18) MIMICRY,
    @WamEnumConstant(19) MIMICRY_ATTRIBUTION,
    @WamEnumConstant(20) MEDIA_VIEWER,
    @WamEnumConstant(21) EVENT_COVER_BOTTOM_SHEET,
    @WamEnumConstant(22) FORWARD,
    @WamEnumConstant(23) DEEP_LINK,
    @WamEnumConstant(24) AI_TAB
}
