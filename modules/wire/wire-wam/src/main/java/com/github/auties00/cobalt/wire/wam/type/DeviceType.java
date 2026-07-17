package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeviceType")
@WamEnum
public enum DeviceType {
    @WamEnumConstant(1) PRIMARY,
    @WamEnumConstant(2) COMPANION
}
