package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallTrigger")
@WamEnum
public enum CallTrigger {
    @WamEnumConstant(0) ONLINE_STANZA,
    @WamEnumConstant(1) OFFLINE_STANZA,
    @WamEnumConstant(2) FCM_PUSH_PAYLOAD,
    @WamEnumConstant(3) FBNS_PUSH_PAYLOAD
}
