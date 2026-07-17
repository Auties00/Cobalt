package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumFmxEntryPoint")
@WamEnum
public enum FmxEntryPoint {
    @WamEnumConstant(0) FMX_CARD,
    @WamEnumConstant(1) SAFETY_TOOLS
}
