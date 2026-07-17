package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignalOrigin")
@WamEnum
public enum SignalOrigin {
    @WamEnumConstant(0) CTA_URL_CLICK,
    @WamEnumConstant(1) BODY_URL_CLICK,
    @WamEnumConstant(2) BODY_URL_LONG_PRESS,
    @WamEnumConstant(3) CTA_APP_CLICK
}
