package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReadEntryPoint")
@WamEnum
public enum ReadEntryPoint {
    @WamEnumConstant(1) CHAT_LIST,
    @WamEnumConstant(2) CHAT
}
