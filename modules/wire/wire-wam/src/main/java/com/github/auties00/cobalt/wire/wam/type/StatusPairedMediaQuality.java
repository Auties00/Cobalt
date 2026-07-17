package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusPairedMediaQuality")
@WamEnum
public enum StatusPairedMediaQuality {
    @WamEnumConstant(0) SD,
    @WamEnumConstant(1) HD
}
