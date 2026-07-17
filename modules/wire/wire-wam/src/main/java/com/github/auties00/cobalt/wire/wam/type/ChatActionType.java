package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatActionType")
@WamEnum
public enum ChatActionType {
    @WamEnumConstant(1) MUTE,
    @WamEnumConstant(2) UNMUTE,
    @WamEnumConstant(3) ARCHIVE,
    @WamEnumConstant(4) CLEAR,
    @WamEnumConstant(5) EXIT_GROUP,
    @WamEnumConstant(6) DELETE,
    @WamEnumConstant(7) PIN,
    @WamEnumConstant(8) UNREAD,
    @WamEnumConstant(9) READ,
    @WamEnumConstant(10) SUSPEND_CHAT_DELETE
}
