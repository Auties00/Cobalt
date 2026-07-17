package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumConsumerBizEntryPointEnum")
@WamEnum
public enum ConsumerBizEntryPointEnum {
    @WamEnumConstant(0) CHAT_LIST,
    @WamEnumConstant(1) CHAT_THREAD,
    @WamEnumConstant(2) CONTACT_INFO,
    @WamEnumConstant(3) SEARCH_BAR,
    @WamEnumConstant(4) SHARE_SHEET,
    @WamEnumConstant(5) NEW_CHAT
}
