package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumInAppNotificationAlertStyle")
@WamEnum
public enum InAppNotificationAlertStyle {
    @WamEnumConstant(1) NONE,
    @WamEnumConstant(2) BANNERS,
    @WamEnumConstant(3) ALERTS
}
