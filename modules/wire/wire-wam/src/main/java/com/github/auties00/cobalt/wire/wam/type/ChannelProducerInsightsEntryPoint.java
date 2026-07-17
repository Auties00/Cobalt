package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelProducerInsightsEntryPoint")
@WamEnum
public enum ChannelProducerInsightsEntryPoint {
    @WamEnumConstant(0) PROFILE_SEE_ALL,
    @WamEnumConstant(1) PROFILE_ACCOUNTS_REACHED,
    @WamEnumConstant(2) PROFILE_NET_FOLLOWS
}
