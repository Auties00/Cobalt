package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGenaiBotType")
@WamEnum
public enum GenaiBotType {
    @WamEnumConstant(1) META_AI,
    @WamEnumConstant(2) UGC,
    @WamEnumConstant(3) CHARACTER
}
