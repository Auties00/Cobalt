package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessToolsLinkedAccountType")
@WamEnum
public enum BusinessToolsLinkedAccountType {
    @WamEnumConstant(0) FACEBOOK,
    @WamEnumConstant(1) INSTAGRAM
}
