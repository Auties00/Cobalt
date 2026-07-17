package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaymentTransactionStatusType")
@WamEnum
public enum PaymentTransactionStatusType {
    @WamEnumConstant(1) FAILED,
    @WamEnumConstant(2) COMPLETED,
    @WamEnumConstant(3) PENDING
}
