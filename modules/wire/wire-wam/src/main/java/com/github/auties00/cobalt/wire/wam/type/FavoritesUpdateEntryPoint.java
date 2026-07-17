package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumFavoritesUpdateEntryPoint")
@WamEnum
public enum FavoritesUpdateEntryPoint {
    @WamEnumConstant(1) CONTACT_INFO,
    @WamEnumConstant(2) GROUP_INFO,
    @WamEnumConstant(3) CHAT_MORE_OPTIONS,
    @WamEnumConstant(4) CHAT_LONG_PRESS_OPTIONS,
    @WamEnumConstant(5) MESSAGE_FAVORITES_CONTACT_PICKER,
    @WamEnumConstant(6) FAVORITE_SETTINGS,
    @WamEnumConstant(7) FILTER_CONTEXT_MENU,
    @WamEnumConstant(8) FILTER_EMPTY_STATE_ACTION,
    @WamEnumConstant(9) FILTER_MANAGE_FAVORITE_ACTION,
    @WamEnumConstant(10) CALLING_TAB_FAVORITE_PICKER,
    @WamEnumConstant(11) CALLING_TAB_FAVORITE_EDIT,
    @WamEnumConstant(12) CHAT_CONTEXT_MENU,
    @WamEnumConstant(13) CHAT_HEADER_CONTEXT_MENU,
    @WamEnumConstant(14) CALL_LOG_LONG_PRESS_OPTIONS,
    @WamEnumConstant(15) CALL_LOG_MORE_OPTIONS
}
