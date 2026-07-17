package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebDbVersionSourceType")
@WamEnum
public enum WebDbVersionSourceType {
    @WamEnumConstant(1) KNOB,
    @WamEnumConstant(2) LOCAL,
    @WamEnumConstant(3) STATIC,
    @WamEnumConstant(4) KNOB_WITH_LOCAL_OVERRIDE
}
