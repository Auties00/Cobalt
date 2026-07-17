package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelProducerInsightsSurface")
@WamEnum
public enum ChannelProducerInsightsSurface {
    @WamEnumConstant(0) CHANNEL_INFO,
    @WamEnumConstant(1) REACH_TAB,
    @WamEnumConstant(2) GROWTH_TAB,
    @WamEnumConstant(3) FOLLOWERS_TAB
}
