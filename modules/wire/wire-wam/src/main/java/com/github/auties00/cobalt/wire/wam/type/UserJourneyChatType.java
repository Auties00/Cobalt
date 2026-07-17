package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUserJourneyChatType")
@WamEnum
public enum UserJourneyChatType {
    @WamEnumConstant(1) INDIVIDUAL,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) BROADCAST,
    @WamEnumConstant(4) STATUS,
    @WamEnumConstant(5) CHANNEL,
    @WamEnumConstant(6) INTEROP,
    @WamEnumConstant(7) MULTIPLE,
    @WamEnumConstant(8) FLOWS,
    @WamEnumConstant(9) CATALOG,
    @WamEnumConstant(10) META_AI
}
