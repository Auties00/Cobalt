package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReachoutTimelockAction")
@WamEnum
public enum ReachoutTimelockAction {
    @WamEnumConstant(1) IMPRESSION,
    @WamEnumConstant(2) SMB_MM_BB_OPTION_IMPRESSION,
    @WamEnumConstant(3) CLICK_BUSINESS_TOOLS
}
