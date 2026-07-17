package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcButterbarBbType")
@WamEnum
public enum WebcButterbarBbType {
    @WamEnumConstant(1) OFFLINE,
    @WamEnumConstant(2) RESUME_CONNECTING,
    @WamEnumConstant(3) RESUME_LOADING_MSGS_PROGRESS,
    @WamEnumConstant(4) UPDATE_DUE_TO_SOFT_MIN,
    @WamEnumConstant(5) UWP_UPSELL,
    @WamEnumConstant(6) NOTIFICATION,
    @WamEnumConstant(7) OFFLINE_NOTIFICATION,
    @WamEnumConstant(8) NOTIFICATION_DISABLED,
    @WamEnumConstant(9) NOTIFICATION_AWARENESS
}
