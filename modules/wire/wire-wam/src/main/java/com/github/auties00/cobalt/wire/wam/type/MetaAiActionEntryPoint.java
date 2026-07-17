package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaAiActionEntryPoint")
@WamEnum
public enum MetaAiActionEntryPoint {
    @WamEnumConstant(1) THREE_DOT_MENU,
    @WamEnumConstant(2) THREAD_LIST_VIEW,
    @WamEnumConstant(3) TOP_NAV,
    @WamEnumConstant(4) CHAT_LIST,
    @WamEnumConstant(5) NAVIGATION_BAR_BUTTON,
    @WamEnumConstant(6) CONTINUE_CHAT_MODULE
}
