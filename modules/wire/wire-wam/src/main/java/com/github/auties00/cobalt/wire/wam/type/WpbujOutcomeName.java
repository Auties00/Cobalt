package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWpbujOutcomeName")
@WamEnum
public enum WpbujOutcomeName {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) FAILED,
    @WamEnumConstant(3) FAILED_NO_INTERNET_CONNECTION
}
