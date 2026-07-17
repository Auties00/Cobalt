package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignalSurface")
@WamEnum
public enum SignalSurface {
    @WamEnumConstant(0) BIZ_PROFILE_SCREEN,
    @WamEnumConstant(1) CHAT_THREAD,
    @WamEnumConstant(2) CHAT_LIST,
    @WamEnumConstant(3) OTHER
}
