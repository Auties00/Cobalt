package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcQuickActionEventType")
@WamEnum
public enum WebcQuickActionEventType {
    @WamEnumConstant(1) SURFACE_VIEW,
    @WamEnumConstant(2) IMPRESSION,
    @WamEnumConstant(3) TAP
}
