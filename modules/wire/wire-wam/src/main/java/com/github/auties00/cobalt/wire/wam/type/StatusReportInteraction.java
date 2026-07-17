package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusReportInteraction")
@WamEnum
public enum StatusReportInteraction {
    @WamEnumConstant(0) CLICK_REPORT,
    @WamEnumConstant(1) CLICK_SUBMIT_REPORT,
    @WamEnumConstant(2) CLICK_CANCEL_REPORT,
    @WamEnumConstant(3) CLICK_SUBMIT_REPORT_BLOCK
}
