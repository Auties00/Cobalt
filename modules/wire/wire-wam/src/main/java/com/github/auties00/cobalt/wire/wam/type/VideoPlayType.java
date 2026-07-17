package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVideoPlayType")
@WamEnum
public enum VideoPlayType {
    @WamEnumConstant(1) FILE,
    @WamEnumConstant(2) STREAM,
    @WamEnumConstant(3) SENT
}
