package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMmDirectionFrom")
@WamEnum
public enum MmDirectionFrom {
    @WamEnumConstant(0) CUSTOMER,
    @WamEnumConstant(1) BUSINESS
}
