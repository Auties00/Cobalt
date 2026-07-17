package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSwAecType")
@WamEnum
public enum SwAecType {
    @WamEnumConstant(1) NONE,
    @WamEnumConstant(2) SPEEX,
    @WamEnumConstant(3) WEBRTC,
    @WamEnumConstant(4) MWEBRTC,
    @WamEnumConstant(5) ECHOSUPPRESSOR
}
