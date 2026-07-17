package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelEventUnit")
@WamEnum
public enum ChannelEventUnit {
    @WamEnumConstant(1) RECOMMENDED_CHANNELS,
    @WamEnumConstant(2) SIMILAR_CHANNELS,
    @WamEnumConstant(3) RECENT_SEARCHES
}
