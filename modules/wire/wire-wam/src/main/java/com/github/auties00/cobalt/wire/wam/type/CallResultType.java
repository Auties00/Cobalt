package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallResultType")
@WamEnum
public enum CallResultType {
    @WamEnumConstant(0) INVALID,
    @WamEnumConstant(1) CONNECTED,
    @WamEnumConstant(2) REJECTED_BY_USER,
    @WamEnumConstant(3) REJECTED_BY_SERVER,
    @WamEnumConstant(4) MISSED,
    @WamEnumConstant(5) BUSY,
    @WamEnumConstant(6) SETUP_ERROR,
    @WamEnumConstant(7) SERVER_NACK,
    @WamEnumConstant(8) CALL_OFFER_ACK_NOT_RECEIVED,
    @WamEnumConstant(9) MISSED_NO_RECEIPT,
    @WamEnumConstant(10) ACCEPTED_BUT_NOT_CONNECTED,
    @WamEnumConstant(11) CALL_CANCELED_CELLULAR_IN_PROGRESS,
    @WamEnumConstant(12) CALL_CANCELED_AIRPLANE_MODE_ON,
    @WamEnumConstant(13) CALL_CANCELED_NO_NETWORK,
    @WamEnumConstant(14) CALL_OFFER_ACK_CORRUPT,
    @WamEnumConstant(15) CALL_REJECTED_TOS,
    @WamEnumConstant(16) CALL_REJECTED_E2E,
    @WamEnumConstant(17) CALL_REJECTED_UNAVAILABLE,
    @WamEnumConstant(18) CALL_CANCELED_OFFER_NOT_SENT,
    @WamEnumConstant(19) PEER_SETUP_ERROR,
    @WamEnumConstant(20) ACTIVE_ELSEWHERE,
    @WamEnumConstant(21) NO_DECRYPTED_OFFER,
    @WamEnumConstant(22) ACCEPTED_ELSEWHERE,
    @WamEnumConstant(23) REJECTED_ELSEWHERE,
    @WamEnumConstant(24) LONELY,
    @WamEnumConstant(25) CALL_IS_FULL,
    @WamEnumConstant(26) SILENCED,
    @WamEnumConstant(27) CALL_MISSED_SILENCED,
    @WamEnumConstant(28) CALL_DOES_NOT_EXIST_FOR_REJOIN
}
