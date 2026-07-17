package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBannerOperations")
@WamEnum
public enum BannerOperations {
    @WamEnumConstant(1) SHOWN,
    @WamEnumConstant(2) CLICK,
    @WamEnumConstant(3) DISMISS,
    @WamEnumConstant(4) ELIGIBLE,
    @WamEnumConstant(5) REVOKED,
    @WamEnumConstant(6) RENDERED
}
