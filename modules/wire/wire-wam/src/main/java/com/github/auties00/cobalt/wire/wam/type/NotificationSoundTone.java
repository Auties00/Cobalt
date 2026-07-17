package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumNotificationSoundTone")
@WamEnum
public enum NotificationSoundTone {
    @WamEnumConstant(1) DEFAULT,
    @WamEnumConstant(2) CUSTOM
}
