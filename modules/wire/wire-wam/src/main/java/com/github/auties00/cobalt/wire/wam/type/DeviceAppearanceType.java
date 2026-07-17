package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeviceAppearanceType")
@WamEnum
public enum DeviceAppearanceType {
    @WamEnumConstant(0) LIGHT,
    @WamEnumConstant(1) DARK
}
