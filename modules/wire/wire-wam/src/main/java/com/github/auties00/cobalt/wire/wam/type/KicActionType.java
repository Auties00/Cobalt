package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumKicActionType")
@WamEnum
public enum KicActionType {
    @WamEnumConstant(1) KEEP_MESSAGE,
    @WamEnumConstant(2) UNKEEP_MESSAGE
}
