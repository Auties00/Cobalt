package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbListSurfaceType")
@WamEnum
public enum SmbListSurfaceType {
    @WamEnumConstant(1) BIZ_TOOLS,
    @WamEnumConstant(2) MANAGE_LISTS,
    @WamEnumConstant(3) NEW_LIST,
    @WamEnumConstant(4) INBOX,
    @WamEnumConstant(5) INBOX_CHAT_ROW_MORE,
    @WamEnumConstant(6) INBOX_CHAT_BOTTOM_SHEET,
    @WamEnumConstant(7) INBOX_CHAT_BOTTOM_SHEET_ADD_TO_LIST,
    @WamEnumConstant(8) CHOOSE_LIST_SHEET
}
