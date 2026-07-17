package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWpbujSource")
@WamEnum
public enum WpbujSource {
    @WamEnumConstant(1) APP_WIDE,
    @WamEnumConstant(2) ONE_TO_ONE,
    @WamEnumConstant(3) GROUP,
    @WamEnumConstant(4) LIST
}
