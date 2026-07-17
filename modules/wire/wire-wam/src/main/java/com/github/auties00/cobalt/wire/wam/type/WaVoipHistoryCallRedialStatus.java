package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaVoipHistoryCallRedialStatus")
@WamEnum
public enum WaVoipHistoryCallRedialStatus {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) IS_REDIAL,
    @WamEnumConstant(2) NOT_REDIAL
}
