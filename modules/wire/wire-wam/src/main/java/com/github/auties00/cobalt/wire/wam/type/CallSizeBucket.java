package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallSizeBucket")
@WamEnum
public enum CallSizeBucket {
    @WamEnumConstant(1) SMALL,
    @WamEnumConstant(2) MEDIUM,
    @WamEnumConstant(3) LARGE,
    @WamEnumConstant(4) XLARGE
}
