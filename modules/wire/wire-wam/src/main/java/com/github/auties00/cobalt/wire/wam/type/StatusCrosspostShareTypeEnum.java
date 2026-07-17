package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusCrosspostShareTypeEnum")
@WamEnum
public enum StatusCrosspostShareTypeEnum {
    @WamEnumConstant(1) AUTO,
    @WamEnumConstant(2) MANUAL
}
