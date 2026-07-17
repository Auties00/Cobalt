package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAppLaunchType")
@WamEnum
public enum AppLaunchType {
    @WamEnumConstant(1) COLD,
    @WamEnumConstant(2) WARM,
    @WamEnumConstant(3) LUKEWARM
}
