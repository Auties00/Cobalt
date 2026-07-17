package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGifSearchProvider")
@WamEnum
public enum GifSearchProvider {
    @WamEnumConstant(0) GIPHY,
    @WamEnumConstant(1) TENOR,
    @WamEnumConstant(2) KLIPY
}
