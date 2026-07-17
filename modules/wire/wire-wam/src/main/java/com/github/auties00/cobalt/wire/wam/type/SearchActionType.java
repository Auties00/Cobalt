package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchActionType")
@WamEnum
public enum SearchActionType {
    @WamEnumConstant(1) NULL_STATE_SHOW,
    @WamEnumConstant(2) NULL_STATE_ITEM_CLICK,
    @WamEnumConstant(3) TYPEAHEAD_SHOW,
    @WamEnumConstant(4) TYPEAHEAD_ITEM_CLICK,
    @WamEnumConstant(5) RESULT_PAGE_SHOW,
    @WamEnumConstant(6) RESULT_ITEM_CLICK,
    @WamEnumConstant(7) SEARCH_BUTTON_CLICK,
    @WamEnumConstant(8) AIRPLANE_BUTTON_CLICK,
    @WamEnumConstant(9) ASK_META_AI_BUTTON_CLICK,
    @WamEnumConstant(10) NO_RESULT_STATE_ASK_META_AI_CLICK,
    @WamEnumConstant(11) WEARABLES_REQUEST
}
