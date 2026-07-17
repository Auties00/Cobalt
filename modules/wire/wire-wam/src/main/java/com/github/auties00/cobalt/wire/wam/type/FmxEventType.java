package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumFmxEvent")
@WamEnum
public enum FmxEventType {
    @WamEnumConstant(0) BLOCK,
    @WamEnumConstant(1) ADD_CONTACT,
    @WamEnumConstant(2) REPORT,
    @WamEnumConstant(3) SAFETY_TOOLS,
    @WamEnumConstant(4) COMMON_GROUPS,
    @WamEnumConstant(5) CONTACT_INFO,
    @WamEnumConstant(6) FMX_CARD_INSERTED,
    @WamEnumConstant(7) FMX_CARD_VIEWED,
    @WamEnumConstant(8) LEARN_MORE,
    @WamEnumConstant(9) HIGHLIGHT_GROUP_NAME,
    @WamEnumConstant(10) FMX_CARD_TRUST_SIGNALS_FB_IG_VIEWED,
    @WamEnumConstant(11) STOP_MARKETING_MSG_OFFERS,
    @WamEnumConstant(12) MESSAGE_PREFERENCES,
    @WamEnumConstant(13) FMX_CARD_FIRST_VIEW_IN_CHAT_OPEN,
    @WamEnumConstant(14) TRUST_QUESTION_TAP,
    @WamEnumConstant(15) TRUST_BTN_TAPPED
}
