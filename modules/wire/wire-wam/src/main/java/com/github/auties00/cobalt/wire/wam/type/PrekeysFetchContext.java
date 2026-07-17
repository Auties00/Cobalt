package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPrekeysFetchContext")
@WamEnum
public enum PrekeysFetchContext {
    @WamEnumConstant(1) SEND_MESSAGE,
    @WamEnumConstant(2) GET_VNAME_CERTIFICATE,
    @WamEnumConstant(3) SEND_LIVE_LOCATION_RETRY,
    @WamEnumConstant(4) SEND_LIVE_LOCATION_KEY,
    @WamEnumConstant(5) SEND_PEER_MESSAGE,
    @WamEnumConstant(6) MULTI_DEVICE_CALL,
    @WamEnumConstant(7) CALL_PEER_E2E_FAIL,
    @WamEnumConstant(8) IDENTITY_CHANGE_NOTIFICATION,
    @WamEnumConstant(9) BACK_OFF,
    @WamEnumConstant(10) USER_INTENT_PREFETCH,
    @WamEnumConstant(11) RESEND_MESSAGE,
    @WamEnumConstant(12) RETRY_MESSAGE,
    @WamEnumConstant(13) USER_INTENT_STATUS_PREFETCH,
    @WamEnumConstant(14) SEND_SENDERKEY,
    @WamEnumConstant(15) AEA_SEND_TIME_RECONCILATION,
    @WamEnumConstant(16) AEA_CONSUMER_BACKFILL,
    @WamEnumConstant(17) AEA_GOSSIP_MISMATCH
}
