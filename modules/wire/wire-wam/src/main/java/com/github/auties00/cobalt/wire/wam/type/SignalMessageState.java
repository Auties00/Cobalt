package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignalMessageState")
@WamEnum
public enum SignalMessageState {
    @WamEnumConstant(0) TRUNCATED,
    @WamEnumConstant(1) EXPANDED,
    @WamEnumConstant(2) ORIGINAL
}
