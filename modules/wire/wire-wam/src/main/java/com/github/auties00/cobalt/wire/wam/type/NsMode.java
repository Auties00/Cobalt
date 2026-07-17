package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumNsMode")
@WamEnum
public enum NsMode {
    @WamEnumConstant(1) NONE,
    @WamEnumConstant(2) SOFTWARE,
    @WamEnumConstant(3) BUILTIN
}
