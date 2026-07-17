package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBusinessType")
@WamEnum
public enum BusinessType {
    @WamEnumConstant(1) SMB,
    @WamEnumConstant(2) API_DC,
    @WamEnumConstant(3) API
}
