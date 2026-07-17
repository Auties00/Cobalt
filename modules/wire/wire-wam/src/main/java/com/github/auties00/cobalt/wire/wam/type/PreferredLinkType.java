package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPreferredLinkType")
@WamEnum
public enum PreferredLinkType {
    @WamEnumConstant(0) LOCAL,
    @WamEnumConstant(1) UNIVERSAL
}
