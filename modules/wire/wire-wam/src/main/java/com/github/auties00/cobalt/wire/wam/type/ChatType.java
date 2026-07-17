package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatType")
@WamEnum
public enum ChatType {
    @WamEnumConstant(1) INDIVIDUAL,
    @WamEnumConstant(2) SMB,
    @WamEnumConstant(3) ENT,
    @WamEnumConstant(4) INTEROP,
    @WamEnumConstant(5) UNKNOWN,
    @WamEnumConstant(6) BUSINESS
}
