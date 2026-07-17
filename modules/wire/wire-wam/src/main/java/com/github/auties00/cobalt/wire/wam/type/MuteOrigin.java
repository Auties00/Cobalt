package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMuteOrigin")
@WamEnum
public enum MuteOrigin {
    @WamEnumConstant(1) STATUS_LIST,
    @WamEnumConstant(2) STATUS_VIEWER,
    @WamEnumConstant(3) CHATS_TAB_STATUS_TRAY
}
