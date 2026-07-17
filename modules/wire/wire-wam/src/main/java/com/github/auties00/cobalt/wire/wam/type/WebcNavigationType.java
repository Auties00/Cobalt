package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcNavigationType")
@WamEnum
public enum WebcNavigationType {
    @WamEnumConstant(0) NAVIGATE_NEXT,
    @WamEnumConstant(1) RELOAD,
    @WamEnumConstant(2) BACK_FORWARD,
    @WamEnumConstant(255) UNDEFINED
}
