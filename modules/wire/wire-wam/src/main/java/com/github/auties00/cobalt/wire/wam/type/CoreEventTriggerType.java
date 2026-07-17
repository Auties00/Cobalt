package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCoreEventTriggerType")
@WamEnum
public enum CoreEventTriggerType {
    @WamEnumConstant(1) USER,
    @WamEnumConstant(2) REPORT
}
