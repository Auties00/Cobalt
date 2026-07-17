package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumToggleUpdateAction")
@WamEnum
public enum ToggleUpdateAction {
    @WamEnumConstant(0) TURN_ON,
    @WamEnumConstant(1) TURN_OFF
}
