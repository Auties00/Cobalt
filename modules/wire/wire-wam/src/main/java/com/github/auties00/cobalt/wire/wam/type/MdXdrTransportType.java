package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdXdrTransportType")
@WamEnum
public enum MdXdrTransportType {
    @WamEnumConstant(1) WNS,
    @WamEnumConstant(2) SDK
}
