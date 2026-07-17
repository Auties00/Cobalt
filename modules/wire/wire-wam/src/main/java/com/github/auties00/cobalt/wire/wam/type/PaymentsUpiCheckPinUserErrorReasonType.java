package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaymentsUpiCheckPinUserErrorReasonType")
@WamEnum
public enum PaymentsUpiCheckPinUserErrorReasonType {
    @WamEnumConstant(1) PIN_INVALID,
    @WamEnumConstant(2) INSUFFICIENT_BALANCE,
    @WamEnumConstant(3) SENDER_VPA_HANDLE,
    @WamEnumConstant(4) RECEIVER_VPA_HANDLE
}
