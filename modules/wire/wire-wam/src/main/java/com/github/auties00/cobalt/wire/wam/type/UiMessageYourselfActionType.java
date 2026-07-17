package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUiMessageYourselfActionType")
@WamEnum
public enum UiMessageYourselfActionType {
    @WamEnumConstant(1) NEW_CHAT_PRESSED,
    @WamEnumConstant(2) YOU_SELECTED,
    @WamEnumConstant(3) SEARCH_BAR_PRESSED,
    @WamEnumConstant(4) SEARCH_FULL_NAME_YOU_SELECTED,
    @WamEnumConstant(5) NEW_NTS_CREATED,
    @WamEnumConstant(6) EXISTING_NTS_OPENED
}
