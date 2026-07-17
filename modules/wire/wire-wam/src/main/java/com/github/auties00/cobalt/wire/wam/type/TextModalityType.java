package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumTextModalityType")
@WamEnum
public enum TextModalityType {
    @WamEnumConstant(1) TEXT,
    @WamEnumConstant(2) PTT
}
