package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignupUserJourneyOperation")
@WamEnum
public enum SignupUserJourneyOperation {
    @WamEnumConstant(0) DEEP_LINK_PARSED,
    @WamEnumConstant(1) APP_UPDATE_REQUIRED_SHOWN,
    @WamEnumConstant(2) APP_UPDATE_DISMISSED,
    @WamEnumConstant(3) APP_UPDATE_UPDATE_CLICKED,
    @WamEnumConstant(4) LAND_ON_CHAT_THREAD,
    @WamEnumConstant(5) GET_METADATA_CALL_INITIATED,
    @WamEnumConstant(6) GET_METADATA_CALL_SUCCESS,
    @WamEnumConstant(7) GET_METADATA_CALL_FAILURE,
    @WamEnumConstant(8) AGM_SPINNER_SHOWN,
    @WamEnumConstant(9) AGM_INJECTED,
    @WamEnumConstant(10) AGM_CANCELLED_USER_LEFT,
    @WamEnumConstant(11) AGM_CTA_CLICKED,
    @WamEnumConstant(12) AGM_VIEW_DETAILS_CLICKED,
    @WamEnumConstant(13) AGM_BOTTOMSHEET_SHOWN,
    @WamEnumConstant(14) AGM_BOTTOMSHEET_CLOSED,
    @WamEnumConstant(15) AGM_BOTTOMSHEET_EXTERNAL_NAVIGATION,
    @WamEnumConstant(16) SIGNUP_REQUEST_SENT,
    @WamEnumConstant(17) WEBHOOK_REQUEST_SENT,
    @WamEnumConstant(18) WEBHOOK_REQUEST_SUCCESS,
    @WamEnumConstant(19) WEBHOOK_REQUEST_FAILURE,
    @WamEnumConstant(20) SIGNUP_CONFIRMATION_RECEIVED,
    @WamEnumConstant(21) SIGNUP_CONFIRMATION_CTA_CLICKED,
    @WamEnumConstant(22) SIGNUP_CONFIRMATION_VIEW_DETAILS_CLICKED,
    @WamEnumConstant(23) SIGNUP_CONFIRMATION_BOTTOMSHEET_SHOWN,
    @WamEnumConstant(24) SIGNUP_CONFIRMATION_BOTTOMSHEET_EXTERNAL_NAVIGATION,
    @WamEnumConstant(25) SIGNUP_CONFIRMATION_BOTTOMSHEET_CLOSED,
    @WamEnumConstant(26) SIGNUP_CONFIRMATION_WEBSITE_CTA_CLICKED
}
