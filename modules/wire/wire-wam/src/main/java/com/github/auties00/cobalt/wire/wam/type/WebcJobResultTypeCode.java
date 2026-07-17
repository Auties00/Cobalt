package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcJobResultTypeCode")
@WamEnum
public enum WebcJobResultTypeCode {
    @WamEnumConstant(0) COMPLETED,
    @WamEnumConstant(1) ERROR,
    @WamEnumConstant(2) TIMEOUT,
    @WamEnumConstant(3) ABORTED
}
