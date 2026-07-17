package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcStreamModeCode")
@WamEnum
public enum WebcStreamModeCode {
    @WamEnumConstant(0) QR,
    @WamEnumConstant(1) MAIN,
    @WamEnumConstant(2) SYNCING,
    @WamEnumConstant(3) OFFLINE,
    @WamEnumConstant(4) CONFLICT,
    @WamEnumConstant(5) PROXYBLOCK,
    @WamEnumConstant(6) TOS_BLOCK,
    @WamEnumConstant(7) SMB_TOS_BLOCK,
    @WamEnumConstant(8) DEPRECATED_VERSION,
    @WamEnumConstant(9) LOCK
}
