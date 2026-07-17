package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumNativeContactsNuxEntryPoint")
@WamEnum
public enum NativeContactsNuxEntryPoint {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) NEW_CONTACT,
    @WamEnumConstant(2) CONTACT_HEADER,
    @WamEnumConstant(3) GROUP,
    @WamEnumConstant(4) SHARED_CONTACT,
    @WamEnumConstant(5) CONTACT_SETTINGS,
    @WamEnumConstant(6) CHANGE_NUMBER_BANNER,
    @WamEnumConstant(7) MESSAGE_SENDER,
    @WamEnumConstant(8) SPAM_PROMPT,
    @WamEnumConstant(9) CONVERSATION_ROW,
    @WamEnumConstant(10) CHAT_SEARCH,
    @WamEnumConstant(11) CHAT,
    @WamEnumConstant(12) CALL_HISTORY,
    @WamEnumConstant(13) STATUS,
    @WamEnumConstant(14) BROADCAST_PART_LIST,
    @WamEnumConstant(15) PRODUCT,
    @WamEnumConstant(16) BLACKLIST,
    @WamEnumConstant(17) NEW_CHAT_SEARCH,
    @WamEnumConstant(18) CALL_GRID,
    @WamEnumConstant(19) CALL_HISTORY_NEW_CALL,
    @WamEnumConstant(20) CONTACT_LIST,
    @WamEnumConstant(21) DIALER,
    @WamEnumConstant(22) CONTACT_QR_CODE,
    @WamEnumConstant(23) APP_STARTUP,
    @WamEnumConstant(24) DEPENDENT_MESSAGE_REQUEST_ACTION
}
