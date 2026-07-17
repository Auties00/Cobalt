package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumNotificationActionType")
@WamEnum
public enum NotificationActionType {
    @WamEnumConstant(1) SHOW,
    @WamEnumConstant(2) REMOVE
}
