package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatGatedReason")
@WamEnum
public enum ChatGatedReason {
    @WamEnumConstant(1) TOS3,
    @WamEnumConstant(2) COUNTRY
}
