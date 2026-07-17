package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReplyExitMethod")
@WamEnum
public enum ReplyExitMethod {
    @WamEnumConstant(1) BACK_BUTTON,
    @WamEnumConstant(2) SWIPE_DOWN,
    @WamEnumConstant(3) TAP_SCREEN,
    @WamEnumConstant(4) OTHER
}
