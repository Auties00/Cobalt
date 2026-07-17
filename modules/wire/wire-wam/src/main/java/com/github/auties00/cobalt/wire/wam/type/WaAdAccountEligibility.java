package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaAdAccountEligibility")
@WamEnum
public enum WaAdAccountEligibility {
    @WamEnumConstant(1) WA_ONLY,
    @WamEnumConstant(2) FB_ONLY,
    @WamEnumConstant(3) FB_OR_WA
}
