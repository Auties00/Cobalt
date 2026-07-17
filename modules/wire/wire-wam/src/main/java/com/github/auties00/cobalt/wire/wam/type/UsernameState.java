package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameState")
@WamEnum
public enum UsernameState {
    @WamEnumConstant(1) RESERVED,
    @WamEnumConstant(2) CREATED,
    @WamEnumConstant(3) ACTIVATED
}
