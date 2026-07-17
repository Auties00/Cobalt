package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumApplicationState")
@WamEnum
public enum ApplicationState {
    @WamEnumConstant(1) FOREGROUND,
    @WamEnumConstant(2) BACKGROUND
}
