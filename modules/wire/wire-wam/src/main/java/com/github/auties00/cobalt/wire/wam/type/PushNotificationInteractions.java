package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPushNotificationInteractions")
@WamEnum
public enum PushNotificationInteractions {
    @WamEnumConstant(1) SHOWN,
    @WamEnumConstant(2) CLICKED
}
