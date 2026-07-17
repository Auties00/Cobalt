package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcMessageQueryDirection")
@WamEnum
public enum WebcMessageQueryDirection {
    @WamEnumConstant(0) LOAD_PREV,
    @WamEnumConstant(1) LOAD_NEXT,
    @WamEnumConstant(2) LOAD_AROUND
}
