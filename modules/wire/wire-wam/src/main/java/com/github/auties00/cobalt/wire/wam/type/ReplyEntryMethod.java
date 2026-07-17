package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReplyEntryMethod")
@WamEnum
public enum ReplyEntryMethod {
    @WamEnumConstant(1) SWIPE_UP,
    @WamEnumConstant(2) TAP_REPLY_BAR
}
