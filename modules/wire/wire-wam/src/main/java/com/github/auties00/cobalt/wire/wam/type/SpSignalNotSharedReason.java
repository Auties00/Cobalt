package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSpSignalNotSharedReason")
@WamEnum
public enum SpSignalNotSharedReason {
    @WamEnumConstant(0) VALUE_NOT_AVAILABLE,
    @WamEnumConstant(1) WINDOW_EXPIRED,
    @WamEnumConstant(2) TOKEN_EXPIRED,
    @WamEnumConstant(3) SIGNAL_NOT_ALLOWLISTED,
    @WamEnumConstant(4) INVALID_URL
}
