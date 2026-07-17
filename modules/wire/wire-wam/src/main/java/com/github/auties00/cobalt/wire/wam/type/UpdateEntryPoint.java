package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUpdateEntryPoint")
@WamEnum
public enum UpdateEntryPoint {
    @WamEnumConstant(1) CONTACT_INFO,
    @WamEnumConstant(2) GROUP_INFO,
    @WamEnumConstant(3) CHAT_MORE_OPTIONS,
    @WamEnumConstant(4) CHAT_LONG_PRESS_OPTIONS,
    @WamEnumConstant(5) FILTER_CONTEXT_MENU,
    @WamEnumConstant(6) ADD_LIST_FILTER,
    @WamEnumConstant(7) LIST_SETTINGS,
    @WamEnumConstant(8) LIST_NUX,
    @WamEnumConstant(9) DEEPLINK,
    @WamEnumConstant(10) PIN_ALERT,
    @WamEnumConstant(11) AE_SYSTEM_MESSAGE,
    @WamEnumConstant(12) BIZ_TOOLS,
    @WamEnumConstant(13) CHAT_LIST_SWIPE_MORE_MENU,
    @WamEnumConstant(14) CHAT_HEADER,
    @WamEnumConstant(15) MESSAGE_CONTEXT_MENU,
    @WamEnumConstant(16) CHAT_LIST_CONTEXT_MENU,
    @WamEnumConstant(17) BROADCAST_LIST_CHAT_OVERFLOW,
    @WamEnumConstant(18) BROADCAST_LIST_CHAT_INFO_OVERFLOW,
    @WamEnumConstant(19) CHAT_LIST_FILTER_MANAGE,
    @WamEnumConstant(20) QP_BIZ_TOOLS,
    @WamEnumConstant(21) CHAT_LIST_FILTER_EMPTY,
    @WamEnumConstant(22) CHAT_FILTER_PILL,
    @WamEnumConstant(23) CREATE_CUSTOM_LIST,
    @WamEnumConstant(24) LIST_TOAST,
    @WamEnumConstant(25) EDIT_LIST_VIEW,
    @WamEnumConstant(26) LIST_DETAIL_VIEW,
    @WamEnumConstant(27) AUTO_CREATED,
    @WamEnumConstant(28) MULTI_SELECT,
    @WamEnumConstant(29) QUICK_REPLIES,
    @WamEnumConstant(30) LISTS_MORE_BOTTOM_SHEET,
    @WamEnumConstant(31) LIST_MORE_BOTTOM_SHEET_OPEN
}
