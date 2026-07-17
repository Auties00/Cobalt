package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusRowEntryMethod")
@WamEnum
public enum StatusRowEntryMethod {
    @WamEnumConstant(1) DIRECT_ROW_TAP,
    @WamEnumConstant(2) BACKWARDS_SWIPE,
    @WamEnumConstant(3) FOWARDS_SWIPE,
    @WamEnumConstant(4) BACKWARDS_TAP,
    @WamEnumConstant(5) FOWARDS_TAP,
    @WamEnumConstant(6) PREVIOUS_ROW_TIMEOUT
}
