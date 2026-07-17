package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumFieldStatsRowType")
@WamEnum
public enum FieldStatsRowType {
    @WamEnumConstant(1) BOTH,
    @WamEnumConstant(2) SELF,
    @WamEnumConstant(3) PEER
}
