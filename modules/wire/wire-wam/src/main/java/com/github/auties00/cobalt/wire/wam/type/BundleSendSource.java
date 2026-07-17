package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBundleSendSource")
@WamEnum
public enum BundleSendSource {
    @WamEnumConstant(1) NOTIFICATION,
    @WamEnumConstant(2) IQ_RESPONSE,
    @WamEnumConstant(3) SYSTEM_MESSAGE,
    @WamEnumConstant(4) CONTACT_CARD
}
