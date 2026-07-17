package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEngagementCardVariant")
@WamEnum
public enum EngagementCardVariant {
    @WamEnumConstant(1) MIMICRY,
    @WamEnumConstant(2) MUSIC,
    @WamEnumConstant(3) CREATIVE_TOOLS,
    @WamEnumConstant(4) CONTINUE_CHAIN,
    @WamEnumConstant(5) MIMICRY_UPDATES_TAB,
    @WamEnumConstant(6) AD4AD_BOOST_POPULAR_ALL_STATUSES,
    @WamEnumConstant(7) CHANNEL_PROMO
}
