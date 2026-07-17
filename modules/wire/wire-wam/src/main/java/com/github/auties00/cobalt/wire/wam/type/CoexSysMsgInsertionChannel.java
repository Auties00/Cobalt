package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCoexSysMsgInsertionChannel")
@WamEnum
public enum CoexSysMsgInsertionChannel {
    @WamEnumConstant(0) CHAT_OPEN,
    @WamEnumConstant(1) MESSAGE_RECEIVE,
    @WamEnumConstant(2) HISTORY_SYNC
}
