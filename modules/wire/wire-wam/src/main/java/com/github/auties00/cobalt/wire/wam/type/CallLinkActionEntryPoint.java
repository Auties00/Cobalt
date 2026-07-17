package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallLinkActionEntryPoint")
@WamEnum
public enum CallLinkActionEntryPoint {
    @WamEnumConstant(1) LINK_CREATION,
    @WamEnumConstant(2) CALL_LOG_INFO,
    @WamEnumConstant(3) CONTACT_PICKER,
    @WamEnumConstant(4) BOTTOM_SHEET_LOBBY,
    @WamEnumConstant(5) BOTTOM_SHEET_CALL,
    @WamEnumConstant(6) LINK_CREATION_IN_CHAT,
    @WamEnumConstant(7) EVENT_CREATION,
    @WamEnumConstant(8) EVENT_CREATION_IN_CHAT,
    @WamEnumConstant(9) CHAT_THREAD,
    @WamEnumConstant(10) EVENT_DETAILS_SHEET,
    @WamEnumConstant(11) EVENT_CREATION_CHAT_ATTACHMENT,
    @WamEnumConstant(12) EVENT_EDIT_FROM_CHAT_BUBBLE,
    @WamEnumConstant(13) SCHEDULE_CALL_CALLS_TAB,
    @WamEnumConstant(14) SCHEDULE_CALL_CHAT_HEADER,
    @WamEnumConstant(15) SCHEDULE_CALL_LIST_PAGE_CREATE,
    @WamEnumConstant(16) SCHEDULE_CALL_LIST_PAGE_EDIT,
    @WamEnumConstant(17) SCHEDULE_CALL_EMPTY_PAGE,
    @WamEnumConstant(18) EVENT_EDIT_FROM_CONTACT_DETAILS,
    @WamEnumConstant(19) EVENT_EDIT_FROM_CONTACT_DETAILS_ALL_EVENTS,
    @WamEnumConstant(20) SEND_CALL_LINK_CHAT_HEADER,
    @WamEnumConstant(21) SEND_CALL_LINK_CALLS_TAB,
    @WamEnumConstant(22) SCHEDULE_CALL_CALLS_TAB_H_SCROLL,
    @WamEnumConstant(23) SEND_CALL_LINK_CALLS_TAB_H_SCROLL,
    @WamEnumConstant(24) IN_CALL_PARTICIPANT_LIST
}
