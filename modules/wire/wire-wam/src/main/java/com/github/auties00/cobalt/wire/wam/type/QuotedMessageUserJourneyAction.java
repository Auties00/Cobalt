package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuotedMessageUserJourneyAction")
@WamEnum
public enum QuotedMessageUserJourneyAction {
    @WamEnumConstant(1) QUOTED_MESSAGE_ADDED,
    @WamEnumConstant(2) QUOTED_MESSAGE_DELETED,
    @WamEnumConstant(3) QUOTED_MESSAGE_TAPPED_IN_COMPOSER,
    @WamEnumConstant(4) QUOTED_MESSAGE_BUBBLE_TAPPED,
    @WamEnumConstant(5) QUOTED_MESSAGE_SENT,
    @WamEnumConstant(6) QUOTED_MESSAGE_UPDATED
}
