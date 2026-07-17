package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCanonicalEntRecoveryCompanionEvent")
@WamEnum
public enum CanonicalEntRecoveryCompanionEventType {
    @WamEnumConstant(0) COMPANION_REGISTERED,
    @WamEnumConstant(1) REQUEST_NONCE_FROM_PRIMARY,
    @WamEnumConstant(2) FETCH_COMPANION_NONCE,
    @WamEnumConstant(3) FORWARD_NONCE_PRIMARY_TO_COMPANION,
    @WamEnumConstant(4) RECEIVED_COMPANION_NONCE_FROM_PRIMARY,
    @WamEnumConstant(5) EXCHANGE_NONCE,
    @WamEnumConstant(6) CREDENTIALS_STORED,
    @WamEnumConstant(7) VALIDATE_ACCESS_TOKEN,
    @WamEnumConstant(8) CRED_REQUEST_STARTED,
    @WamEnumConstant(9) CRED_REQUEST_SUCCEEDED_FROM_STORAGE,
    @WamEnumConstant(10) CRED_REQUEST_SUCCEEDED_VIA_RECOVERY,
    @WamEnumConstant(11) CRED_REQUEST_FAILED_TIMEOUT,
    @WamEnumConstant(12) CRED_REQUEST_FAILED_ERROR
}
