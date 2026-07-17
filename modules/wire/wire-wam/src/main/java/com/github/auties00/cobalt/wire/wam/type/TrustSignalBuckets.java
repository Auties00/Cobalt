package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumTrustSignalBuckets")
@WamEnum
public enum TrustSignalBuckets {
    @WamEnumConstant(1) B0,
    @WamEnumConstant(2) B1,
    @WamEnumConstant(3) B2,
    @WamEnumConstant(4) B11,
    @WamEnumConstant(5) B51,
    @WamEnumConstant(6) B101,
    @WamEnumConstant(7) B501,
    @WamEnumConstant(8) B1K,
    @WamEnumConstant(9) B10K,
    @WamEnumConstant(10) B100K,
    @WamEnumConstant(11) B1M
}
