package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageDropReasonType")
@WamEnum
public enum MessageDropReasonType {
    @WamEnumConstant(1) SYNCD_DELETION,
    @WamEnumConstant(2) ADMIN_REVOKE_NOT_ENABLED,
    @WamEnumConstant(3) RECEIVED_WITH_OLD_COUNTER,
    @WamEnumConstant(4) INVALID_STANZA,
    @WamEnumConstant(5) INVALID_PROTOBUF,
    @WamEnumConstant(6) MESSAGE_SECRET_ERROR,
    @WamEnumConstant(7) INVALID_LID_ADDRESSED_MESSAGE,
    @WamEnumConstant(8) UNKNOWN_MESSAGE_TYPE,
    @WamEnumConstant(9) DB_OPERATION_FAILED,
    @WamEnumConstant(10) INTERNAL_ERROR,
    @WamEnumConstant(11) EXPIRED,
    @WamEnumConstant(12) INVALID_HOSTED_COMPANION_STANZA,
    @WamEnumConstant(13) MESSAGE_REVOKED,
    @WamEnumConstant(14) PAYMENT_MESSAGE_REVOKED,
    @WamEnumConstant(15) DUPLICATE_MESSAGE,
    @WamEnumConstant(16) DUPLICATE_DELIVERY,
    @WamEnumConstant(17) INVALID_MESSAGE_REFERENCE,
    @WamEnumConstant(18) UNSUPPORTED_MESSAGE,
    @WamEnumConstant(19) MALICIOUS_DUPLICATE_MESSAGE,
    @WamEnumConstant(20) PEER_MESSAGE_FROM_OTHER_USER,
    @WamEnumConstant(21) INVALID_PEER_MESSAGE,
    @WamEnumConstant(22) INVALID_REPORTING_TOKEN,
    @WamEnumConstant(23) MISSING_REPORTING_TOKEN,
    @WamEnumConstant(24) APPDATA_MISMATCH,
    @WamEnumConstant(25) COEX_V2_RECV_UNSUPPORTED,
    @WamEnumConstant(26) COEX_V2_INVALID_SENDER
}
