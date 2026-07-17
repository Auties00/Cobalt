package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatActionEntryPoint")
@WamEnum
public enum ChatActionEntryPoint {
    @WamEnumConstant(1) CONVERSATION_LIST,
    @WamEnumConstant(2) CONTACT_INFO,
    @WamEnumConstant(3) GROUP_INFO,
    @WamEnumConstant(4) SEARCH_LIST,
    @WamEnumConstant(5) CONVERSATION_LIST_BULK_EDIT,
    @WamEnumConstant(6) CONVERSATION_MENU,
    @WamEnumConstant(7) WEB_ACTION,
    @WamEnumConstant(8) SYSTEM_NOTIFICATIONS,
    @WamEnumConstant(9) SPAM_FOLDER,
    @WamEnumConstant(10) DEPENDENT_MESSAGE_REQUESTS_FOLDER,
    @WamEnumConstant(11) DEPENDENT_CONVERSATION_MESAGE_REQUEST
}
