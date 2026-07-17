package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumExportModeType")
@WamEnum
public enum ExportModeType {
    @WamEnumConstant(1) TEXT_ONLY,
    @WamEnumConstant(2) WITH_MEDIA
}
