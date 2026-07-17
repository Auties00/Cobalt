package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUpiPaymentsPspIdType")
@WamEnum
public enum UpiPaymentsPspIdType {
    @WamEnumConstant(1) ICICI,
    @WamEnumConstant(2) HDFC,
    @WamEnumConstant(3) AXIS,
    @WamEnumConstant(4) SBI
}
