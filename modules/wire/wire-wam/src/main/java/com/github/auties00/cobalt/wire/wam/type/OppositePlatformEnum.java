package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOppositePlatformEnum")
@WamEnum
public enum OppositePlatformEnum {
    @WamEnumConstant(0) CONSUMER,
    @WamEnumConstant(1) BUSINESS
}
