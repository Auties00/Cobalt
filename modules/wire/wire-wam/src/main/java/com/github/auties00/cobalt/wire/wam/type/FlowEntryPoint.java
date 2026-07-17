package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumFlowEntryPoint")
@WamEnum
public enum FlowEntryPoint {
    @WamEnumConstant(0) MESSAGE_CTA,
    @WamEnumConstant(1) BIZ_CARD_CTA,
    @WamEnumConstant(2) CART_FAB,
    @WamEnumConstant(3) MESSAGE_BODY,
    @WamEnumConstant(4) MESSAGE_IMAGE,
    @WamEnumConstant(5) GREETING_MESSAGE
}
