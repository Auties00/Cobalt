package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOptimisticFlagType")
@WamEnum
public enum OptimisticFlagType {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) OPTIMISTIC,
    @WamEnumConstant(2) OPT_USED,
    @WamEnumConstant(3) OPT_TAKEOVER,
    @WamEnumConstant(4) OPT_DISABLED
}
