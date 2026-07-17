package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageDistributionEnumType")
@WamEnum
public enum MessageDistributionEnumType {
    @WamEnumConstant(0) REGULAR_MESSAGE,
    @WamEnumConstant(1) DIRECT_MESSAGE,
    @WamEnumConstant(2) SENDER_KEY_DISTRIBUTION_MESSAGE
}
