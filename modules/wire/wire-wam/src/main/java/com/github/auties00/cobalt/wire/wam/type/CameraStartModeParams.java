package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCameraStartModeParams")
@WamEnum
public enum CameraStartModeParams {
    @WamEnumConstant(0) DEFAULT,
    @WamEnumConstant(1) CONSERVATIVE,
    @WamEnumConstant(2) STRICT,
    @WamEnumConstant(11) ERROR
}
