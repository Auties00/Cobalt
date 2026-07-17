package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumExpiryReason")
@WamEnum
public enum ExpiryReason {
    @WamEnumConstant(1) USER_LEAVE,
    @WamEnumConstant(2) DEVICE_UNPAIR,
    @WamEnumConstant(3) IDENTITY_CHANGE,
    @WamEnumConstant(4) AUDIENCE_CHANGE,
    @WamEnumConstant(5) PERIODIC_ROTATION,
    @WamEnumConstant(6) KEY_CORRUPTION,
    @WamEnumConstant(7) PEER_COMPANION_UNPAIR,
    @WamEnumConstant(8) OTHER_DEVICE_UNPAIR
}
