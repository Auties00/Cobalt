package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureType")
@WamEnum
public enum DisclosureType {
    @WamEnumConstant(0) NON_BLOCKING,
    @WamEnumConstant(1) BLOCKING,
    @WamEnumConstant(2) INFO,
    @WamEnumConstant(3) IN_THREAD_BLOCKING,
    @WamEnumConstant(4) IN_THREAD_BLOCKING_WITH_BACK
}
