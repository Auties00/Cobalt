package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcRuntimeEnvCode")
@WamEnum
public enum WebcRuntimeEnvCode {
    @WamEnumConstant(1) MAIN,
    @WamEnumConstant(2) WEB_WORKER,
    @WamEnumConstant(3) SERVICE_WORKER
}
