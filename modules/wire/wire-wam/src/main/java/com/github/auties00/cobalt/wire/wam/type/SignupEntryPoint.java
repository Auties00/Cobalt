package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignupEntryPoint")
@WamEnum
public enum SignupEntryPoint {
    @WamEnumConstant(0) CHAT_THREAD_BUSINESS,
    @WamEnumConstant(1) CHAT_THREAD_OTHER,
    @WamEnumConstant(2) CHANNELS,
    @WamEnumConstant(3) STATUS,
    @WamEnumConstant(4) FACEBOOK,
    @WamEnumConstant(5) INSTAGRAM,
    @WamEnumConstant(6) EXTERNAL,
    @WamEnumConstant(7) MESSENGER,
    @WamEnumConstant(8) THREADS
}
