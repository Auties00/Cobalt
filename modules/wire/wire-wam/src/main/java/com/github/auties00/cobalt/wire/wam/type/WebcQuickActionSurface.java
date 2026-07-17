package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcQuickActionSurface")
@WamEnum
public enum WebcQuickActionSurface {
    @WamEnumConstant(1) STATUS,
    @WamEnumConstant(2) CHANNELS,
    @WamEnumConstant(3) COMMUNITY_NAVIGATION,
    @WamEnumConstant(4) CHATS,
    @WamEnumConstant(5) SETTINGS_ME
}
