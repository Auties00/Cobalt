package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMediaQuality")
@WamEnum
public enum MediaQuality {
    @WamEnumConstant(0) AUTO,
    @WamEnumConstant(1) DATA_SAVER,
    @WamEnumConstant(2) HIGH_QUALITY,
    @WamEnumConstant(3) HIGHEST_QUALITY
}
