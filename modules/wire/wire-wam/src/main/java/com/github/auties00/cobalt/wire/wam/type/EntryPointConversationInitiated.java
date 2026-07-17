package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEntryPointConversationInitiated")
@WamEnum
public enum EntryPointConversationInitiated {
    @WamEnumConstant(0) BUSINESS_INITIATED,
    @WamEnumConstant(1) CONSUMER_INITIATED,
    @WamEnumConstant(2) NO_MESSAGES_LAST_24H
}
