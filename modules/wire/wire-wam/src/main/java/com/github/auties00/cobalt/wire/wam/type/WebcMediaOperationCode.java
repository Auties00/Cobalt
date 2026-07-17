package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcMediaOperationCode")
@WamEnum
public enum WebcMediaOperationCode {
    @WamEnumConstant(1) DOWNLOAD,
    @WamEnumConstant(2) UPLOAD
}
