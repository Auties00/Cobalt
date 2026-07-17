package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReceiptAggregationType")
@WamEnum
public enum ReceiptAggregationType {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) MULTI_MESSAGES,
    @WamEnumConstant(2) MULTI_PARTICIPANTS,
    @WamEnumConstant(3) AGGREGATE_BY_ID
}
