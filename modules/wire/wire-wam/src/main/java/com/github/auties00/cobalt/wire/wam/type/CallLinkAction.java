package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallLinkAction")
@WamEnum
public enum CallLinkAction {
    @WamEnumConstant(1) COPY,
    @WamEnumConstant(2) SHARE,
    @WamEnumConstant(3) SHARE_VIA_WHATSAPP,
    @WamEnumConstant(4) ADD_TO_CALENDAR,
    @WamEnumConstant(5) CREATE_CALL_LINK,
    @WamEnumConstant(6) EDIT_CALL_LINK,
    @WamEnumConstant(7) DELETE_CALL_LINK,
    @WamEnumConstant(8) SHARE_CALL_LINK_CHAT,
    @WamEnumConstant(9) SHARE_CALL_LINK_CALENDAR,
    @WamEnumConstant(10) SHARE_CALL_LINK_COPY,
    @WamEnumConstant(11) SHARE_CALL_LINK_SHARE_SHEET,
    @WamEnumConstant(12) TOGGLE_WAITING_ROOM
}
