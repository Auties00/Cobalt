package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisclosureInteraction")
@WamEnum
public enum DisclosureInteraction {
    @WamEnumConstant(0) CONTINUE,
    @WamEnumConstant(1) CANCEL,
    @WamEnumConstant(2) DISMISSED,
    @WamEnumConstant(3) LEARN_MORE
}
