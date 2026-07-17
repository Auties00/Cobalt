package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLastMessageDirection")
@WamEnum
public enum LastMessageDirection {
    @WamEnumConstant(0) OPPOSITE_PARTY_INITIATED,
    @WamEnumConstant(1) SELF_INITIATED
}
