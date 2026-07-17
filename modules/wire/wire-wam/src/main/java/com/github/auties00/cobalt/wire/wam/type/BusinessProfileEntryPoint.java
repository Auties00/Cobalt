package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessProfileEntryPoint")
@WamEnum
public enum BusinessProfileEntryPoint {
    @WamEnumConstant(1) REGISTRATION,
    @WamEnumConstant(2) SETTINGS,
    @WamEnumConstant(3) COMPLIANCE,
    @WamEnumConstant(4) QUICK_REPLY_SMART_DEFAULT,
    @WamEnumConstant(5) WA_PAGES,
    @WamEnumConstant(6) PROFILE_COMPLETENESS,
    @WamEnumConstant(7) DIRECTORY_ONBOARDING,
    @WamEnumConstant(8) BUSINESS_HOME,
    @WamEnumConstant(9) DEEPLINK,
    @WamEnumConstant(10) CHAT_BANNER,
    @WamEnumConstant(11) BUSINESS_SEARCH,
    @WamEnumConstant(12) META_VERIFIED,
    @WamEnumConstant(13) UNKNOWN,
    @WamEnumConstant(14) QUICK_REPLY_SETTINGS
}
