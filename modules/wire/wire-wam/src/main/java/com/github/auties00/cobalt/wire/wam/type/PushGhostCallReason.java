package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPushGhostCallReason")
@WamEnum
public enum PushGhostCallReason {
    @WamEnumConstant(0) OFFER_ELAPSED,
    @WamEnumConstant(1) OFFLINE_TERMINATE,
    @WamEnumConstant(2) OFFLINE_MD_REJECT,
    @WamEnumConstant(3) OFFLINE_MD_ACCEPT
}
