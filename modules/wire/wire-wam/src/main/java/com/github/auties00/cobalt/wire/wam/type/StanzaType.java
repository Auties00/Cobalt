package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStanzaType")
@WamEnum
public enum StanzaType {
    @WamEnumConstant(1) MESSAGE,
    @WamEnumConstant(2) RECEIPT,
    @WamEnumConstant(3) CALL,
    @WamEnumConstant(4) NOTIFICATION,
    @WamEnumConstant(5) APPDATA,
    @WamEnumConstant(6) STATUS
}
