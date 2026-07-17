package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaffleLifecycleTraceSourceType")
@WamEnum
public enum WaffleLifecycleTraceSourceType {
    @WamEnumConstant(0) NOT_APPLICABLE,
    @WamEnumConstant(1) SYNCD,
    @WamEnumConstant(2) NOTIFICATION_LINKED,
    @WamEnumConstant(3) NOTIFICATION_UNLINKED,
    @WamEnumConstant(4) NOTIFICATION_RESYNC,
    @WamEnumConstant(5) NONCE_REQUEST,
    @WamEnumConstant(6) NONCE_RESPONSE,
    @WamEnumConstant(7) REFRESH_TOKEN,
    @WamEnumConstant(8) ERROR_RETRY,
    @WamEnumConstant(9) PRIMARY_NONCE_REQUEST,
    @WamEnumConstant(10) PRIMARY_NONCE_RESPONSE
}
