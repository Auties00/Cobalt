package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchUserJourneyAction")
@WamEnum
public enum SearchUserJourneyAction {
    @WamEnumConstant(1) SEARCH_CTA_CLICKED,
    @WamEnumConstant(2) NULL_STATE_SHOW,
    @WamEnumConstant(3) NULL_STATE_ITEM_CLICK,
    @WamEnumConstant(4) SEARCH_START,
    @WamEnumConstant(5) TYPEAHEAD_SHOW,
    @WamEnumConstant(6) TYPEAHEAD_ITEM_CLICK,
    @WamEnumConstant(7) RESULT_PAGE_SHOW,
    @WamEnumConstant(8) RESULT_ITEM_CLICK,
    @WamEnumConstant(9) VIEW_MORE,
    @WamEnumConstant(10) DISMISS,
    @WamEnumConstant(11) PAPER_PLANE_AI_BUTTON_CLICK,
    @WamEnumConstant(12) AI_VOICE_INPUT_BUTTON_CLICKED,
    @WamEnumConstant(13) INCLUDED_FUZZY_SEARCH,
    @WamEnumConstant(14) NO_RESULT_STATE_ASK_META_AI_CLICK,
    @WamEnumConstant(15) ASK_META_AI_BUTTON_CLICK,
    @WamEnumConstant(16) CHAT_BACK_TO_SEARCH_CLICK,
    @WamEnumConstant(17) KEYBOARD_RETURN_BUTTON_CLICK,
    @WamEnumConstant(18) RECENT_SEARCHES_CLEAR_ALL_CLICK,
    @WamEnumConstant(19) INVITE_RESULT_CLICK,
    @WamEnumConstant(20) SEND_INVITE_CLICK,
    @WamEnumConstant(21) DOWNSTREAM_SEND,
    @WamEnumConstant(22) DOWNSTREAM_EXIT,
    @WamEnumConstant(23) DOWNSTREAM_DWELL,
    @WamEnumConstant(24) DOWNSTREAM_CALL
}
