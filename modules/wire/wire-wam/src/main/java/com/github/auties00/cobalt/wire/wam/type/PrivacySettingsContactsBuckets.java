package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPrivacySettingsContactsBuckets")
@WamEnum
public enum PrivacySettingsContactsBuckets {
    @WamEnumConstant(1) B0,
    @WamEnumConstant(2) B1,
    @WamEnumConstant(3) B5,
    @WamEnumConstant(4) B10,
    @WamEnumConstant(5) B15,
    @WamEnumConstant(6) B20,
    @WamEnumConstant(7) B30,
    @WamEnumConstant(8) B40,
    @WamEnumConstant(9) B50,
    @WamEnumConstant(10) B60,
    @WamEnumConstant(11) B70,
    @WamEnumConstant(12) B80,
    @WamEnumConstant(13) B90,
    @WamEnumConstant(14) B100
}
