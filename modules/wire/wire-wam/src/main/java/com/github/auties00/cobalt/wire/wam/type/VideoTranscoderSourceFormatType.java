package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVideoTranscoderSourceFormatType")
@WamEnum
public enum VideoTranscoderSourceFormatType {
    @WamEnumConstant(0) SLOMO,
    @WamEnumConstant(1) VIDEO,
    @WamEnumConstant(2) GIF
}
