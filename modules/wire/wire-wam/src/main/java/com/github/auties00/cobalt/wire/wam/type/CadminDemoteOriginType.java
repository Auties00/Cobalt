package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCadminDemoteOriginType")
@WamEnum
public enum CadminDemoteOriginType {
    @WamEnumConstant(1) PROMOTION_NOTIFICATION,
    @WamEnumConstant(2) MEMBER_LIST
}
