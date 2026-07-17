package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatSearchResultType")
@WamEnum
public enum ChatSearchResultType {
    @WamEnumConstant(0) CONTACT,
    @WamEnumConstant(1) CHAT,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) BROADCAST_LIST,
    @WamEnumConstant(4) MESSAGE,
    @WamEnumConstant(5) BUSINESS,
    @WamEnumConstant(6) GROUP_IN_COMMON
}
