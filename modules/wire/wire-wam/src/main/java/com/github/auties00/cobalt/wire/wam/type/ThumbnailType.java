package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumThumbnailType")
@WamEnum
public enum ThumbnailType {
    @WamEnumConstant(1) HQ,
    @WamEnumConstant(2) CUSTOM,
    @WamEnumConstant(3) MEDIA_BASED
}
