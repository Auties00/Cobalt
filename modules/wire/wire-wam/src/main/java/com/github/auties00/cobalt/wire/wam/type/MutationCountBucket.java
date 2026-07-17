package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMutationCountBucket")
@WamEnum
public enum MutationCountBucket {
    @WamEnumConstant(1) ZERO,
    @WamEnumConstant(2) ONE,
    @WamEnumConstant(3) LT10,
    @WamEnumConstant(4) LT100,
    @WamEnumConstant(5) LT500,
    @WamEnumConstant(6) LT1K,
    @WamEnumConstant(7) LT5K,
    @WamEnumConstant(8) GTE5K
}
