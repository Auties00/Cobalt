package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCa2dExtensionConnectionState")
@WamEnum
public enum Ca2dExtensionConnectionState {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) CREATING,
    @WamEnumConstant(2) CREATED,
    @WamEnumConstant(3) CONNECTING,
    @WamEnumConstant(4) CONNECTED,
    @WamEnumConstant(5) REMOVED
}
