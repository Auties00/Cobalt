package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignalCanceledReason")
@WamEnum
public enum SignalCanceledReason {
    @WamEnumConstant(0) COMPANION_DEVICE,
    @WamEnumConstant(1) DISCLOSURE_DISMISSED,
    @WamEnumConstant(2) INVALID_ORIGINAL_URL
}
