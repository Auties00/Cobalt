package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupStatusSizeBucket")
@WamEnum
public enum GroupStatusSizeBucket {
    @WamEnumConstant(1) EMPTY_GROUP,
    @WamEnumConstant(2) X_SMALL,
    @WamEnumConstant(3) SMALL,
    @WamEnumConstant(4) MEDIUM,
    @WamEnumConstant(5) LARGE
}
