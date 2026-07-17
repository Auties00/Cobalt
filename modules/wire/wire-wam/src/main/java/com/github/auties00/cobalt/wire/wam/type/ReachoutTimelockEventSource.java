package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReachoutTimelockEventSource")
@WamEnum
public enum ReachoutTimelockEventSource {
    @WamEnumConstant(1) BOTTOM_SHEET,
    @WamEnumConstant(2) SMB_APPROVED_TOOLS
}
