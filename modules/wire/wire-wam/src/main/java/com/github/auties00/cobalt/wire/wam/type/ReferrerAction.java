package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReferrerAction")
@WamEnum
public enum ReferrerAction {
    @WamEnumConstant(1) NULLSTATE_ASK_META_AI,
    @WamEnumConstant(2) TYPEAHEAD_ASK_META_AI,
    @WamEnumConstant(3) NULLSTATE_PAPER_PLANE,
    @WamEnumConstant(4) TYPEAHEAD_PAPER_PLANE,
    @WamEnumConstant(5) TYPEAHEAD_SEND,
    @WamEnumConstant(6) FAVICON,
    @WamEnumConstant(7) THREAD_OPEN,
    @WamEnumConstant(8) MY_STATUS_OVERLFOW_MENU_OPTION,
    @WamEnumConstant(9) AI_WIDGET,
    @WamEnumConstant(10) FAB_MM_TAP,
    @WamEnumConstant(11) NO_RESULT_STATE_ASK_META_AI,
    @WamEnumConstant(12) META_AI_THREAD_LIST
}
