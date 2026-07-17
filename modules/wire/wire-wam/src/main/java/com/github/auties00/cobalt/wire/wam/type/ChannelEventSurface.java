package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelEventSurface")
@WamEnum
public enum ChannelEventSurface {
    @WamEnumConstant(1) CHANNEL_UPDATES_HOME,
    @WamEnumConstant(2) CHANNEL_THREAD,
    @WamEnumConstant(3) CHANNEL_DIRECTORY,
    @WamEnumConstant(4) CHANNEL_DIRECTORY_SEARCH,
    @WamEnumConstant(5) CHANNEL_PROFILE,
    @WamEnumConstant(6) CHANNEL_UPDATES_HOME_SEARCH,
    @WamEnumConstant(7) CHANNEL_DIRECTORY_CATEGORIES,
    @WamEnumConstant(8) CHANNEL_DIRECTORY_CATEGORIES_SEARCH
}
