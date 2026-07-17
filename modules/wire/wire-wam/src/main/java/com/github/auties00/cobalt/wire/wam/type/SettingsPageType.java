package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSettingsPageType")
@WamEnum
public enum SettingsPageType {
    @WamEnumConstant(0) SETTINGS,
    @WamEnumConstant(1) ME_TAB,
    @WamEnumConstant(2) AVATAR_COINFLIP
}
