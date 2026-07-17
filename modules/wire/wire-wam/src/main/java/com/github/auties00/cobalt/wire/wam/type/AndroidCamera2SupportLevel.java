package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAndroidCamera2SupportLevel")
@WamEnum
public enum AndroidCamera2SupportLevel {
    @WamEnumConstant(0) LIMITED,
    @WamEnumConstant(1) FULL,
    @WamEnumConstant(2) LEGACY,
    @WamEnumConstant(3) LEVEL_3,
    @WamEnumConstant(4) EXTERNAL
}
