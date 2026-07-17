package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVideoTranscoderResultType")
@WamEnum
public enum VideoTranscoderResultType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) SUCCEEDED,
    @WamEnumConstant(2) FAILED,
    @WamEnumConstant(3) CANCELLED
}
