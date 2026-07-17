package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuickReplyAction")
@WamEnum
public enum QuickReplyAction {
    @WamEnumConstant(1) ACTION_SETTINGS_IMPRESSION,
    @WamEnumConstant(2) ACTION_SETTINGS_ADD_CLICK,
    @WamEnumConstant(3) ACTION_SETTINGS_ADD_ABANDONED,
    @WamEnumConstant(4) ACTION_SETTINGS_ADDED,
    @WamEnumConstant(5) ACTION_SETTINGS_DELETED,
    @WamEnumConstant(6) ACTION_CHAT_IMPRESSION,
    @WamEnumConstant(7) ACTION_CHAT_CLICK,
    @WamEnumConstant(8) ACTION_SETTINGS_EDITED,
    @WamEnumConstant(9) ACTION_CHAT_INVALID_ATTACHMENTS,
    @WamEnumConstant(10) ACTION_SETTINGS_INVALID_ATTACHMENTS,
    @WamEnumConstant(11) ACTION_SETTINGS_MEDIA_TRANSCODE,
    @WamEnumConstant(12) ACTION_CHAT_CLICK_CANCEL,
    @WamEnumConstant(13) ACTION_SMART_DEFAULT_CLICK,
    @WamEnumConstant(14) QUICK_REPLY_MESSAGE_SENT
}
