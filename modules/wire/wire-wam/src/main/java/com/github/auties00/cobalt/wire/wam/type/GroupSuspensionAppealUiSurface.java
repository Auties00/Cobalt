package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupSuspensionAppealUiSurface")
@WamEnum
public enum GroupSuspensionAppealUiSurface {
    @WamEnumConstant(1) GROUP_SUSPENSION_BOTTOM_SHEET,
    @WamEnumConstant(2) COMMUNITY_SUSPENSION_BOTTOM_SHEET,
    @WamEnumConstant(3) SUSPENDED_FOOTER,
    @WamEnumConstant(4) IN_REVIEW_FOOTER,
    @WamEnumConstant(5) REJECT_FOOTER,
    @WamEnumConstant(6) IN_REVIEW_BOTTOM_SHEET,
    @WamEnumConstant(7) ACCEPT_BOTTOM_SHEET,
    @WamEnumConstant(8) REJECT_BOTTOM_SHEET,
    @WamEnumConstant(9) SUSPENDED_NOTIFICATION,
    @WamEnumConstant(10) ACCEPT_NOTIFICATION,
    @WamEnumConstant(11) REJECT_NOTIFICATION,
    @WamEnumConstant(12) REQUEST_REVIEW_ERROR_DIALOG
}
