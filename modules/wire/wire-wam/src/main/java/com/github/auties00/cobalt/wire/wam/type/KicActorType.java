package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumKicActorType")
@WamEnum
public enum KicActorType {
    @WamEnumConstant(1) SENDER,
    @WamEnumConstant(2) RECIPIENT
}
