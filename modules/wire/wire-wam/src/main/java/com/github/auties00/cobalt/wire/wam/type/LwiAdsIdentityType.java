package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiAdsIdentityType")
@WamEnum
public enum LwiAdsIdentityType {
    @WamEnumConstant(1) PAGE,
    @WamEnumConstant(2) WHATSAPP
}
