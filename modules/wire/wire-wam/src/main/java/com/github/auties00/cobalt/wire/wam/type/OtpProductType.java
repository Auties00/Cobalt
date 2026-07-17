package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOtpProductType")
@WamEnum
public enum OtpProductType {
    @WamEnumConstant(0) ONE_TAP,
    @WamEnumConstant(1) ZERO_TAP,
    @WamEnumConstant(2) COPY_CODE
}
