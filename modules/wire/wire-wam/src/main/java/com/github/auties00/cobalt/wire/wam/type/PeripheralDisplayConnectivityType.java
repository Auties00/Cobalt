package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPeripheralDisplayConnectivityType")
@WamEnum
public enum PeripheralDisplayConnectivityType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) TETHERED,
    @WamEnumConstant(2) WHATSAPI
}
