package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaymentsIqCall")
@WamEnum
public enum PaymentsIqCall {
    @WamEnumConstant(0) REQUEST,
    @WamEnumConstant(1) FAILURE_RESPONSE,
    @WamEnumConstant(2) SUCCESS_RESPONSE
}
