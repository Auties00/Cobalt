package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallLinkMedia")
@WamEnum
public enum CallLinkMedia {
    @WamEnumConstant(1) VOICE,
    @WamEnumConstant(2) VIDEO
}
