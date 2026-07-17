package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeliveredPriority")
@WamEnum
public enum DeliveredPriority {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) HIGH,
    @WamEnumConstant(2) NORMAL
}
