package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSurfaceCode")
@WamEnum
public enum SurfaceCode {
    @WamEnumConstant(1) MEDIA,
    @WamEnumConstant(2) LINKS,
    @WamEnumConstant(3) DOCS
}
