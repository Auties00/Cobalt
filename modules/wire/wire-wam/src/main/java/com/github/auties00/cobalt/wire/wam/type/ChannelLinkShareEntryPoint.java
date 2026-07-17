package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelLinkShareEntryPoint")
@WamEnum
public enum ChannelLinkShareEntryPoint {
    @WamEnumConstant(1) CHANNEL_INFO_PAGE,
    @WamEnumConstant(2) CHANNEL_THREAD,
    @WamEnumConstant(3) PRODUCER_CONTEXT_CARD,
    @WamEnumConstant(4) UPDATES_TAB,
    @WamEnumConstant(5) SHARE_LINK_SCREEN,
    @WamEnumConstant(6) CHANNEL_ADMIN_ONBOARDING
}
