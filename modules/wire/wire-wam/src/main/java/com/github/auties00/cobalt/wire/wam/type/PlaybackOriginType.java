package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPlaybackOriginType")
@WamEnum
public enum PlaybackOriginType {
    @WamEnumConstant(1) CONVERSATION,
    @WamEnumConstant(2) STATUS,
    @WamEnumConstant(3) CHANNELS
}
