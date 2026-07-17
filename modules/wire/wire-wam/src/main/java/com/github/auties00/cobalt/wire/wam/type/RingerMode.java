package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumRingerMode")
@WamEnum
public enum RingerMode {
    @WamEnumConstant(0) SILENT,
    @WamEnumConstant(1) VIBRATE,
    @WamEnumConstant(2) NORMAL
}
