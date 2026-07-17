package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdBootstrapPayloadType")
@WamEnum
public enum MdBootstrapPayloadType {
    @WamEnumConstant(1) CRITICAL,
    @WamEnumConstant(2) NON_CRITICAL
}
