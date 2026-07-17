package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeviceArch")
@WamEnum
public enum DeviceArch {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) ARM64,
    @WamEnumConstant(2) ARMV7,
    @WamEnumConstant(3) X86,
    @WamEnumConstant(4) X86_64
}
