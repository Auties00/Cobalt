package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumClientIpVersion")
@WamEnum
public enum ClientIpVersion {
    @WamEnumConstant(1) IPV4,
    @WamEnumConstant(2) IPV6
}
