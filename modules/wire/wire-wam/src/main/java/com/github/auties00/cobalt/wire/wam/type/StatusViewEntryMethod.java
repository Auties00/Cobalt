package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusViewEntryMethod")
@WamEnum
public enum StatusViewEntryMethod {
    @WamEnumConstant(1) FORWARD_TAP,
    @WamEnumConstant(2) BACK_TAP,
    @WamEnumConstant(3) FORWARD_SWIPE,
    @WamEnumConstant(4) BACK_SWIPE,
    @WamEnumConstant(5) DIRECT_POG_TAP,
    @WamEnumConstant(6) AUTO_NAVIGATE_TIMER_END,
    @WamEnumConstant(7) STATUS_WIDGET,
    @WamEnumConstant(8) CHAINING_PILL_TAP,
    @WamEnumConstant(9) ENGAGEMENT_CARD_CTA_TAP,
    @WamEnumConstant(10) MY_STATUSES_SCREEN
}
