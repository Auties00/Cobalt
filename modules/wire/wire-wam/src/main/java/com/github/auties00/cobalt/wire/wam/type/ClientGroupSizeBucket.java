package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumClientGroupSizeBucket")
@WamEnum
public enum ClientGroupSizeBucket {
    @WamEnumConstant(1) SMALL,
    @WamEnumConstant(2) MEDIUM,
    @WamEnumConstant(3) LARGE,
    @WamEnumConstant(4) EXTRA_LARGE,
    @WamEnumConstant(5) XX_LARGE,
    @WamEnumConstant(6) XXX_LARGE,
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
