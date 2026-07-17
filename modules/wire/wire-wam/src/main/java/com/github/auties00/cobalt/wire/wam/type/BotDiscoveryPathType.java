package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBotDiscoveryPathType")
@WamEnum
public enum BotDiscoveryPathType {
    @WamEnumConstant(1) CURATED_DISPLAY,
    @WamEnumConstant(2) SEARCH,
    @WamEnumConstant(3) VIEW_ALL,
    @WamEnumConstant(4) DEEPLINK_USER_SHARED,
    @WamEnumConstant(5) INFINITE_SCROLL
}
