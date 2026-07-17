package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumImagineMediaType")
@WamEnum
public enum ImagineMediaType {
    @WamEnumConstant(1) IMAGE,
    @WamEnumConstant(2) VIDEO,
    @WamEnumConstant(3) DOCUMENT,
    @WamEnumConstant(4) MIXED
}
