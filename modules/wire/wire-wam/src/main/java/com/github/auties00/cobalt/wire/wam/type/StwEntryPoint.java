package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStwEntryPoint")
@WamEnum
public enum StwEntryPoint {
    @WamEnumConstant(0) HIGHLY_FORWARDED_MESSAGE,
    @WamEnumConstant(1) URL_LONG_PRESS,
    @WamEnumConstant(2) MEDIA_VIEWER,
    @WamEnumConstant(3) CONTEXT_MENU
}
