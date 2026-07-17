package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdAppStateKeyRotationReasonCode")
@WamEnum
public enum MdAppStateKeyRotationReasonCode {
    @WamEnumConstant(1) APP_STATE_SYNC_KEY_EXPIRY,
    @WamEnumConstant(2) DEVICE_DEREGISTERATION,
    @WamEnumConstant(3) NO_KEYS
}
