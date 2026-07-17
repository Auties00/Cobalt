package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOppositeVisibleIdentificationType")
@WamEnum
public enum OppositeVisibleIdentificationType {
    @WamEnumConstant(1) PHONE_NUMBER,
    @WamEnumConstant(2) SAVED_CONTACT_NAME,
    @WamEnumConstant(3) USERNAME,
    @WamEnumConstant(4) MASKED_PHONE_NUMBER,
    @WamEnumConstant(5) VERIFIED_BUSINESS_NAME,
    @WamEnumConstant(6) PLACEHOLDER,
    @WamEnumConstant(7) PUSHNAME
}
