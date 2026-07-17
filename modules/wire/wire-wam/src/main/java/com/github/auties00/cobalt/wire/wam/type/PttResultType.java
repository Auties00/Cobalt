package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPttResultType")
@WamEnum
public enum PttResultType {
    @WamEnumConstant(1) SENT,
    @WamEnumConstant(2) CANCELLED,
    @WamEnumConstant(3) TOO_SHORT
}
