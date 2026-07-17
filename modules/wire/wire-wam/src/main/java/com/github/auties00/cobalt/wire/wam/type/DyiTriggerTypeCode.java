package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDyiTriggerTypeCode")
@WamEnum
public enum DyiTriggerTypeCode {
    @WamEnumConstant(1) ADHOC,
    @WamEnumConstant(2) SCHEDULED
}
