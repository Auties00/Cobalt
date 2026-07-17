package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAiFetchMediaType")
@WamEnum
public enum AiFetchMediaType {
    @WamEnumConstant(1) IMAGE_SINGLE,
    @WamEnumConstant(2) GRID_HIGH_RES,
    @WamEnumConstant(3) GRID_LOW_RES,
    @WamEnumConstant(4) INLINE_HIGH_RES,
    @WamEnumConstant(5) INLINE_LOW_RES
}
