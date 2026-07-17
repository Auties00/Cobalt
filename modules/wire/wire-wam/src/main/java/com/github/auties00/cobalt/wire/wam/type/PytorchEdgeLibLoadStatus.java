package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPytorchEdgeLibLoadStatus")
@WamEnum
public enum PytorchEdgeLibLoadStatus {
    @WamEnumConstant(0) PT_LOAD_STATUS_NONE,
    @WamEnumConstant(1) PT_LOAD_STATUS_SUCCESS,
    @WamEnumConstant(2) PT_LOAD_STATUS_FAILED
}
