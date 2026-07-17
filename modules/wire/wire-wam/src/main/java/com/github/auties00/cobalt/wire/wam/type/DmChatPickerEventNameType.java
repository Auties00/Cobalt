package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDmChatPickerEventNameType")
@WamEnum
public enum DmChatPickerEventNameType {
    @WamEnumConstant(0) CHAT_PICKER_LINK_IMPRESSION,
    @WamEnumConstant(1) CHAT_PICKER_TRAY_OPEN,
    @WamEnumConstant(2) CHAT_PICKER_TRAY_EXIT,
    @WamEnumConstant(3) CHAT_PICKER_CHATS_SELECTED
}
