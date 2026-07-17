package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuotedMessageUserJourneyEntryPoint")
@WamEnum
public enum QuotedMessageUserJourneyEntryPoint {
    @WamEnumConstant(1) CONTEXT_MENU_REPLY_BUTTON,
    @WamEnumConstant(2) SWIPED_TO_REPLY,
    @WamEnumConstant(3) AUTO,
    @WamEnumConstant(4) MESSAGE_DOUBLE_TAP,
    @WamEnumConstant(5) KEYBOARD_SHORTCUT,
    @WamEnumConstant(6) INTENT_BASED
}
