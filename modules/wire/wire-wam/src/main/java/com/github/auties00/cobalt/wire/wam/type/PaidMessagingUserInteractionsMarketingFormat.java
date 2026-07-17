package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaidMessagingUserInteractionsMarketingFormat")
@WamEnum
public enum PaidMessagingUserInteractionsMarketingFormat {
    @WamEnumConstant(0) CAROUSEL,
    @WamEnumConstant(1) MPM,
    @WamEnumConstant(2) CUSTOM,
    @WamEnumConstant(3) ALBUM
}
