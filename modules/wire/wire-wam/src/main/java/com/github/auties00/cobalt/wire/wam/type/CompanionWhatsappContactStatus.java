package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCompanionWhatsappContactStatus")
@WamEnum
public enum CompanionWhatsappContactStatus {
    @WamEnumConstant(0) IN_NETWORK,
    @WamEnumConstant(1) OUT_OF_NETWORK
}
