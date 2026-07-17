package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignCredentialResult")
@WamEnum
public enum SignCredentialResult {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) ERROR_BAD_REQUEST,
    @WamEnumConstant(3) ERROR_SERVER,
    @WamEnumConstant(4) ERROR_OTHER,
    @WamEnumConstant(5) ERROR_CLIENT_NETWORK
}
