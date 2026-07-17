package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaffleLifecycleErrorCodeType")
@WamEnum
public enum WaffleLifecycleErrorCodeType {
    @WamEnumConstant(0) NOT_APPLICABLE,
    @WamEnumConstant(1) TIMEOUT,
    @WamEnumConstant(2) RATE_OVERLIMIT,
    @WamEnumConstant(3) NOT_AUTHORIZED,
    @WamEnumConstant(4) INVALID_PASSWORD,
    @WamEnumConstant(5) WF_NOT_FOUND,
    @WamEnumConstant(6) WF_STATE_MISMATCH,
    @WamEnumConstant(7) WF_SUSPENDED,
    @WamEnumConstant(8) UNKNOWN
}
