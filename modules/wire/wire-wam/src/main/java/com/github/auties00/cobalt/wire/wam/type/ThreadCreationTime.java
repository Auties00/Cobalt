package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumThreadCreationTime")
@WamEnum
public enum ThreadCreationTime {
    @WamEnumConstant(0) LESS_THAN_1_DAY_AGO,
    @WamEnumConstant(1) LESS_THAN_7_DAYS_AGO,
    @WamEnumConstant(2) LESS_THAN_30_DAYS_AGO,
    @WamEnumConstant(3) MORE_THAN_30_DAYS_AGO
}
