package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallLinkShareChatType")
@WamEnum
public enum CallLinkShareChatType {
    @WamEnumConstant(1) INDIVIDUAL,
    @WamEnumConstant(2) BUSINESS,
    @WamEnumConstant(3) GROUP_LARGE,
    @WamEnumConstant(4) GROUP_3P,
    @WamEnumConstant(5) GROUP_8P,
    @WamEnumConstant(6) GROUP_32P
}
