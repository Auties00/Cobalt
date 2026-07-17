package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDataChannelConnectionState")
@WamEnum
public enum DataChannelConnectionState {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) INITED,
    @WamEnumConstant(2) CONNECTING,
    @WamEnumConstant(3) CONNECTED,
    @WamEnumConstant(4) CLOSED,
    @WamEnumConstant(5) ERROR
}
