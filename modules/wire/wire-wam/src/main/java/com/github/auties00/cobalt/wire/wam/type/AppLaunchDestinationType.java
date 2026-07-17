package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAppLaunchDestinationType")
@WamEnum
public enum AppLaunchDestinationType {
    @WamEnumConstant(1) CHATLIST,
    @WamEnumConstant(2) CHAT,
    @WamEnumConstant(3) SHARE,
    @WamEnumConstant(4) CALL,
    @WamEnumConstant(5) CHANNEL
}
