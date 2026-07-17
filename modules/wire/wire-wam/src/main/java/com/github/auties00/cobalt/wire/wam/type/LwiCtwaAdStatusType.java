package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiCtwaAdStatusType")
@WamEnum
public enum LwiCtwaAdStatusType {
    @WamEnumConstant(1) ACTIVE,
    @WamEnumConstant(2) SCHEDULED,
    @WamEnumConstant(3) PAUSED,
    @WamEnumConstant(4) NOT_DELIVERING,
    @WamEnumConstant(5) REJECTED,
    @WamEnumConstant(6) FINISHED,
    @WamEnumConstant(7) IN_REVIEW,
    @WamEnumConstant(8) COMPLETED,
    @WamEnumConstant(9) EXTENDABLE,
    @WamEnumConstant(10) UNABLE_TO_CREATE,
    @WamEnumConstant(11) LIMITED_DELIVERY
}
