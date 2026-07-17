package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebFlowType")
@WamEnum
public enum WebFlowType {
    @WamEnumConstant(0) NATIVE_WEB,
    @WamEnumConstant(1) EXTERNAL_WEB
}
