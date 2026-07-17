package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumForwardOrigin")
@WamEnum
public enum ForwardOrigin {
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) CHAT,
    @WamEnumConstant(3) STATUS,
    @WamEnumConstant(4) CHANNELS,
    @WamEnumConstant(5) META_AI
}
