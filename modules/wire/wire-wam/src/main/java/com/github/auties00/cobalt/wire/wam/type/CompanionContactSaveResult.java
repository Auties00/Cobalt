package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCompanionContactSaveResult")
@WamEnum
public enum CompanionContactSaveResult {
    @WamEnumConstant(0) SUCCESS,
    @WamEnumConstant(1) NETWORK_UNAVAILABLE,
    @WamEnumConstant(2) USYNC_UNAVAILABLE,
    @WamEnumConstant(3) CLIENT_ERROR,
    @WamEnumConstant(4) PHONE_NUMBER_USERNAME_NOT_MATCH,
    @WamEnumConstant(5) INVALID_USERNAME,
    @WamEnumConstant(6) PIN_REQUIRED,
    @WamEnumConstant(7) WRONG_PIN
}
