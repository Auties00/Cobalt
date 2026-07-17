package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReportToAdminInteraction")
@WamEnum
public enum ReportToAdminInteraction {
    @WamEnumConstant(0) CLICK_OPEN_ADMIN_DASHBOARD,
    @WamEnumConstant(1) CLICK_SEND_FOR_ADMIN_REVIEW,
    @WamEnumConstant(2) CLICK_CONFIRM_SEND_FOR_ADMIN_REVIEW,
    @WamEnumConstant(3) CLICK_CANCEL_SEND_FOR_ADMIN_REVIEW
}
