package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCtwaLabelType")
@WamEnum
public enum CtwaLabelType {
    @WamEnumConstant(0) NEW_ORDER,
    @WamEnumConstant(1) PENDING_PAYMENT,
    @WamEnumConstant(2) PAID,
    @WamEnumConstant(3) ORDER_COMPLETE,
    @WamEnumConstant(4) NEW_CUSTOMER,
    @WamEnumConstant(5) DELIVERED,
    @WamEnumConstant(6) LEAD,
    @WamEnumConstant(7) FOLLOW_UP,
    @WamEnumConstant(8) APPOINTMENT,
    @WamEnumConstant(9) IMPORTANT,
    @WamEnumConstant(10) DO_NEW_ORDER,
    @WamEnumConstant(11) DO_LEAD,
    @WamEnumConstant(12) FAVORITES
}
