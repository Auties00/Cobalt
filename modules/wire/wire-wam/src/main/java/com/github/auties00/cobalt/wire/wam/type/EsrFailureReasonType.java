package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEsrFailureReasonType")
@WamEnum
public enum EsrFailureReasonType {
    @WamEnumConstant(1) ESR_ABPROP_OFF,
    @WamEnumConstant(2) DM_RELIABILITY_ABPROP_OFF,
    @WamEnumConstant(3) INVALID_MESSAGE_TYPE,
    @WamEnumConstant(4) OLDER_EPHEMERAL_SETTING_TIMESTAMP,
    @WamEnumConstant(5) NO_EPHEMERAL_INFO,
    @WamEnumConstant(6) ATTEMPTS_EXHAUSTED,
    @WamEnumConstant(7) NO_USER_INFO,
    @WamEnumConstant(8) NO_CHAT_SESSION,
    @WamEnumConstant(9) INVALID_EPHEMERAL_DURATION
}
