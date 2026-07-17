package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdCompanionDeviceAccountType")
@WamEnum
public enum MdCompanionDeviceAccountType {
    @WamEnumConstant(1) E2EE_DEVICE,
    @WamEnumConstant(2) HOSTED_DEVICE
}
