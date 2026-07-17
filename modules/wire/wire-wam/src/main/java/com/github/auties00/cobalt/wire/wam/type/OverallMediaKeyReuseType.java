package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOverallMediaKeyReuseType")
@WamEnum
public enum OverallMediaKeyReuseType {
    @WamEnumConstant(1) NONE_NEW_CONTENT,
    @WamEnumConstant(2) NONE_EXPIRED,
    @WamEnumConstant(3) REUSED,
    @WamEnumConstant(4) NONE_WAS_STATUS
}
