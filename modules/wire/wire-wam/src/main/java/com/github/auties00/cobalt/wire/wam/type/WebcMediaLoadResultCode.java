package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcMediaLoadResultCode")
@WamEnum
public enum WebcMediaLoadResultCode {
    @WamEnumConstant(0) SUCCESS,
    @WamEnumConstant(1) SILENCE,
    @WamEnumConstant(2) ZEROWIDTH
}
