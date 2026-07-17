package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAboutRequestType")
@WamEnum
public enum AboutRequestType {
    @WamEnumConstant(1) CREATE_NEW,
    @WamEnumConstant(2) UPDATE_EXISTING,
    @WamEnumConstant(3) CLEAR_EXISTING
}
