package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumContactSearchEntrypoint")
@WamEnum
public enum ContactSearchEntrypoint {
    @WamEnumConstant(1) NEW_CHAT,
    @WamEnumConstant(2) NEW_GROUP,
    @WamEnumConstant(3) NEW_CALL,
    @WamEnumConstant(4) ADD_TO_GROUP,
    @WamEnumConstant(5) CHATS_LIST_GLOBAL_SEARCH,
    @WamEnumConstant(6) CALLS_TAB_GLOBAL_SEARCH
}
