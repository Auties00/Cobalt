package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusCategory")
@WamEnum
public enum StatusCategory {
    @WamEnumConstant(1) REGULAR_STATUS,
    @WamEnumConstant(2) GROUP_STATUS,
    @WamEnumConstant(3) CHANNEL_STATUS,
    @WamEnumConstant(4) ENGAGEMENT_CARD
}
