package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageBodyTypeEnum")
@WamEnum
public enum MessageBodyTypeEnum {
    @WamEnumConstant(1) MESSAGE,
    @WamEnumConstant(2) CAROUSEL_CARD
}
