package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMuteChatType")
@WamEnum
public enum MuteChatType {
    @WamEnumConstant(1) ONE_ON_ONE,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) CHANNEL,
    @WamEnumConstant(4) INTEROP,
    @WamEnumConstant(5) INORGANIC_NOTIFICATION
}
