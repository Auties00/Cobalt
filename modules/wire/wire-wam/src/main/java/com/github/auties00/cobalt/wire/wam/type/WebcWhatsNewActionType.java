package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcWhatsNewActionType")
@WamEnum
public enum WebcWhatsNewActionType {
    @WamEnumConstant(1) IMPRESSION,
    @WamEnumConstant(2) DISMISS_BUTTON,
    @WamEnumConstant(3) DISMISS_OVERLAY,
    @WamEnumConstant(4) BANNER_CLICK,
    @WamEnumConstant(5) BANNER_DISMISS
}
