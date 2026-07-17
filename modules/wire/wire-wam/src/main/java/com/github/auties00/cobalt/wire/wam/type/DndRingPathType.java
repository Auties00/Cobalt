package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDndRingPathType")
@WamEnum
public enum DndRingPathType {
    @WamEnumConstant(1) IMPERATIVE,
    @WamEnumConstant(2) CHANNEL,
    @WamEnumConstant(3) NONE
}
