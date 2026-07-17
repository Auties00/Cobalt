package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumNotificationSettingType")
@WamEnum
public enum NotificationSettingType {
    @WamEnumConstant(1) ALLOWED,
    @WamEnumConstant(2) BLOCKED,
    @WamEnumConstant(3) UNKNOWN
}
