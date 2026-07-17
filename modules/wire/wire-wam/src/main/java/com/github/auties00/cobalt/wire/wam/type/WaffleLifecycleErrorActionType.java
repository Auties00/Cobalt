package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaffleLifecycleErrorActionType")
@WamEnum
public enum WaffleLifecycleErrorActionType {
    @WamEnumConstant(0) NOT_APPLICABLE,
    @WamEnumConstant(1) RETRY,
    @WamEnumConstant(2) REQUEST_NONCE,
    @WamEnumConstant(3) PURGE,
    @WamEnumConstant(4) PAUSE,
    @WamEnumConstant(5) FAIL
}
