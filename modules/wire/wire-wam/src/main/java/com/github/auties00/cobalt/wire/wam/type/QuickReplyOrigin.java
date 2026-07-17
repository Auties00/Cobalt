package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuickReplyOrigin")
@WamEnum
public enum QuickReplyOrigin {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) CONVERSATIONS,
    @WamEnumConstant(2) BUTTON,
    @WamEnumConstant(3) KEYBOARD
}
