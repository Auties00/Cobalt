package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameLinkType")
@WamEnum
public enum UsernameLinkType {
    @WamEnumConstant(1) NEW,
    @WamEnumConstant(2) EXISTING
}
