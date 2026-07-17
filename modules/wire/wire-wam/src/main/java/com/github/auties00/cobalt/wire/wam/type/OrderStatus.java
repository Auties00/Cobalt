package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOrderStatus")
@WamEnum
public enum OrderStatus {
    @WamEnumConstant(0) PROCESSING,
    @WamEnumConstant(1) SHIPPED,
    @WamEnumConstant(2) COMPLETED,
    @WamEnumConstant(3) CANCELLED,
    @WamEnumConstant(4) PENDING,
    @WamEnumConstant(5) PARTIALLY_SHIPPED,
    @WamEnumConstant(6) PAID_CHANGE
}
