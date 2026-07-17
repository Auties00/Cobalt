package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuotedMessageUserJourneyNavigateResult")
@WamEnum
public enum QuotedMessageUserJourneyNavigateResult {
    @WamEnumConstant(1) NAVIGATE_SUCCESS_SAME_CHAT,
    @WamEnumConstant(2) NAVIGATE_SUCCESS_DIFFERENT_CHAT,
    @WamEnumConstant(3) NAVIGATE_FAILURE_MISSING_MESSAGE,
    @WamEnumConstant(4) NAVIGATE_FAILURE_OTHER
}
