package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWpbujBenefitStatus")
@WamEnum
public enum WpbujBenefitStatus {
    @WamEnumConstant(1) DISABLED,
    @WamEnumConstant(2) NOT_ACTIVE,
    @WamEnumConstant(3) ACTIVE
}
