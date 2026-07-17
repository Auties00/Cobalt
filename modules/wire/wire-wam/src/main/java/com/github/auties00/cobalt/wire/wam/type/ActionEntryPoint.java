package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumActionEntryPoint")
@WamEnum
public enum ActionEntryPoint {
    @WamEnumConstant(0) CHAT_INFO,
    @WamEnumConstant(1) CHAT_CONTEXT_MENU,
    @WamEnumConstant(2) NOTIFICATIONS,
    @WamEnumConstant(3) CONTACTS,
    @WamEnumConstant(4) CHAT_LIST,
    @WamEnumConstant(5) UNKNOWN,
    @WamEnumConstant(6) PRIVATE_REPLY,
    @WamEnumConstant(7) STATUS_REPLY,
    @WamEnumConstant(8) SEARCH,
    @WamEnumConstant(9) DIRECT_MESSAGE,
    @WamEnumConstant(10) PRIVACY_SETTINGS,
    @WamEnumConstant(11) CHAT_LONG_PRESS_OPTIONS,
    @WamEnumConstant(12) CHAT_MORE_OPTIONS,
    @WamEnumConstant(13) SIDEBAR
}
