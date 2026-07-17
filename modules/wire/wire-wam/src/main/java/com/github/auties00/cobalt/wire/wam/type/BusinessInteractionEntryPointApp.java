package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessInteractionEntryPointApp")
@WamEnum
public enum BusinessInteractionEntryPointApp {
    @WamEnumConstant(1) FACEBOOK,
    @WamEnumConstant(2) INSTAGRAM,
    @WamEnumConstant(3) WHATSAPP,
    @WamEnumConstant(4) EXTERNAL
}
