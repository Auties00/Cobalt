package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumConsumerBizSurfaceEnum")
@WamEnum
public enum ConsumerBizSurfaceEnum {
    @WamEnumConstant(0) SEARCH_RESULTS,
    @WamEnumConstant(1) BUSINESS_PROFILE,
    @WamEnumConstant(2) CONTACT_CARD,
    @WamEnumConstant(3) SHARE_DIALOG,
    @WamEnumConstant(4) CHAT_HEADER,
    @WamEnumConstant(5) FMX_CARD,
    @WamEnumConstant(6) BLOCKED_COMPOSER,
    @WamEnumConstant(7) OFFBOARDED_BOTTOM_SHEET,
    @WamEnumConstant(8) CHAT_THREAD
}
