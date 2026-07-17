package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDnsResolutionMethodType")
@WamEnum
public enum DnsResolutionMethodType {
    @WamEnumConstant(1) SYSTEM,
    @WamEnumConstant(2) GOOGLE,
    @WamEnumConstant(3) HARDCODED,
    @WamEnumConstant(4) NO_DNS,
    @WamEnumConstant(5) MNS,
    @WamEnumConstant(6) SOCKS_PROXY,
    @WamEnumConstant(7) MNS_SECONDARY
}
