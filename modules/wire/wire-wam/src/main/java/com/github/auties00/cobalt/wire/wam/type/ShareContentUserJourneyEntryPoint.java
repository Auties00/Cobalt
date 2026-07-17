package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumShareContentUserJourneyEntryPoint")
@WamEnum
public enum ShareContentUserJourneyEntryPoint {
    @WamEnumConstant(1) CONTEXT_MENU,
    @WamEnumConstant(2) TOOLBAR,
    @WamEnumConstant(3) FASTFORWARD,
    @WamEnumConstant(4) OTHERS
}
