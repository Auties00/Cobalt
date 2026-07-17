package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaidMessagingUserInteractionsComponentType")
@WamEnum
public enum PaidMessagingUserInteractionsComponentType {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) HEADER,
    @WamEnumConstant(2) BUTTON,
    @WamEnumConstant(3) BODY,
    @WamEnumConstant(4) FOOTER
}
