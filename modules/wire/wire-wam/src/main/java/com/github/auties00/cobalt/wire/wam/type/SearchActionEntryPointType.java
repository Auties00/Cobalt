package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchActionEntryPointType")
@WamEnum
public enum SearchActionEntryPointType {
    @WamEnumConstant(1) CHATS_LIST,
    @WamEnumConstant(2) UPDATES,
    @WamEnumConstant(3) CALLS,
    @WamEnumConstant(4) CHAT_INFO,
    @WamEnumConstant(5) CHATS_NO_CONTACTS_PERMISSION,
    @WamEnumConstant(6) CHATS_NULL_STATE
}
