package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusyReason")
@WamEnum
public enum BusyReason {
    @WamEnumConstant(0) PSTN_RINGING,
    @WamEnumConstant(1) PSTN_OFFHOOK,
    @WamEnumConstant(2) WA_CALL_RINGING_OR_PENDING
}
