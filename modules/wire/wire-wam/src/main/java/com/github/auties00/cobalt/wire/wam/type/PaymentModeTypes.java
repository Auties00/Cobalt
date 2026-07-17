package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaymentModeTypes")
@WamEnum
public enum PaymentModeTypes {
    @WamEnumConstant(0) CONSUMER,
    @WamEnumConstant(1) MERCHANT
}
