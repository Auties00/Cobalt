package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageChatType")
@WamEnum
public enum MessageChatType {
    @WamEnumConstant(0) OTHER,
    @WamEnumConstant(1) INDIVIDUAL,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) STATUS,
    @WamEnumConstant(4) BROADCAST,
    @WamEnumConstant(5) CHANNEL
}
