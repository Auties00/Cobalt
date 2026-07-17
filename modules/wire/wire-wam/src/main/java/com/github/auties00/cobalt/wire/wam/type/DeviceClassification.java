package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeviceClassification")
@WamEnum
public enum DeviceClassification {
    @WamEnumConstant(0) MOBILE,
    @WamEnumConstant(1) TABLET,
    @WamEnumConstant(2) WEARABLES,
    @WamEnumConstant(3) VR,
    @WamEnumConstant(4) DESKTOP,
    @WamEnumConstant(5) FOLDABLE,
    @WamEnumConstant(6) AR_GLASS,
    @WamEnumConstant(7) WEARABLES_WHATSAPI,
    @WamEnumConstant(100) UNDEFINED
}
