package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPrivacyControlEntryPointType")
@WamEnum
public enum PrivacyControlEntryPointType {
    @WamEnumConstant(0) PRIVACY_SETTINGS,
    @WamEnumConstant(1) PROFILE_PHOTO_JIT,
    @WamEnumConstant(2) SETTINGS_SEARCH,
    @WamEnumConstant(3) DEEP_LINK,
    @WamEnumConstant(4) PRIVACY_CHECKUP_BANNER,
    @WamEnumConstant(5) PRIVACY_CHECKUP_DEEP_LINK,
    @WamEnumConstant(6) PRIVACY_CHECKUP_WA_CHAT,
    @WamEnumConstant(7) PRIVACY_CHECKUP_SETTINGS_SEARCH,
    @WamEnumConstant(8) DEFENSE_MODE_LOCKED_INTERSTITIAL,
    @WamEnumConstant(9) STICKER_INFO_SHEET,
    @WamEnumConstant(10) DEFENSE_MODE_QUARANTINED_INTERSTITIAL,
    @WamEnumConstant(11) DEFENSE_MODE_SETTINGS_REMINDER_BANNER
}
