package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOrderSignalType")
@WamEnum
public enum OrderSignalType {
    @WamEnumConstant(0) CREATED,
    @WamEnumConstant(1) UPDATED
}
