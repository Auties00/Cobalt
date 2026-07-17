package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCanonicalEntEventMarker")
@WamEnum
public enum CanonicalEntEventMarker {
    @WamEnumConstant(0) START,
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) ERROR
}
