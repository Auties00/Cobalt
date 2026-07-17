package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcWindowNavigatorWebdriverType")
@WamEnum
public enum WebcWindowNavigatorWebdriverType {
    @WamEnumConstant(0) FALSE,
    @WamEnumConstant(1) TRUE,
    @WamEnumConstant(2) UNDEFINED
}
