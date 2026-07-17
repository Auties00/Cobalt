package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebContactListStartNewChatType")
@WamEnum
public enum WebContactListStartNewChatType {
    @WamEnumConstant(1) CONTACT,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) CONTACTLESS
}
