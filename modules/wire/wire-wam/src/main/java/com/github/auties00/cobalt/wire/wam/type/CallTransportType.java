package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallTransportType")
@WamEnum
public enum CallTransportType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) P2P,
    @WamEnumConstant(2) UDP_RELAY,
    @WamEnumConstant(3) TCP_RELAY,
    @WamEnumConstant(4) MIXED
}
