package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCompanionAddContactEventType")
@WamEnum
public enum CompanionAddContactEventType {
    @WamEnumConstant(0) CREATE_NEW,
    @WamEnumConstant(1) EDIT
}
