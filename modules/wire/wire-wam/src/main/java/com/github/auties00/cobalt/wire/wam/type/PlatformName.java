package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPlatformName")
@WamEnum
public enum PlatformName {
    @WamEnumConstant(0) WINDOWS,
    @WamEnumConstant(1) MAC,
    @WamEnumConstant(2) LINUX,
    @WamEnumConstant(3) ANDROID,
    @WamEnumConstant(4) CHROME_OS,
    @WamEnumConstant(5) IOS,
    @WamEnumConstant(6) UNKNOWN
}
