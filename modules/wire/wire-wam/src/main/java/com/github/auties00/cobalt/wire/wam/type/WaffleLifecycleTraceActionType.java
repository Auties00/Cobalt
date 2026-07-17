package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaffleLifecycleTraceActionType")
@WamEnum
public enum WaffleLifecycleTraceActionType {
    @WamEnumConstant(0) NOT_APPLICABLE,
    @WamEnumConstant(1) SYNCD_BROADCAST,
    @WamEnumConstant(2) SYNCD_RECEIVED,
    @WamEnumConstant(3) SYNCD_RECEIVED_NO_EXISTING_ROW,
    @WamEnumConstant(4) SYNCD_RECEIVED_ALREADY_ACTIVE,
    @WamEnumConstant(5) SYNCD_RECEIVED_STATE_TRANSITION,
    @WamEnumConstant(6) NONCE_FETCH_INITIATED,
    @WamEnumConstant(7) NONCE_FETCH_DEDUPLICATED,
    @WamEnumConstant(8) REFRESH_TOKEN_INITIATED,
    @WamEnumConstant(9) REFRESH_TOKEN_DEDUPLICATED,
    @WamEnumConstant(10) REFRESH_TOKEN_SUCCESS,
    @WamEnumConstant(11) REFRESH_TOKEN_ERROR,
    @WamEnumConstant(12) PURGE,
    @WamEnumConstant(13) PING
}
