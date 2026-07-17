package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCtwaBizUserJourneyOperation")
@WamEnum
public enum CtwaBizUserJourneyOperation {
    @WamEnumConstant(1) AGM_INJECTED,
    @WamEnumConstant(2) AGM_CTA_CLICKED,
    @WamEnumConstant(3) AGM_VIEW_DETAILS_CLICKED,
    @WamEnumConstant(4) AGM_BOTTOMSHEET_SHOWN,
    @WamEnumConstant(5) AGM_BOTTOMSHEET_CLOSED,
    @WamEnumConstant(6) AGM_BOTTOMSHEET_EXTERNAL_NAVIGATION,
    @WamEnumConstant(7) DUPLICATED_AGM_NOT_INJECTED,
    @WamEnumConstant(8) AGM_WELCOME_MESSAGE_NULL,
    @WamEnumConstant(9) AGM_INVALID_CTA_TYPE_FROM_MESSAGE,
    @WamEnumConstant(10) AGM_INVALID_CTA_PAYLOAD_FROM_MESSAGE,
    @WamEnumConstant(11) AGM_INVALID_SOURCE_APP_FROM_MESSAGE,
    @WamEnumConstant(12) AGM_BOTTOMSHEET_IMAGE_ERROR,
    @WamEnumConstant(13) AGM_BOTTOMSHEET_TEXT_ERROR,
    @WamEnumConstant(14) AD_PREVIEW_DEEPLINK_V1_CLICKED,
    @WamEnumConstant(15) AD_PREVIEW_FETCH_FROM_DEEPLINK_V2_STARTED,
    @WamEnumConstant(16) AD_PREVIEW_FETCH_FROM_DEEPLINK_V2_SUCCESS,
    @WamEnumConstant(17) AD_PREVIEW_FETCH_FROM_DEEPLINK_V2_FAILED,
    @WamEnumConstant(18) AD_PREVIEW_OPEN,
    @WamEnumConstant(19) AD_PREVIEW_CLOSE,
    @WamEnumConstant(20) AD_PREVIEW_RENDER_FAILED
}
