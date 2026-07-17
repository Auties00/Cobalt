package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiActionType")
@WamEnum
public enum LwiActionType {
    @WamEnumConstant(1) START_LOADING,
    @WamEnumConstant(2) STOP_LOADING,
    @WamEnumConstant(3) RESUME_LOADING,
    @WamEnumConstant(4) ERROR_LOADING,
    @WamEnumConstant(5) PAUSE_LOADING
}
