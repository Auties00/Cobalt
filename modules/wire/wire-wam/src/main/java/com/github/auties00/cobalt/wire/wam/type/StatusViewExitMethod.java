package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusViewExitMethod")
@WamEnum
public enum StatusViewExitMethod {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) SWIPE_DOWN,
    @WamEnumConstant(2) BACK_ARROW_TAP,
    @WamEnumConstant(3) BACK_BUTTON_TAP,
    @WamEnumConstant(4) STATUS_TIMEOUT,
    @WamEnumConstant(5) STATUS_DISMISSED,
    @WamEnumConstant(6) BACKWARD_SWIPE,
    @WamEnumConstant(7) FORWARD_SWIPE,
    @WamEnumConstant(8) BACKWARD_TAP,
    @WamEnumConstant(9) FORWARD_TAP,
    @WamEnumConstant(10) FORWARD_TAP_AUTO_CLOSE,
    @WamEnumConstant(11) STATUS_TIMEOUT_AUTO_CLOSE,
    @WamEnumConstant(12) FORWARD_SWIPE_AUTO_CLOSE,
    @WamEnumConstant(13) BACKWARD_SWIPE_AUTO_CLOSE,
    @WamEnumConstant(14) CHAINING_PILL_TAP,
    @WamEnumConstant(15) CHAINING_PILL_AUTO_CLOSE,
    @WamEnumConstant(16) ENGAGEMENT_CARD_CTA_TAP
}
