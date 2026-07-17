package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcNavbarItemLabel")
@WamEnum
public enum WebcNavbarItemLabel {
    @WamEnumConstant(1) CHATS,
    @WamEnumConstant(2) COMMUNITIES,
    @WamEnumConstant(3) NEWSLETTERS,
    @WamEnumConstant(4) STATUS,
    @WamEnumConstant(5) STARRED,
    @WamEnumConstant(6) ARCHIVED,
    @WamEnumConstant(7) SETTINGS_HELP,
    @WamEnumConstant(8) SETTINGS,
    @WamEnumConstant(9) PROFILE,
    @WamEnumConstant(10) ADS_CREATION,
    @WamEnumConstant(11) BUSINESS_TOOLS,
    @WamEnumConstant(12) UPDATES
}
