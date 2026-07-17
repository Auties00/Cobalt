package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUpdateTabSearchEventType")
@WamEnum
public enum UpdateTabSearchEventType {
    @WamEnumConstant(1) SEARCH_TAP,
    @WamEnumConstant(5) SEARCH,
    @WamEnumConstant(6) SERP_LOADED,
    @WamEnumConstant(7) SEARCH_RESULT_SCROLL,
    @WamEnumConstant(8) SEARCH_SUBMIT,
    @WamEnumConstant(9) ITEM_TAP,
    @WamEnumConstant(10) ITEM_REMOVE,
    @WamEnumConstant(11) FOLLOW,
    @WamEnumConstant(12) UNFOLLOW
}
