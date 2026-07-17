package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReshareSource")
@WamEnum
public enum ReshareSource {
    @WamEnumConstant(1) MENTIONS_RESHARE,
    @WamEnumConstant(2) STATUS_RESHARE,
    @WamEnumConstant(3) FORWARDED_FROM_STATUS
}
