package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupSuspensionAppealUiAction")
@WamEnum
public enum GroupSuspensionAppealUiAction {
    @WamEnumConstant(1) CONTACT_SUPPORT_CLICK,
    @WamEnumConstant(2) DELETE_GROUP_CLICK,
    @WamEnumConstant(3) SEE_GROUP_CLICK,
    @WamEnumConstant(4) DEACTIVATE_COMMUNITY,
    @WamEnumConstant(5) LEAVE_COMMUNITY_CLICK,
    @WamEnumConstant(6) SUSPENSION_BOTTOM_SHEET_IMPRESSION,
    @WamEnumConstant(7) REQUEST_REVIEW_CLICK,
    @WamEnumConstant(8) OK_CLICK,
    @WamEnumConstant(9) SEE_DETAILS_CLICK,
    @WamEnumConstant(10) NOTIFICATION_CLICK,
    @WamEnumConstant(11) IN_REVIEW_BOTTOM_SHEET_IMPRESSION,
    @WamEnumConstant(12) ACCEPT_BOTTOM_SHEET_IMPRESSION,
    @WamEnumConstant(13) REJECT_BOTTOM_SHEET_IMPRESSION,
    @WamEnumConstant(14) REQUEST_REVIEW_SUBMIT_SUCCESS,
    @WamEnumConstant(15) REQUEST_REVIEW_SUBMIT_FAILURE,
    @WamEnumConstant(16) FOOTER_IMPRESSION,
    @WamEnumConstant(17) NOTIFICATION_IMPRESSION
}
