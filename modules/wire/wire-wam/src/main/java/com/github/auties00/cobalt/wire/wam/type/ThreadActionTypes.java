package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumThreadActionTypes")
@WamEnum
public enum ThreadActionTypes {
    @WamEnumConstant(1) PIN,
    @WamEnumConstant(2) UNPIN,
    @WamEnumConstant(3) DELETE,
    @WamEnumConstant(4) RENAME,
    @WamEnumConstant(5) CLICK_NEW_CHAT,
    @WamEnumConstant(6) CLICK_CHAT_HISTORY,
    @WamEnumConstant(7) CLICK_CONVERSATION_THREAD,
    @WamEnumConstant(8) THREAD_ENTER,
    @WamEnumConstant(9) THREAD_EXIT,
    @WamEnumConstant(10) THREAD_LIST_IMPRESSION,
    @WamEnumConstant(11) THREE_DOT_MENU,
    @WamEnumConstant(12) FIRST_PROMPT_SENT,
    @WamEnumConstant(13) SEARCH_RESULT_CLICK,
    @WamEnumConstant(14) SEARCH_RESULT_SHOWN,
    @WamEnumConstant(15) AI_HOME_IMPRESSION,
    @WamEnumConstant(16) CLICK_CONTINUE_CHAT_MODULE
}
