package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallRelayBindStatus")
@WamEnum
public enum CallRelayBindStatus {
    @WamEnumConstant(1) UNBOUND,
    @WamEnumConstant(2) BINDED
}
