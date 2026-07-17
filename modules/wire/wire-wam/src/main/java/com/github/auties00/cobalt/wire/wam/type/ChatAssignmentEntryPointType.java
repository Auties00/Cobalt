package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatAssignmentEntryPointType")
@WamEnum
public enum ChatAssignmentEntryPointType {
    @WamEnumConstant(0) CONVERSATION_OVERFLOW_MENU,
    @WamEnumConstant(1) CONTACT_INFO_SCREEN,
    @WamEnumConstant(2) MULTI_SELECT,
    @WamEnumConstant(3) SYSTEM_MESSAGE,
    @WamEnumConstant(4) CHAT_LIST_SWIPE,
    @WamEnumConstant(5) AI_REPLIES
}
