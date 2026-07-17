package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageSendSource")
@WamEnum
public enum MessageSendSource {
    @WamEnumConstant(1) NONE,
    @WamEnumConstant(2) UNANSWERED_CALL_UPSELL,
    @WamEnumConstant(3) UNANSWERED_CALL_UPSELL_REST,
    @WamEnumConstant(4) VIEW_ALL_REPLIES
}
