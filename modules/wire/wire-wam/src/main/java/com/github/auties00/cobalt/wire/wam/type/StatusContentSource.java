package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusContentSource")
@WamEnum
public enum StatusContentSource {
    @WamEnumConstant(1) CAMERA,
    @WamEnumConstant(2) EXTERNAL,
    @WamEnumConstant(3) FORWARD,
    @WamEnumConstant(4) GALLERY,
    @WamEnumConstant(5) CHANNEL,
    @WamEnumConstant(6) RESHARE,
    @WamEnumConstant(7) AI_IMAGINE,
    @WamEnumConstant(8) DRAFT
}
