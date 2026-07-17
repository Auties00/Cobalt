package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumE2eDeviceType")
@WamEnum
public enum E2eDeviceType {
    @WamEnumConstant(1) MY_PRIMARY,
    @WamEnumConstant(2) OTHER_PRIMARY,
    @WamEnumConstant(3) MY_COMPANION,
    @WamEnumConstant(4) OTHER_COMPANION,
    @WamEnumConstant(5) MY_HOSTED_COMPANION,
    @WamEnumConstant(6) OTHER_HOSTED_COMPANION,
    @WamEnumConstant(7) MY_COEX_V2,
    @WamEnumConstant(8) OTHER_COEX_V2
}
