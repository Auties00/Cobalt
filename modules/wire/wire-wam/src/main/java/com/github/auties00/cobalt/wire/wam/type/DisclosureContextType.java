package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureContextType")
@WamEnum
public enum DisclosureContextType {
    @WamEnumConstant(0) PREFILL_TEXT,
    @WamEnumConstant(1) EMPTY_PREFILL_TEXT,
    @WamEnumConstant(2) ICEBREAKERS,
    @WamEnumConstant(3) NOT_APPLICABLE
}
