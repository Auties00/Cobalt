package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessToolsEntryPointType")
@WamEnum
public enum BusinessToolsEntryPointType {
    @WamEnumConstant(0) ENTRY_BANNER,
    @WamEnumConstant(1) ENTRY_REGISTRATION_ONBOARDING,
    @WamEnumConstant(2) ENTRY_CONVERSATIONS,
    @WamEnumConstant(3) ENTRY_SETTINGS,
    @WamEnumConstant(4) ENTRY_DEEPLINK,
    @WamEnumConstant(5) ENTRY_STATUS_TAB_MENU,
    @WamEnumConstant(6) ENTRY_CALLS_TAB_MENU,
    @WamEnumConstant(7) ENTRY_BUSINESS_TOOLS_TAB
}
