package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumInitBweSource")
@WamEnum
public enum InitBweSource {
    @WamEnumConstant(0) DEFAULT,
    @WamEnumConstant(1) PROBING_ROTT_TO_RELAY,
    @WamEnumConstant(2) PROBING_E2E_PEER_RX,
    @WamEnumConstant(3) PROBING_E2E_RX,
    @WamEnumConstant(4) HIS_RECENT_PEER_RX,
    @WamEnumConstant(5) HIS_RECENT_RX,
    @WamEnumConstant(6) HIS_RECENT_PROBING_ROTT_TO_RELAY,
    @WamEnumConstant(7) HIS_RECENT_PROBING_E2E_PEER_RX,
    @WamEnumConstant(8) HIS_RECENT_PROBING_E2E_RX,
    @WamEnumConstant(9) ONE_SIDE_INITIAL_BANDWIDTH_ESTIMATION
}
