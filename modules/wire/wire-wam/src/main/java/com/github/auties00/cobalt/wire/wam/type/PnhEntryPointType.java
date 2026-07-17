package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPnhEntryPointType")
@WamEnum
public enum PnhEntryPointType {
    @WamEnumConstant(1) CHAT_CREATION,
    @WamEnumConstant(2) CHAT_INFO_REQUEST,
    @WamEnumConstant(3) AUDIO,
    @WamEnumConstant(4) VIDEO,
    @WamEnumConstant(5) PN_REQUEST,
    @WamEnumConstant(6) SYSTEM_MESSAGE,
    @WamEnumConstant(7) CHAT_INFO_PN_VISIBILITY
}
