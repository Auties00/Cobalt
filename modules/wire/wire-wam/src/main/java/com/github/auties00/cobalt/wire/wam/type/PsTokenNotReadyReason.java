package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsTokenNotReadyReason")
@WamEnum
public enum PsTokenNotReadyReason {
    @WamEnumConstant(0) NO_REASON,
    @WamEnumConstant(1) REASON_UNKNOWN,
    @WamEnumConstant(2) REASON_WAIT_FOR_FIRST_TOKEN,
    @WamEnumConstant(3) REASON_INVALID_SHARED_KEY,
    @WamEnumConstant(4) REASON_INVALID_FACTOR,
    @WamEnumConstant(5) REASON_GEN_FACTOR_FAILURE,
    @WamEnumConstant(6) REASON_COMPUTE_HMAC_FAILURE,
    @WamEnumConstant(7) REASON_BLIND_FAILURE,
    @WamEnumConstant(8) REASON_UNBLIND_FAILURE,
    @WamEnumConstant(9) REASON_LAST_SIGNREQ_NETWORK_FAILURE,
    @WamEnumConstant(10) REASON_LAST_SIGNREQ_SERVER_ERROR,
    @WamEnumConstant(11) REASON_LAST_SIGNREQ_BAD_REQUEST,
    @WamEnumConstant(12) REASON_LAST_SIGNREQ_OTHER_ERROR,
    @WamEnumConstant(13) REASON_WAIT_FOR_GEN_TOKEN,
    @WamEnumConstant(14) REASON_GEN_SHAREDKEY_FAILURE,
    @WamEnumConstant(15) REASON_WAIT_FOR_GEN_FIRST_TOKEN
}
