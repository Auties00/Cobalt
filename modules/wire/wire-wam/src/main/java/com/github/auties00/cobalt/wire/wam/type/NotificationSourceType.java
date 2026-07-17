package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumNotificationSourceType")
@WamEnum
public enum NotificationSourceType {
    @WamEnumConstant(1) PUSH_TRIGGERED,
    @WamEnumConstant(2) MAIN_APP,
    @WamEnumConstant(3) IN_APP,
    @WamEnumConstant(4) VOIP_PUSH_TRIGGERED
}
