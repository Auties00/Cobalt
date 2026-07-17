package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPreciseSizeBucket")
@WamEnum
public enum PreciseSizeBucket {
    @WamEnumConstant(1) LT4,
    @WamEnumConstant(2) LT8,
    @WamEnumConstant(3) LT16,
    @WamEnumConstant(4) LT32,
    @WamEnumConstant(5) LT64,
    @WamEnumConstant(6) LT128,
    @WamEnumConstant(7) LT256,
    @WamEnumConstant(8) LT512,
    @WamEnumConstant(9) LT1000,
    @WamEnumConstant(10) LT1500,
    @WamEnumConstant(11) LT2000,
    @WamEnumConstant(12) LT2500,
    @WamEnumConstant(13) LT3000,
    @WamEnumConstant(14) LT3500,
    @WamEnumConstant(15) LT4000,
    @WamEnumConstant(16) LT4500,
    @WamEnumConstant(17) LT5000,
    @WamEnumConstant(18) LARGEST_BUCKET
}
