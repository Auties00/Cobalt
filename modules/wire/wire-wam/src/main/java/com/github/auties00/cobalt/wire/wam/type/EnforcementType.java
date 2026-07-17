package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEnforcementType")
@WamEnum
public enum EnforcementType {
    @WamEnumConstant(0) CH_S,
    @WamEnumConstant(1) CH_SI,
    @WamEnumConstant(2) CH_GS,
    @WamEnumConstant(3) CH_GSI,
    @WamEnumConstant(4) CH_HCH,
    @WamEnumConstant(5) CH_PPD,
    @WamEnumConstant(6) CH_HAP
}
