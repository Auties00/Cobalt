package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPushOfferResult")
@WamEnum
public enum PushOfferResult {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) RECEIVE_OFFER,
    @WamEnumConstant(2) ACCEPT_BEFORE_OFFER,
    @WamEnumConstant(3) ACCEPT_END_CALL,
    @WamEnumConstant(4) ACCEPT_TIMEOUT,
    @WamEnumConstant(5) REJECT,
    @WamEnumConstant(6) TIMEOUT,
    @WamEnumConstant(7) ERROR,
    @WamEnumConstant(8) TERMINATE_PUSH,
    @WamEnumConstant(9) ACCEPT_TERMINATE_PUSH
}
