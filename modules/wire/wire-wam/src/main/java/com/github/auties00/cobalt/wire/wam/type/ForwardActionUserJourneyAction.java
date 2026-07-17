package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumForwardActionUserJourneyAction")
@WamEnum
public enum ForwardActionUserJourneyAction {
    @WamEnumConstant(1) CONTEXT_MENU_SHOWN_WITH_FORWARD,
    @WamEnumConstant(2) CONTEXT_MENU_SHOWN_WITHOUT_FORWARD,
    @WamEnumConstant(3) FORWARD_TAPPED_IN_CONTEXT_MENU,
    @WamEnumConstant(4) FAST_FORWARD_BUTTON_TAPPED,
    @WamEnumConstant(5) MESSAGE_FORWARD_SELECT,
    @WamEnumConstant(6) MESSAGE_FORWARD_UNSELECT,
    @WamEnumConstant(7) SHARE_BUTTON_TAPPED_IN_FORWARD_TOOLBAR,
    @WamEnumConstant(8) CANCEL_IN_FORWARD_TOOLBAR,
    @WamEnumConstant(9) CONTEXT_MENU_DISMISSED,
    @WamEnumConstant(10) FORWARD_BUTTON_TAPPED_IN_FORWARD_TOOLBAR,
    @WamEnumConstant(11) FORWARD_TOOLBAR_DISMISSED,
    @WamEnumConstant(12) SELECT_TAPPED_IN_CONTEXT_MENU,
    @WamEnumConstant(13) SELECT_TAPPED_IN_MORE_MENU
}
