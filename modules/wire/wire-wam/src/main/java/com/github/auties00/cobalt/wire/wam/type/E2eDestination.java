package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumE2eDestination")
@WamEnum
public enum E2eDestination {
    @WamEnumConstant(0) INDIVIDUAL,
    @WamEnumConstant(1) GROUP,
    @WamEnumConstant(2) LIST,
    @WamEnumConstant(3) STATUS,
    @WamEnumConstant(4) CHANNEL,
    @WamEnumConstant(5) INTEROP
}
