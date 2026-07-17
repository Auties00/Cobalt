package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBugReportEntryPointName")
@WamEnum
public enum BugReportEntryPointName {
    @WamEnumConstant(1) BUG_REPORT_ENTRY_POINT_SETTINGS,
    @WamEnumConstant(2) BUG_REPORT_ENTRY_POINT_HELP,
    @WamEnumConstant(3) BUG_REPORT_ENTRY_POINT_RAGE_SHAKE,
    @WamEnumConstant(4) BUG_REPORT_ENTRY_POINT_REPORT_MESSAGE,
    @WamEnumConstant(5) BUG_REPORT_ENTRY_POINT_VOIP_BUGNUB,
    @WamEnumConstant(6) BUG_REPORT_ENTRY_POINT_VOIP_CALL_MENU,
    @WamEnumConstant(7) BUG_REPORT_ENTRY_POINT_BLOKS,
    @WamEnumConstant(8) BUG_REPORT_ENTRY_POINT_SIDEBAR_BUGNUB
}
