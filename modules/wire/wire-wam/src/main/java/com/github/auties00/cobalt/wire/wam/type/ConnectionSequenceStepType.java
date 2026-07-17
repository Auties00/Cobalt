package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumConnectionSequenceStepType")
@WamEnum
public enum ConnectionSequenceStepType {
    @WamEnumConstant(1) PUSH_OVERRIDES,
    @WamEnumConstant(2) PRIMARY,
    @WamEnumConstant(4) PUSH_FALLBACKS,
    @WamEnumConstant(5) HOST_FALLBACK,
    @WamEnumConstant(6) NO_DNS,
    @WamEnumConstant(7) SOFTLAYER,
    @WamEnumConstant(8) PRIMARY_HTTP,
    @WamEnumConstant(9) SOFTLAYER_HTTP,
    @WamEnumConstant(10) HOST_FALLBACK_HTTP,
    @WamEnumConstant(11) NO_DNS_HTTP
}
