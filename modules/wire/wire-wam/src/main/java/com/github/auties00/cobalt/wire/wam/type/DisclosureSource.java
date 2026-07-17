package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureSource")
@WamEnum
public enum DisclosureSource {
    @WamEnumConstant(0) BLOCKING,
    @WamEnumConstant(1) NON_BLOCKING,
    @WamEnumConstant(2) INFO
}
