package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPinInChatType")
@WamEnum
public enum PinInChatType {
    @WamEnumConstant(1) PIN_FOR_ALL,
    @WamEnumConstant(2) UNPIN_FOR_ALL
}
