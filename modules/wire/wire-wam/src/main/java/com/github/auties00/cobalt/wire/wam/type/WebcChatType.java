package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcChatType")
@WamEnum
public enum WebcChatType {
    @WamEnumConstant(0) INDIVIDUAL,
    @WamEnumConstant(1) GROUP,
    @WamEnumConstant(2) BROADCAST_LIST,
    @WamEnumConstant(3) COMMUNITY,
    @WamEnumConstant(4) NEWSLETTER
}
