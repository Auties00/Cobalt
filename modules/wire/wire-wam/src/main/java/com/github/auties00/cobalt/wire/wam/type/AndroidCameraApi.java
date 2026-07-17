package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAndroidCameraApi")
@WamEnum
public enum AndroidCameraApi {
    @WamEnumConstant(1) API_1,
    @WamEnumConstant(2) API_2
}
