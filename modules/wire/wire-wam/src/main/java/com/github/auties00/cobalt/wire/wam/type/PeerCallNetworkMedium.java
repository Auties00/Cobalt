package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPeerCallNetworkMedium")
@WamEnum
public enum PeerCallNetworkMedium {
    @WamEnumConstant(1) CELLULAR,
    @WamEnumConstant(2) WIFI,
    @WamEnumConstant(3) NONE
}
