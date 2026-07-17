package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSizeBucket")
@WamEnum
public enum SizeBucket {
    @WamEnumConstant(1) LT32,
    @WamEnumConstant(2) LT64,
    @WamEnumConstant(3) LT128,
    @WamEnumConstant(4) LT256,
    @WamEnumConstant(5) LT512,
    @WamEnumConstant(6) LT1000,
    @WamEnumConstant(16) LT1024,
    @WamEnumConstant(7) LT1500,
    @WamEnumConstant(8) LT2000,
    @WamEnumConstant(9) LT2500,
    @WamEnumConstant(10) LT3000,
    @WamEnumConstant(11) LT3500,
    @WamEnumConstant(12) LT4000,
    @WamEnumConstant(13) LT4500,
    @WamEnumConstant(14) LT5000,
    @WamEnumConstant(15) LARGEST_BUCKET
}
