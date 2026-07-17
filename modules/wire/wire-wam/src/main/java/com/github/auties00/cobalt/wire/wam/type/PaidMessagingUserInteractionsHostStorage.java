package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaidMessagingUserInteractionsHostStorage")
@WamEnum
public enum PaidMessagingUserInteractionsHostStorage {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) ON_PREMISE,
    @WamEnumConstant(2) FACEBOOK
}
