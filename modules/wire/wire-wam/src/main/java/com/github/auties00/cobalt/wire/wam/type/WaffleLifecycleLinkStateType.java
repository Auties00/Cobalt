package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaffleLifecycleLinkStateType")
@WamEnum
public enum WaffleLifecycleLinkStateType {
    @WamEnumConstant(0) NOT_APPLICABLE,
    @WamEnumConstant(1) ACTIVE,
    @WamEnumConstant(2) PAUSED,
    @WamEnumConstant(3) UNLINKED
}
