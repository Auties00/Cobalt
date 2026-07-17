package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcMenuType")
@WamEnum
public enum WebcMenuType {
    @WamEnumConstant(1) THREADS_SCREEN_CLICK,
    @WamEnumConstant(2) CHAT_SCREEN_CLICK,
    @WamEnumConstant(3) SETTINGS_SCREEN_CLICK
}
