package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCrosspostDestinationType")
@WamEnum
public enum CrosspostDestinationType {
    @WamEnumConstant(1) FB,
    @WamEnumConstant(2) IG
}
