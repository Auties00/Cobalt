package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMediaAutoDownloadQuality")
@WamEnum
public enum MediaAutoDownloadQuality {
    @WamEnumConstant(0) AUTO,
    @WamEnumConstant(1) SD_QUALITY,
    @WamEnumConstant(2) HD_QUALITY
}
