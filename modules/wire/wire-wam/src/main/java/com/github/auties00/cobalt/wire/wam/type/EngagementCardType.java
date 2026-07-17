package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEngagementCardType")
@WamEnum
public enum EngagementCardType {
    @WamEnumConstant(1) MID_CARD,
    @WamEnumConstant(2) END_CARD
}
