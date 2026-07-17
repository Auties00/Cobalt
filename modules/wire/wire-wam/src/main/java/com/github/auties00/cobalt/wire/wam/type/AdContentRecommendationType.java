package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAdContentRecommendationType")
@WamEnum
public enum AdContentRecommendationType {
    @WamEnumConstant(1) REGULAR,
    @WamEnumConstant(2) RECOMMENDED
}
