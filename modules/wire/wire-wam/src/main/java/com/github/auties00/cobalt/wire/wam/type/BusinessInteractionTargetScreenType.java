package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessInteractionTargetScreenType")
@WamEnum
public enum BusinessInteractionTargetScreenType {
    @WamEnumConstant(1) INDIVIDUAL_CHAT,
    @WamEnumConstant(2) LANDING_PAGE,
    @WamEnumConstant(3) OTHER
}
