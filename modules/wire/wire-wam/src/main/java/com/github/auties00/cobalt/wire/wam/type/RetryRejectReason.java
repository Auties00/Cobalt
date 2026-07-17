package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumRetryRejectReason")
@WamEnum
public enum RetryRejectReason {
    @WamEnumConstant(0) OTHER,
    @WamEnumConstant(1) DOUBLE_CHECKMARK,
    @WamEnumConstant(2) IDENTITY_CHANGE,
    @WamEnumConstant(3) MESSAGE_NOT_EXIST,
    @WamEnumConstant(4) HIGH_RETRY_COUNT,
    @WamEnumConstant(5) MESSAGE_EXPIRED,
    @WamEnumConstant(6) STATUS_EXPIRED,
    @WamEnumConstant(7) INCORRECT_CHAT_TYPE,
    @WamEnumConstant(8) RECEIPT_NOT_FOUND,
    @WamEnumConstant(9) DEVICE_NOT_RECIPIENT,
    @WamEnumConstant(10) HOSTED_DEVICE_CHANGED,
    @WamEnumConstant(11) DEVICE_NOT_IN_DATABASE,
    @WamEnumConstant(12) DEVICE_REVOKED
}
