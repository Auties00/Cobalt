package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSettingsClickEntryPoint")
@WamEnum
public enum SettingsClickEntryPoint {
    @WamEnumConstant(0) SETTINGS_SCREEN,
    @WamEnumConstant(1) SETTINGS_SEARCH,
    @WamEnumConstant(2) DEEP_LINK,
    @WamEnumConstant(3) PRIVACY_CHECKUP,
    @WamEnumConstant(4) METAB_SCREEN,
    @WamEnumConstant(5) THREE_DOT_MENU
}
