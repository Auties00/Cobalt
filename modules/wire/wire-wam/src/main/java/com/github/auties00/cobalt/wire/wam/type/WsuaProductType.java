package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWsuaProductType")
@WamEnum
public enum WsuaProductType {
    @WamEnumConstant(1) NOVA,
    @WamEnumConstant(2) WA_PLUS,
    @WamEnumConstant(3) META_AI,
    @WamEnumConstant(4) META_ONE,
    @WamEnumConstant(5) META_ONE_CONSUMER,
    @WamEnumConstant(6) META_ONE_BUSINESS
}
