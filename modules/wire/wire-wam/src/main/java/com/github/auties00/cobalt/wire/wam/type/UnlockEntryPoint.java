package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUnlockEntryPoint")
@WamEnum
public enum UnlockEntryPoint {
    @WamEnumConstant(0) CHAT_LIST,
    @WamEnumConstant(1) NOTIFICATION,
    @WamEnumConstant(2) PRIVATE_REPLY,
    @WamEnumConstant(3) STATUS_REPLY,
    @WamEnumConstant(4) CHAT_INFO,
    @WamEnumConstant(5) CONTACT_PICKER,
    @WamEnumConstant(6) LOCK_CHAT_HELPER,
    @WamEnumConstant(7) SEARCH,
    @WamEnumConstant(8) UNKNOWN,
    @WamEnumConstant(9) DIRECT_MESSAGE,
    @WamEnumConstant(10) MEDIA_VIEWER,
    @WamEnumConstant(11) SIDEBAR
}
