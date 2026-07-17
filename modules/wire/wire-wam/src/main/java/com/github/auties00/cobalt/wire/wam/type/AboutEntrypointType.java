package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAboutEntrypointType")
@WamEnum
public enum AboutEntrypointType {
    @WamEnumConstant(1) SETTINGS,
    @WamEnumConstant(2) PROFILE,
    @WamEnumConstant(3) ME_TAB,
    @WamEnumConstant(4) DEEP_LINK,
    @WamEnumConstant(5) ONE_ON_ONE_CHAT
}
