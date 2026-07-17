package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageActionEntryPoint")
@WamEnum
public enum MessageActionEntryPoint {
    @WamEnumConstant(0) CHATLIST,
    @WamEnumConstant(1) URL_CLICK_BANNER
}
