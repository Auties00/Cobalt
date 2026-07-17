package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeeplinkSource")
@WamEnum
public enum DeeplinkSource {
    @WamEnumConstant(0) INSTAGRAM_STORIES,
    @WamEnumConstant(1) INSTAGRAM_STATUS_RESHARE,
    @WamEnumConstant(2) INSTAGRAM_PROFILE,
    @WamEnumConstant(3) INSTAGRAM_ADS,
    @WamEnumConstant(4) INSTAGRAM_QP,
    @WamEnumConstant(5) INSTAGRAM_DM
}
