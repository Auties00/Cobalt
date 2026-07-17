package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEnforcementInteractionEvent")
@WamEnum
public enum EnforcementInteractionEventType {
    @WamEnumConstant(0) CLICK_CHANNEL_ALERTS,
    @WamEnumConstant(1) CLICK_ENFORCEMENT_DETAIL,
    @WamEnumConstant(2) CLICK_CHANNEL_GUIDELINES,
    @WamEnumConstant(3) CLICK_LEARN_MORE_HOW,
    @WamEnumConstant(4) CLICK_LEARN_MORE_WHY,
    @WamEnumConstant(5) CLICK_LEARN_MORE_EU,
    @WamEnumConstant(6) CLICK_SEE_RULE,
    @WamEnumConstant(7) CLICK_SEE_OPTIONS,
    @WamEnumConstant(8) CLICK_REQUEST_REVIEW,
    @WamEnumConstant(9) CLICK_SUBMIT_REQUEST_REVIEW,
    @WamEnumConstant(10) CLICK_SUBMIT_APPEAL,
    @WamEnumConstant(11) CLICK_SEE_REVIEW_DETAILS,
    @WamEnumConstant(12) CLICK_DELETE_FROM_CHANNEL,
    @WamEnumConstant(13) CLICK_DELETE_UPDATE_CONFIRM,
    @WamEnumConstant(14) CLICK_DELETE_CHANNEL,
    @WamEnumConstant(15) CLICK_DELETE_CHANNEL_CONFIRM,
    @WamEnumConstant(16) CLICK_REFER_DSB,
    @WamEnumConstant(17) CLICK_GET_REFERENCE_NUMBER,
    @WamEnumConstant(18) CLICK_COPY_REFERENCE_NUMBER,
    @WamEnumConstant(19) CLICK_CHANNEL_DELETE_PHONE_NUMBER_CONFIRM,
    @WamEnumConstant(20) CLICK_NEXT,
    @WamEnumConstant(21) CLICK_BACK,
    @WamEnumConstant(22) CLICK_DISMISS_BOTTOM_SHEET,
    @WamEnumConstant(23) CLICK_DISMISS_DRAWER,
    @WamEnumConstant(24) CLICK_EMAIL_REPORTER,
    @WamEnumConstant(25) CLICK_GET_REPORT_DETAILS,
    @WamEnumConstant(26) CLICK_COPY_REPORT_NUMBER,
    @WamEnumConstant(27) CLICK_COPY_REPORTER_NAME,
    @WamEnumConstant(28) CLICK_COPY_REPORTER_EMAIL,
    @WamEnumConstant(29) CLICK_GO_TO_HELP_CENTRE,
    @WamEnumConstant(30) CLICK_ALERT_BANNER_DISMISS,
    @WamEnumConstant(31) CLICK_SEE_ENFORCEMENT_POLICY_DETAIL
}
