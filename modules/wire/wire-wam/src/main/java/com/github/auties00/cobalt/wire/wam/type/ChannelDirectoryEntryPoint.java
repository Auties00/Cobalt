package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelDirectoryEntryPoint")
@WamEnum
public enum ChannelDirectoryEntryPoint {
    @WamEnumConstant(1) SUGGESTED_CHANNELS,
    @WamEnumConstant(2) FIND_CHANNELS_MENU,
    @WamEnumConstant(3) DEEP_LINK,
    @WamEnumConstant(4) WAITLIST_NOTIFICATION,
    @WamEnumConstant(5) NUX_BANNER,
    @WamEnumConstant(6) FIND_CHANNELS_BUTTON,
    @WamEnumConstant(7) UPDATES_TAB_SEARCH,
    @WamEnumConstant(8) EXPLORE_SECTION_HEADER_BUTTON,
    @WamEnumConstant(9) UPDATES_TAB_SEARCH_NULL_STATE
}
