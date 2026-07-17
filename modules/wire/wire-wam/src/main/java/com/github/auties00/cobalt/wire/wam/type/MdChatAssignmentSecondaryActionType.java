package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdChatAssignmentSecondaryActionType")
@WamEnum
public enum MdChatAssignmentSecondaryActionType {
    @WamEnumConstant(0) ACTION_SYSTEM_MESSAGE_ADDED_TO_CHAT_HISTORY,
    @WamEnumConstant(1) ACTION_SYSTEM_MESSAGE_CREATION_ERROR,
    @WamEnumConstant(2) ACTION_SYSTEM_MESSAGE_RENDERED,
    @WamEnumConstant(3) ACTION_CHAT_STATUS_TICKER_SHOWN,
    @WamEnumConstant(4) ACTION_TOOLTIP_SHOWN
}
