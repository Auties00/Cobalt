package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallSizeType")
@WamEnum
public enum CallSizeType {
    @WamEnumConstant(1) ONE_TO_ONE,
    @WamEnumConstant(2) ADHOC,
    @WamEnumConstant(3) LGC,
    @WamEnumConstant(4) CALL_LINK
}
