package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcNativeUpsellCtaEventType")
@WamEnum
public enum WebcNativeUpsellCtaEventType {
    @WamEnumConstant(1) IMPRESSION,
    @WamEnumConstant(2) CTA_BTN_CLICK,
    @WamEnumConstant(3) CTA_DISMISS
}
