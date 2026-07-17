package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStreamSocketProviderType")
@WamEnum
public enum StreamSocketProviderType {
    @WamEnumConstant(0) UNKNOWN_SOCKET,
    @WamEnumConstant(1) PLATFORM_SOCKET,
    @WamEnumConstant(2) MNS_SOCKET,
    @WamEnumConstant(3) SOCKS_PROXY
}
