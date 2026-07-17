package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumListUpdateUserJourneyAction")
@WamEnum
public enum ListUpdateUserJourneyAction {
    @WamEnumConstant(0) START,
    @WamEnumConstant(1) SELECT_PREDEFINED,
    @WamEnumConstant(2) CREATE_LIST,
    @WamEnumConstant(3) MORE_BOTTOM_SHEET_OPEN,
    @WamEnumConstant(4) MORE_BUTTON_TAP,
    @WamEnumConstant(5) LIST_ITEM_TAP,
    @WamEnumConstant(6) CREATE_LIST_TAP,
    @WamEnumConstant(7) EDIT_TAP
}
