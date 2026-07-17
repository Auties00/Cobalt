package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPeripheralDeviceType")
@WamEnum
public enum PeripheralDeviceType {
    @WamEnumConstant(1) APPLE_WATCH,
    @WamEnumConstant(6) GRAPEVINE,
    @WamEnumConstant(7) STARFISH,
    @WamEnumConstant(8) HAMMERHEAD,
    @WamEnumConstant(9) GREATHAMMERHEAD,
    @WamEnumConstant(10) GREATWHITE,
    @WamEnumConstant(11) COLADA,
    @WamEnumConstant(12) SWIFTLET,
    @WamEnumConstant(13) MAKO,
    @WamEnumConstant(14) SILVERTIP,
    @WamEnumConstant(15) ZEBRA,
    @WamEnumConstant(16) LAGER,
    @WamEnumConstant(17) PYLADES,
    @WamEnumConstant(18) DIAMOND,
    @WamEnumConstant(19) UNKNOWN,
    @WamEnumConstant(20) WAG,
    @WamEnumConstant(21) CARPLAY
}
