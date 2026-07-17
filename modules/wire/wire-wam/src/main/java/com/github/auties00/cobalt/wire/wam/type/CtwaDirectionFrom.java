package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCtwaDirectionFrom")
@WamEnum
public enum CtwaDirectionFrom {
    @WamEnumConstant(0) CUSTOMER,
    @WamEnumConstant(1) BUSINESS
}
