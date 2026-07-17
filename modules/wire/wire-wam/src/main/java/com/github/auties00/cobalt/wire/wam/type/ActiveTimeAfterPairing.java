package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumActiveTimeAfterPairing")
@WamEnum
public enum ActiveTimeAfterPairing {
    @WamEnumConstant(1) MINS_10,
    @WamEnumConstant(2) MINS_20,
    @WamEnumConstant(3) MINS_40,
    @WamEnumConstant(4) MINS_60,
    @WamEnumConstant(5) MINS_5
}
