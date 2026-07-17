package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReachabilityStatus")
@WamEnum
public enum ReachabilityStatus {
    @WamEnumConstant(0) NOT_REACHABLE,
    @WamEnumConstant(1) REACHABLE_VIA_WIFI,
    @WamEnumConstant(2) REACHABLE_VIA_WWAN,
    @WamEnumConstant(3) REACHABILITY_UNKNOWN
}
