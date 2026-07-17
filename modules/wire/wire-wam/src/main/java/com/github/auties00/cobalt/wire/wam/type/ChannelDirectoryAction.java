package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelDirectoryAction")
@WamEnum
public enum ChannelDirectoryAction {
    @WamEnumConstant(1) DIRECTORY_OPEN_TAP,
    @WamEnumConstant(2) DIRECTORY_CLOSED_TAP,
    @WamEnumConstant(3) DIRECTORY_SEARCH_TAP,
    @WamEnumConstant(4) DIRECTORY_SERVER_ERROR,
    @WamEnumConstant(5) DIRECTORY_NAVIGATE_TO_CHANNEL,
    @WamEnumConstant(6) DIRECTORY_RETURN_FROM_CHANNEL,
    @WamEnumConstant(7) DIRECTORY_FOLLOW_TAP,
    @WamEnumConstant(8) DIRECTORY_UNFOLLOW_TAP,
    @WamEnumConstant(9) DIRECTORY_SORT_BY_RECENTLY_ADDED,
    @WamEnumConstant(10) DIRECTORY_SORT_BY_POPULARITY,
    @WamEnumConstant(11) DIRECTORY_SORT_BY_ALPHABETICALLY,
    @WamEnumConstant(12) DIRECTORY_IMP,
    @WamEnumConstant(13) DIRECTORY_SEARCH_IMP,
    @WamEnumConstant(14) DIRECTORY_PILL_SELECTION,
    @WamEnumConstant(15) DIRECTORY_COUNTRY_SELECTION,
    @WamEnumConstant(16) DIRECTORY_CATEGORY_SEE_ALL,
    @WamEnumConstant(17) SEARCH,
    @WamEnumConstant(18) SERP_LOADED
}
