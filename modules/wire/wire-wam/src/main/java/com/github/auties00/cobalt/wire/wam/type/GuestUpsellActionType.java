package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGuestUpsellActionType")
@WamEnum
public enum GuestUpsellActionType {
    @WamEnumConstant(1) VIEW,
    @WamEnumConstant(2) CLICK,
    @WamEnumConstant(3) DISMISS,
    @WamEnumConstant(4) DOWNLOAD_CTA_CLICK
}
