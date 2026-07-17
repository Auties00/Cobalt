package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiSurface")
@WamEnum
public enum LwiSurface {
    @WamEnumConstant(1) AD_DESIGN,
    @WamEnumConstant(2) FB_WEB_SSO,
    @WamEnumConstant(3) LOGIN_ACCOUNT_SELECTION,
    @WamEnumConstant(4) RESOLVE_PAGE_PERMISSION,
    @WamEnumConstant(5) ACCOUNT_MANAGEMENT
}
