package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisappearingModeEntryPointType")
@WamEnum
public enum DisappearingModeEntryPointType {
    @WamEnumConstant(1) ACCOUNT_SETTINGS,
    @WamEnumConstant(2) SYSTEM_MESSAGE,
    @WamEnumConstant(3) INDIVIDUAL_CHAT_DISAPPEARING_MESSAGES_SETTING,
    @WamEnumConstant(4) GROUP_CHAT_DISAPPEARING_MESSAGES_SETTING,
    @WamEnumConstant(5) DEEP_LINK,
    @WamEnumConstant(6) STORAGE_SETTINGS,
    @WamEnumConstant(7) PRIVACY_SETTINGS
}
