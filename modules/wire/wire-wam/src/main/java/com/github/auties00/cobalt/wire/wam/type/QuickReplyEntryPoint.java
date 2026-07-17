package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuickReplyEntryPoint")
@WamEnum
public enum QuickReplyEntryPoint {
    @WamEnumConstant(1) QUICK_REPLY_ENTRY_POINT_KEYBOARD,
    @WamEnumConstant(2) QUICK_REPLY_ENTRY_POINT_ATTACHMENT_PANEL,
    @WamEnumConstant(3) QUICK_REPLY_ENTRY_POINT_SETTINGS_MENU,
    @WamEnumConstant(4) QUICK_REPLY_ENTRY_POINT_BANNERS,
    @WamEnumConstant(5) QUICK_REPLY_ENTRY_POINT_NUX,
    @WamEnumConstant(6) QUICK_REPLY_ENTRY_POINT_ACTION_BAR,
    @WamEnumConstant(7) QUICK_REPLY_ENTRY_POINT_DEEPLINK,
    @WamEnumConstant(8) QUICK_REPLY_ENTRY_POINT_MESSAGE_BUBBLE
}
