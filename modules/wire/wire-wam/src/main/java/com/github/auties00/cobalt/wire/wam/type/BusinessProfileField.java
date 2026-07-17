package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessProfileField")
@WamEnum
public enum BusinessProfileField {
    @WamEnumConstant(1) DESCRIPTION,
    @WamEnumConstant(2) HOURS,
    @WamEnumConstant(3) ADDRESS,
    @WamEnumConstant(4) EMAIL,
    @WamEnumConstant(5) WEBSITE,
    @WamEnumConstant(6) CATEGORY,
    @WamEnumConstant(7) PROFILE,
    @WamEnumConstant(8) PIX
}
