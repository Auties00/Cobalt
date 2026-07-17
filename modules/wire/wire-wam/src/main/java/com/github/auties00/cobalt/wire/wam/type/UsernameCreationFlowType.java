package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameCreationFlowType")
@WamEnum
public enum UsernameCreationFlowType {
    @WamEnumConstant(1) CREATION,
    @WamEnumConstant(2) RESERVATION,
    @WamEnumConstant(3) ACTIVATION,
    @WamEnumConstant(4) MANAGEMENT
}
