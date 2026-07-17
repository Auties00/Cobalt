package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameSource")
@WamEnum
public enum UsernameSource {
    @WamEnumConstant(1) FB,
    @WamEnumConstant(2) IG,
    @WamEnumConstant(3) USER_INPUT,
    @WamEnumConstant(4) SUGGESTED_USERNAME,
    @WamEnumConstant(5) RECOMMENDATION_LIST
}
