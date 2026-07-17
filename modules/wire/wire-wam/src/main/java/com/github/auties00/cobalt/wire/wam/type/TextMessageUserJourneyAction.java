package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumTextMessageUserJourneyAction")
@WamEnum
public enum TextMessageUserJourneyAction {
    @WamEnumConstant(1) TYPING_START,
    @WamEnumConstant(2) CLEARED,
    @WamEnumConstant(3) SENT,
    @WamEnumConstant(4) DRAFT_SAVED,
    @WamEnumConstant(5) EXIT,
    @WamEnumConstant(6) CLICK_ON_COMPOSER,
    @WamEnumConstant(7) DELIVERED,
    @WamEnumConstant(8) DRAFT_LOADED
}
