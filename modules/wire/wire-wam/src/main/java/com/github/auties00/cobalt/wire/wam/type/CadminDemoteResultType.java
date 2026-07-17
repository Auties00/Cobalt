package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCadminDemoteResultType")
@WamEnum
public enum CadminDemoteResultType {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) FAILURE,
    @WamEnumConstant(3) CANCEL,
    @WamEnumConstant(4) RETRY_SUCCESS,
    @WamEnumConstant(5) RETRY_FAILURE,
    @WamEnumConstant(6) RETRY_CANCEL
}
