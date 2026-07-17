package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaymentActionTypes")
@WamEnum
public enum PaymentActionTypes {
    @WamEnumConstant(0) VIEW,
    @WamEnumConstant(1) CLICK,
    @WamEnumConstant(2) ENTER,
    @WamEnumConstant(3) API,
    @WamEnumConstant(4) SHOW_ERROR
}
