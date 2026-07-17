package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUpdatesTabSearchResultType")
@WamEnum
public enum UpdatesTabSearchResultType {
    @WamEnumConstant(0) STATUS,
    @WamEnumConstant(1) FOLLOWED_CHANNELS,
    @WamEnumConstant(2) OTHER_CHANNELS,
    @WamEnumConstant(3) SEARCH_QUERY,
    @WamEnumConstant(4) CONTACT
}
