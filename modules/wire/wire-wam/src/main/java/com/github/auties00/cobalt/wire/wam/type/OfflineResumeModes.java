package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOfflineResumeModes")
@WamEnum
public enum OfflineResumeModes {
    @WamEnumConstant(1) RESUME_FROM_RESTART,
    @WamEnumConstant(2) RESUME_FROM_OPEN_TAB,
    @WamEnumConstant(3) UNKNOWN,
    @WamEnumConstant(4) CONNECT_REASON_USER,
    @WamEnumConstant(5) CONNECT_REASON_PUSH,
    @WamEnumConstant(6) CONNECT_REASON_BACKOFF
}
