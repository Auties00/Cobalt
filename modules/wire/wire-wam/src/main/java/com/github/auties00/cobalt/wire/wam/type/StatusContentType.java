package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusContentType")
@WamEnum
public enum StatusContentType {
    @WamEnumConstant(1) PHOTO,
    @WamEnumConstant(2) TEXT,
    @WamEnumConstant(3) URL,
    @WamEnumConstant(4) VIDEO,
    @WamEnumConstant(5) GIF,
    @WamEnumConstant(6) VOICE,
    @WamEnumConstant(7) FUTURE,
    @WamEnumConstant(8) PLACEHOLDER
}
