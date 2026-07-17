package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameLinkOriginSurface")
@WamEnum
public enum UsernameLinkOriginSurface {
    @WamEnumConstant(1) EDUCATION_UPSELL,
    @WamEnumConstant(2) LINK_ERROR_UPSELL
}
