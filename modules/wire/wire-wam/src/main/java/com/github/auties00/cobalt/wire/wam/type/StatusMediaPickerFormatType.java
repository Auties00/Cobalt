package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusMediaPickerFormatType")
@WamEnum
public enum StatusMediaPickerFormatType {
    @WamEnumConstant(1) TEXT,
    @WamEnumConstant(2) VOICE,
    @WamEnumConstant(3) LAYOUTS,
    @WamEnumConstant(4) MUSIC,
    @WamEnumConstant(5) AI_IMAGINE,
    @WamEnumConstant(6) LOCATION
}
