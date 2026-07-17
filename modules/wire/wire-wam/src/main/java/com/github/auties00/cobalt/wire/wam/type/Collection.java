package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCollection")
@WamEnum
public enum Collection {
    @WamEnumConstant(1) REGULAR,
    @WamEnumConstant(2) REGULAR_LOW,
    @WamEnumConstant(3) REGULAR_HIGH,
    @WamEnumConstant(4) CRITICAL_BLOCK,
    @WamEnumConstant(5) CRITICAL_UNBLOCK_LOW
}
