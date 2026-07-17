package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMerchantTypeType")
@WamEnum
public enum MerchantTypeType {
    @WamEnumConstant(1) API,
    @WamEnumConstant(2) SMB
}
