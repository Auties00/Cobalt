package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCameraPreviewMode")
@WamEnum
public enum CameraPreviewMode {
    @WamEnumConstant(0) DEFAULT,
    @WamEnumConstant(1) TEXTURE,
    @WamEnumConstant(2) TEXTURE_FALLBACK
}
