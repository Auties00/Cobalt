package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumInteractionSurface")
@WamEnum
public enum InteractionSurface {
    @WamEnumConstant(0) NEWSLETTER_INFO_DRAWER,
    @WamEnumConstant(1) NEWSLETTER_CONVERSATION_SCREEN,
    @WamEnumConstant(2) ENFORCEMENT_ALERT_LIST,
    @WamEnumConstant(3) ENFORCEMENT_DETAIL_SCREEN,
    @WamEnumConstant(4) REMEDIATION_OPTION_SCREEN,
    @WamEnumConstant(5) REQUEST_REVIEW_DESCRIPTION_SCREEN,
    @WamEnumConstant(6) REQUEST_REVIEW_REASON_SCREEN,
    @WamEnumConstant(7) REQUEST_REVIEW_TEXT_REASON_SCREEN,
    @WamEnumConstant(8) SEE_REVIEW_DETAILS_SCREEN,
    @WamEnumConstant(9) DSB_FLOW_SCREEN_1,
    @WamEnumConstant(10) DSB_FLOW_SCREEN_2,
    @WamEnumConstant(11) DSB_FLOW_SCREEN_3,
    @WamEnumConstant(12) DELETE_CHANNEL_MSG_CONFIRMATION_SCREEN,
    @WamEnumConstant(13) DELETE_CHANNEL_CONFIRMATION_SCREEN,
    @WamEnumConstant(14) DELETE_CHANNEL_PHONE_NUMBER_CONFIRM,
    @WamEnumConstant(15) CONTACT_REPORTER_SCREEN_1,
    @WamEnumConstant(16) CONTACT_REPORTER_SCREEN_2,
    @WamEnumConstant(17) ENFORCEMENT_POLICY_DETAILS_BOTTOM_SHEET
}
