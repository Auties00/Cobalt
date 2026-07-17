package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessOwnerPlatform")
@WamEnum
public enum BusinessOwnerPlatform {
    @WamEnumConstant(1) SMBA,
    @WamEnumConstant(2) SMBI,
    @WamEnumConstant(3) ENT
}
