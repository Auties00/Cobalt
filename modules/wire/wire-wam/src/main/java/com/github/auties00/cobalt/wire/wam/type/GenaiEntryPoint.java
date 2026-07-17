package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGenaiEntryPoint")
@WamEnum
public enum GenaiEntryPoint {
    @WamEnumConstant(1) AI_SEARCH_CHATS_LIST,
    @WamEnumConstant(2) CHAT_THREAD,
    @WamEnumConstant(3) FAB,
    @WamEnumConstant(4) FAB_MM_TAP,
    @WamEnumConstant(5) MM_VOICE_TRANSITION,
    @WamEnumConstant(6) AI_TAB,
    @WamEnumConstant(7) CALL_HISTORY_FAB
}
