package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumConsumerBizActionTypeEnum")
@WamEnum
public enum ConsumerBizActionTypeEnum {
    @WamEnumConstant(0) IMPRESSION,
    @WamEnumConstant(1) TAP,
    @WamEnumConstant(2) SCROLL,
    @WamEnumConstant(3) LONG_PRESS,
    @WamEnumConstant(4) SWIPE,
    @WamEnumConstant(5) CLICK,
    @WamEnumConstant(6) VIEW,
    @WamEnumConstant(7) OPEN_CHAT
}
