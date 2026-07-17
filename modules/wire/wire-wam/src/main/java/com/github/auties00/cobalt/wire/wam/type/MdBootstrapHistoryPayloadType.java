package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdBootstrapHistoryPayloadType")
@WamEnum
public enum MdBootstrapHistoryPayloadType {
    @WamEnumConstant(1) INITIAL,
    @WamEnumConstant(2) RECENT_HISTORY,
    @WamEnumConstant(3) FULL_HISTORY,
    @WamEnumConstant(4) PUSHNAME,
    @WamEnumConstant(5) STATUS_V3,
    @WamEnumConstant(6) NON_BLOCKING_DATA,
    @WamEnumConstant(7) ON_DEMAND
}
