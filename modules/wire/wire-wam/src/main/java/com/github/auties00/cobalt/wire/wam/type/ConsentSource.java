package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumConsentSource")
@WamEnum
public enum ConsentSource {
    @WamEnumConstant(0) DISCLOSURE,
    @WamEnumConstant(1) ACCOUNT_LINKING
}
