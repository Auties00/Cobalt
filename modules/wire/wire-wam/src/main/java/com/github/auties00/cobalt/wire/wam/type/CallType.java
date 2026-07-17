package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallType")
@WamEnum
public enum CallType {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) VIDEO,
    @WamEnumConstant(2) VOICE
}
