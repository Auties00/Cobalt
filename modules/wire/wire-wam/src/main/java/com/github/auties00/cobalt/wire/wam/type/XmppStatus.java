package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumXmppStatus")
@WamEnum
public enum XmppStatus {
    @WamEnumConstant(1) DISCONNECTED,
    @WamEnumConstant(2) CONNECTING,
    @WamEnumConstant(3) CONNECTED,
    @WamEnumConstant(4) UNKNOWN
}
