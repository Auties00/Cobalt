package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDiscoveryOriginType")
@WamEnum
public enum DiscoveryOriginType {
    @WamEnumConstant(1) AI_TAB,
    @WamEnumConstant(2) AI_HOME,
    @WamEnumConstant(3) AI_HOME_IN_TAB
}
