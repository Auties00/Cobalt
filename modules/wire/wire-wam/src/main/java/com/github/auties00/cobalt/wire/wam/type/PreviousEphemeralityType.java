package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPreviousEphemeralityType")
@WamEnum
public enum PreviousEphemeralityType {
    @WamEnumConstant(1) AFTER_READ,
    @WamEnumConstant(2) DISAPPEARING_MESSAGE
}
